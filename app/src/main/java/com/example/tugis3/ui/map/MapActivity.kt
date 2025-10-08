package com.example.tugis3.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.gnss.TrackRepository
import com.example.tugis3.gnss.model.FixType
import com.example.tugis3.settings.AppSettings
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.*

// ---- Top-level yardımcı fonksiyonlar ----
private fun douglasPeucker(points: List<TrackRepository.TrackPoint>, toleranceMeters: Double): List<TrackRepository.TrackPoint> {
    if (points.size < 3) return points
    val first = 0
    val last = points.size - 1
    val stack = ArrayDeque<Pair<Int, Int>>()
    val keep = BooleanArray(points.size)
    keep[first] = true; keep[last] = true
    fun perpendicularDistance(p: TrackRepository.TrackPoint, a: TrackRepository.TrackPoint, b: TrackRepository.TrackPoint): Double {
        val latRad = Math.toRadians((a.lat + b.lat)/2.0)
        val kx = 6371000.0 * Math.cos(latRad)
        val ky = 6371000.0
        val ax = a.lon * kx; val ay = a.lat * ky
        val bx = b.lon * kx; val by = b.lat * ky
        val px = p.lon * kx; val py = p.lat * ky
        val dx = bx - ax; val dy = by - ay
        if (dx == 0.0 && dy == 0.0) return Math.hypot(px-ax, py-ay)
        val t = ((px-ax)*dx + (py-ay)*dy) / (dx*dx + dy*dy)
        val tClamped = t.coerceIn(0.0, 1.0)
        val cx = ax + tClamped * dx; val cy = ay + tClamped * dy
        return Math.hypot(px - cx, py - cy)
    }
    stack.add(first to last)
    while (stack.isNotEmpty()) {
        val (s, e) = stack.removeLast()
        var maxDist = 0.0
        var index = -1
        val a = points[s]; val b = points[e]
        for (i in s+1 until e) {
            val d = perpendicularDistance(points[i], a, b)
            if (d > maxDist) { maxDist = d; index = i }
        }
        if (index != -1 && maxDist > toleranceMeters) {
            keep[index] = true
            stack.add(s to index)
            stack.add(index to e)
        }
    }
    val out = ArrayList<TrackRepository.TrackPoint>()
    for (i in points.indices) if (keep[i]) out.add(points[i])
    return out
}

private fun adaptiveSimplify(full: List<TrackRepository.TrackPoint>, scale: Double): List<TrackRepository.TrackPoint> {
    val size = full.size
    if (size <= 5000) return full
    val baseTol = when {
        size > 60000 -> 6.0
        size > 50000 -> 5.0
        size > 40000 -> 4.0
        size > 30000 -> 3.0
        size > 20000 -> 2.0
        size > 10000 -> 1.5
        else -> 1.0
    }
    val tol = baseTol * scale
    val simplified = douglasPeucker(full, tol)
    val MAX_DRAW = (8000 * scale.coerceAtLeast(0.5)).toInt().coerceAtMost(15000)
    return if (simplified.size > MAX_DRAW) simplified.takeLast(MAX_DRAW) else simplified
}

private fun hybridSample(points: List<TrackRepository.TrackPoint>, baseTol: Double, scale: Double): List<TrackRepository.TrackPoint> {
    if (points.size < 3) return points
    val minStepMeters = (baseTol * scale * 0.7).coerceAtLeast(0.5)
    val out = ArrayList<TrackRepository.TrackPoint>(points.size)
    var last = points.first()
    out.add(last)
    var acc = 0.0
    for (i in 1 until points.size - 1) {
        val p = points[i]
        val d = haversineQuick(last.lat, last.lon, p.lat, p.lon)
        acc += d
        val next = points[i+1]
        val turn = run {
            val a1 = Math.toRadians(p.lat - last.lat)
            val b1 = Math.toRadians(p.lon - last.lon)
            val a2 = Math.toRadians(next.lat - p.lat)
            val b2 = Math.toRadians(next.lon - p.lon)
            val dot = (a1 * a2 + b1 * b2)
            val mag1 = Math.hypot(a1, b1)
            val mag2 = Math.hypot(a2, b2)
            if (mag1 == 0.0 || mag2 == 0.0) 0.0 else dot / (mag1 * mag2)
        }
        val angleSharp = turn < 0.98
        if (acc >= minStepMeters || angleSharp) {
            out.add(p)
            last = p
            acc = 0.0
        }
    }
    out.add(points.last())
    return out
}

