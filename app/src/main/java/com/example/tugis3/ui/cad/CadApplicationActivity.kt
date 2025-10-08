package com.example.tugis3.ui.cad

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Anchor
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tugis3.data.repository.MeasurementLogRepository
import javax.inject.Inject

@AndroidEntryPoint
class CadApplicationActivity : ComponentActivity() {
    private lateinit var openDocLauncher: ActivityResultLauncher<Array<String>>
    @Inject lateinit var measurementLogRepository: MeasurementLogRepository
    private val logScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // Kalıcı okuma izni al (kullanıcı seçimi sonrası)
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val ext = (uri.lastPathSegment ?: "").lowercase().substringAfterLast('.', "")
                val evt = when (ext) {
                    "dxf" -> "CAD_IMPORT_DXF"
                    "csv" -> "CAD_IMPORT_CSV"
                    "json", "geojson" -> "CAD_IMPORT_JSON"
                    "zip" -> "CAD_IMPORT_ZIP"
                    else -> "CAD_IMPORT_OTHER"
                }
                logScope.launch { runCatching { measurementLogRepository.log(null, null, evt, uri.lastPathSegment) } }
                pendingUri = uri
            }
        }
        setContent {
            val viewModel: CadViewModel = hiltViewModel()
            val mode by viewModel.mode.collectAsState()
            val dynamicSnap by viewModel.dynamicSnap.collectAsState()
            val effectiveSnap by viewModel.effectiveSnapTolerancePx.collectAsState()
            val snapEnabled by viewModel.snapEnabled.collectAsState()
            val selectionMode by viewModel.selectionMode.collectAsState()
            val selectedEntity by viewModel.selectedEntity.collectAsState()
            val hasSelection = selectedEntity != null
            val gridVisible by viewModel.gridVisible.collectAsState()
            val undoDepth by viewModel.undoDepth.collectAsState()
            val redoDepth by viewModel.redoDepth.collectAsState()
            val worldSnapMode by viewModel.snapWorldMode.collectAsState()
            val worldSnapTol by viewModel.snapWorldToleranceM.collectAsState()
            val scope = rememberCoroutineScope()
            val context = this
            val snackbar = remember { SnackbarHostState() }
            // Eğer dışarıda dosya seçilmişse burada işleyelim
            val pu = pendingUri
            if (pu != null) {
                LaunchedEffect(pu) {
                    viewModel.loadFromUri(contentResolver, pu)
                    snackbar.showSnackbar("Dosya yüklendi: ${pu.lastPathSegment ?: "seçildi"}")
                    logScope.launch { runCatching { measurementLogRepository.log(null, null, "CAD_LOAD", pu.lastPathSegment) } }
                    pendingUri = null
                }
            }
            CadScaffold(
                mode = mode,
                snapEnabled = snapEnabled,
                effectiveSnapTolerance = effectiveSnap,
                dynamicSnap = dynamicSnap,
                selectionMode = selectionMode,
                hasSelection = hasSelection,
                gridVisible = gridVisible,
                onToggleMode = { viewModel.toggleMode() },
                onToggleSnap = { viewModel.toggleSnap() },
                onCycleSnapTol = { viewModel.cycleSnapTolerance() },
                onToggleDynamicSnap = { viewModel.toggleDynamicSnap() },
                onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                onDeleteSelected = { viewModel.deleteSelectedEntity() },
                onToggleGrid = { viewModel.toggleGrid() },
                onUndo = { viewModel.undoLast() },
                onRedo = { viewModel.redoLast() },
                undoDepth = undoDepth,
                redoDepth = redoDepth,
                onToggleWorldSnap = { viewModel.toggleSnapWorldMode() },
                onCycleWorldSnapTol = { viewModel.cycleSnapWorldTolerance() },
                worldSnapMode = worldSnapMode,
                worldSnapTol = worldSnapTol,
                onExport = {
                    val r = viewModel.exportPicked(context)
                    scope.launch { snackbar.showSnackbar(r.fold({"CSV: ${it.name}"},{"Hata: ${it.message}"})) }
                },
                onExportGeoJson = {
                    val r = viewModel.exportPickedGeoJson(context)
                    scope.launch { snackbar.showSnackbar(r.fold({"GeoJSON: ${it.name}"},{"Hata: ${it.message}"})) }
                },
                onExportDxf = {
                    val r = viewModel.exportPickedDxf(context)
                    scope.launch { snackbar.showSnackbar(r.fold({"DXF: ${it.name}"},{"Hata: ${it.message}"})) }
                },
                onOpen = { openDocLauncher.launch(arrayOf("application/dxf","application/octet-stream","text/plain","text/csv","application/json","application/zip","*/*")) },
                onReloadSample = { viewModel.loadSample() },
                onClearMeasure = { viewModel.clearPicked() },
                snackbar = snackbar
            ) {
                CadScreen(vm = viewModel, snackbarHost = snackbar)
            }
        }
    }

    companion object {
        // Basit state aktarımı (config change durumunda kaybolabilir; prod için SavedStateHandle tercih edin)
        private var pendingUri: Uri? = null
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CadScaffold(
    mode: CadViewModel.MeasurementMode,
    snapEnabled: Boolean,
    effectiveSnapTolerance: Int,
    dynamicSnap: Boolean,
    selectionMode: Boolean,
    hasSelection: Boolean,
    gridVisible: Boolean,
    onToggleMode: () -> Unit,
    onToggleSnap: () -> Unit,
    onCycleSnapTol: () -> Unit,
    onToggleDynamicSnap: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onDeleteSelected: () -> Unit,
    onToggleGrid: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    undoDepth: Int,
    redoDepth: Int,
    onToggleWorldSnap: () -> Unit,
    onCycleWorldSnapTol: () -> Unit,
    worldSnapMode: Boolean,
    worldSnapTol: Double,
    onExport: () -> Unit,
    onExportGeoJson: () -> Unit,
    onExportDxf: () -> Unit,
    onOpen: () -> Unit,
    onReloadSample: () -> Unit,
    onClearMeasure: () -> Unit,
    snackbar: SnackbarHostState,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val snapPart = if (snapEnabled) {
                        val modeStr = if (dynamicSnap) "Dyn" else "Fix"
                        ", Snap:${effectiveSnapTolerance}px($modeStr)"
                    } else ""
                    val worldSnapPart = if (worldSnapMode) ", WSnap:${"%.2f".format(worldSnapTol)}" else ""
                    val undoInfo = if (undoDepth>0 || redoDepth>0) ", U:${undoDepth}/R:${redoDepth}" else ""
                    Text("CAD (${if (mode==CadViewModel.MeasurementMode.DISTANCE) "Mesafe" else "Alan"}$snapPart$worldSnapPart$undoInfo${if (selectionMode) ", Seçim" else ""})")
                },
                actions = {
                    IconButton(onClick = onToggleMode) { Icon(Icons.Outlined.SwapHoriz, contentDescription = "Mod") }
                    IconButton(onClick = onToggleSnap) { Icon(Icons.Outlined.Anchor, contentDescription = "Snap") }
                    IconButton(onClick = onCycleSnapTol, enabled = snapEnabled) { Icon(Icons.Outlined.Tune, contentDescription = "Snap Tolerans") }
                    IconButton(onClick = onToggleDynamicSnap, enabled = snapEnabled) { Icon(if (dynamicSnap) Icons.Outlined.GridOn else Icons.Outlined.GridOff, contentDescription = "Dinamik Snap") }
                    IconButton(onClick = onToggleWorldSnap) { Icon(if (worldSnapMode) Icons.Outlined.SelectAll else Icons.Outlined.SelectAll, contentDescription = "World Snap") }
                    IconButton(onClick = onCycleWorldSnapTol, enabled = worldSnapMode) { Icon(Icons.Outlined.Tune, contentDescription = "World Snap Tol") }
                    IconButton(onClick = onToggleSelectionMode) { Icon(Icons.Outlined.SelectAll, contentDescription = "Seçim Modu") }
                    IconButton(onClick = onDeleteSelected, enabled = hasSelection) { Icon(Icons.Outlined.DeleteForever, contentDescription = "Seçili Sil") }
                    IconButton(onClick = onUndo, enabled = undoDepth>0) { Icon(Icons.Outlined.Undo, contentDescription = "Geri Al") }
                    IconButton(onClick = onRedo, enabled = redoDepth>0) { Icon(Icons.Outlined.Refresh, contentDescription = "İleri Al") }
                    IconButton(onClick = onExport) { Icon(Icons.Outlined.Save, contentDescription = "CSV") }
                    IconButton(onClick = onExportGeoJson) { Icon(Icons.Outlined.Share, contentDescription = "GeoJSON") }
                    IconButton(onClick = onExportDxf) { Icon(Icons.Outlined.FolderOpen, contentDescription = "DXF Export") }
                    IconButton(onClick = onOpen, enabled = true) { Icon(Icons.Outlined.FolderOpen, contentDescription = "Aç") }
                    IconButton(onClick = onReloadSample) { Icon(Icons.Outlined.Refresh, contentDescription = "Örnek") }
                    IconButton(onClick = onClearMeasure) { Icon(Icons.Outlined.Delete, contentDescription = "Temizle") }
                    IconButton(onClick = onToggleGrid) { Icon(if (gridVisible) Icons.Outlined.GridOff else Icons.Outlined.GridOn, contentDescription = "Grid") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        content = { inner ->
            Box(Modifier.padding(inner)) { content() }
        }
    )
}
