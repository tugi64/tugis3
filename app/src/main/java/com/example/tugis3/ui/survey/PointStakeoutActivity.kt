@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.survey

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.ui.map.MapScreen
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import android.content.Context

@AndroidEntryPoint
class PointStakeoutActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            // İzinler işlendi
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                PointStakeoutScreen(onBack = { finish() })
            }
        }
        ensurePermissions()
    }

    private fun ensurePermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

// Kalıcı ölçüm noktaları veri sınıfı
private data class MeasuredPoint(
    val time: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double?, // ellipsoidal
    val easting: Double,
    val northing: Double,
    val zone: Int,
    val northHemisphere: Boolean,
    val name: String?,
    val desc: String?
)

// Kalıcılık yardımcıları
private fun measuredFile(ctx: Context) = File(ctx.filesDir, "measured_points.json")
private fun loadMeasuredPoints(ctx: Context): List<MeasuredPoint> = runCatching {
    val f = measuredFile(ctx); if (!f.exists()) return emptyList()
    val txt = f.readText(); if (txt.isBlank()) return emptyList()
    val arr = org.json.JSONArray(txt)
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        MeasuredPoint(
            time = o.optLong("time"),
            lat = o.optDouble("lat"),
            lon = o.optDouble("lon"),
            alt = if (o.has("alt") && !o.isNull("alt")) o.optDouble("alt") else null,
            easting = o.optDouble("easting"),
            northing = o.optDouble("northing"),
            zone = o.optInt("zone"),
            northHemisphere = o.optString("hemisphere","N") == "N",
            name = o.optString("name").ifBlank { null },
            desc = o.optString("desc").ifBlank { null }
        )
    }
}.getOrElse { emptyList() }

private fun saveMeasuredPoints(ctx: Context, list: List<MeasuredPoint>) = runCatching {
    val sb = StringBuilder().append('[')
    list.forEachIndexed { idx, p ->
        if (idx>0) sb.append(',')
        sb.append('{')
        fun esc(s:String?) = s?.replace("\"","\\\"")
        sb.append("\"time\":${p.time},")
        sb.append("\"lat\":${p.lat},\"lon\":${p.lon},")
        p.alt?.let { sb.append("\"alt\":$it,") }
        sb.append("\"easting\":${p.easting},\"northing\":${p.northing},")
        sb.append("\"zone\":${p.zone},\"hemisphere\":\"${if (p.northHemisphere) 'N' else 'S'}\",")
        sb.append("\"name\":\"${esc(p.name)?:""}\",\"desc\":\"${esc(p.desc)?:""}\"")
        sb.append('}')
    }
    sb.append(']')
    measuredFile(ctx).writeText(sb.toString())
}