private fun adaptiveSimplifyHybrid(full: List<TrackRepository.TrackPoint>, scale: Double): List<TrackRepository.TrackPoint> {
    val size = full.size
    if (size <= 5000) return full
    val baseTol = when {
        size > 120000 -> 7.0
        size > 90000 -> 6.0
        size > 60000 -> 5.0
        size > 40000 -> 4.0
        size > 30000 -> 3.0
        size > 20000 -> 2.0
        size > 10000 -> 1.5
        else -> 1.0
    }
    val dp = douglasPeucker(full, baseTol * scale)
    val hybrid = hybridSample(dp, baseTol, scale)
    val MAX_DRAW = (10000 * scale.coerceAtLeast(0.5)).toInt().coerceAtMost(20000)
    return if (hybrid.size > MAX_DRAW) hybrid.takeLast(MAX_DRAW) else hybrid
}
// ---- Yardımcı fonksiyonlar sonu ----

class MapActivity : ComponentActivity() {
    private lateinit var mapVm: MapViewModel
    private val locationPermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) obtainLastKnownLocationOrFallback() else fallbackTurkeyIfEmpty()
        }
    }
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var liveLocationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapVm = ViewModelProvider(this)[MapViewModel::class.java]

        // Intent ile gelen koordinat varsa önce onu set et.
        val intentLat = intent?.getDoubleExtra("lat", Double.NaN)
        val intentLon = intent?.getDoubleExtra("lon", Double.NaN)
        if (intentLat != null && !intentLat.isNaN() && intentLon != null && !intentLon.isNaN()) {
            mapVm.setLocation(intentLat, intentLon)
        } else {
            obtainLastKnownLocationOrFallback()
        }

        setContent {
            MapScreen(onRequestLocation = { requestSingleFreshLocation() })
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun obtainLastKnownLocationOrFallback() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    mapVm.setLocation(loc.latitude, loc.longitude)
                } else {
                    // Eski yöntemle fallback dene
                    val lm = getSystemService(LOCATION_SERVICE) as? LocationManager
                    if (lm != null) {
                        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                        var best: Location? = null
                        for (p in providers) {
                            try {
                                val l = lm.getLastKnownLocation(p) ?: continue
                                if (best == null || (l.accuracy < best!!.accuracy)) best = l
                            } catch (_: SecurityException) {}
                        }
                        if (best != null) {
                            mapVm.setLocation(best.latitude, best.longitude)
                        } else fallbackTurkeyIfEmpty()
                    } else fallbackTurkeyIfEmpty()
                }
            }.addOnFailureListener { fallbackTurkeyIfEmpty() }
        } catch (_: SecurityException) { fallbackTurkeyIfEmpty() }
    }

    private fun fallbackTurkeyIfEmpty() {
        if (mapVm.latitude.value == null || mapVm.longitude.value == null) {
            // Türkiye (Ankara civarı) fallback
            mapVm.setLocation(39.0, 35.0)
        }
    }

    private fun requestSingleFreshLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        // Mevcut callback varsa iptal et
        liveLocationCallback?.let { fusedClient.removeLocationUpdates(it) }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdates(1)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) mapVm.setLocation(loc.latitude, loc.longitude)
                fusedClient.removeLocationUpdates(this)
                liveLocationCallback = null
            }
        }
        liveLocationCallback = callback
        try {
            fusedClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // izin reddedilmiş olabilir
            return
        }
        // 5 sn timeout fallback
        lifecycleScope.launch {
            delay(5000)
            if (liveLocationCallback != null) {
                fusedClient.removeLocationUpdates(callback)
                liveLocationCallback = null
            }
        }
    }

    // onRequestPermissionsResult kaldırıldı (Activity Result API kullanılıyor)
}

