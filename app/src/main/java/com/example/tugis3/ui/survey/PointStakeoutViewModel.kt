package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.db.entity.ProjectEntity
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.gnss.GnssEngine
import com.example.tugis3.gnss.model.GnssObservation
import com.example.tugis3.coord.transform.NoOpTransformer
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.coord.transform.CoordinateTransformer
import com.example.tugis3.prefs.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.abs

@HiltViewModel
class PointStakeoutViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository,
    private val surveyPointRepo: SurveyPointRepository,
    private val gnss: GnssEngine,
    private val prefs: PrefsRepository
) : ViewModel() {

    data class StakeoutTarget(
        val name: String?,
        val easting: Double?,
        val northing: Double?,
        val elevation: Double?
    )

    data class StakeoutState(
        val observation: GnssObservation?,
        val target: StakeoutTarget?,
        val horizontalDist: Double?,
        val verticalDiff: Double?,
        val offsetN: Double?,
        val offsetE: Double?,
        val bearingDeg: Double?,
        val withinHorizontal: Boolean,
        val withinVertical: Boolean,
        val withinAll: Boolean
    )

    private val _manualTarget = MutableStateFlow<StakeoutTarget?>(null)
    private val _selectedPointName = MutableStateFlow<String?>(null)

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Projedeki kayıtlı noktalar (silinmemiş)
    val projectPoints: StateFlow<List<PointEntity>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else pointRepo.observePoints(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val observation = gnss.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val targetFlow: Flow<StakeoutTarget?> = combine(_manualTarget, _selectedPointName, projectPoints) { manual, selectedName, list ->
        manual ?: run {
            if (selectedName == null) return@run null
            val p = list.firstOrNull { it.name == selectedName } ?: return@run null
            StakeoutTarget(p.name, p.easting, p.northing, p.ellipsoidalHeight)
        }
    }

    // Kullanıcı toleransları (UI günceller)
    private val _hTol = MutableStateFlow(0.10)
    private val _vTol = MutableStateFlow(0.05)
    val horizTol = _hTol.asStateFlow()
    val vertTol = _vTol.asStateFlow()

    private val _simulate = MutableStateFlow(false)
    private val _simOffsetN = MutableStateFlow(0.0)
    private val _simOffsetE = MutableStateFlow(0.0)
    val simulate = _simulate.asStateFlow()
    val simOffsetN = _simOffsetN.asStateFlow()
    val simOffsetE = _simOffsetE.asStateFlow()

    private var autoProjectionApplied = false // ilk başarılı GNSS fix ile projeksiyon parametreleri otomatik ayarlansın

    init {
        // Prefs'ten toleranslar ve son hedef
        viewModelScope.launch { prefs.stakeHorizontalTol.collect { _hTol.value = it } }
        viewModelScope.launch { prefs.stakeVerticalTol.collect { _vTol.value = it } }
        viewModelScope.launch {
            prefs.stakeoutPointName.collect { savedName ->
                if (savedName != null && _selectedPointName.value == null) {
                    _selectedPointName.value = savedName
                }
            }
        }
        // Otomatik projeksiyon parametresi doldurma (kullanıcı tercihi ile)
        viewModelScope.launch {
            combine(activeProject, observation, prefs.autoProjectionEnabled) { proj, obs, auto -> Triple(proj, obs, auto) }
                .collect { (proj, obs, auto) ->
                    if (!auto) return@collect
                    if (autoProjectionApplied) return@collect
                    val lat = obs?.latDeg
                    val lon = obs?.lonDeg
                    if (proj == null || lat == null || lon == null) return@collect
                    val needsEllipsoid = proj.semiMajorA == null || proj.invFlattening == null || proj.ellipsoidName == null
                    val needsUtm = proj.utmZone == null
                    if (!needsEllipsoid && !needsUtm) {
                        autoProjectionApplied = true
                        return@collect
                    }
                    if (needsEllipsoid) {
                        launch { projectRepo.updateEllipsoid(proj.id, "WGS84", 6378137.0, 298.257223563) }
                    }
                    if (needsUtm) {
                        val zone = (((lon + 180.0) / 6.0).toInt() + 1).coerceIn(1, 60)
                        val north = lat >= 0.0
                        val epsg = (if (north) 32600 else 32700) + zone
                        launch { projectRepo.updateUtm(proj.id, zone, north, epsg) }
                    }
                    autoProjectionApplied = true
                }
        }
    }

    val stakeoutState: StateFlow<StakeoutState> = combine(
        observation,
        targetFlow,
        activeProject,
        horizTol,
        vertTol,
        _simulate,
        _simOffsetN,
        _simOffsetE
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val obs = arr[0] as GnssObservation?
        val target = arr[1] as StakeoutTarget?
        val project = arr[2] as ProjectEntity?
        val hTol = arr[3] as Double
        val vTol = arr[4] as Double
        val sim = arr[5] as Boolean
        val sN = arr[6] as Double
        val sE = arr[7] as Double
        if (obs == null || project == null || target?.easting == null || target.northing == null) {
            return@combine StakeoutState(obs, target, null, null, null, null, null, false, false, false)
        }
        val transformer = ProjectionEngine.forProject(project)
        val (baseE, baseN) = if (obs.latDeg != null && obs.lonDeg != null) {
            if (transformer !== NoOpTransformer) {
                val (x,y) = transformer.forward(obs.latDeg, obs.lonDeg)
                x to y
            } else {
                (obs.lonDeg * 111000) to (obs.latDeg * 111000)
            }
        } else 0.0 to 0.0
        val curE = if (sim) baseE + sE else baseE
        val curN = if (sim) baseN + sN else baseN
        val offsetN = target.northing - curN
        val offsetE = target.easting - curE
        val horizontal = sqrt(offsetN*offsetN + offsetE*offsetE)
        val vertDiff = if (target.elevation != null && obs.ellipsoidalHeight != null) target.elevation - obs.ellipsoidalHeight else null
        val bearing = ((Math.toDegrees(atan2(offsetE, offsetN)) + 360) % 360)
        val withinH = horizontal <= hTol
        val withinV = if (vertDiff != null) abs(vertDiff) <= vTol else false
        StakeoutState(obs, target, horizontal, vertDiff, offsetN, offsetE, bearing, withinH, withinV, withinH && withinV)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StakeoutState(null,null,null,null,null,null,null,false,false,false))

    fun toggleSimulation() { _simulate.value = !_simulate.value }
    fun setSimOffsetN(v:Double){ _simOffsetN.value = v }
    fun setSimOffsetE(v:Double){ _simOffsetE.value = v }

    fun setHorizontalTol(v: Double) { if (v>0) { _hTol.value = v; viewModelScope.launch { prefs.setStakeHorizontalTol(v) } } }
    fun setVerticalTol(v: Double) { if (v>0) { _vTol.value = v; viewModelScope.launch { prefs.setStakeVerticalTol(v) } } }
    fun resetTolerances() { setHorizontalTol(0.10); setVerticalTol(0.05) }

    fun selectPoint(name: String) {
        _manualTarget.value = null
        _selectedPointName.value = name
        viewModelScope.launch { prefs.setStakeoutPointName(name) }
    }

    fun clearTarget() {
        _manualTarget.value = null
        _selectedPointName.value = null
        viewModelScope.launch { prefs.setStakeoutPointName(null) }
    }

    fun setManualTarget(easting: Double?, northing: Double?, elevation: Double?) {
        _selectedPointName.value = null
        _manualTarget.value = StakeoutTarget(null, easting, northing, elevation)
    }

    fun startEngine() = gnss.start()
    fun stopEngine() = gnss.stop()

    fun acceptStakeout(save: Boolean = true) {
        val st = stakeoutState.value
        val proj = activeProject.value ?: return
        if (!st.withinAll) return
        val obs = st.observation ?: return
        if (!save) return
        viewModelScope.launch {
            val transformer = ProjectionEngine.forProject(proj)
            val (easting, northing) = if (obs.latDeg != null && obs.lonDeg != null) transformer.forward(obs.latDeg, obs.lonDeg) else (0.0 to 0.0)
            surveyPointRepo.insert(
                SurveyPointEntity(
                    projectId = proj.id,
                    name = (st.target?.name ?: "STK") + "_STK",
                    code = "STK",
                    latitude = obs.latDeg,
                    longitude = obs.lonDeg,
                    elevation = obs.ellipsoidalHeight,
                    northing = northing,
                    easting = easting,
                    zone = proj.utmZone?.let { it.toString() + if (proj.utmNorthHemisphere) "N" else "S" },
                    hrms = obs.hrms,
                    vrms = obs.vrms,
                    pdop = obs.pdop,
                    satellites = obs.satellitesInUse,
                    fixType = obs.fixType.name,
                    antennaHeight = null,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun localToLatLon(e: Double, n: Double): Pair<Double, Double>? {
        val proj = activeProject.value ?: return null
        val tf = ProjectionEngine.forProject(proj)
        return try { tf.inverse(e, n) } catch (_: Exception) { null }
    }

    override fun onCleared() {
        stopEngine()
        super.onCleared()
    }
}
