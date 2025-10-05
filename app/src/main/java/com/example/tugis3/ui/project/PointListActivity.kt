package com.example.tugis3.ui.project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.tugis3.ui.theme.Tugis3Theme
import com.example.tugis3.Tugis3Application
import com.example.tugis3.util.ShareExportUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@AndroidEntryPoint
class PointListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { PointListScreen() } }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PointListScreen(vm: PointListViewModel = hiltViewModel()) {
    val active by vm.activeProject.collectAsState()
    val filtered by vm.filtered.collectAsState()
    val search by vm.search.collectAsState()
    val deleted by vm.deletedPoints.collectAsState()
    val autoResolve by vm.autoResolveDuplicates.collectAsState() // StateFlow.value yerine collectAsState

    var showAdd by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf<Long?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showDeleteMulti by remember { mutableStateOf(false) }
    var showDeletedPanel by remember { mutableStateOf(false) }
    // İhracat durum state'leri
    var exportInProgress by remember { mutableStateOf(false) }
    var exportProgressLabel by remember { mutableStateOf("") }

    // Add / Edit fields
    var manualName by remember { mutableStateOf("") }
    var north by remember { mutableStateOf("") }
    var east by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var featureCode by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    // Çoklu seçim
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun resetEntryFields() {
        manualName=""; north=""; east=""; height=""; featureCode=""; desc=""; nameError=null
    }

    fun populateForEdit(id: Long) {
        val p = filtered.firstOrNull { it.id == id } ?: return
        manualName = p.name
        north = p.northing.toString()
        east = p.easting.toString()
        height = p.ellipsoidalHeight?.toString() ?: ""
        featureCode = p.featureCode ?: ""
        desc = p.description ?: ""
        nameError = null
    }

