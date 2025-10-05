package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.gnss.GnssEngine
import com.example.tugis3.gnss.model.GnssObservation
import com.example.tugis3.coord.transform.NoOpTransformer
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.prefs.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LineStakeoutViewModel @Inject constructor(
    projectRepo: ProjectRepository,
    private val pointRepo: PointRepository,
    private val surveyPointRepo: SurveyPointRepository,
    private val gnss: GnssEngine,
    private val prefs: PrefsRepository
) : ViewModel() {

    data class LineDef(
        val name: String,
        val start: PointEntity?,
        val end: PointEntity?,
        val manualStartE: Double? = null,
        val manualStartN: Double? = null,
        val manualEndE: Double? = null,
        val manualEndN: Double? = null
    ) {
        fun startE() = start?.easting ?: manualStartE
        fun startN() = start?.northing ?: manualStartN
        fun endE() = end?.easting ?: manualEndE
        fun endN() = end?.northing ?: manualEndN
        val valid: Boolean get() = startE()!=null && startN()!=null && endE()!=null && endN()!=null
    }

    data class StakeStation(
        val chain: Double, // metre
        val e: Double,
        val n: Double
    )

    data class LineStakeoutState(
        val observation: GnssObservation?,
        val line: LineDef?,
        val lineLength: Double?,
        val chain: Double?,
        val offset: Double?,
        val bearingLineDeg: Double?,
        val bearingToEndDeg: Double?,
        val stations: List<StakeStation>,
        val nearestStation: StakeStation?,
        val lateralWithin: Boolean,
        val chainWithin: Boolean
    )

    private val _selectedStartName = MutableStateFlow<String?>(null)
    private val _selectedEndName = MutableStateFlow<String?>(null)
    private val _manualStart = MutableStateFlow<Pair<Double?,Double?>>(null to null)
    private val _manualEnd = MutableStateFlow<Pair<Double?,Double?>>(null to null)
    private val _lineName = MutableStateFlow("Hat_1")
    private val _stakeInterval = MutableStateFlow(10.0)
    private val _latTol = MutableStateFlow(0.20) // lateral metre
    private val _chainTol = MutableStateFlow(0.50) // chain metre farkı
    private val _sim = MutableStateFlow(false)
    private val _simLat = MutableStateFlow(0.0) // lateral (m)
    private val _simChain = MutableStateFlow(0.0) // zinciraj ek offset (m)

    val lineName = _lineName.asStateFlow()
    val stakeInterval = _stakeInterval.asStateFlow()
    val lateralTolerance = _latTol.asStateFlow()
    val chainTolerance = _chainTol.asStateFlow()
    val simulate = _sim.asStateFlow()
    val simLat = _simLat.asStateFlow()
    val simChain = _simChain.asStateFlow()

    init {
        viewModelScope.launch { prefs.lineInterval.collect { _stakeInterval.value = it } }
        viewModelScope.launch { prefs.lineLatTol.collect { _latTol.value = it } }
        viewModelScope.launch { prefs.lineChainTol.collect { _chainTol.value = it } }
        viewModelScope.launch { prefs.lineNameFlow.collect { saved -> if (saved != null && _lineName.value == "Hat_1") _lineName.value = saved } }
    }

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val projectPoints: StateFlow<List<PointEntity>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else pointRepo.observePoints(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val observation = gnss.observation.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val lineFlow = combine(
        _selectedStartName,
        _selectedEndName,
        _manualStart,
        _manualEnd,
        _lineName,
        projectPoints
    ) { args: Array<Any?> ->
        val startName = args[0] as String?
        val endName = args[1] as String?
        val mStart = args[2] as Pair<Double?, Double?>
        val mEnd = args[3] as Pair<Double?, Double?>
        val name = args[4] as String
        val points = args[5] as List<PointEntity>
        val start = startName?.let { n -> points.firstOrNull { p -> p.name == n } }
        val end = endName?.let { n -> points.firstOrNull { p -> p.name == n } }
        val manualStartPair = if (mStart.first == null && mStart.second == null) null else mStart
        val manualEndPair = if (mEnd.first == null && mEnd.second == null) null else mEnd

        LineDef(
            name = name,
            start = start,
            end = end,
            manualStartE = manualStartPair?.first,
            manualStartN = manualStartPair?.second,
            manualEndE = manualEndPair?.first,
            manualEndN = manualEndPair?.second
        ).takeIf { it.valid }
    }

    val state = combine(
        observation,
        lineFlow,
        activeProject,
        stakeInterval,
        lateralTolerance,
        chainTolerance,
        _sim,
        _simLat,
        _simChain
    ) { args: Array<Any?> ->
        val obs = args[0] as GnssObservation?
        val line = args[1] as LineDef?
        val project = args[2] as com.example.tugis3.data.db.entity.ProjectEntity?
        val interval = args[3] as Double
        val latTol = args[4] as Double
        val chainTol = args[5] as Double
        val sim = args[6] as Boolean
        val simLat = args[7] as Double
        val simChain = args[8] as Double
        if (obs == null || line == null || !line.valid || project == null) {
            LineStakeoutState(obs, line, null, null, null, null, null, emptyList(), null, false, false)
        } else {
            val sE = line.startE()!!
            val sN = line.startN()!!
            val eE = line.endE()!!
            val eN = line.endN()!!
            val dE = eE - sE
            val dN = eN - sN
            val length = sqrt(dE * dE + dN * dN)
            val transformer = ProjectionEngine.forProject(project)
            val (rawE, rawN) = if (obs.latDeg != null && obs.lonDeg != null) {
                if (transformer !== NoOpTransformer) {
                    transformer.forward(obs.latDeg, obs.lonDeg)
                } else {
                    Pair(obs.lonDeg * 111000.0, obs.latDeg * 111000.0)
                }
            } else {
                Pair(0.0, 0.0)
            }
            val dx = rawE - sE
            val dy = rawN - sN
            var projected = (dx * dE + dy * dN) / length
            var cross = (dy * dE - dx * dN) / length
            if (sim) {
                projected += simChain
                cross += simLat
            }
            val chain = projected.coerceIn(0.0, length)
            val offset = cross
            val bearingLine = (Math.toDegrees(atan2(dE, dN)) + 360.0) % 360.0
            val bearingToEnd = (Math.toDegrees(atan2(eE - rawE, eN - rawN)) + 360.0) % 360.0
            val stations = mutableListOf<StakeStation>()
            var pos = 0.0
            while (pos <= length + 0.01) {
                val ratio = pos / length
                stations.add(StakeStation(pos, sE + dE * ratio, sN + dN * ratio))
                pos = pos + interval
            }
            val nearest = stations.minByOrNull { st -> abs(st.chain - chain) + (abs(offset) * 0.5) }
            val lateralWithin = abs(offset) <= latTol
            val chainWithin = nearest?.let { abs(it.chain - chain) <= chainTol } ?: false
            LineStakeoutState(obs, line, length, chain, offset, bearingLine, bearingToEnd, stations, nearest, lateralWithin, chainWithin)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LineStakeoutState(null, null, null, null, null, null, null, emptyList(), null, false, false)
    )

    fun toggleSim() { _sim.value = !_sim.value }
    fun setSimLat(v:Double){ _simLat.value = v }
    fun setSimChain(v:Double){ _simChain.value = v }

    fun setLineName(n:String){
        if (_lineName.value != n) {
            _lineName.value = n
            viewModelScope.launch { prefs.setLineName(n) }
        }
    }
    fun setStakeInterval(v:Double){ if (v>0){ _stakeInterval.value=v; viewModelScope.launch { prefs.setLineInterval(v) } } }
    fun setLateralTol(v:Double){ if (v>0){ _latTol.value=v; viewModelScope.launch { prefs.setLineLatTol(v) } } }
    fun setChainTol(v:Double){ if (v>0){ _chainTol.value=v; viewModelScope.launch { prefs.setLineChainTol(v) } } }

    fun selectStart(name:String){ _manualStart.value = null to null; _selectedStartName.value = name }
    fun selectEnd(name:String){ _manualEnd.value = null to null; _selectedEndName.value = name }
    fun setManualStart(e:Double?, n:Double?){ _selectedStartName.value = null; _manualStart.value = e to n }
    fun setManualEnd(e:Double?, n:Double?){ _selectedEndName.value = null; _manualEnd.value = e to n }
    fun clearLine(){ _selectedStartName.value = null; _selectedEndName.value = null; _manualStart.value = null to null; _manualEnd.value = null to null }

    fun startEngine() = gnss.start()
    fun stopEngine() = gnss.stop()

    fun saveStakeIfWithin() {
        val st = state.value
        val proj = activeProject.value ?: return
        val obs = st.observation ?: return
        if (!(st.lateralWithin && st.chainWithin)) return
        viewModelScope.launch {
            val transformer = ProjectionEngine.forProject(proj)
            val (easting, northing) = if (obs.latDeg!=null && obs.lonDeg!=null) transformer.forward(obs.latDeg, obs.lonDeg) else (0.0 to 0.0)
            surveyPointRepo.insert(
                SurveyPointEntity(
                    projectId = proj.id,
                    name = (st.line?.name ?: "HAT") + "_" + String.format(java.util.Locale.US, "%.1f", st.chain ?: 0.0),
                    code = "LINE_STK",
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

    override fun onCleared() {
        stopEngine()
        super.onCleared()
    }
}
