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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

// Yol istasyonu veri sınıfı (km alanı kilometre cinsinden)
data class RoadStation(
    val km: Double,          // km cinsinden (0.120 = 120 m)
    val northing: Double,
    val easting: Double,
    val elevation: Double
)

data class RoadStakeoutUiState(
    val roadName: String = "YOL_001",
    val stationInterval: Double = 20.0,
    val roadWidth: Double = 7.0,
    val cutSlope: Double = 1.5,
    val fillSlope: Double = 2.0,
    val selectedMode: String = "Tasarım",

    // Alignment
    val roadPoints: List<RoadStation> = emptyList(),
    val totalAlignmentLength: Double = 0.0, // metre

    // GNSS / Current
    val observation: GnssObservation? = null,
    val currentNorthing: Double? = null,
    val currentEasting: Double? = null,
    val currentElevation: Double? = null,
    val chainProgress: Double? = null,   // m
    val lateralOffset: Double? = null,   // m (sol - / sağ +)

    // Nearest design station (listede) - bilgilendirme
    val nearestStation: RoadStation? = null,

    // Design target (kullanıcı girişi)
    val targetChain: Double? = null,     // m
    val targetOffset: Double? = 0.0,     // m
    val targetDesignNorthing: Double? = null,
    val targetDesignEasting: Double? = null,
    val targetDesignElevation: Double? = null,

    // Delta değerleri
    val deltaChain: Double? = null,      // m (current -> target)
    val deltaLateral: Double? = null,    // m (current -> target offset)
    val deltaElevation: Double? = null,  // m (current -> target elev)

    // Toleranslar
    val chainTolerance: Double = 0.20,
    val lateralTolerance: Double = 0.10,
    val elevationTolerance: Double = 0.05,

    val withinChain: Boolean = false,
    val withinLateral: Boolean = false,
    val withinElevation: Boolean = false,
    val withinAll: Boolean = false,

    // Durum / mesaj
    val statusMessage: String = ""
)

