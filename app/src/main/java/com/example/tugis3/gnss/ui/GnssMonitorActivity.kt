package com.example.tugis3.gnss.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.gnss.NmeaLogConfig
import com.example.tugis3.gnss.model.FixType
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class GnssMonitorActivity : ComponentActivity() {

    private val vm: GnssMonitorViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            val ok = granted.values.all { it }
            if (ok) vm.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { GnssMonitorScreen() } }
        ensurePermissions()
    }

    override fun onDestroy() {
        vm.stop()
        super.onDestroy()
    }

    private fun ensurePermissions() {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }
        val ask = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ask.isNotEmpty()) permissionLauncher.launch(ask.toTypedArray()) else vm.start()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GnssMonitorScreen(vm: GnssMonitorViewModel = hiltViewModel()) {
    val obs by vm.observation.collectAsState()
    val active by vm.activeProject.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val projects by vm.projects.collectAsState()
    val lastSaved by vm.lastSavedPoint.collectAsState()
    var showProjectSheet by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fixStats by vm.fixStats.collectAsState()
    var nmeaLogging by remember { mutableStateOf(NmeaLogConfig.enabled) }
    val satellites by vm.satellites.collectAsState()
    val nmeaCount by vm.nmeaLineCount.collectAsState()

    LaunchedEffect(Unit) {
        vm.uiEvents.collect { e ->
            when (e) {
                is GnssMonitorViewModel.UiEvent.Saved -> snackbarHostState.showSnackbar("Kaydedildi: ${e.pointName}")
                is GnssMonitorViewModel.UiEvent.Error -> snackbarHostState.showSnackbar(e.message)
            }
        }
    }

    // Ana içerik + sheet
    if (showProjectSheet) {
        ModalBottomSheet(onDismissRequest = { showProjectSheet = false }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Projeyi Seç", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (projects.isEmpty()) {
                    Text("Hiç proje yok. Proje Yöneticisinden ekleyin.", style = MaterialTheme.typography.bodySmall)
                } else {
                    projects.forEach { p ->
                        val selected = p.id == active?.id
                        ElevatedCard(
                            onClick = {
                                vm.setActiveProject(p.id)
                                showProjectSheet = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    p.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                                if (selected) Text("Aktif", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { showNewProjectDialog = true }) { Text("Yeni Proje Oluştur") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Yeni Proje") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(newProjectName, { newProjectName = it }, label = { Text("Ad") }, singleLine = true)
                    OutlinedTextField(newProjectDesc, { newProjectDesc = it }, label = { Text("Açıklama (ops)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newProjectName.ifBlank { "Proje" + System.currentTimeMillis().toString().takeLast(4) }
                    vm.createAndActivateProject(name, newProjectDesc.ifBlank { null })
                    newProjectName = ""; newProjectDesc = ""; showNewProjectDialog = false; showProjectSheet = false
                }) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { showNewProjectDialog = false }) { Text("İptal") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GNSS İzleme") },
                actions = {
                    IconButton(onClick = {
                        nmeaLogging = !nmeaLogging
                        NmeaLogConfig.enabled = nmeaLogging
                    }) {
                        val iconColor = if (nmeaLogging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(Icons.Default.Save, contentDescription = "NMEA Log", tint = iconColor)
                    }
                    TextButton(onClick = { showProjectSheet = true }) { Text(active?.name?.take(12) ?: "Proje Seç") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val isEnabled = obs?.latDeg != null && active != null
            FloatingActionButton(
                onClick = { if (isEnabled) vm.saveCurrentPoint() },
                modifier = Modifier.graphicsLayer(alpha = if (isEnabled) 1f else 0.5f)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Noktayı Kaydet")
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fix stats bar
            if (fixStats.isNotEmpty()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fixStats.entries.sortedBy { it.key }.forEach { (k,v) ->
                            AssistChip(onClick = {}, label = { Text("$k:$v") })
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { vm.clearFixStats() }) { Text("Sıfırla") }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GpsFixed, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Anlık Gözlem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            active?.let { Text("Aktif Proje: ${it.name}", style = MaterialTheme.typography.bodyMedium) }
            if (obs == null) {
                CircularProgressIndicator()
                Text("Veri bekleniyor... (İzinler / Gökyüzü görüşü gerekebilir)")
            } else {
                val ageSec = remember(obs?.epochMillis) { ((System.currentTimeMillis() - (obs?.epochMillis ?: 0)) / 1000.0) }
                val stale = ageSec > 5.0
                GnssObservationCard(obs!!.fixType, obs!!.latDeg, obs!!.lonDeg, obs!!.ellipsoidalHeight,
                    obs!!.satellitesInUse, obs!!.satellitesVisible, obs!!.hrms, obs!!.vrms, ageSec, stale)
            }
            // SKY PLOT + SNR Bars
            if (satellites.isNotEmpty()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Uydu Dağılımı / SNR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            AssistChip(onClick = {}, label = { Text("NMEA: $nmeaCount") })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SkyplotView(satellites = satellites, modifier = Modifier.weight(1f).aspectRatio(1f))
                            SatelliteSnrList(satellites = satellites, modifier = Modifier.weight(1f).height(180.dp))
                        }
                    }
                }
            }
            lastSaved?.let { info ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Son Kaydedilen Nokta: ${info.name}", fontWeight = FontWeight.Bold)
                            Text(
                                "Fix: ${info.fix}  Lat/Lon: " +
                                    (info.lat?.let { String.format(Locale.US, "%.6f", it) } ?: "-") +
                                    " / " +
                                    (info.lon?.let { String.format(Locale.US, "%.6f", it) } ?: "-"),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        val delta = (System.currentTimeMillis() - info.timestamp)/1000.0
                        Text(String.format(Locale.US, "%.1f sn", delta), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "RTK / NTRIP entegrasyonu ilerleyen aşamada. Kaydet ile nokta aktif projeye eklenir.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GnssObservationCard(
    fix: FixType,
    lat: Double?,
    lon: Double?,
    h: Double?,
    used: Int,
    vis: Int,
    hrms: Double?,
    vrms: Double?,
    ageSec: Double = 0.0,
    stale: Boolean = false
) {
    val containerColor = when (fix) {
        FixType.NO_FIX -> MaterialTheme.colorScheme.errorContainer
        FixType.SINGLE -> MaterialTheme.colorScheme.surfaceVariant
        FixType.DGPS -> MaterialTheme.colorScheme.secondaryContainer
        FixType.RTK_FLOAT -> MaterialTheme.colorScheme.tertiaryContainer
        FixType.RTK_FIX -> MaterialTheme.colorScheme.primaryContainer
        FixType.PPP -> MaterialTheme.colorScheme.tertiaryContainer
        FixType.MANUAL -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = contentColorFor(containerColor)
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {},
                label = { Text(fix.displayName) },
                colors = AssistChipDefaults.assistChipColors(containerColor = containerColor, labelColor = contentColor)
            )
            val ageLabel = String.format(Locale.US, "%.1f sn", ageSec)
            val ageColor = if (stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            Text(ageLabel + if (stale) " (Eski)" else "", color = ageColor, style = MaterialTheme.typography.labelSmall)
        }
        Text("Enlem: " + (lat?.let { String.format(Locale.US, "%.8f", it) } ?: "-"))
        Text("Boylam: " + (lon?.let { String.format(Locale.US, "%.8f", it) } ?: "-"))
        Text("Elipsoit Yükseklik: " + (h?.let { String.format(Locale.US, "%.3f m", it) } ?: "-"))
        Text("Uydu (Kullanılan/Görülen): $used/$vis")
        Text(
            "HRMS: " + (hrms?.let { String.format(Locale.US, "%.2f m", it) } ?: "-") +
                "  VRMS: " + (vrms?.let { String.format(Locale.US, "%.2f m", it) } ?: "-")
        )
    } }
}

@Composable
private fun SkyplotView(satellites: List<GnssMonitorViewModel.SatelliteUi>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val outline = scheme.outline
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    val secondary = scheme.secondary
    val errorCol = scheme.error
    val onSurfaceVar = scheme.onSurfaceVariant
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.minDimension
            val center = Offset(size.width/2, size.height/2)
            val maxR = w/2 * 0.95f
            // Halkalar
            drawCircle(color = outline.copy(alpha=0.4f), radius = maxR, center = center, style = Stroke(1f))
            drawCircle(color = outline.copy(alpha=0.3f), radius = maxR * (1 - 30/90f), center = center, style = Stroke(1f))
            drawCircle(color = outline.copy(alpha=0.2f), radius = maxR * (1 - 60/90f), center = center, style = Stroke(1f))
            // Artı işareti
            drawLine(outline, Offset(center.x - maxR, center.y), Offset(center.x + maxR, center.y), 1f)
            drawLine(outline, Offset(center.x, center.y - maxR), Offset(center.x, center.y + maxR), 1f)
            // Noktalar
            satellites.forEach { s ->
                val elev = s.elevationDeg.coerceIn(0.0, 90.0)
                val r = (1 - elev/90.0) * maxR
                val azRad = (s.azimuthDeg * PI / 180.0)
                val x = center.x + (r * sin(azRad)).toFloat()
                val y = center.y - (r * cos(azRad)).toFloat()
                val base = when (s.constellation) {
                    "GPS" -> primary
                    "GAL" -> tertiary
                    "BDS" -> secondary
                    "GLONASS" -> errorCol
                    else -> onSurfaceVar
                }
                val color = if (s.used) base else base.copy(alpha = 0.35f)
                drawCircle(color = color, radius = 6f, center = Offset(x,y))
            }
        }
    }
}

@Composable
private fun SatelliteSnrList(satellites: List<GnssMonitorViewModel.SatelliteUi>, modifier: Modifier = Modifier) {
    val maxSnr = (satellites.maxOfOrNull { it.snr } ?: 50.0).coerceAtLeast(10.0)
    LazyColumn(modifier) {
        items(satellites.take(30)) { s ->
            val ratio = (s.snr / maxSnr).coerceIn(0.0, 1.0)
            val barColor = if (s.used) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.constellation}${s.id}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
                    Box(Modifier.weight(1f).height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(ratio.toFloat()).background(barColor, shape = MaterialTheme.shapes.extraSmall))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(String.format(Locale.US, "%.0f", s.snr), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