// Basit geoit (placeholder) - gerçek model yoksa 0 döner.
private fun approximateGeoidUndulation(lat: Double, lon: Double): Double {
    // Çok kaba örnek: enlem bantlarına göre birkaç sabit offset (sadece demo)
    val aLat = kotlin.math.abs(lat)
    return when {
        aLat < 10 -> -5.0
        aLat < 30 -> -10.0
        aLat < 50 -> -20.0
        else -> -30.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointStakeoutScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLat by remember { mutableStateOf("39.9334") }
    var selectedLng by remember { mutableStateOf("32.8597") }
    var showMap by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pos by GnssPositionRepository.position.collectAsState()

    // Ölçüm listesi kalıcı yükleme
    val measured = remember { mutableStateListOf<MeasuredPoint>().apply { addAll(loadMeasuredPoints(context)) } }
    var applyGeoid by remember { mutableStateOf(false) }
    var manualLatLon by remember { mutableStateOf(false) }
    var showMeasureMap by remember { mutableStateOf(true) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pointName by remember { mutableStateOf("") }
    var pointDesc by remember { mutableStateOf("") }
    var showAllPoints by remember { mutableStateOf(false) }

    // Ölçüm marker JSON (yalın)
    val measuredJson by remember(measured.size) {
        mutableStateOf(buildString {
            append('[')
            measured.forEachIndexed { idx, p ->
                if (idx>0) append(',')
                append('{')
                fun esc(s:String?) = s?.replace("\"","\\\"")
                append("\"index\":${idx+1},\"lat\":${p.lat},\"lon\":${p.lon},")
                p.alt?.let { append("\"alt\":$it,") }
                append("\"name\":\"${esc(p.name)?:""}\"")
                append('}')
            }
            append(']')
        })
    }

    // Otomatik kaydet (liste değişince serialize)
    LaunchedEffect(measured.size) { saveMeasuredPoints(context, measured) }

    // GNSS otomatik doldurma (manuel müdahale edilmediyse)
    LaunchedEffect(pos.lat, pos.lon, manualLatLon) {
        if (!manualLatLon) {
            pos.lat?.let { selectedLat = String.format(Locale.US, "%.6f", it) }
            pos.lon?.let { selectedLng = String.format(Locale.US, "%.6f", it) }
        }
    }

    fun latLonToUtm(latDeg: Double, lonDeg: Double): Triple<MeasuredPoint, String, String>? {
        if (latDeg.isNaN() || lonDeg.isNaN()) return null
        val a = 6378137.0
        val f = 1 / 298.257223563
        val k0 = 0.9996
        val e2 = f * (2 - f)
        val ePrime2 = e2 / (1 - e2)
        val zone = ((lonDeg + 180) / 6 + 1).toInt().coerceIn(1, 60)
        val lon0 = (zone - 1) * 6 - 180 + 3
        val latRad = Math.toRadians(latDeg)
        val lonRad = Math.toRadians(lonDeg)
        val lon0Rad = Math.toRadians(lon0.toDouble())
        val sinLat = kotlin.math.sin(latRad)
        val cosLat = kotlin.math.cos(latRad)
        val tanLat = kotlin.math.tan(latRad)
        val N = a / kotlin.math.sqrt(1 - e2 * sinLat * sinLat)
        val T = tanLat * tanLat
        val C = ePrime2 * cosLat * cosLat
        val A = cosLat * (lonRad - lon0Rad)
        val e4 = e2 * e2
        val e6 = e4 * e2
        val M = a * (
            (1 - e2 / 4 - 3 * e4 / 64 - 5 * e6 / 256) * latRad -
                (3 * e2 / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * kotlin.math.sin(2 * latRad) +
                (15 * e4 / 256 + 45 * e6 / 1024) * kotlin.math.sin(4 * latRad) -
                (35 * e6 / 3072) * kotlin.math.sin(6 * latRad)
            )
        val easting = k0 * N * (
            A + (1 - T + C) * A * A * A / 6 +
                (5 - 18 * T + T * T + 72 * C - 58 * ePrime2) * A * A * A * A * A / 120
            ) + 500000.0
        var northing = k0 * (M + N * tanLat * (
            A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24 +
                (61 - 58 * T + T * T + 600 * C - 330 * ePrime2) * A * A * A * A * A * A / 720
            ))
        val northHemisphere = latDeg >= 0
        if (!northHemisphere) northing += 10000000.0
        val mp = MeasuredPoint(System.currentTimeMillis(), latDeg, lonDeg, pos.alt, easting, northing, zone, northHemisphere, null, null)
        val xy = "X: %.3f  Y: %.3f".format(Locale.US, easting, northing)
        val ll = "Lat: %.6f  Lon: %.6f".format(Locale.US, latDeg, lonDeg)
        return Triple(mp, xy, ll)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nokta Aplikasyonu") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") } }
            )
        },
        snackbarHost = { SnackbarHost(snackHost) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Başlık
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nokta Aplikasyonu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Nokta arama, koordinat girişi ve navigasyon", style = MaterialTheme.typography.bodyMedium)
                }
            }
            // Arama
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Nokta Arama", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Nokta ID") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Nokta Ara") }
                }
            }
            // Koordinat girişi
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Koordinat Girişi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(selectedLat, { selectedLat = it; manualLatLon = true }, label = { Text("Enlem") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(selectedLng, { selectedLng = it; manualLatLon = true }, label = { Text("Boylam") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showMap = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Map, null); Spacer(Modifier.width(4.dp)); Text("Haritada Göster") }
                        OutlinedButton(onClick = {
                            val glat = pos.lat; val glon = pos.lon
                            if (glat != null && glon != null) {
                                selectedLat = String.format(Locale.US, "%.6f", glat)
                                selectedLng = String.format(Locale.US, "%.6f", glon)
                                manualLatLon = false
                                scope.launch { snackHost.showSnackbar("GNSS konumu alındı") }
                            } else scope.launch { snackHost.showSnackbar("GNSS verisi yok") }
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.MyLocation, null); Spacer(Modifier.width(4.dp)); Text("Konumum") }
                    }
                }
            }
            // Aktif nokta (manuel/ seçim)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aktif Nokta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Enlem:"); Text(selectedLat, fontWeight = FontWeight.Medium) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Boylam:"); Text(selectedLng, fontWeight = FontWeight.Medium) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val lat = selectedLat.replace(',', '.').toDoubleOrNull(); val lng = selectedLng.replace(',', '.').toDoubleOrNull()
                            if (lat == null || lng == null) {
                                scope.launch { snackHost.showSnackbar("Geçersiz koordinat") }
                            } else {
                                val navUri = Uri.parse("google.navigation:q=$lat,$lng")
                                val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply { setPackage("com.google.android.apps.maps") }
                                val canGoogle = navIntent.resolveActivity(context.packageManager) != null
                                try {
                                    if (canGoogle) context.startActivity(navIntent) else {
                                        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                                        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        val canGeo = geoIntent.resolveActivity(context.packageManager) != null
                                        if (canGeo) context.startActivity(geoIntent) else scope.launch { snackHost.showSnackbar("Uygun harita uygulaması bulunamadı") }
                                    }
                                } catch (e: Exception) { scope.launch { snackHost.showSnackbar("Açılamadı: ${e.message}") } }
                            }
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(4.dp)); Text("Git") }
                        OutlinedButton(onClick = { /* gelecekte kayıtla ilişkilendirilebilir */ }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text("Kaydet") }
                    }
                }
            }
            // Ölçüm
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Nokta Ölçümü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showMeasureMap = !showMeasureMap }) { Text(if (showMeasureMap) "Haritayı Gizle" else "Haritayı Göster") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = applyGeoid, onCheckedChange = { applyGeoid = it })
                        Text("Geoit düzeltmesi (yaklaşık)", style = MaterialTheme.typography.labelSmall)
                    }
                    val latVal = pos.lat; val lonVal = pos.lon; val altVal = pos.alt
                    if (latVal == null || lonVal == null) {
                        Text("GNSS konumu bekleniyor...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        val utm = latLonToUtm(latVal, lonVal)
                        val orthoAlt = if (applyGeoid && altVal != null) altVal - approximateGeoidUndulation(latVal, lonVal) else altVal
                        utm?.let { (_, xy, ll) ->
                            Text(ll, style = MaterialTheme.typography.bodySmall)
                            Text(xy, style = MaterialTheme.typography.bodySmall)
                            Text("Z: ${orthoAlt?.let { String.format(Locale.US, "%.2f m", it) } ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (latVal == null || lonVal == null) {
                                    scope.launch { snackHost.showSnackbar("GNSS yok") }
                                } else {
                                    pointName = ""; pointDesc = ""; showSaveDialog = true
                                }
                            }) { Text("Kaydet") }
                            OutlinedButton(onClick = { measured.clear(); scope.launch { snackHost.showSnackbar("Liste temizlendi") } }) { Text("Temizle") }
                            if (measured.isNotEmpty()) TextButton(onClick = { showAllPoints = true }) { Text("Tümü (${measured.size})") }
                        }
                        if (measured.isNotEmpty()) {
                            val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Son 5 Kayıt", style = MaterialTheme.typography.labelLarge)
                                measured.takeLast(5).asReversed().forEach { p ->
                                    Text("${sdf.format(Date(p.time))}  ${p.name.orEmpty()} X=%.2f Y=%.2f Z=%s".format(Locale.US, p.easting, p.northing, p.alt?.let { String.format(Locale.US, "%.2f", it) } ?: "-"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (showMeasureMap) {
                            HorizontalDivider()
                            Text("Canlı Harita", style = MaterialTheme.typography.labelLarge)
                            Box(Modifier.height(220.dp).fillMaxWidth()) { MapScreen(measuredPointsJson = measuredJson) }
                        }
                    }
                }
            }
            // Opsiyonel küçük harita diyaloğu tetiklenmişse
            if (showMap) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Harita Yükleniyor...", style = MaterialTheme.typography.bodyMedium)
                        Text("Koordinat: $selectedLat, $selectedLng", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { showMap = false }) { Text("Kapat") }
                    }
                }
            }
        }
    }

    // Tüm Noktalar Dialog
    if (showAllPoints) {
        AlertDialog(
            onDismissRequest = { showAllPoints = false },
            confirmButton = { TextButton(onClick = { showAllPoints = false }) { Text("Kapat") } },
            title = { Text("Kaydedilen Noktalar (${measured.size})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportPointsCsv(measured, context, snackHost, scope) }) { Text("CSV Dışa Aktar") }
                        OutlinedButton(onClick = { exportPointsJson(measured, context, snackHost, scope) }) { Text("JSON Dışa Aktar") }
                    }
                    val listState = rememberLazyListState()
                    LazyColumn(Modifier.heightIn(max = 400.dp), state = listState) {
                        itemsIndexed(measured) { idx, p ->
                            Text("${idx+1}. ${p.name ?: "(adsız)"} Z=${p.alt?.let { String.format(Locale.US, "%.2f", it) } ?: "-"}\nLat=%.6f Lon=%.6f X=%.2f Y=%.2f Zone=${p.zone}${if (p.northHemisphere) 'N' else 'S'}".format(Locale.US, p.lat, p.lon, p.easting, p.northing) + (p.desc?.let { "\n$it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                        }
                    }
                }
            }
        )
    }

    // Kaydet Dialogu
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val lat = pos.lat; val lon = pos.lon
                    if (lat != null && lon != null) {
                        val triple = latLonToUtm(lat, lon)
                        if (triple != null) {
                            val base = triple.first
                            val idx = measured.size + 1
                            measured += base.copy(name = pointName.ifBlank { "P$idx" }, desc = pointDesc.ifBlank { null })
                            scope.launch { snackHost.showSnackbar("Nokta kaydedildi (#${measured.size})") }
                        }
                    }
                    showSaveDialog = false
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("İptal") } },
            title = { Text("Nokta Kaydet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pointName, { pointName = it }, label = { Text("Ad (isteğe bağlı)") }, singleLine = true)
                    OutlinedTextField(pointDesc, { pointDesc = it }, label = { Text("Açıklama (isteğe bağlı)") }, minLines = 2, maxLines = 4)
                    val lat = pos.lat; val lon = pos.lon
                    Text(if (lat != null && lon != null) "Lat/Lon: %.6f %.6f".format(Locale.US, lat, lon) else "GNSS bekleniyor", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }
}

// Export fonksiyonları gerçek implementasyon
private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "com.example.tugis3.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (file.extension.equals("csv", true)) "text/csv" else "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Paylaş"))
}

private fun exportPointsCsv(points: List<MeasuredPoint>, context: Context, snack: SnackbarHostState, scope: CoroutineScope) {
    if (points.isEmpty()) { scope.launch { snack.showSnackbar("Dışa aktarılacak nokta yok") }; return }
    scope.launch {
        runCatching {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val file = File(context.filesDir, "points_${sdf.format(Date())}.csv")
            file.bufferedWriter().use { w ->
                w.appendLine("Index,Time,Name,Desc,Lat,Lon,Alt,X,Y,Zone,Hemisphere")
                points.forEachIndexed { i, p ->
                    val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(p.time))
                    w.appendLine("${i+1},${timeStr},${p.name.orEmpty()},${p.desc.orEmpty()},${p.lat},${p.lon},${p.alt ?: ""},${p.easting},${p.northing},${p.zone},${if (p.northHemisphere) "N" else "S"}")
                }
            }
            file
        }.onSuccess { f ->
            snack.showSnackbar("CSV oluşturuldu: ${f.name}")
            shareFile(context, f)
        }.onFailure { e -> snack.showSnackbar("CSV hata: ${e.message}") }
    }
}