@HiltViewModel
class RoadStakeoutViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val surveyPointRepo: SurveyPointRepository,
    private val gnssEngine: GnssEngine
) : ViewModel() {

    private val _ui = MutableStateFlow(initialState())
    val uiState: StateFlow<RoadStakeoutUiState> = _ui.asStateFlow()

    private val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val observation = gnssEngine.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // GNSS gözlemini dinleyip yeniden hesaplama
        viewModelScope.launch {
            observation.collect { obs ->
                recompute(obs = obs)
            }
        }
    }

    private fun initialState(): RoadStakeoutUiState {
        // Basit kısa sentetik alignment (100 m) başlangıç
        val pts = listOf(
            RoadStation(0.000, 4330100.0, 355650.0, 850.000),
            RoadStation(0.020, 4330110.0, 355660.0, 850.400),
            RoadStation(0.040, 4330120.0, 355670.0, 850.800),
            RoadStation(0.060, 4330130.0, 355680.0, 851.100),
            RoadStation(0.080, 4330140.0, 355690.0, 851.400),
            RoadStation(0.100, 4330150.0, 355700.0, 851.700)
        )
        return RoadStakeoutUiState(roadPoints = pts).recalcDesign()
    }

    // --- Public Update API ---
    fun updateRoadName(v: String) = modify { copy(roadName = v) }
    fun updateStationInterval(v: Double) = modify { if (v>0) copy(stationInterval = v) else this }
    fun updateTargetChain(chainMeters: Double?) = modify { copy(targetChain = chainMeters) }
    fun updateTargetOffset(offset: Double?) = modify { copy(targetOffset = offset) }
    fun updateChainTolerance(v: Double) = modify { if (v>0) copy(chainTolerance = v) else this }
    fun updateLateralTolerance(v: Double) = modify { if (v>0) copy(lateralTolerance = v) else this }
    fun updateElevationTolerance(v: Double) = modify { if (v>0) copy(elevationTolerance = v) else this }
    fun updateMode(mode: String) = modify { copy(selectedMode = mode) }

    fun generateSyntheticAlignment(totalLengthMeters: Double = 400.0) {
        val interval = _ui.value.stationInterval
        if (interval <= 0) return
        val start = _ui.value.roadPoints.firstOrNull() ?: return
        val list = buildList {
            var ch = 0.0
            while (ch <= totalLengthMeters + 1e-6) {
                val km = ch / 1000.0
                // Basit doğrusal + hafif elev trend
                val n = start.northing + ch * 0.45
                val e = start.easting + ch * 0.30
                val z = start.elevation + ch * 0.0015
                add(RoadStation(km, n, e, z))
                ch += interval
            }
        }
        modify { copy(roadPoints = list) }
    }

    fun startEngine() = gnssEngine.start()
    fun stopEngine() = gnssEngine.stop()

    fun saveStakePoint() {
        val st = _ui.value
        if (!st.withinAll) return
        val proj = activeProject.value ?: return
        val obs = st.observation ?: return
        val lat = obs.latDeg
        val lon = obs.lonDeg
        if (lat == null || lon == null) return
        viewModelScope.launch {
            val transformer = ProjectionEngine.forProject(proj)
            val (easting, northing) = if (transformer !== NoOpTransformer) transformer.forward(lat, lon) else (lon*111000) to (lat*111000)
            surveyPointRepo.insert(
                SurveyPointEntity(
                    projectId = proj.id,
                    name = buildString {
                        append("RD_")
                        st.targetChain?.let { append(String.format("%.0f", it)) } ?: append(System.currentTimeMillis().toString().takeLast(4))
                    },
                    code = "ROAD_STK",
                    latitude = lat,
                    longitude = lon,
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

    // --- Internal ---
    private inline fun modify(block: RoadStakeoutUiState.() -> RoadStakeoutUiState) {
        _ui.value = block(_ui.value).recalcDesign()
        // Sonra GNSS ile yeniden hesap
        recompute()
    }

    private fun recompute(obs: GnssObservation? = observation.value) {
        val proj = activeProject.value
        val current = _ui.value
        if (proj == null) {
            _ui.value = current.copy(observation = obs)
            return
        }
        val lat = obs?.latDeg
        val lon = obs?.lonDeg
        var curN: Double? = null
        var curE: Double? = null
        var curZ: Double? = obs?.ellipsoidalHeight
        if (lat != null && lon != null) {
            val transformer = ProjectionEngine.forProject(proj)
            val (e, n) = if (transformer !== NoOpTransformer) transformer.forward(lat, lon) else (lon*111000) to (lat*111000)
            curN = n; curE = e
        }
        val afterPos = current.copy(
            observation = obs,
            currentNorthing = curN ?: current.currentNorthing,
            currentEasting = curE ?: current.currentEasting,
            currentElevation = curZ ?: current.currentElevation
        ).recalcDesign()
        _ui.value = afterPos
    }

    private fun RoadStakeoutUiState.recalcDesign(): RoadStakeoutUiState {
        if (roadPoints.isEmpty()) return copy(nearestStation = null, totalAlignmentLength = 0.0, statusMessage = "Alignment yok")
        val totalLenMeters = roadPoints.last().km * 1000.0

        // Mevcut pozisyona göre chain & lateral offset
        val (chainProgress, lateral, nearestStation) = if (currentNorthing!=null && currentEasting!=null) {
            projectPointToAlignment(currentNorthing, currentEasting, roadPoints)
        } else Triple(null, null, null)

        // Hedef chain/offset (kullanıcı) -> design nokta
        val targetChainM = targetChain
        val targetOffsetM = targetOffset ?: 0.0
        val (tN, tE, tElev) = if (targetChainM != null) computePointFromChainageOffsetInternal(targetChainM, targetOffsetM) else Triple(null,null,null)

        // Delta hesapları
        val dChain = if (targetChainM!=null && chainProgress!=null) targetChainM - chainProgress else null
        val dLat = if (lateral!=null && targetOffsetM.isFinite()) targetOffsetM - lateral else null
        val dElev = if (tElev!=null && currentElevation!=null) tElev - currentElevation else null

        val withinChain = dChain?.let { abs(it) <= chainTolerance } ?: false
        val withinLateral = dLat?.let { abs(it) <= lateralTolerance } ?: false
        val withinElevation = dElev?.let { abs(it) <= elevationTolerance } ?: false
        val withinAll = withinChain && withinLateral && withinElevation

        val msg = when {
            withinAll -> "✅ Hedefe Ulaşıldı"
            chainProgress == null -> "Konum bekleniyor"
            targetChainM == null -> "Hedef chain girin"
            else -> buildString {
                append("ΔCh:")
                dChain?.let { append(String.format("%.3f", it)) } ?: append("-")
                append(" ΔLat:")
                dLat?.let { append(String.format("%.3f", it)) } ?: append("-")
                append(" ΔZ:")
                dElev?.let { append(String.format("%.3f", it)) } ?: append("-")
            }
        }

        return copy(
            totalAlignmentLength = totalLenMeters,
            nearestStation = nearestStation,
            chainProgress = chainProgress,
            lateralOffset = lateral,
            targetDesignNorthing = tN,
            targetDesignEasting = tE,
            targetDesignElevation = tElev,
            deltaChain = dChain,
            deltaLateral = dLat,
            deltaElevation = dElev,
            withinChain = withinChain,
            withinLateral = withinLateral,
            withinElevation = withinElevation,
            withinAll = withinAll,
            statusMessage = msg
        )
    }

    private fun computePointFromChainageOffsetInternal(chainageMeters: Double, offset: Double): Triple<Double,Double,Double?> {
        val stations = _ui.value.roadPoints
        val chainages = stations.map { it.km * 1000.0 }
        if (stations.isEmpty()) return Triple(Double.NaN, Double.NaN, null)
        if (chainageMeters <= chainages.first()) {
            val s0 = stations.first(); val s1 = stations.getOrNull(1) ?: s0
            return offsetEndpoint(s0, s1, offset).let { Triple(it.first, it.second, s0.elevation) }
        }
        if (chainageMeters >= chainages.last()) {
            val sL = stations.last(); val prev = stations.getOrNull(stations.lastIndex-1) ?: sL
            return offsetEndpoint(sL, prev, offset).let { Triple(it.first, it.second, sL.elevation) }
        }
        var idx = 0
        while (idx < chainages.lastIndex && chainages[idx+1] < chainageMeters) idx++
        val ch0 = chainages[idx]; val ch1 = chainages[idx+1]
        val s0 = stations[idx]; val s1 = stations[idx+1]
        val seg = ch1 - ch0
        val t = if (seg <= 1e-9) 0.0 else (chainageMeters - ch0)/seg
        val n = s0.northing + (s1.northing - s0.northing) * t
        val e = s0.easting + (s1.easting - s0.easting) * t
        val z = s0.elevation + (s1.elevation - s0.elevation) * t
        val (nO, eO) = offsetVector(n,e,s0,s1,offset)
        return Triple(nO,eO,z)
    }

    // Projeksiyon: bu örnekte alignment doğrusal segmentler listesi
    private fun projectPointToAlignment(n: Double, e: Double, stations: List<RoadStation>): Triple<Double?, Double?, RoadStation?> {
        if (stations.size < 2) return Triple(null,null, stations.firstOrNull())
        var cumulative = 0.0
        var bestDist = Double.MAX_VALUE
        var bestChain: Double? = null
        var bestLateral: Double? = null
        var nearestStation: RoadStation? = null
        for (i in 0 until stations.lastIndex) {
            val s0 = stations[i]; val s1 = stations[i+1]
            val segN = s1.northing - s0.northing
            val segE = s1.easting - s0.easting
            val segLen2 = segN*segN + segE*segE
            if (segLen2 < 1e-9) continue
            val t = ((n - s0.northing) * segN + (e - s0.easting) * segE)/segLen2
            val tClip = t.coerceIn(0.0,1.0)
            val projN = s0.northing + segN * tClip
            val projE = s0.easting + segE * tClip
            val dn = n - projN
            val de = e - projE
            val dist = sqrt(dn*dn + de*de)
            if (dist < bestDist) {
                bestDist = dist
                val segLen = sqrt(segLen2)
                val chain = cumulative + segLen * tClip
                // Lateral işareti: sağ/sol (sağ pozitif);
                val un = -segE/segLen; val ue = segN/segLen
                val lateral = (n - projN)*un + (e - projE)*ue
                bestChain = chain
                bestLateral = lateral
                nearestStation = if (tClip < 0.5) s0 else s1
            }
            cumulative += sqrt(segLen2)
        }
        return Triple(bestChain, bestLateral, nearestStation)
    }

    private fun offsetEndpoint(base: RoadStation, other: RoadStation, offset: Double): Pair<Double, Double> {
        if (abs(offset) < 1e-9) return Pair(base.northing, base.easting)
        val dn = other.northing - base.northing
        val de = other.easting - base.easting
        val len = sqrt(dn*dn + de*de)
        if (len < 1e-9) return Pair(base.northing, base.easting + offset)
        val un = -de/len; val ue = dn/len
        return Pair(base.northing + un*offset, base.easting + ue*offset)
    }

    private fun offsetVector(n: Double, e: Double, s0: RoadStation, s1: RoadStation, offset: Double): Pair<Double, Double> {
        if (abs(offset) < 1e-9) return Pair(n,e)
        val dn = s1.northing - s0.northing
        val de = s1.easting - s0.easting
        val len = sqrt(dn*dn + de*de)
        if (len < 1e-9) return Pair(n, e+offset)
        val un = -de/len; val ue = dn/len
        return Pair(n + un*offset, e + ue*offset)
    }
}
