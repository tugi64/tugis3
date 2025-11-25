package com.example.tugis3.ui.map

import android.Manifest
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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.gnss.TrackRepository
import com.example.tugis3.gnss.model.FixType
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        val intentLat = intent?.getDoubleExtra("lat", Double.NaN)
        val intentLon = intent?.getDoubleExtra("lon", Double.NaN)
        if (intentLat != null && !intentLat.isNaN() && intentLon != null && !intentLon.isNaN()) {
            mapVm.setLocation(intentLat, intentLon)
        } else {
            obtainLastKnownLocationOrFallback()
        }
        setContent {
            MapScreen(
                onRequestLocation = { requestSingleFreshLocation() }
            )
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
                    val lm = getSystemService(LOCATION_SERVICE) as? LocationManager
                    if (lm != null) {
                        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                        var best: Location? = null
                        for (p in providers) {
                            try {
                                val l = lm.getLastKnownLocation(p) ?: continue
                                if (best == null || (l.accuracy < best.accuracy)) best = l
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

    var showSettings by remember { mutableStateOf(false) }

    // ViewModel fallback konumları
    val initLat by vm.latitude.collectAsState()
    val initLon by vm.longitude.collectAsState()
    val effectiveLat = latRepo ?: initLat
    val effectiveLon = lonRepo ?: initLon

    // WebView referansı
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var lastApplied by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun fixColor(ft: FixType?): String = when (ft) {
        FixType.RTK_FIX -> "#2E7D32"
        FixType.RTK_FLOAT -> "#FDD835"
        FixType.DGPS -> "#0288D1"
        FixType.PPP -> "#00897B"
        FixType.SINGLE -> "#FB8C00"
        FixType.MANUAL -> "#616161"
        FixType.NO_FIX, null -> "#D32F2F"
    }

    // Ana ekran düzeni - Grafik Alım benzeri
    Row(modifier = Modifier.fillMaxSize()) {
        // Sol Panel - Koordinat ve Kalite Bilgileri
        Card(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlık
                Text(
                    text = "HARITA UYGULAMASI",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                // Koordinat Bilgileri - Grafik Alım Stili
                Text(
                    text = "KOORDİNAT BİLGİLERİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                CoordinateDisplayItem(
                    label = "KUZEY (N)",
                    value = latRepo?.let { String.format(Locale.US, "%.8f", it) } ?: "---",
                    unit = "°"
                )

                CoordinateDisplayItem(
                    label = "DOĞU (E)",
                    value = lonRepo?.let { String.format(Locale.US, "%.8f", it) } ?: "---",
                    unit = "°"
                )

                CoordinateDisplayItem(
                    label = "YÜKSEKLİK",
                    value = pos.alt?.let { String.format(Locale.US, "%.3f", it) } ?: "---",
                    unit = "m"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Kalite Bilgileri - Grafik Alım Stili
                Text(
                    text = "KALİTE GÖSTERGELERİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                QualityDisplayItem(
                    label = "FIX TİPİ",
                    value = when(fix) {
                        FixType.RTK_FIX -> "RTK FIX"
                        FixType.RTK_FLOAT -> "RTK FLOAT"
                        FixType.DGPS -> "DGPS"
                        FixType.SINGLE -> "GPS 3D"
                        else -> "YOK"
                    },
                    isGood = fix == FixType.RTK_FIX
                )

                QualityDisplayItem(
                    label = "HDOP",
                    value = hdop?.let { String.format(Locale.US, "%.2f", it) } ?: "---",
                    isGood = (hdop ?: 99.0) < 2.0
                )

                QualityDisplayItem(
                    label = "UYDU SAYISI",
                    value = sats?.toString() ?: "0",
                    isGood = (sats ?: 0) >= 8
                )

                QualityDisplayItem(
                    label = "VERİ YAŞI",
                    value = String.format(Locale.US, "%.1f sn", ageSec),
                    isGood = ageSec < 5.0
                )

                Spacer(modifier = Modifier.weight(1f))

                // Kontrol Butonları
                Button(
                    onClick = onRequestLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("KONUM AL")
                }

                Button(
                    onClick = {
                        if (trackRecording) {
                            TrackRepository.stopRecording()
                        } else {
                            TrackRepository.startRecording()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (trackRecording) Color(0xFFE53935) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = if (trackRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (trackRecording) "İZ DURDUR" else "İZ KAYDI")
                }

                Button(
                    onClick = { showSettings = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("AYARLAR")
                }
            }
        }

        // Ortada - Harita Alanı
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
        ) {
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
                                    val js = "setOrMoveMarkerStyled(${effectiveLat}, ${effectiveLon}, 'Konum', '${fixColor(fix)}', 10.0);"
                                    view?.evaluateJavascript(js, null)
                                    lastApplied = effectiveLat to effectiveLon
                                }
                            }
                        }
                        loadUrl("file:///android_asset/map.html")
                    }
                },
                update = { view ->
                    if (effectiveLat != null && effectiveLon != null) {
                        val current = effectiveLat to effectiveLon
                        if (current != lastApplied) {
                            val js = "setOrMoveMarkerStyled(${effectiveLat}, ${effectiveLon}, 'Konum', '${fixColor(fix)}', 10.0);"
                            view.evaluateJavascript(js, null)
                            lastApplied = current
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Harita üzerinde koordinat göstergesi (sağ alt)
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "ANLIK KOORDİNAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lat: ${latRepo?.let { String.format(Locale.US, "%.6f", it) } ?: "---"}",
                        fontSize = 9.sp
                    )
                    Text(
                        text = "Lon: ${lonRepo?.let { String.format(Locale.US, "%.6f", it) } ?: "---"}",
                        fontSize = 9.sp
                    )
                    Text(
                        text = "Fix: ${fix?.name ?: "---"}",
                        fontSize = 9.sp,
                        color = when(fix) {
                            FixType.RTK_FIX -> Color(0xFF4CAF50)
                            FixType.RTK_FLOAT -> Color(0xFFFFC107)
                            else -> Color(0xFF757575)
                        }
                    )
                }
            }
        }

        // Sağ Panel - İz ve Mesafe Bilgileri
        Card(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "İZ KAYDI BİLGİLERİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                TrackInfoItem(
                    label = "DURUM",
                    value = if (trackRecording) "KAYIT EDİYOR" else "DURDU",
                    color = if (trackRecording) Color(0xFF4CAF50) else Color(0xFF757575)
                )

                TrackInfoItem(
                    label = "NOKTA SAYISI",
                    value = trackPoints.size.toString(),
                    color = Color(0xFF2196F3)
                )

                TrackInfoItem(
                    label = "TOPLAM MESAFE",
                    value = String.format(Locale.US, "%.2f m", totalDist),
                    color = Color(0xFF2196F3)
                )

                TrackInfoItem(
                    label = "RTK MESAFE",
                    value = String.format(Locale.US, "%.2f m", rtkDist),
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GPS Durum Özeti
                Text(
                    text = "GPS DURUM ÖZETİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when(fix) {
                            FixType.RTK_FIX -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            FixType.RTK_FLOAT -> Color(0xFFFFC107).copy(alpha = 0.1f)
                            else -> Color(0xFF757575).copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when(fix) {
                                FixType.RTK_FIX -> Icons.Default.GpsFixed
                                FixType.RTK_FLOAT -> Icons.Default.GpsNotFixed
                                else -> Icons.Default.GpsOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = when(fix) {
                                FixType.RTK_FIX -> Color(0xFF4CAF50)
                                FixType.RTK_FLOAT -> Color(0xFFFFC107)
                                else -> Color(0xFF757575)
                            }
                        )

                        Text(
                            text = when(fix) {
                                FixType.RTK_FIX -> "RTK FIX"
                                FixType.RTK_FLOAT -> "RTK FLOAT"
                                FixType.DGPS -> "DGPS"
                                FixType.SINGLE -> "GPS 3D"
                                else -> "GPS YOK"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when(fix) {
                                FixType.RTK_FIX -> Color(0xFF4CAF50)
                                FixType.RTK_FLOAT -> Color(0xFFFFC107)
                                else -> Color(0xFF757575)
                            }
                        )

                        Text(
                            text = "${sats ?: 0} Uydu",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Alt bilgi
                Text(
                    text = "© TUGIS Harita v3.0",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Ölçülen noktalar marker güncellemesi
    LaunchedEffect(measuredPointsJson) {
        val view = webViewRef.value ?: return@LaunchedEffect
        measuredPointsJson?.let {
            val esc = it.replace("\\", "\\\\").replace("'", "\\'")
            view.post { view.evaluateJavascript("updateMeasuredPoints('${esc}')", null) }
        }
    }

    // Dialog'lar
    if (showSettings) {
        MapSettingsDialog(
            onDismiss = { showSettings = false }
        )
    }
}

// Koordinat gösterim bileşeni
@Composable
private fun CoordinateDisplayItem(
    label: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value $unit",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Kalite gösterim bileşeni
@Composable
private fun QualityDisplayItem(
    label: String,
    value: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isGood) Color(0xFF4CAF50) else Color(0xFFFFC107)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGood) Color(0xFF4CAF50) else Color(0xFFFFC107)
            )
        }
    }
}

// Track bilgi bileşeni
@Composable
private fun TrackInfoItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Detay bilgi bileşeni
@Composable
private fun DetailInfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Harita ayarları dialog'u
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSettingsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Harita Ayarları") },
        text = {
            Column {
                Text("Harita Görünüm Ayarları:")
                Text("• Yakınlaştırma seviyesi")
                Text("• Track görünürlüğü")
                Text("• Marker stilleri")
                Text("• Koordinat formatı")
                Spacer(modifier = Modifier.height(8.dp))
                Text("GNSS Ayarları:")
                Text("• Fix tipi gösterimi")
                Text("• Doğruluk çemberi")
                Text("• Güncellenme sıklığı")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("TAMAM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("VARSAYILAN")
            }
        }
    )
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
