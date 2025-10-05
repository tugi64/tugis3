package com.example.tugis3.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.data.db.entity.CalibrationPointEntity
import com.example.tugis3.data.repository.CalibrationPointRepository
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.gnss.GnssEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalizationViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val calibRepo: CalibrationPointRepository,
    private val gnss: GnssEngine
) : ViewModel() {

    data class SolvePreview(
        val scale: Double,
        val rotDeg: Double,
        val tx: Double,
        val ty: Double,
        val rms: Double,
        val pointCount: Int
    )

    data class CalibrationPointUi(
        val entity: CalibrationPointEntity,
        val residualE: Double? = null,
        val residualN: Double? = null
    )

    data class State(
        val projectId: Long? = null,
        val points: List<CalibrationPointUi> = emptyList(),
        val observationFix: String? = null,
        val pdop: Double? = null,
        val canAdd: Boolean = false,
        val dstNorthText: String = "",
        val dstEastText: String = "",
        val weightText: String = "1.0",
        val solving: Boolean = false,
        val solveResult: SolvePreview? = null,
        val status: String = "Bekleniyor"
    )

    private val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val observation = gnss.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val calibPoints: StateFlow<List<CalibrationPointEntity>> = activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList()) else calibRepo.observe(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var lastSolveParams: CalibrationPointRepository.SolveResult? = null

    init {
        viewModelScope.launch {
            combine(activeProject, observation, calibPoints) { proj, obs, pts -> Triple(proj, obs, pts) }
                .collect { (proj, obs, pts) ->
                    val canAdd = proj != null && obs?.latDeg != null && obs.lonDeg != null && obs.fixType.isDifferential() && _state.value.dstNorthText.isNotBlank() && _state.value.dstEastText.isNotBlank()
                    val residualMapped = mapResiduals(pts, lastSolveParams)
                    _state.update {
                        it.copy(
                            projectId = proj?.id,
                            points = residualMapped,
                            observationFix = obs?.fixType?.name,
                            pdop = obs?.pdop,
                            canAdd = canAdd,
                            status = buildStatus(proj, obs, pts)
                        )
                    }
                }
        }
    }

    private fun mapResiduals(list: List<CalibrationPointEntity>, params: CalibrationPointRepository.SolveResult?): List<CalibrationPointUi> {
        if (params == null) return list.map { CalibrationPointUi(it) }
        val cosR = cos(params.rotRad)
        val sinR = sin(params.rotRad)
        return list.map { e ->
            val predE = params.scale * (cosR * e.srcEast - sinR * e.srcNorth) + params.tx
            val predN = params.scale * (sinR * e.srcEast + cosR * e.srcNorth) + params.ty
            CalibrationPointUi(
                entity = e,
                residualE = e.dstEast - predE,
                residualN = e.dstNorth - predN
            )
        }
    }

    private fun buildStatus(proj: Any?, obs: com.example.tugis3.gnss.model.GnssObservation?, pts: List<CalibrationPointEntity>): String = when {
        proj == null -> "Aktif proje yok"
        obs == null -> "GNSS bekleniyor"
        !obs.fixType.isDifferential() -> "RTK değil"
        pts.size < 2 -> "Çözüm için ≥2 nokta gerekli"
        else -> "Hazır"
    }

    fun setDstNorth(text: String) { _state.update { it.copy(dstNorthText = text) } }
    fun setDstEast(text: String) { _state.update { it.copy(dstEastText = text) } }
    fun setWeight(text: String) { _state.update { it.copy(weightText = text) } }

    fun addPoint() {
        val proj = activeProject.value ?: return
        val obs = observation.value ?: return
        val lat = obs.latDeg ?: return
        val lon = obs.lonDeg ?: return
        val dstN = _state.value.dstNorthText.toDoubleOrNull() ?: return
        val dstE = _state.value.dstEastText.toDoubleOrNull() ?: return
        val w = _state.value.weightText.toDoubleOrNull() ?: 1.0
        val transformer = ProjectionEngine.forProject(proj)
        val (easting, northing) = transformer.forward(lat, lon)
        viewModelScope.launch {
            calibRepo.add(
                projectId = proj.id,
                srcNorth = northing,
                srcEast = easting,
                dstNorth = dstN,
                dstEast = dstE,
                weight = w
            )
            // Clear input fields (keep weight)
            _state.update { it.copy(dstNorthText = "", dstEastText = "") }
        }
    }

    fun toggleInclude(point: CalibrationPointEntity) {
        viewModelScope.launch { calibRepo.toggleInclude(point.id, point.include == 0) }
    }

    fun deletePoint(point: CalibrationPointEntity) {
        viewModelScope.launch { calibRepo.delete(point) }
    }

    fun solve() {
        val projId = _state.value.projectId ?: return
        _state.update { it.copy(solving = true) }
        viewModelScope.launch {
            val result = calibRepo.solveAndApply(projId)
            result.onSuccess { r ->
                lastSolveParams = r
                _state.update {
                    it.copy(
                        solving = false,
                        solveResult = SolvePreview(
                            scale = r.scale,
                            rotDeg = Math.toDegrees(r.rotRad),
                            tx = r.tx,
                            ty = r.ty,
                            rms = r.rms,
                            pointCount = r.pointCount
                        ),
                        // residualları güncelle
                        points = mapResiduals(calibPoints.value, r)
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(solving = false, status = "Hata: ${e.message}") }
            }
        }
    }

    fun clearInputs() { _state.update { it.copy(dstNorthText = "", dstEastText = "") } }

    fun startEngine() = gnss.start()
    fun stopEngine() = gnss.stop()

    override fun onCleared() {
        stopEngine()
        super.onCleared()
    }
}
