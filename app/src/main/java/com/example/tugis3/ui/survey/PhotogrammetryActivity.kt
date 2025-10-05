package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class PhotogrammetryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { PhotogrammetryScreen(onBackPressed = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotogrammetryScreen(onBackPressed: () -> Unit, vm: PhotogrammetryViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val cfg = ui.config
    var showGcpDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }
    val projectPoints by vm.projectPoints.collectAsState()
    var pointFilter by remember { mutableStateOf("") }
    val filteredPoints = remember(projectPoints, pointFilter) {
        if (pointFilter.isBlank()) projectPoints else projectPoints.filter { it.name.contains(pointFilter, true) }
    }

    // Config edit local states (two‑way binding to VM)
    var nameField by remember(cfg.projectName) { mutableStateOf(cfg.projectName) }
    var lengthField by remember(cfg.areaLengthM) { mutableStateOf(cfg.areaLengthM.toString()) }
    var widthField by remember(cfg.areaWidthM) { mutableStateOf(cfg.areaWidthM.toString()) }
    var heightField by remember(cfg.flightHeightM) { mutableStateOf(cfg.flightHeightM.toString()) }
    var fwdOverlapField by remember(cfg.overlapForwardPct) { mutableStateOf(cfg.overlapForwardPct.toString()) }
    var sideOverlapField by remember(cfg.overlapSidePct) { mutableStateOf(cfg.overlapSidePct.toString()) }
    var gsdReqField by remember(cfg.gsdRequiredCm) { mutableStateOf(cfg.gsdRequiredCm.toString()) }

    val cameraOptions = listOf(12,16,20,24,48)
    var cameraExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotogrametri") },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") } },
                actions = {
                    IconButton(onClick = { vm.generatePlan() }) { Icon(Icons.Default.Map, "Plan Üret") }
                    IconButton(onClick = { vm.capturePhoto() }) { Icon(Icons.Default.CameraAlt, "Foto") }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PlanStatusCard(ui)
            }

            // Proje / Alan ayarları
            item {
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Proje Ayarları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(nameField, { nameField = it; vm.updateProjectName(it) }, label = { Text("Proje Adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(lengthField, {
                            lengthField = it; it.toDoubleOrNull()?.let(vm::updateAreaLength)
                        }, label = { Text("Alan Uzunluğu (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(widthField, {
                            widthField = it; it.toDoubleOrNull()?.let(vm::updateAreaWidth)
                        }, label = { Text("Alan Genişliği (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                    Text("Toplam Alan: ${String.format(Locale.US, "%.2f", ui.summary.totalAreaHa)} ha", fontWeight = FontWeight.Medium)
                } }
            }

            // Uçuş parametreleri
            item {
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Uçuş Parametreleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(heightField, {
                        heightField = it; it.toDoubleOrNull()?.let(vm::updateFlightHeight)
                    }, label = { Text("Uçuş Yüksekliği (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(fwdOverlapField, {
                            fwdOverlapField = it; it.toIntOrNull()?.let(vm::updateForwardOverlap)
                        }, label = { Text("İleri Örtüşme %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(sideOverlapField, {
                            sideOverlapField = it; it.toIntOrNull()?.let(vm::updateSideOverlap)
                        }, label = { Text("Yan Örtüşme %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(gsdReqField, {
                            gsdReqField = it; it.toDoubleOrNull()?.let(vm::updateGsdRequired)
                        }, label = { Text("Gerekli GSD (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        ExposedDropdownMenuBox(expanded = cameraExpanded, onExpandedChange = { cameraExpanded = !cameraExpanded }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = "${cfg.cameraResolutionMp}MP",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kamera") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cameraExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = cameraExpanded, onDismissRequest = { cameraExpanded = false }) {
                                cameraOptions.forEach { mp ->
                                    DropdownMenuItem(text = { Text("${mp}MP") }, onClick = { vm.updateCameraResolution(mp); cameraExpanded = false })
                                }
                            }
                        }
                    }
                } }
            }

            // Uçuş hesaplamaları
            item {
                Card(colors = CardDefaults.cardColors(containerColor = if (ui.summary.meetsGsd) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Uçuş Hesaplamaları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Hesaplanan GSD"); Text(String.format(Locale.US, "%.2f cm", ui.summary.actualGsdCm), fontWeight = FontWeight.Medium, color = if (ui.summary.meetsGsd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tahmini Fotoğraf"); Text("${ui.summary.estimatedPhotos}") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tahmini Süre"); Text("${ui.summary.estimatedFlightMinutes} dk") }
                        if (!ui.summary.meetsGsd) {
                            Spacer(Modifier.height(6.dp))
                            Text("⚠️ Yüksekliği azaltın veya daha yüksek çözünürlüklü kamera kullanın", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // GCP Yönetimi
            item {
                Card { Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Yer Kontrol Noktaları (GCP)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = { showPointsDialog = true }) { Icon(Icons.Default.List, null); Spacer(Modifier.width(4.dp)); Text("Projeden") }
                            Button(onClick = { showGcpDialog = true }) { Icon(Icons.Default.AddLocation, null); Spacer(Modifier.width(4.dp)); Text("Manuel") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (ui.gcps.isEmpty()) {
                        Text("Henüz GCP yok. En az 4 önerilir.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Toplam ${ui.gcps.size} GCP", fontWeight = FontWeight.Medium)
                        ui.gcps.forEach { g ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column { Text(g.pointName, fontWeight = FontWeight.Medium); Text("E:${String.format(Locale.US, "%.2f", g.e)} N:${String.format(Locale.US, "%.2f", g.n)} Z:${g.z?.let { String.format(Locale.US, "%.2f", it) } ?: "-"}", style = MaterialTheme.typography.bodySmall) }
                                    IconButton(onClick = { vm.removeGcp(g.pointName) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                } }
            }

            // Waypoint Planı
            item {
                Card { Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Uçuş Planı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.generatePlan() }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Üret") }
                            OutlinedButton(onClick = { vm.clearPlan() }, enabled = ui.waypoints.isNotEmpty()) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(4.dp)); Text("Temizle") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (ui.waypoints.isEmpty()) Text("Plan yok", style = MaterialTheme.typography.bodySmall)
                    else LazyColumn(Modifier.heightIn(max = 240.dp)) {
                        items(ui.waypoints) { wp ->
                            ListItem(
                                headlineContent = { Text("WP ${wp.index} (Line ${wp.lineIndex})") },
                                supportingContent = { Text("E:${String.format(Locale.US, "%.1f", wp.e)} N:${String.format(Locale.US, "%.1f", wp.n)} Alt:${wp.altitude}") },
                                trailingContent = { if (wp.isTurn) Icon(Icons.Default.UTurnLeft, null) }
                            )
                            Divider()
                        }
                    }
                } }
            }

            // Foto Çekimleri
            item {
                Card { Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Foto Çekimleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { vm.capturePhoto() }) { Icon(Icons.Default.Camera, null); Spacer(Modifier.width(4.dp)); Text("Foto") }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (ui.photoShots.isEmpty()) Text("Henüz foto yok", style = MaterialTheme.typography.bodySmall)
                    else LazyColumn(Modifier.heightIn(max = 220.dp)) {
                        items(ui.photoShots) { s ->
                            ListItem(
                                headlineContent = { Text("#${s.index} - ${s.timestamp}") },
                                supportingContent = { Text("E:${s.e?.let { String.format(Locale.US, "%.1f", it) } ?: "-"} N:${s.n?.let { String.format(Locale.US, "%.1f", it) } ?: "-"} Line:${s.lineIndex ?: "-"}") }
                            )
                            Divider()
                        }
                    }
                } }
            }

            // Export
            item {
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dışa Aktarım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            vm.exportPlanCsv().onSuccess { }.onFailure { }
                        }, enabled = ui.waypoints.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.Map, null); Spacer(Modifier.width(4.dp)); Text("Plan CSV") }
                        OutlinedButton(onClick = { vm.exportGcpCsv() }, enabled = ui.gcps.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.Place, null); Spacer(Modifier.width(4.dp)); Text("GCP CSV") }
                        OutlinedButton(onClick = { vm.exportPhotosCsv() }, enabled = ui.photoShots.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.Camera, null); Spacer(Modifier.width(4.dp)); Text("Foto CSV") }
                    }
                    Text(ui.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
            }
        }
    }

    // Manuel GCP ekleme
    if (showGcpDialog) {
        var gName by remember { mutableStateOf("GCP_${ui.gcps.size + 1}") }
        var gE by remember { mutableStateOf( ui.originE?.let { String.format(Locale.US, "%.3f", it) } ?: "0.0" ) }
        var gN by remember { mutableStateOf( ui.originN?.let { String.format(Locale.US, "%.3f", it) } ?: "0.0" ) }
        var gZ by remember { mutableStateOf("0.0") }
        AlertDialog(
            onDismissRequest = { showGcpDialog = false },
            title = { Text("Manuel GCP") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(gName, { gName = it }, label = { Text("Ad") })
                OutlinedTextField(gE, { gE = it }, label = { Text("E (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(gN, { gN = it }, label = { Text("N (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(gZ, { gZ = it }, label = { Text("Z (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            } },
            confirmButton = { Button(onClick = {
                val e = gE.toDoubleOrNull(); val n = gN.toDoubleOrNull(); val z = gZ.toDoubleOrNull()
                if (e!=null && n!=null) vm.addGcpFromPoint(
                    com.example.tugis3.data.db.entity.PointEntity(
                        id = 0L,
                        projectId = 0L,
                        name = gName,
                        northing = n,
                        easting = e,
                        ellipsoidalHeight = z,
                        orthoHeight = null,
                        latDeg = null,
                        lonDeg = null,
                        fixType = null,
                        hrms = null,
                        pdop = null,
                        hdop = null,
                        vdop = null,
                        featureCode = null,
                        description = null,
                        deleted = 0,
                        deletedAt = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                showGcpDialog = false
            }) { Text("Ekle") } },
            dismissButton = { TextButton(onClick = { showGcpDialog = false }) { Text("İptal") } }
        )
    }

    // GCP projeden seçim diyaloğu
    if (showPointsDialog) {
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Projeden GCP Seç") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pointFilter, { pointFilter = it }, label = { Text("Ara") }, singleLine = true)
                    if (filteredPoints.isEmpty()) Text("Sonuç yok", style = MaterialTheme.typography.bodySmall) else LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(filteredPoints) { p ->
                            ListItem(
                                headlineContent = { Text(p.name) },
                                supportingContent = { Text("E:${String.format(Locale.US, "%.2f", p.easting)} N:${String.format(Locale.US, "%.2f", p.northing)}") },
                                trailingContent = { if (ui.gcps.any { it.pointName == p.name }) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.fillMaxWidth().clickable { vm.addGcpFromPoint(p) }
                            )
                            Divider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPointsDialog = false }) { Text("Kapat") } },
            dismissButton = { TextButton(onClick = { vm.clearPlan(); showPointsDialog = false }) { Text("Planı Temizle") } }
        )
    }
}

@Composable
private fun PlanStatusCard(ui: PhotogrammetryViewModel.UiState) {
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui.status, style = MaterialTheme.typography.bodySmall)
        if (ui.originE != null && ui.originN != null) Text("Origin E:${String.format(Locale.US, "%.1f", ui.originE)} N:${String.format(Locale.US, "%.1f", ui.originN)}", style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("WP Sayısı:"); Text(ui.waypoints.size.toString())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GCP Sayısı:"); Text(ui.gcps.size.toString())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Foto Sayısı:"); Text(ui.photoShots.size.toString())
        }
    } }
}
