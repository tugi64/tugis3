package com.example.tugis3.ui.survey

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.coord.transform.NoOpTransformer
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.gnss.GnssEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.pow

/** Basit fotogrametri planlama + GCP/Gözlem kaydı ViewModel'i */
@HiltViewModel
class PhotogrammetryViewModel @Inject constructor(
    app: Application,
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository,
    private val surveyPointRepo: SurveyPointRepository,
    private val gnss: GnssEngine
) : AndroidViewModel(app) {

    // Konfigürasyon
    data class FlightConfig(
        val projectName: String = "PHOTO_PROJECT_001",
        val areaLengthM: Double = 500.0, // kuzey-güney yönünde varsayım
        val areaWidthM: Double = 300.0,  // doğu-batı yönünde varsayım
        val flightHeightM: Double = 100.0,
        val overlapForwardPct: Int = 80,
        val overlapSidePct: Int = 70,
        val cameraResolutionMp: Int = 20,
        val gsdRequiredCm: Double = 2.5
    )

    data class Waypoint(
        val index: Int,
        val lineIndex: Int,
        val e: Double,
        val n: Double,
        val altitude: Double,
        val isTurn: Boolean
    )

    data class Gcp(val pointName: String, val e: Double, val n: Double, val z: Double?)

    data class PhotoShot(
        val index: Int,
        val timestamp: Long,
        val lat: Double?,
        val lon: Double?,
        val ellH: Double?,
        val e: Double?,
        val n: Double?,
        val lineIndex: Int?,
        val wpIndex: Int?
    )

    data class Summary(
        val totalAreaHa: Double = 0.0,
        val estimatedPhotos: Int = 0,
        val estimatedFlightMinutes: Int = 0,
        val actualGsdCm: Double = 0.0,
        val meetsGsd: Boolean = true
    )

    data class UiState(
        val config: FlightConfig = FlightConfig(),
        val originE: Double? = null,
        val originN: Double? = null,
        val waypoints: List<Waypoint> = emptyList(),
        val gcps: List<Gcp> = emptyList(),
        val photoShots: List<PhotoShot> = emptyList(),
        val summary: Summary = Summary(),
        val status: String = "Hazır",
        val hasFix: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val observation = gnss.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val projectPoints: StateFlow<List<PointEntity>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else pointRepo.observePoints(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        gnss.start()
        // GNSS fix güncellendikçe hasFix ve origin (ilk plan üretiminde kullanılabilir) güncelle
        viewModelScope.launch {
            observation.collect { obs ->
                val hasFix = obs?.latDeg != null && obs.lonDeg != null
                _ui.update { it.copy(hasFix = hasFix) }
            }
        }
        recomputeSummary()
    }

    // --- Config güncellemeleri ---
    fun updateProjectName(v: String) = updateConfig { copy(projectName = v) }
    fun updateAreaLength(v: Double) = updateConfig { copy(areaLengthM = v) }
    fun updateAreaWidth(v: Double) = updateConfig { copy(areaWidthM = v) }
    fun updateFlightHeight(v: Double) = updateConfig { copy(flightHeightM = v) }
    fun updateForwardOverlap(v: Int) = updateConfig { copy(overlapForwardPct = v.coerceIn(50,95)) }
    fun updateSideOverlap(v: Int) = updateConfig { copy(overlapSidePct = v.coerceIn(50,95)) }
    fun updateCameraResolution(mp: Int) = updateConfig { copy(cameraResolutionMp = mp) }
    fun updateGsdRequired(cm: Double) = updateConfig { copy(gsdRequiredCm = cm) }

    private inline fun updateConfig(block: FlightConfig.() -> FlightConfig) {
        _ui.update { state ->
            val newCfg = block(state.config)
            state.copy(config = newCfg)
        }
        recomputeSummary()
    }

    private fun recomputeSummary() {
        val cfg = _ui.value.config
        // Çok basit GSD modeli: (yükseklik(cm) / (MP kök * sabit)). Burada sembolik.
        val actualGsdCm = (cfg.flightHeightM * 100.0) / (cfg.cameraResolutionMp.toDouble().pow(0.5) * 150.0)
        val totalAreaHa = (cfg.areaLengthM * cfg.areaWidthM) / 10000.0
        // Foto tahmini: grid spacing forward/side = footprint*(1-overlap)
        // Footprint kaba model: flightHeight * 0.7 (m) her iki yönde (temsili)
        val baseFootprint = cfg.flightHeightM * 0.7
        val forwardSpacing = baseFootprint * (1 - cfg.overlapForwardPct / 100.0)
        val sideSpacing = baseFootprint * (1 - cfg.overlapSidePct / 100.0)
        val lines = if (sideSpacing > 0) ceil(cfg.areaWidthM / sideSpacing) else 0.0
        val photosPerLine = if (forwardSpacing > 0) ceil(cfg.areaLengthM / forwardSpacing) else 0.0
        val estimatedPhotos = (lines * photosPerLine).toInt().coerceAtLeast(1)
        val estimatedTimeMin = (estimatedPhotos * 2 / 60.0).coerceAtLeast(1.0).toInt() // her foto 0.5s + dönüşler ~2s ortalama ~2s/foto
        val meets = actualGsdCm <= cfg.gsdRequiredCm
        _ui.update { it.copy(summary = Summary(totalAreaHa, estimatedPhotos, estimatedTimeMin, actualGsdCm, meets)) }
    }

    fun generatePlan() {
        val cfg = _ui.value.config
        val obs = observation.value
        if (obs?.latDeg == null || obs.lonDeg == null) {
            _ui.update { it.copy(status = "GNSS yok – plan üretilemedi") }
            return
        }
        val proj = activeProject.value
        val transformer = if (proj!=null) ProjectionEngine.forProject(proj) else NoOpTransformer
        val (originE, originN) = try {
            if (transformer !== NoOpTransformer) transformer.forward(obs.latDeg, obs.lonDeg) else (obs.lonDeg * 111000) to (obs.latDeg*111000)
        } catch (_:Exception) { (obs.lonDeg*111000) to (obs.latDeg*111000) }

        val baseFootprint = cfg.flightHeightM * 0.7
        val forwardSpacing = baseFootprint * (1 - cfg.overlapForwardPct / 100.0)
        val sideSpacing = baseFootprint * (1 - cfg.overlapSidePct / 100.0)
        if (forwardSpacing <= 0 || sideSpacing <= 0) {
            _ui.update { it.copy(status = "Geçersiz aralık hesaplandı") }
            return
        }
        val lineCount = ceil(cfg.areaWidthM / sideSpacing).toInt().coerceAtLeast(1)
        val photosPerLine = ceil(cfg.areaLengthM / forwardSpacing).toInt().coerceAtLeast(1)
        val waypoints = mutableListOf<Waypoint>()
        var wpIndex = 0
        for (line in 0 until lineCount) {
            val eOffset = line * sideSpacing
            val northForward = (0 until photosPerLine).map { it * forwardSpacing }
            val seq = if (line % 2 == 0) northForward else northForward.reversed()
            seq.forEachIndexed { idx, forward ->
                waypoints += Waypoint(
                    index = ++wpIndex,
                    lineIndex = line,
                    e = originE + eOffset,
                    n = originN + forward,
                    altitude = cfg.flightHeightM,
                    isTurn = (idx==0 || idx==seq.lastIndex)
                )
            }
        }
        _ui.update { it.copy(waypoints = waypoints, originE = originE, originN = originN, status = "Plan üretildi (${waypoints.size} WP)") }
    }

    fun clearPlan() { _ui.update { it.copy(waypoints = emptyList(), status = "Plan temizlendi") } }

    fun addGcpFromPoint(p: PointEntity) {
        if (_ui.value.gcps.any { it.pointName == p.name }) return
        _ui.update { state -> state.copy(gcps = state.gcps + Gcp(p.name, p.easting, p.northing, p.ellipsoidalHeight)) }
    }

    fun removeGcp(name: String) { _ui.update { it.copy(gcps = it.gcps.filterNot { g -> g.pointName == name }) } }

    fun capturePhoto() {
        val obs = observation.value ?: return
        val proj = activeProject.value
        if (obs.latDeg == null || obs.lonDeg == null) return
        val transformer = if (proj!=null) ProjectionEngine.forProject(proj) else NoOpTransformer
        val (e, n) = try {
            if (transformer !== NoOpTransformer) transformer.forward(obs.latDeg, obs.lonDeg) else (obs.lonDeg * 111000) to (obs.latDeg * 111000)
        } catch (_:Exception) { (obs.lonDeg * 111000) to (obs.latDeg * 111000) }
        val list = _ui.value.photoShots
        // En yakın waypoint (isteğe bağlı)
        val wp = _ui.value.waypoints.minByOrNull { hypot(it.e - e, it.n - n) }
        val shot = PhotoShot(
            index = list.size + 1,
            timestamp = System.currentTimeMillis(),
            lat = obs.latDeg,
            lon = obs.lonDeg,
            ellH = obs.ellipsoidalHeight,
            e = e,
            n = n,
            lineIndex = wp?.lineIndex,
            wpIndex = wp?.index
        )
        _ui.update { it.copy(photoShots = it.photoShots + shot, status = "Foto #${shot.index} kaydedildi") }
    }

    private fun exportDir(): File = File(getApplication<Application>().filesDir, "photogrammetry").apply { mkdirs() }

    fun exportPlanCsv(): Result<File> = runCatching {
        val f = File(exportDir(), timeStampName("plan", "csv"))
        f.printWriter().use { pw ->
            pw.println("index,line,e,n,alt,isTurn")
            _ui.value.waypoints.forEach { w -> pw.println("${w.index},${w.lineIndex},${w.e},${w.n},${w.altitude},${w.isTurn}") }
        }
        f
    }

    fun exportGcpCsv(): Result<File> = runCatching {
        val f = File(exportDir(), timeStampName("gcps", "csv"))
        f.printWriter().use { pw ->
            pw.println("name,e,n,z")
            _ui.value.gcps.forEach { g -> pw.println("${g.pointName},${g.e},${g.n},${g.z ?: ""}") }
        }
        f
    }

    fun exportPhotosCsv(): Result<File> = runCatching {
        val f = File(exportDir(), timeStampName("photos", "csv"))
        f.printWriter().use { pw ->
            pw.println("index,timestamp,lat,lon,ellH,e,n,line,wp")
            _ui.value.photoShots.forEach { s ->
                pw.println("${s.index},${s.timestamp},${s.lat ?: ""},${s.lon ?: ""},${s.ellH ?: ""},${s.e ?: ""},${s.n ?: ""},${s.lineIndex ?: ""},${s.wpIndex ?: ""}")
            }
        }
        f
    }

    private fun timeStampName(prefix: String, ext: String): String = buildString {
        append(prefix)
        append('_')
        append(SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()))
        append('.')
        append(ext)
    }

    override fun onCleared() {
        gnss.stop()
        super.onCleared()
    }
}
