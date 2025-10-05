package com.example.tugis3.gnss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.gnss.GnssEngine
import com.example.tugis3.gnss.model.GnssObservation
import com.example.tugis3.coord.transform.ProjectionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GnssMonitorViewModel @Inject constructor(
    private val engine: GnssEngine,
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository
) : ViewModel() {

    sealed class UiEvent { data class Saved(val pointName: String): UiEvent(); data class Error(val message: String): UiEvent() }
    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val observation: StateFlow<GnssObservation?> = engine.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val projects = projectRepo.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class SavedPointInfo(
        val name: String,
        val lat: Double?,
        val lon: Double?,
        val fix: String,
        val timestamp: Long
    )
    private val _lastSavedPoint = MutableStateFlow<SavedPointInfo?>(null)
    val lastSavedPoint: StateFlow<SavedPointInfo?> = _lastSavedPoint

    private val _fixStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val fixStats: StateFlow<Map<String, Int>> = _fixStats

    data class SatelliteUi(
        val id: Int,
        val constellation: String,
        val azimuthDeg: Double,
        val elevationDeg: Double,
        val snr: Double,
        val used: Boolean
    )

    val satellites: StateFlow<List<SatelliteUi>> = engine.satellites
        .map { list ->
            list.map { s ->
                SatelliteUi(
                    id = s.svid,
                    constellation = s.constellation,
                    azimuthDeg = s.azimuthDeg.toDouble(),
                    elevationDeg = s.elevationDeg.toDouble(),
                    snr = s.cn0DbHz.toDouble(),
                    used = s.usedInFix
                )
            }.sortedByDescending { it.snr }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nmeaLineCount: StateFlow<Long> = engine.nmeaLineCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    init {
        // Observation akışını dinleyip fix istatistiklerini güncelle
        viewModelScope.launch {
            observation.collect { obs ->
                val f = obs?.fixType?.name ?: return@collect
                val current = _fixStats.value.toMutableMap()
                current[f] = (current[f] ?: 0) + 1
                _fixStats.value = current
            }
        }
    }

    fun setActiveProject(id: Long) = viewModelScope.launch { projectRepo.setActive(id) }

    fun start() = engine.start()
    fun stop() = engine.stop()

    fun createAndActivateProject(name: String, description: String?) = viewModelScope.launch {
        projectRepo.createProject(name, description, activate = true)
    }

    fun saveCurrentPoint() = viewModelScope.launch {
        val proj = activeProject.value ?: run { _uiEvents.tryEmit(UiEvent.Error("Aktif proje yok")); return@launch }
        val obs = observation.value ?: run { _uiEvents.tryEmit(UiEvent.Error("Geçerli GNSS gözlemi yok")); return@launch }
        val lat = obs.latDeg ?: run { _uiEvents.tryEmit(UiEvent.Error("Konum çözülmedi (lat)")); return@launch }
        val lon = obs.lonDeg ?: run { _uiEvents.tryEmit(UiEvent.Error("Konum çözülmedi (lon)")); return@launch }
        runCatching {
            val transformer = ProjectionEngine.forProject(proj)
            val (northing, easting) = transformer.forward(lat, lon)
            val name = "P" + System.currentTimeMillis().toString().takeLast(6)
            pointRepo.upsert(
                PointEntity(
                    projectId = proj.id,
                    name = name,
                    northing = northing,
                    easting = easting,
                    ellipsoidalHeight = obs.ellipsoidalHeight,
                    orthoHeight = null, // TODO: Geoid hesaplanınca doldur
                    latDeg = obs.latDeg,
                    lonDeg = obs.lonDeg,
                    fixType = obs.fixType.name,
                    hrms = obs.hrms,
                    pdop = obs.pdop,
                    hdop = obs.hdop,
                    vdop = obs.vdop
                )
            )
            _lastSavedPoint.value = SavedPointInfo(name, obs.latDeg, obs.lonDeg, obs.fixType.name, System.currentTimeMillis())
            _uiEvents.tryEmit(UiEvent.Saved(name))
        }.onFailure { ex ->
            _uiEvents.tryEmit(UiEvent.Error(ex.message ?: "Kaydetme hatası"))
        }
    }

    fun clearFixStats() { _fixStats.value = emptyMap() }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