@Composable
fun MapScreen(onRequestLocation: () -> Unit = {}, vm: MapViewModel = viewModel(), measuredPointsJson: String? = null) {
    // GNSS konumu
    val pos by GnssPositionRepository.position.collectAsState()
    val latRepo = pos.lat
    val lonRepo = pos.lon
    val hdop = pos.hdop
    val fix = pos.fixType
    val sats = pos.satsInUse
    val ageSec = remember(pos.timestamp) { ((System.currentTimeMillis() - pos.timestamp) / 1000.0) }

    // Track state
    val trackPoints by TrackRepository.points.collectAsState()
    val trackRecording by TrackRepository.recording.collectAsState()
    val totalDist by TrackRepository.totalDistance.collectAsState()
    val rtkDist by TrackRepository.rtkFixDistance.collectAsState()
    val floatDist by TrackRepository.rtkFloatDistance.collectAsState()
    val exportSuggest by TrackRepository.exportSuggestion.collectAsState()
    val startTime by TrackRepository.startTime.collectAsState()
    val endTime by TrackRepository.endTime.collectAsState()
    val repoDuration by TrackRepository.durationMillis.collectAsState()

    // Added smoothing window state (default 30s)
    var smoothWindowSec by remember { mutableStateOf(30) }

    // Canlı süre tetiklemek için her saniye artacak bir "tick" state'i
    var liveTick by remember { mutableStateOf(0L) }
    LaunchedEffect(trackRecording, startTime) {
        // Kayıt sürerken saniyelik timer
        while (trackRecording && startTime != null) {
            delay(1000)
            liveTick++
        }
    }

    // Canlı süre hesaplama (tick değiştikçe recomposition tetiklenir)
    val liveDurationMillis = remember(trackRecording, startTime, endTime, repoDuration, liveTick) {
        when {
            trackRecording && startTime != null -> System.currentTimeMillis() - startTime!!
            else -> repoDuration
        }
    }

    // Ortalama hız (m/s ve km/h)
    val avgSpeedMs = if (liveDurationMillis != null && liveDurationMillis > 0L) totalDist / (liveDurationMillis / 1000.0) else 0.0
    val avgSpeedKmh = avgSpeedMs * 3.6
    val scope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val simplifyScale by AppSettings.simplifyScaleFlow(ctx).collectAsState(initial = 1.0)
    val scopeSettings = rememberCoroutineScope()

    // WebView referansları marker / polyline güncelleme için önce gerekli
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var lastApplied by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var lastFixHash by remember { mutableStateOf(0) }
    var lastTrackSize by remember { mutableStateOf(0) }

    fun shareFile(file: java.io.File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, "com.example.tugis3.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Paylaş"))
    }
    fun exportTrackGpx() { scope.launch { TrackRepository.exportGpx(ctx, simplifyScale).onSuccess { f -> exportMessage = "GPX export: ${f.name}"; shareFile(f, "application/gpx+xml") }.onFailure { e -> exportMessage = "GPX export hata: ${e.message}" } } }
    fun exportTrackCsv() { scope.launch { TrackRepository.exportCsv(ctx).onSuccess { f -> exportMessage = "CSV export: ${f.name}"; shareFile(f, "text/csv") }.onFailure { e -> exportMessage = "CSV export hata: ${e.message}" } } }
    fun exportTrackZip() { scope.launch { TrackRepository.exportBothZip(ctx, simplifyScale).onSuccess { f -> exportMessage = "ZIP export: ${f.name}"; shareFile(f, "application/zip") }.onFailure { e -> exportMessage = "ZIP export hata: ${e.message}" } } }

    // ViewModel fallback konumları
    val initLat by vm.latitude.collectAsState()
    val initLon by vm.longitude.collectAsState()
    val effectiveLat = latRepo ?: initLat
    val effectiveLon = lonRepo ?: initLon

    fun fixColor(ft: FixType?): String = when (ft) {
        FixType.RTK_FIX -> "#2E7D32"
        FixType.RTK_FLOAT -> "#FDD835"
        FixType.DGPS -> "#0288D1"
        FixType.PPP -> "#00897B"
        FixType.SINGLE -> "#FB8C00"
        FixType.MANUAL -> "#616161"
        FixType.NO_FIX, null -> "#D32F2F"
    }
    fun estAccuracyMeters(ft: FixType?, hd: Double?): Double? {
        if (ft == FixType.NO_FIX || ft == null) return null
        val h = hd ?: return when (ft) {
            FixType.RTK_FIX -> 0.03
            FixType.RTK_FLOAT -> 0.4
            FixType.DGPS -> 1.5
            FixType.PPP -> 0.2
            else -> 5.0
        }
        val base = when (ft) {
            FixType.RTK_FIX -> h * 0.5
            FixType.RTK_FLOAT -> h * 1.0
            FixType.DGPS -> h * 3.0
            FixType.PPP -> h * 1.2
            FixType.SINGLE, FixType.MANUAL -> h * 5.0
            FixType.NO_FIX -> return null
        }
        return base.coerceIn(0.02, 25.0)
    }
    val acc = estAccuracyMeters(fix, hdop)
    val panelColor = when {
        ageSec > 30 -> Color(0xFFB71C1C).copy(alpha = 0.78f)
        ageSec > 10 -> Color(0xFFF57F17).copy(alpha = 0.78f)
        else -> Color(0xFF212121).copy(alpha = 0.75f)
    }

    // Track polyline incremental güncellemesi
    LaunchedEffect(trackPoints) {
        val view = webViewRef.value ?: return@LaunchedEffect
        if (trackPoints.isEmpty()) {
            // temizle
            view.post { view.evaluateJavascript("updateTrackSegments('[]')", null) }
            lastTrackSize = 0
            return@LaunchedEffect
        }
        // Büyük track durumunda ( >5000 ) incremental çizimi tamamen atla
        if (trackPoints.size > 5000) return@LaunchedEffect
        val needsSimplify = false // forced false since >5000 return edildi
        if (needsSimplify) {
            // Büyük veri: her seferinde simplify + tam çizim (incremental karmaşıklığından kaçın)
            val drawList = adaptiveSimplify(trackPoints, simplifyScale)
            val json = buildString {
                append('[')
                drawList.forEachIndexed { idx, p ->
                    if (idx>0) append(',')
                    append('{')
                    append("\"lat\":").append(p.lat).append(',')
                    append("\"lon\":").append(p.lon).append(',')
                    append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"")
                    append('}')
                }
                append(']')
            }
            val esc = json.replace("\\", "\\\\").replace("'", "\\'")
            view.post { view.evaluateJavascript("updateTrackSegments('${esc}')", null) }
            lastTrackSize = drawList.size // sadece referans
        } else {
            // Mevcut incremental mantık
            val slice = trackPoints // 5000 altı - hepsi
            if (lastTrackSize == 0 || slice.size < lastTrackSize || (slice.size - lastTrackSize) > 200) {
                val fullJson = buildString {
                    append('[')
                    slice.forEachIndexed { idx, p ->
                        if (idx>0) append(',')
                        append('{')
                        append("\"lat\":").append(p.lat).append(',')
                        append("\"lon\":").append(p.lon).append(',')
                        append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"")
                        append('}')
                    }
                    append(']')
                }
                val esc = fullJson.replace("\\", "\\\\").replace("'", "\\'")
                view.post { view.evaluateJavascript("updateTrackSegments('${esc}')", null) }
                lastTrackSize = slice.size
            } else if (slice.size > lastTrackSize) {
                // incremental: önceki son nokta + yeni noktalar
                val fromIdx = (lastTrackSize - 1).coerceAtLeast(0)
                val inc = slice.subList(fromIdx, slice.size)
                if (inc.size >= 2) {
                    val json = buildString {
                        append('[')
                        inc.forEachIndexed { idx, p ->
                            if (idx>0) append(',')
                            append('{')
                            append("\"lat\":").append(p.lat).append(',')
                            append("\"lon\":").append(p.lon).append(',')
                            append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"")
                            append('}')
                        }
                        append(']')
                    }
                    val esc = json.replace("\\", "\\\\").replace("'", "\\'")
                    view.post { view.evaluateJavascript("appendTrackSegments('${esc}')", null) }
                }
                lastTrackSize = slice.size
            }
        }
    }

    // Down-sampling (Douglas-Peucker) büyük tracklerde
    // ---- Yardımcı lokal fonksiyonlar (ilk kullanımdan önce) ----
    fun douglasPeucker(points: List<TrackRepository.TrackPoint>, toleranceMeters: Double): List<TrackRepository.TrackPoint> {
        if (points.size < 3) return points
        // Rekürsif değil iterative stack yaklaşımı
        val first = 0
        val last = points.size - 1
        val stack = ArrayDeque<Pair<Int, Int>>()
        val keep = BooleanArray(points.size)
        keep[first] = true; keep[last] = true

        fun perpendicularDistance(p: TrackRepository.TrackPoint, a: TrackRepository.TrackPoint, b: TrackRepository.TrackPoint): Double {
            // Convert to meters using simple equirectangular projection around segment midpoint for speed
            val latRad = Math.toRadians((a.lat + b.lat)/2.0)
            val kx = 6371000.0 * Math.cos(latRad)
            val ky = 6371000.0
            val ax = a.lon * kx; val ay = a.lat * ky
            val bx = b.lon * kx; val by = b.lat * ky
            val px = p.lon * kx; val py = p.lat * ky
            val dx = bx - ax; val dy = by - ay
            if (dx == 0.0 && dy == 0.0) return Math.hypot(px-ax, py-ay)
            val t = ((px-ax)*dx + (py-ay)*dy) / (dx*dx + dy*dy)
            val tClamped = t.coerceIn(0.0, 1.0)
            val cx = ax + tClamped * dx; val cy = ay + tClamped * dy
            return Math.hypot(px - cx, py - cy)
        }
        stack.add(first to last)
        while (stack.isNotEmpty()) {
            val (s, e) = stack.removeLast()
            var maxDist = 0.0
            var index = -1
            val a = points[s]; val b = points[e]
            for (i in s+1 until e) {
                val d = perpendicularDistance(points[i], a, b)
                if (d > maxDist) { maxDist = d; index = i }
            }
            if (index != -1 && maxDist > toleranceMeters) {
                keep[index] = true
                stack.add(s to index)
                stack.add(index to e)
            }
        }
        val out = ArrayList<TrackRepository.TrackPoint>()
        for (i in points.indices) if (keep[i]) out.add(points[i])
        return out
    }
    fun adaptiveSimplify(full: List<TrackRepository.TrackPoint>, scale: Double): List<TrackRepository.TrackPoint> {
        val size = full.size
        if (size <= 5000) return full
        val baseTol = when {
            size > 60000 -> 6.0
            size > 50000 -> 5.0
            size > 40000 -> 4.0
            size > 30000 -> 3.0
            size > 20000 -> 2.0
            size > 10000 -> 1.5
            else -> 1.0
        }
        val tol = baseTol * scale
        val simplified = douglasPeucker(full, tol)
        val MAX_DRAW = (8000 * scale.coerceAtLeast(0.5)).toInt().coerceAtMost(15000)
        return if (simplified.size > MAX_DRAW) simplified.takeLast(MAX_DRAW) else simplified
    }
    fun hybridSample(points: List<TrackRepository.TrackPoint>, baseTol: Double, scale: Double): List<TrackRepository.TrackPoint> {
        if (points.size < 3) return points
        val minStepMeters = (baseTol * scale * 0.7).coerceAtLeast(0.5)
        val out = ArrayList<TrackRepository.TrackPoint>(points.size)
        var last = points.first()
        out.add(last)
        var acc = 0.0
        for (i in 1 until points.size - 1) {
            val p = points[i]
            val d = haversineQuick(last.lat, last.lon, p.lat, p.lon)
            acc += d
            // Eğrilik (basit açı testi)
            val next = points[i+1]
            val turn = kotlin.run {
                val a1 = Math.toRadians(p.lat - last.lat)
                val b1 = Math.toRadians(p.lon - last.lon)
                val a2 = Math.toRadians(next.lat - p.lat)
                val b2 = Math.toRadians(next.lon - p.lon)
                val dot = (a1 * a2 + b1 * b2)
                val mag1 = Math.hypot(a1, b1)
                val mag2 = Math.hypot(a2, b2)
                if (mag1 == 0.0 || mag2 == 0.0) 0.0 else dot / (mag1 * mag2)
            }
            val angleSharp = turn < 0.98 // yaklaşık > ~11 derece
            if (acc >= minStepMeters || angleSharp) {
                out.add(p)
                last = p
                acc = 0.0
            }
        }
        out.add(points.last())
        return out
    }
    fun adaptiveSimplifyHybrid(full: List<TrackRepository.TrackPoint>, scale: Double): List<TrackRepository.TrackPoint> {
        val size = full.size
        if (size <= 5000) return full
        val baseTol = when {
            size > 120000 -> 7.0
            size > 90000 -> 6.0
            size > 60000 -> 5.0
            size > 40000 -> 4.0
            size > 30000 -> 3.0
            size > 20000 -> 2.0
            size > 10000 -> 1.5
            else -> 1.0
        }
        // İlk pass: Douglas-Peucker
        val dp = douglasPeucker(full, baseTol * scale)
        // İkinci pass: hibrit yoğunluk / eğrilik (uygulama)
        val hybrid = hybridSample(dp, baseTol, scale)
        val MAX_DRAW = (10000 * scale.coerceAtLeast(0.5)).toInt().coerceAtMost(20000)
        return if (hybrid.size > MAX_DRAW) hybrid.takeLast(MAX_DRAW) else hybrid
    }
    // ---- Yardımcı fonksiyonlar sonu ----

    // Hareketli (kullanıcı seçimi) ortalama hız hesaplama
    val smoothSpeedKmh = remember(trackPoints, liveTick, smoothWindowSec) {
        val windowMs = smoothWindowSec * 1000L
        if (trackPoints.size < 2) 0.0 else {
            val lastTime = trackPoints.last().time
            val cutoff = lastTime - windowMs
            var idx = trackPoints.size - 1
            while (idx > 0 && trackPoints[idx].time >= cutoff) idx--
            val window = trackPoints.subList(idx.coerceAtLeast(0), trackPoints.size)
            if (window.size < 2) 0.0 else {
                var dist = 0.0
                fun hv(a: TrackRepository.TrackPoint, b: TrackRepository.TrackPoint): Double {
                    val R = 6371000.0
                    val dLat = Math.toRadians(b.lat - a.lat)
                    val dLon = Math.toRadians(b.lon - a.lon)
                    val aa = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(a.lat))*Math.cos(Math.toRadians(b.lat))*Math.sin(dLon/2)*Math.sin(dLon/2)
                    val c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1-aa))
                    return R * c
                }
                for (i in 1 until window.size) dist += hv(window[i-1], window[i])
                val dtMs = (window.last().time - window.first().time).coerceAtLeast(1L)
                (dist / (dtMs / 1000.0)) * 3.6
            }
        }
    }

    // Instant hız (son 3 sn) hesaplama
    val instSpeedKmh = remember(trackPoints) {
        if (trackPoints.size < 2) 0.0 else {
            val latest = trackPoints.last()
            val cutoff = latest.time - 3000
            var idx = trackPoints.size - 2
            while (idx >= 0 && trackPoints[idx].time > cutoff) idx--
            val refIdx = (idx + 1).coerceAtMost(trackPoints.size - 2).coerceAtLeast(0)
            val ref = trackPoints[refIdx]
            val dt = (latest.time - ref.time).coerceAtLeast(1L) / 1000.0
            val d = haversineQuick(ref.lat, ref.lon, latest.lat, latest.lon)
            (d / dt) * 3.6
        }
    }

    // Çizim: büyük track simplify işlemini arka planda yap
    LaunchedEffect(trackPoints, simplifyScale) {
        val view = webViewRef.value ?: return@LaunchedEffect
        if (trackPoints.isEmpty()) {
            view.post { view.evaluateJavascript("updateTrackSegments('[]')", null) }
            lastTrackSize = 0
            return@LaunchedEffect
        }
        if (trackPoints.size > 5000) {
            val snapshot = trackPoints.toList()
            val escaped = withContext(Dispatchers.Default) {
                val drawList = adaptiveSimplify(snapshot, simplifyScale)
                val json = buildString {
                    append('[')
                    drawList.forEachIndexed { idx, p ->
                        if (idx>0) append(',')
                        append('{')
                        append("\"lat\":").append(p.lat).append(',')
                        append("\"lon\":").append(p.lon).append(',')
                        append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"")
                        append('}')
                    }
                    append(']')
                }
                json.replace("\\", "\\\\").replace("'", "\\'")
            }
            view.post { view.evaluateJavascript("updateTrackSegments('${escaped}')", null) }
            lastTrackSize = trackPoints.size
        } else {
            // küçük dataset incremental (mevcut mantık) -> tekrar kullan
            val slice = trackPoints
            if (lastTrackSize == 0 || slice.size < lastTrackSize || (slice.size - lastTrackSize) > 200) {
                val fullJson = buildString {
                    append('[')
                    slice.forEachIndexed { idx, p ->
                        if (idx>0) append(',')
                        append('{')
                        append("\"lat\":").append(p.lat).append(',')
                        append("\"lon\":").append(p.lon).append(',')
                        append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"")
                        append('}')
                    }
                    append(']')
                }
                val esc = fullJson.replace("\\", "\\\\").replace("'", "\\'")
                view.post { view.evaluateJavascript("updateTrackSegments('${esc}')", null) }
                lastTrackSize = slice.size
            } else if (slice.size > lastTrackSize) {
                val fromIdx = (lastTrackSize - 1).coerceAtLeast(0)
                val inc = slice.subList(fromIdx, slice.size)
                if (inc.size >= 2) {
                    val json = buildString {
                        append('[')
                        inc.forEachIndexed { idx, p ->
                            if (idx>0) append(',')
                            append('{')
                            append("\"lat\":").append(p.lat).append(',')
                            append("\"lon\":").append(p.lon).append(',')
                            append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"")
                            append('}')
                        }
                        append(']')
                    }
                    val esc = json.replace("\\", "\\\\").replace("'", "\\'")
                    view.post { view.evaluateJavascript("appendTrackSegments('${esc}')", null) }
                }
                lastTrackSize = slice.size
            }
        }
    }

    // Throttle state
    val lastSimplifyEpoch = remember { mutableStateOf(0L) }
    val cachedLargeEscaped = remember { mutableStateOf<String?>(null) }

    // Büyük track hibrit + DP birleşik adaptif (Sadece üstteki tanım kullanılacak)
    // Throttle + hibrit simplify (büyük track) arka plan
    LaunchedEffect(trackPoints, simplifyScale) {
        val view = webViewRef.value ?: return@LaunchedEffect
        if (trackPoints.isEmpty()) {
            view.post { view.evaluateJavascript("updateTrackSegments('[]')", null) }
            lastTrackSize = 0; cachedLargeEscaped.value = null
            return@LaunchedEffect
        }
        if (trackPoints.size <= 5000) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - lastSimplifyEpoch.value < 2000 && cachedLargeEscaped.value != null) return@LaunchedEffect
        val snapshot = trackPoints.toList()
        val escaped = withContext(Dispatchers.Default) {
            val drawList = adaptiveSimplifyHybrid(snapshot, simplifyScale)
            val json = buildString {
                append('[')
                drawList.forEachIndexed { idx, p ->
                    if (idx>0) append(',')
                    append('{')
                    append("\"lat\":").append(p.lat).append(',')
                    append("\"lon\":").append(p.lon).append(',')
                    append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"")
                    append('}')
                }
                append(']')
            }
            json.replace("\\", "\\\\").replace("'", "\\'")
        }
        cachedLargeEscaped.value = escaped
        lastSimplifyEpoch.value = now
        view.post { view.evaluateJavascript("updateTrackSegments('${escaped}')", null) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Map View", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onRequestLocation() }) { Text("Konumumu Göster") }
            }
        }
        Column(
            Modifier.fillMaxWidth().wrapContentHeight().background(panelColor).padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val fixName = fix?.displayName ?: "Fix Yok"
            val info = buildString {
                append(fixName)
                if (sats != null) append(" | Uydu: $sats")
                if (hdop != null) append(" | HDOP: ${"%.1f".format(hdop)}")
                if (acc != null) append(" | ±${"%.2f".format(acc)} m")
                append(" | Yaş: ${"%.1f".format(ageSec)} sn")
                append(" | Dist: ${"%.1f".format(totalDist)} m")
                if (rtkDist > 0.0) append(" | RTK: ${"%.1f".format(rtkDist)} m")
                if (floatDist > 0.0) append(" | FLOAT: ${"%.1f".format(floatDist)} m")
                if (liveDurationMillis != null && liveDurationMillis > 0) {
                    val durSec = liveDurationMillis / 1000.0
                    append(" | Süre: ${formatDuration(liveDurationMillis)}")
                    if (avgSpeedMs > 0.001) append(" | Avg: ${"%.2f".format(avgSpeedKmh)} km/h")
                    if (smoothSpeedKmh > 0.001) append(" | Avg${smoothWindowSec}s: ${"%.2f".format(smoothSpeedKmh)} km/h")
                }
                if (instSpeedKmh > 0.05) append(" | Inst: ${"%.2f".format(instSpeedKmh)} km/h")
                if (trackRecording) append(" | Track Pts: ${trackPoints.size}")
                append(" | Scl:${"%.2f".format(simplifyScale)}")
            }
            Text(info, color = Color.White, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = trackRecording,
                    onClick = { TrackRepository.setRecording(!trackRecording) },
                    label = { Text(if (trackRecording) "Kayıt AÇIK" else "Track Kaydı") },
                    leadingIcon = { Icon(if (trackRecording) Icons.Filled.FiberManualRecord else Icons.Filled.Timeline, contentDescription = null) }
                )
                OutlinedButton(onClick = { exportTrackGpx() }, enabled = trackPoints.isNotEmpty()) { Text("GPX") }
                OutlinedButton(onClick = { exportTrackCsv() }, enabled = trackPoints.isNotEmpty()) { Text("CSV") }
                OutlinedButton(onClick = { exportTrackZip() }, enabled = trackPoints.isNotEmpty()) { Text("ZIP") }
                OutlinedButton(onClick = { scope.launch { TrackRepository.clear() } }, enabled = trackPoints.isNotEmpty()) { Text("Temizle") }
            }
            // Hız penceresi seçim satırı
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("AvgWin:", color = Color.White, style = MaterialTheme.typography.labelSmall)
                listOf(10,30,60).forEach { sec ->
                    AssistChip(
                        onClick = { smoothWindowSec = sec },
                        label = { Text("${sec}s") },
                        enabled = smoothWindowSec != sec,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = if (smoothWindowSec == sec) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = if (smoothWindowSec == sec) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            // Simplify ölçek chipleri
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text("Simplify:", color = Color.White, style = MaterialTheme.typography.labelSmall)
                listOf(0.5,1.0,2.0).forEach { sc ->
                    AssistChip(
                        onClick = { scopeSettings.launch { AppSettings.setSimplifyScale(ctx, sc) } },
                        label = { Text("${sc}x") },
                        enabled = simplifyScale != sc,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = if (simplifyScale == sc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = if (simplifyScale == sc) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            exportMessage?.let { msg -> Text(msg, color = Color.LightGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp)) }
        }
        @SuppressLint("SetJavaScriptEnabled")
        AndroidView(
            factory = { c ->
                WebView(c).apply {
                    webViewRef.value = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (effectiveLat != null && effectiveLon != null) {
                                val js = "setOrMoveMarkerStyled(${effectiveLat}, ${effectiveLon}, 'Konum', '${fixColor(fix)}', ${acc ?: 0.0});"
                                view?.evaluateJavascript(js, null)
                                lastApplied = effectiveLat to effectiveLon
                                lastFixHash = (fix?.ordinal ?: -1)
                            }
                            // İlk açılışta segmentli çizim
                            if (trackPoints.isNotEmpty()) {
                                val slice = if (trackPoints.size > 5000) trackPoints.takeLast(5000) else trackPoints
                                val json = buildString {
                                    append('[')
                                    slice.forEachIndexed { idx, p -> if (idx>0) append(','); append('{').append("\"lat\":").append(p.lat).append(',').append("\"lon\":").append(p.lon).append(',').append("\"fixType\":\"").append(p.fixType?.name ?: "").append("\"").append('}') }
                                    append(']')
                                }
                                val esc = json.replace("\\", "\\\\").replace("'", "\\'")
                                view?.evaluateJavascript("updateTrackSegments('${esc}')", null)
                            }
                        }
                    }
                    loadUrl("file:///android_asset/map.html")
                }
            },
            update = { view ->
                if (effectiveLat != null && effectiveLon != null) {
                    val current = effectiveLat to effectiveLon
                    val hash = (fix?.ordinal ?: -1)
                    if (current != lastApplied || hash != lastFixHash) {
                        val js = "setOrMoveMarkerStyled(${effectiveLat}, ${effectiveLon}, 'Konum', '${fixColor(fix)}', ${acc ?: 0.0});"
                        view.evaluateJavascript(js, null)
                        lastApplied = current
                        lastFixHash = hash
                    }
                }
            },
            modifier = Modifier.weight(1f)
        )
    }

    // Ölçülen noktalar marker güncellemesi
    LaunchedEffect(measuredPointsJson) {
        val view = webViewRef.value ?: return@LaunchedEffect
        measuredPointsJson?.let {
            val esc = it.replace("\\", "\\\\").replace("'", "\\'")
            view.post { view.evaluateJavascript("updateMeasuredPoints('${esc}')", null) }
        }
    }

    if (exportSuggest) {
        AlertDialog(
            onDismissRequest = { TrackRepository.consumeExportSuggestion() },
            title = { Text("Track Kaydı Tamamlandı") },
            text = {
                val durTxt = liveDurationMillis?.let { formatDuration(it) } ?: "--"
                val instPart = if (instSpeedKmh > 0.05) " | Inst: ${"%.2f".format(instSpeedKmh)} km/h" else ""
                Text("Toplam: ${"%.1f".format(totalDist)} m\nRTK: ${"%.1f".format(rtkDist)} m | FLOAT: ${"%.1f".format(floatDist)} m\nSüre: $durTxt | Avg: ${"%.2f".format(avgSpeedKmh)} km/h | Avg${smoothWindowSec}s: ${"%.2f".format(smoothSpeedKmh)} km/h$instPart\nDışa aktarmak ister misiniz?")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { TrackRepository.consumeExportSuggestion(); exportTrackGpx() }) { Text("GPX") }
                    TextButton(onClick = { TrackRepository.consumeExportSuggestion(); exportTrackCsv() }) { Text("CSV") }
                    TextButton(onClick = { TrackRepository.consumeExportSuggestion(); exportTrackZip() }) { Text("ZIP") }
                    TextButton(onClick = { TrackRepository.consumeExportSuggestion() }) { Text("Kapat") }
                }
            }
        )
    }
}

// Yardımcı süre formatlayıcı
private fun formatDuration(millis: Long): String {
    val totalSec = millis / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        m > 0 -> String.format(Locale.US, "%d:%02d", m, s)
        else -> String.format(Locale.US, "%ds", s)
    }
}

private fun haversineQuick(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat/2).pow(2.0) + cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    return R * c
}