private fun exportPointsJson(points: List<MeasuredPoint>, context: Context, snack: SnackbarHostState, scope: CoroutineScope) {
    if (points.isEmpty()) { scope.launch { snack.showSnackbar("Dışa aktarılacak nokta yok") }; return }
    scope.launch {
        runCatching {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val file = File(context.filesDir, "points_${sdf.format(Date())}.json")
            file.bufferedWriter().use { w ->
                w.append('[')
                points.forEachIndexed { i, p ->
                    if (i>0) w.append(',')
                    fun esc(s:String?) = s?.replace("\"","\\\"")
                    w.append('{')
                    w.append("\"index\":${i+1},\"time\":${p.time},\"lat\":${p.lat},\"lon\":${p.lon},")
                    p.alt?.let { w.append("\"alt\":$it,") }
                    w.append("\"easting\":${p.easting},\"northing\":${p.northing},\"zone\":${p.zone},\"hemisphere\":\"${if (p.northHemisphere) 'N' else 'S'}\",\"name\":\"${esc(p.name)?:""}\",\"desc\":\"${esc(p.desc)?:""}\"")
                    w.append('}')
                }
                w.append(']')
            }
            file
        }.onSuccess { f ->
            snack.showSnackbar("JSON oluşturuldu: ${f.name}")
            shareFile(context, f)
        }.onFailure { e -> snack.showSnackbar("JSON hata: ${e.message}") }
    }
}
