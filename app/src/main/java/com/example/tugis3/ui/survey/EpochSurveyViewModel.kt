package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.coord.transform.NoOpTransformer
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.gnss.GnssEngine
import com.example.tugis3.gnss.model.GnssObservation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class EpochSurveyViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val surveyRepo: SurveyPointRepository,
    private val gnss: GnssEngine
) : ViewModel() {

    data class Config(
        val pointName: String = "EPOCH_001",
        val pointCode: String? = null,
        val targetEpochCount: Int = 10,
        val minFixQual: Int = 2,        // accuracyLevel min (DGPS/RTK)
        val maxHrms: Double? = null,    // opsiyonel filtre
        val maxPdop: Double? = 6.0
    )

    data class EpochSample(
        val index: Int,
        val epochMillis: Long,
        val lat: Double,
        val lon: Double,
        val ellH: Double?,
        val hrms: Double?,
        val vrms: Double?,
        val pdop: Double?,
        val sat: Int?,
        val fix: String
    )

    data class State(
        val config: Config = Config(),
        val observation: GnssObservation? = null,
        val canStart: Boolean = false,
        val isRecording: Boolean = false,
        val isCompleted: Boolean = false,
        val currentEpoch: Int = 0,
        val samples: List<EpochSample> = emptyList(),
        val avgLat: Double? = null,
        val avgLon: Double? = null,
        val avgEllH: Double? = null,
        val horizontalRms: Double? = null,
        val saved: Boolean = false,
        val status: String = "Hazır"
    )

    private val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val obsFlow = gnss.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var loopJob: Job? = null

    init {
        viewModelScope.launch {
            obsFlow.collect { obs -> updateObservation(obs) }
        }
    }

    private fun updateObservation(obs: GnssObservation?) {
        val cfg = _state.value.config
        val canStart = obs != null && obs.fixType.accuracyLevel >= cfg.minFixQual &&
            (cfg.maxPdop == null || (obs.pdop?.let { it <= cfg.maxPdop } ?: true)) &&
            (cfg.maxHrms == null || (obs.hrms?.let { it <= cfg.maxHrms } ?: true))
        val status = when {
            obs == null -> "GNSS bekleniyor"
            obs.fixType.accuracyLevel < cfg.minFixQual -> "Fix yetersiz"
            cfg.maxPdop != null && (obs.pdop ?: 99.0) > cfg.maxPdop -> "PDOP yüksek"
            else -> if (!(_state.value.isRecording || _state.value.isCompleted)) "Hazır" else _state.value.status
        }
        _state.update { it.copy(observation = obs, canStart = canStart && !it.isRecording && !it.isCompleted, status = status) }
    }

    // --- Public API ---
    fun setPointName(n: String) = _state.update { it.copy(config = it.config.copy(pointName = n), saved = false) }
    fun setPointCode(c: String?) = _state.update { it.copy(config = it.config.copy(pointCode = c?.ifBlank { null }), saved = false) }
    fun setTargetEpochCount(k: Int) = _state.update { if (k>0) it.copy(config = it.config.copy(targetEpochCount = k)) else it }
    fun setMaxPdop(p: Double?) = _state.update { it.copy(config = it.config.copy(maxPdop = p)) }
    fun setMaxHrms(h: Double?) = _state.update { it.copy(config = it.config.copy(maxHrms = h)) }

    fun start() {
        val s = _state.value
        if (!s.canStart) return
        _state.update { it.copy(isRecording = true, isCompleted = false, currentEpoch = 0, samples = emptyList(), saved = false, status = "Kayıt") }
        loopJob?.cancel()
        loopJob = viewModelScope.launch { recordLoop() }
    }

    fun stop() {
        loopJob?.cancel()
        _state.update { it.copy(isRecording = false, status = if (it.isCompleted) "Tamamlandı" else "Durdu") }
    }

    private suspend fun recordLoop() {
        while (_state.value.isRecording) {
            appendSample()
            val cfg = _state.value.config
            if (_state.value.currentEpoch >= cfg.targetEpochCount) {
                _state.update { it.copy(isRecording = false, isCompleted = true, status = "Tamamlandı") }
                break
            }
            delay(1000L) // 1s epoch
        }
    }

    private fun appendSample() {
        val obs = _state.value.observation ?: return
        val lat = obs.latDeg ?: return
        val lon = obs.lonDeg ?: return
        val s0 = _state.value
        val newSample = EpochSample(
            index = s0.samples.size + 1,
            epochMillis = System.currentTimeMillis(),
            lat = lat,
            lon = lon,
            ellH = obs.ellipsoidalHeight,
            hrms = obs.hrms,
            vrms = obs.vrms,
            pdop = obs.pdop,
            sat = obs.satellitesInUse,
            fix = obs.fixType.name
        )
        val list = s0.samples + newSample
        val avgLat = list.map { it.lat }.average()
        val avgLon = list.map { it.lon }.average()
        val avgEll = list.mapNotNull { it.ellH }.takeIf { it.isNotEmpty() }?.average()
        // Basit yatay RMS (lat/lon dereceden metre approx değil — burada sadece derece RMS; daha sonra projeksiyonla geliştirilebilir)
        val horizontalRms = sqrt(list.map { (it.lat - avgLat)*(it.lat - avgLat) + (it.lon - avgLon)*(it.lon - avgLon) }.average())
        _state.update { it.copy(samples = list, currentEpoch = list.size, avgLat = avgLat, avgLon = avgLon, avgEllH = avgEll, horizontalRms = horizontalRms) }
    }

    fun saveAveragedPoint() {
        val s = _state.value
        if (!s.isCompleted || s.avgLat == null || s.avgLon == null) return
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            val transformer = ProjectionEngine.forProject(proj)
            val (easting, northing) = if (transformer !== NoOpTransformer) transformer.forward(s.avgLat, s.avgLon) else (s.avgLon * 111000) to (s.avgLat * 111000)
            surveyRepo.insert(
                SurveyPointEntity(
                    projectId = proj.id,
                    name = s.config.pointName,
                    code = s.config.pointCode ?: "EPOCH",
                    latitude = s.avgLat,
                    longitude = s.avgLon,
                    elevation = s.avgEllH,
                    northing = northing,
                    easting = easting,
                    zone = proj.utmZone?.let { it.toString() + if (proj.utmNorthHemisphere) "N" else "S" },
                    hrms = s.samples.mapNotNull { it.hrms }.averageOrNull(),
                    vrms = s.samples.mapNotNull { it.vrms }.averageOrNull(),
                    pdop = s.samples.mapNotNull { it.pdop }.averageOrNull(),
                    satellites = s.samples.lastOrNull()?.sat,
                    fixType = s.samples.lastOrNull()?.fix,
                    antennaHeight = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            _state.update { it.copy(saved = true, status = "Kaydedildi") }
        }
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    fun startEngine() = gnss.start()
    fun stopEngine() = gnss.stop()

    override fun onCleared() {
        stop()
        stopEngine()
        super.onCleared()
    }
}