    // --- Dialoglar ---
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Yeni Nokta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        manualName,
                        onValueChange = { manualName = it; nameError = null },
                        label = { Text("Ad") },
                        isError = nameError != null,
                        supportingText = { if (nameError!=null) Text(nameError!!, color = MaterialTheme.colorScheme.error) }
                    )
                    OutlinedTextField(featureCode, { featureCode = it.uppercase().take(12) }, label = { Text("Kod") }, singleLine = true)
                    OutlinedTextField(desc, { desc = it }, label = { Text("Açıklama") })
                    OutlinedTextField(north, { north = it.filter { c -> c.isDigit() || c=='.' || c=='-' } }, label = { Text("Kuzey / Enlem") })
                    OutlinedTextField(east, { east = it.filter { c -> c.isDigit() || c=='.' || c=='-' } }, label = { Text("Doğu / Boylam") })
                    OutlinedTextField(height, { height = it.filter { c -> c.isDigit() || c=='.' || c=='-' } }, label = { Text("Elipsoit Yükseklik") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val res = vm.attemptAddManual(
                            manualName.ifBlank { manualName },
                            north.toDoubleOrNull(), east.toDoubleOrNull(), height.toDoubleOrNull(),
                            featureCode.ifBlank { null }, desc.ifBlank { null }, resolveDuplicate = false
                        )
                        res.onFailure { nameError = it.message }
                        if (res.isSuccess) {
                            resetEntryFields(); showAdd=false
                            snackbar.showSnackbar("Eklendi")
                        }
                    }
                }, enabled = north.toDoubleOrNull()!=null && east.toDoubleOrNull()!=null) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
        )
    }

    showEdit?.let { editId ->
        AlertDialog(
            onDismissRequest = { showEdit = null },
            title = { Text("Noktayı Düzenle") },
            text = {
                LaunchedEffect(editId) { populateForEdit(editId) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(manualName, { manualName = it; nameError=null }, label = { Text("Ad") }, isError = nameError!=null,
                        supportingText = { if (nameError!=null) Text(nameError!!, color = MaterialTheme.colorScheme.error) })
                    OutlinedTextField(featureCode, { featureCode = it.uppercase().take(12) }, label = { Text("Kod") }, singleLine = true)
                    OutlinedTextField(desc, { desc = it }, label = { Text("Açıklama") })
                    OutlinedTextField(north, { north = it.filter { c -> c.isDigit() || c=='.' || c=='-' } }, label = { Text("Kuzey / Enlem") })
                    OutlinedTextField(east, { east = it.filter { c -> c.isDigit() || c=='.' || c=='-' } }, label = { Text("Doğu / Boylam") })
                    OutlinedTextField(height, { height = it.filter { c -> c.isDigit() || c=='.' || c=='-' } }, label = { Text("Elipsoit Yükseklik") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nVal = north.toDoubleOrNull(); val eVal = east.toDoubleOrNull()
                    if (nVal!=null && eVal!=null) {
                        scope.launch {
                            val res = vm.attemptUpdatePoint(
                                editId,
                                manualName.ifBlank { manualName },
                                nVal, eVal,
                                height.toDoubleOrNull(), featureCode.ifBlank { null }, desc.ifBlank { null }, resolveDuplicate = false
                            )
                            res.onFailure { nameError = it.message }
                            if (res.isSuccess) {
                                snackbar.showSnackbar("Güncellendi")
                                resetEntryFields(); showEdit = null
                            }
                        }
                    }
                }, enabled = north.toDoubleOrNull()!=null && east.toDoubleOrNull()!=null) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showEdit = null }) { Text("İptal") } }
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("CSV / Metin İçe Aktar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Format: ad, northing, easting[, yükseklik][, kod][, açıklama]\nHer satır bir nokta.")
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Veri (yapıştır)") },
                        minLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importFromCsv(importText) { r ->
                        scope.launch { snackbar.showSnackbar("Eklendi: ${r.added} Hatalı: ${r.failed}") }
                    }
                    importText = ""; showImport = false
                }, enabled = importText.isNotBlank()) { Text("İçe Aktar") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("İptal") } }
        )
    }

    if (showDeleteMulti) {
        AlertDialog(
            onDismissRequest = { showDeleteMulti = false },
            title = { Text("Seçilenleri Sil") },
            text = { Text("${selected.size} nokta silinecek. Emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePoints(selected.toSet())
                    selected.clear(); selectionMode = false; showDeleteMulti = false
                    scope.launch {
                        val res = snackbar.showSnackbar("Silindi", actionLabel = "Geri Al")
                        if (res == SnackbarResult.ActionPerformed) vm.undoLastDelete()
                    }
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { showDeleteMulti = false }) { Text("İptal") } }
        )
    }

    // Export format seçimi
    var exportMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var exportFileMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectionMode) "Seçilen: ${selected.size}" else "Noktalar") },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { selectionMode = false; selected.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                actions = {
                    if (!selectionMode) {
                        IconButton(onClick = { showDeletedPanel = !showDeletedPanel }) { Icon(Icons.Default.Recycling, contentDescription = "Silinenler") }
                        IconButton(onClick = { showImport = true }) { Icon(Icons.Default.Download, contentDescription = "İçe Aktar") }
                        IconButton(onClick = { exportMenu = true }) { Icon(Icons.Default.UploadFile, contentDescription = "Dışa Aktar") }
                        DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                            DropdownMenuItem(text = { Text("CSV Paylaş (Metin)") }, onClick = {
                                exportMenu=false
                                if (filtered.isNotEmpty()) {
                                    val points = if (selectionMode && selected.isNotEmpty()) filtered.filter { selected.contains(it.id) } else filtered
                                    val csv = vm.buildCsv(points)
                                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_TEXT, csv) }
                                    Tugis3Application.appContext.startActivity(Intent.createChooser(send, "CSV Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            })
                            DropdownMenuItem(text = { Text("KML Paylaş (Metin)") }, onClick = {
                                exportMenu=false
                                if (filtered.isNotEmpty()) {
                                    val points = if (selectionMode && selected.isNotEmpty()) filtered.filter { selected.contains(it.id) } else filtered
                                    val kml = vm.buildKml(points, active?.name ?: "Points")
                                    val needsWarn = points.any { it.latDeg == null || it.lonDeg == null }
                                    val send = Intent(Intent.ACTION_SEND).apply { type = "application/vnd.google-earth.kml+xml"; putExtra(Intent.EXTRA_TEXT, kml) }
                                    Tugis3Application.appContext.startActivity(Intent.createChooser(send, "KML Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    if (needsWarn) scope.launch { snackbar.showSnackbar("Uyarı: KML için lat/lon yok, projeksiyon X/Y kullanıldı") }
                                }
                            })
                            DropdownMenuItem(text = { Text("GeoJSON Paylaş (Metin)") }, onClick = {
                                exportMenu=false
                                if (filtered.isNotEmpty()) {
                                    val points = if (selectionMode && selected.isNotEmpty()) filtered.filter { selected.contains(it.id) } else filtered
                                    val geo = vm.buildGeoJson(points)
                                    val needsWarn = points.any { it.latDeg == null || it.lonDeg == null }
                                    val send = Intent(Intent.ACTION_SEND).apply { type = "application/geo+json"; putExtra(Intent.EXTRA_TEXT, geo) }
                                    Tugis3Application.appContext.startActivity(Intent.createChooser(send, "GeoJSON Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    if (needsWarn) scope.launch { snackbar.showSnackbar("Uyarı: GeoJSON için lat/lon yok, projeksiyon X/Y kullanıldı") }
                                }
                            })
                            DropdownMenuItem(text = { Text("Dosya Olarak Paylaş") }, onClick = {
                                exportMenu=false; exportFileMenu = true
                            })
                        }
                        DropdownMenu(expanded = exportFileMenu, onDismissRequest = { exportFileMenu = false }) {
                            DropdownMenuItem(text = { Text("CSV Dosyası") }, onClick = {
                                exportFileMenu=false
                                if (filtered.isNotEmpty()) scope.launch {
                                    val points = if (selectionMode && selected.isNotEmpty()) filtered.filter { selected.contains(it.id) } else filtered
                                    val large = points.size > 2000
                                    if (large) { exportInProgress = true; exportProgressLabel = "CSV hazırlanıyor (${points.size})" }
                                    val csv = withContext(Dispatchers.Default) { vm.buildCsv(points) }
                                    ShareExportUtil.shareText(Tugis3Application.appContext, active?.name ?: "points", csv, "text/csv", if (selectionMode && selected.isNotEmpty()) "secili" else null)
                                    if (large) exportInProgress = false
                                }
                            })
                            DropdownMenuItem(text = { Text("KML Dosyası") }, onClick = {
                                exportFileMenu=false
                                if (filtered.isNotEmpty()) scope.launch {
                                    val points = if (selectionMode && selected.isNotEmpty()) filtered.filter { selected.contains(it.id) } else filtered
                                    val large = points.size > 2000
                                    if (large) { exportInProgress = true; exportProgressLabel = "KML hazırlanıyor (${points.size})" }
                                    val kml = withContext(Dispatchers.Default) { vm.buildKml(points) }
                                    val needsWarn = points.any { it.latDeg == null || it.lonDeg == null }
                                    ShareExportUtil.shareText(Tugis3Application.appContext, active?.name ?: "points", kml, "application/vnd.google-earth.kml+xml", if (selectionMode && selected.isNotEmpty()) "secili" else null)
                                    if (needsWarn) snackbar.showSnackbar("Uyarı: KML için lat/lon yok, projeksiyon X/Y kullanıldı")
                                    if (large) exportInProgress = false
                                }
                            })
                            DropdownMenuItem(text = { Text("GeoJSON Dosyası") }, onClick = {
                                exportFileMenu=false
                                if (filtered.isNotEmpty()) scope.launch {
                                    val points = if (selectionMode && selected.isNotEmpty()) filtered.filter { selected.contains(it.id) } else filtered
                                    val large = points.size > 2000
                                    if (large) { exportInProgress = true; exportProgressLabel = "GeoJSON hazırlanıyor (${points.size})" }
                                    val geo = withContext(Dispatchers.Default) { vm.buildGeoJson(points) }
                                    val needsWarn = points.any { it.latDeg == null || it.lonDeg == null }
                                    ShareExportUtil.shareText(Tugis3Application.appContext, active?.name ?: "points", geo, "application/geo+json", if (selectionMode && selected.isNotEmpty()) "secili" else null)
                                    if (needsWarn) snackbar.showSnackbar("Uyarı: GeoJSON için lat/lon yok, projeksiyon X/Y kullanıldı")
                                    if (large) exportInProgress = false
                                }
                            })
                        }
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sırala") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            PointListViewModel.SortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.name) },
                                    onClick = { vm.setSort(opt); sortMenu=false }
                                )
                            }
                        }
                    } else {
                        if (selected.isNotEmpty()) {
                            IconButton(onClick = { showDeleteMulti = true }) { Icon(Icons.Default.Delete, contentDescription = null) }
                            IconButton(onClick = {
                                val txt = buildString {
                                    append("Nokta Listesi (" + selected.size + ")\n")
                                    filtered.filter { selected.contains(it.id) }.forEach { p ->
                                        append("${p.name};${p.northing};${p.easting};${p.ellipsoidalHeight ?: ""}\n")
                                    }
                                }
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, txt) }
                                Tugis3Application.appContext.startActivity(Intent.createChooser(send, "Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }) { Icon(Icons.Default.Share, contentDescription = null) }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val fabEnabled = active!=null && !selectionMode
            FloatingActionButton(
                onClick = { if (fabEnabled) { resetEntryFields(); showAdd = true } },
                modifier = Modifier.alpha(if (fabEnabled) 1f else 0.3f)
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exportInProgress) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(exportProgressLabel.ifBlank { "Dışa aktarma hazırlanıyor" }, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = { vm.setSearch(it) },
                label = { Text("Ara (ad / kod / açıklama)") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (active == null) {
                Text("Aktif proje yok. Proje seçin.", color = MaterialTheme.colorScheme.error)
            } else {
                Text("Aktif Proje: ${active!!.name}", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AssistChip(
                        onClick = { vm.setAutoResolveDuplicates(!autoResolve) }, // düzeltildi
                        label = { Text(if (autoResolve) "Oto İsim: AÇIK" else "Oto İsim: KAPALI") }
                    )
                    AssistChip(
                        onClick = { showDeletedPanel = !showDeletedPanel },
                        label = { Text(if (showDeletedPanel) "Silinenleri Gizle" else "Silinenleri Göster") }
                    )
                }
                if (showDeletedPanel && deleted.isNotEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Silinenler (son ${deleted.size})", fontWeight = FontWeight.Bold)
                                TextButton(onClick = { vm.restoreAllDeleted() }) { Text("Hepsini Geri Al") }
                            }
                            deleted.forEach { dp ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(dp.name, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { vm.restorePoints(listOf(dp.id)) }) { Text("Geri Al") }
                                }
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    Text("Nokta yok")
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f, false)) {
                        items(filtered, key = { it.id }) { p ->
                            val isSel = selected.contains(p.id)
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                if (isSel) selected.remove(p.id) else selected.add(p.id)
                                                if (selected.isEmpty()) selectionMode = false
                                            } else {
                                                showEdit = p.id
                                            }
                                        },
                                        onLongClick = {
                                            if (!selectionMode) {
                                                selectionMode = true
                                                selected.add(p.id)
                                            }
                                        }
                                    )
                            ) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        val code = p.featureCode
                                        if (!code.isNullOrBlank()) Text(code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            "N: " + String.format(Locale.US, "%.6f", p.northing) + "  E: " + String.format(Locale.US, "%.6f", p.easting),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        p.ellipsoidalHeight?.let { Text("H: " + String.format(Locale.US, "%.3f", it), style = MaterialTheme.typography.labelSmall) }
                                        val dsc = p.description
                                        if (!dsc.isNullOrBlank()) Text(dsc, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (!selectionMode) {
                                        IconButton(onClick = {
                                            val txt = "${p.name};${p.northing};${p.easting};${p.ellipsoidalHeight ?: ""}"
                                            val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, txt) }
                                            Tugis3Application.appContext.startActivity(Intent.createChooser(send, "Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        }) { Icon(Icons.Default.Share, contentDescription = null) }
                                        IconButton(onClick = {
                                            vm.deletePoint(p.id)
                                            scope.launch {
                                                val res = snackbar.showSnackbar("Silindi", actionLabel = "Geri Al")
                                                if (res == SnackbarResult.ActionPerformed) vm.undoLastDelete()
                                            }
                                        }) { Icon(Icons.Default.Delete, null) }
                                    } else {
                                        Checkbox(checked = isSel, onCheckedChange = {
                                            if (isSel) selected.remove(p.id) else selected.add(p.id)
                                            if (selected.isEmpty()) selectionMode = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
