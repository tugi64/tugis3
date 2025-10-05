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
import kotlin.math.pow
import kotlin.math.sqrt

@HiltViewModel
class StaticSurveyViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val surveyRepo: SurveyPointRepository,
    private val gnss: GnssEngine
) : ViewModel() {

    data class StaticConfig(
        val pointName: String = "STATIC_001",
        val pdopLimit: Double = 6.0,
        val elevationMask: Int = 10, // degrees
        val recordIntervalSec: Int = 1, // 1 = 1Hz
        val antennaHeight: Double = 1.50,
        val measureType: String = "Faz" // simplified label
    )

    data class StaticEpoch(
        val index: Int,
        val epochMillis: Long,
        val lat: Double,
        val lon: Double,
        val hEll: Double?,
        val hrms: Double?,
        val vrms: Double?,
        val pdop: Double?,
        val satellites: Int?,
        val fix: String
    )

    data class StaticState(
        val config: StaticConfig = StaticConfig(),
        val observation: GnssObservation? = null,
        val isRecording: Boolean = false,
        val elapsedSec: Int = 0,
        val epochs: List<StaticEpoch> = emptyList(),
        val avgLat: Double? = null,
        val avgLon: Double? = null,
        val avgEllH: Double? = null,
        val hrmsAvg: Double? = null,
        val hrmsRms: Double? = null,
        val status: String = "Hazır",
        val canStart: Boolean = false,
        val saved: Boolean = false
    )

    private val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val observation = gnss.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow(StaticState())
    val state: StateFlow<StaticState> = _state.asStateFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            observation.collect { obs -> updateFromObservation(obs) }
        }
    }

    private fun updateFromObservation(obs: GnssObservation?) {
        val cfg = _state.value.config
        val canStart = obs?.pdop?.let { it <= cfg.pdopLimit } == true && obs.fixType.isDifferential()
        _state.update { it.copy(observation = obs, canStart = canStart && !it.isRecording, status = buildStatus(obs, canStart)) }
    }

    private fun buildStatus(obs: GnssObservation?, canStart: Boolean): String = when {
        obs == null -> "GNSS bekleniyor"
        !obs.fixType.isDifferential() -> "RTK değil / düşük çözüm"
        obs.pdop != null && obs.pdop!! > _state.value.config.pdopLimit -> "PDOP Limit Aşıldı"
        else -> if (canStart) "Hazır" else "Kontrol"
    }

    // --- User Actions ---
    fun setPointName(name: String) = _state.update { it.copy(config = it.config.copy(pointName = name), saved = false) }
    fun setPdopLimit(v: Double) = _state.update { it.copy(config = it.config.copy(pdopLimit = v)) }
    fun setElevationMask(v: Int) = _state.update { it.copy(config = it.config.copy(elevationMask = v.coerceIn(0,90))) }
    fun setIntervalFromLabel(label: String) {
        val sec = when(label.uppercase()) {
            "60S" -> 60; "30S" ->30; "15S"->15; "10S"->10; "5S"->5; "2S"->2; "1HZ"->1; "2HZ"->1/2; "5HZ"->1/5; else -> 1
        }.coerceAtLeast(1)
        _state.update { it.copy(config = it.config.copy(recordIntervalSec = sec)) }
    }
    fun setAntennaHeight(h: Double) = _state.update { it.copy(config = it.config.copy(antennaHeight = h)) }

    fun startRecording() {
        val obs = _state.value.observation ?: return
        if (!_state.value.canStart) return
        if (!obs.fixType.isDifferential()) return
        _state.update { it.copy(isRecording = true, elapsedSec = 0, epochs = emptyList(), saved = false, status = "Kayıt") }
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_state.value.isRecording) {
                delay(1000L)
                appendEpoch()
                _state.update { s -> s.copy(elapsedSec = s.elapsedSec + 1) }
            }
        }
    }

    fun stopRecording() {
        tickJob?.cancel()
        _state.update { it.copy(isRecording = false, status = "Durdu") }
    }

    private fun appendEpoch() {
        val obs = _state.value.observation ?: return
        val lat = obs.latDeg ?: return
        val lon = obs.lonDeg ?: return
        val epList = _state.value.epochs
        val epoch = StaticEpoch(
            index = epList.size + 1,
            epochMillis = System.currentTimeMillis(),
            lat = lat,
            lon = lon,
            hEll = obs.ellipsoidalHeight,
            hrms = obs.hrms,
            vrms = obs.vrms,
            pdop = obs.pdop,
            satellites = obs.satellitesInUse,
            fix = obs.fixType.name
        )
        val newList = epList + epoch
        val avgLat = newList.map { it.lat }.average()
        val avgLon = newList.map { it.lon }.average()
        val avgEll = newList.mapNotNull { it.hEll }.takeIf { it.isNotEmpty() }?.average()
        val hrmsAvg = newList.mapNotNull { it.hrms }.takeIf { it.isNotEmpty() }?.average()
        val hrmsRms = newList.mapNotNull { it.hrms }.takeIf { it.isNotEmpty() }?.let { list ->
            sqrt(list.map { (it - hrmsAvg!!) * (it - hrmsAvg) }.average())
        }
        _state.update { it.copy(epochs = newList, avgLat = avgLat, avgLon = avgLon, avgEllH = avgEll, hrmsAvg = hrmsAvg, hrmsRms = hrmsRms) }
    }

    fun saveAveragedPoint() {
        val s = _state.value
        if (s.epochs.isEmpty() || s.avgLat == null || s.avgLon == null) return
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            val transformer = ProjectionEngine.forProject(proj)
            val (easting, northing) = if (transformer !== NoOpTransformer) transformer.forward(s.avgLat, s.avgLon) else (s.avgLon * 111000) to (s.avgLat * 111000)
            surveyRepo.insert(
                SurveyPointEntity(
                    projectId = proj.id,
                    name = s.config.pointName,
                    code = "STATIC",
                    latitude = s.avgLat,
                    longitude = s.avgLon,
                    elevation = s.avgEllH,
                    northing = northing,
                    easting = easting,
                    zone = proj.utmZone?.let { it.toString() + if (proj.utmNorthHemisphere) "N" else "S" },
                    hrms = s.hrmsAvg,
                    vrms = null,
                    pdop = s.observation?.pdop,
                    satellites = s.observation?.satellitesInUse,
                    fixType = s.observation?.fixType?.name,
                    antennaHeight = s.config.antennaHeight,
                    timestamp = System.currentTimeMillis()
                )
            )
            _state.update { it.copy(saved = true, status = "Kaydedildi") }
        }
    }

    fun startEngine() = gnss.start()
    fun stopEngine() = gnss.stop()

    override fun onCleared() {
        stopRecording()
        stopEngine()
        super.onCleared()
    }
}

