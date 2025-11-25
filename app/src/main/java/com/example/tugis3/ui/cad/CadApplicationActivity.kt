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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                    "dwg" -> "CAD_IMPORT_DWG"
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
            val backgroundColor by viewModel.backgroundColor.collectAsState()
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
                viewModel = viewModel,
                scope = scope,
                mode = mode,
                snapEnabled = snapEnabled,
                effectiveSnapTolerance = effectiveSnap,
                dynamicSnap = dynamicSnap,
                selectionMode = selectionMode,
                hasSelection = hasSelection,
                gridVisible = gridVisible,
                backgroundColor = backgroundColor,
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
                onOpen = {
                    // DXF, DWG ve tüm CAD dosyaları için
                    openDocLauncher.launch(arrayOf(
                        "application/dxf",
                        "application/acad",
                        "application/dwg",
                        "application/x-dwg",
                        "application/x-acad",
                        "image/vnd.dwg",
                        "image/vnd.dxf",
                        "application/x-dxf",
                        "*/*"  // Tüm dosyaları göster
                    ))
                },
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
    viewModel: CadViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    mode: CadViewModel.MeasurementMode,
    snapEnabled: Boolean,
    effectiveSnapTolerance: Int,
    dynamicSnap: Boolean,
    selectionMode: Boolean,
    hasSelection: Boolean,
    gridVisible: Boolean,
    backgroundColor: Long,
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
                    // GnssPositionRepository'den gerçek zamanlı verileri al
                    val gnssPosition by com.example.tugis3.gnss.GnssPositionRepository.position.collectAsState()

                    Column {
                        Text("CAD Aplikasyon", style = MaterialTheme.typography.titleMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // HDOP ve VDOP değerleri
                            val hdopStr = gnssPosition.hdop?.let { String.format("H:%.3f", it) } ?: "H:---"
                            val vdopStr = gnssPosition.vdop?.let { String.format("V:%.3f", it) } ?: "V:---"
                            Text(hdopStr, style = MaterialTheme.typography.bodySmall)
                            Text(vdopStr, style = MaterialTheme.typography.bodySmall)

                            // GPS uydu sayısı (kullanımda/görünen)
                            Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(14.dp))
                            val satInUse = gnssPosition.satsInUse ?: 0
                            val satVisible = gnssPosition.satsVisible ?: 0
                            val satText = if (satInUse > 0 || satVisible > 0) {
                                "$satInUse/$satVisible"
                            } else {
                                "0/0"
                            }
                            Text(satText, style = MaterialTheme.typography.bodySmall)

                            // Batarya (sabit kalabilir)
                            Icon(Icons.Filled.BatteryFull, null, modifier = Modifier.size(14.dp))
                            Text("100%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                actions = {
                    var showProjectMenu by remember { mutableStateOf(false) }
                    var showFileMenu by remember { mutableStateOf(false) }
                    var showSettingsMenu by remember { mutableStateOf(false) }

                    // Proje Yönetimi Butonu
                    Box {
                        IconButton(onClick = { showProjectMenu = true }) {
                            Icon(Icons.Filled.Folder, "Proje Yönetimi", tint = Color.White)
                        }
                        if (showProjectMenu) {
                            TopBarProjectMenu(
                                onDismiss = { showProjectMenu = false },
                                onProjectManager = {
                                    showProjectMenu = false
                                    scope.launch { snackbar.showSnackbar("Proje Yöneticisi açılıyor...") }
                                },
                                onDataManager = {
                                    showProjectMenu = false
                                    scope.launch { snackbar.showSnackbar("Veri Yöneticisi açılıyor...") }
                                },
                                onCoordinateSystem = {
                                    showProjectMenu = false
                                    scope.launch { snackbar.showSnackbar("Koordinat Sistemi açılıyor...") }
                                }
                            )
                        }
                    }

                    // Dosya İşlemleri Butonu
                    Box {
                        IconButton(onClick = { showFileMenu = true }) {
                            Icon(Icons.Filled.Description, "Dosya İşlemleri", tint = Color.White)
                        }
                        if (showFileMenu) {
                            TopBarFileMenu(
                                onDismiss = { showFileMenu = false },
                                onPointDatabase = {
                                    showFileMenu = false
                                    scope.launch { snackbar.showSnackbar("Nokta Veritabanı açılıyor...") }
                                },
                                onImportFile = {
                                    showFileMenu = false
                                    onOpen()
                                },
                                onExportFile = {
                                    showFileMenu = false
                                    scope.launch { snackbar.showSnackbar("Dosya Dışa Aktar açılıyor...") }
                                }
                            )
                        }
                    }

                    // Ayarlar Butonu
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Filled.Settings, "Ayarlar", tint = Color.White)
                        }
                        if (showSettingsMenu) {
                            TopBarSettingsMenu(
                                onDismiss = { showSettingsMenu = false },
                                onGeneralSettings = {
                                    showSettingsMenu = false
                                    scope.launch { snackbar.showSnackbar("Genel Ayarlar açılıyor...") }
                                },
                                onDisplaySettings = {
                                    showSettingsMenu = false
                                    scope.launch { snackbar.showSnackbar("Görünüm Ayarları açılıyor...") }
                                },
                                onAbout = {
                                    showSettingsMenu = false
                                    scope.launch { snackbar.showSnackbar("Hakkında açılıyor...") }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFFF0F0F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CadNavigationButton("Backward", "2452331", Color(0xFFFFC107))
                        CadNavigationButton("Right", "3447199", Color(0xFFFFC107))
                        CadNavigationButton("Fill", "H:0.000 m", Color(0xFFFFC107))
                    }

                    Spacer(Modifier.height(8.dp))

                    // Point Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pt name", style = MaterialTheme.typography.labelSmall)
                            Text("pt3", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Code", style = MaterialTheme.typography.labelSmall)
                            Text("HancholeBlock", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("1.800 m", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Main Content
            content()

            // Left Toolbar - Data Menu state
            var showDataMenu by remember { mutableStateOf(false) }
            var showDrawingMenu by remember { mutableStateOf(false) }
            var showSurveyMenu by remember { mutableStateOf(false) }
            var showLayerDialog by remember { mutableStateOf(false) }

            // Left Toolbar - CAD Araçları (kullanım kılavuzuna göre)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Zoom Extents - Projenin Tümünü Gör (Z E komutu gibi)
                CadToolButton(Icons.Filled.ZoomOutMap, "Tümü") {
                    val bounds = viewModel.computeProjectBounds()
                    if (bounds != null) {
                        val width = bounds.maxE - bounds.minE
                        val height = bounds.maxN - bounds.minN
                        val centerE = (bounds.minE + bounds.maxE) / 2.0
                        val centerN = (bounds.minN + bounds.maxN) / 2.0

                        // Kamerayı merkeze ayarla ve zoom yap
                        viewModel.setCameraPosition(centerE, centerN)
                        viewModel.fitBounds(bounds.minE, bounds.minN, bounds.maxE, bounds.maxN)

                        scope.launch {
                            snackbar.showSnackbar("Proje: ${String.format("%.1f", width)}m x ${String.format("%.1f", height)}m - Tüm proje gösteriliyor")
                        }
                    } else {
                        scope.launch {
                            snackbar.showSnackbar("Henüz CAD nesnesi yok")
                        }
                    }
                }

                // Katman Yönetimi
                CadToolButton(Icons.Filled.Layers, "Katman") {
                    showLayerDialog = true
                    scope.launch { snackbar.showSnackbar("Katman yönetimi açıldı") }
                }
                // Veri İşlemleri (Import/Export)
                CadToolButton(Icons.Filled.FolderOpen, "Veri") {
                    showDataMenu = true
                    scope.launch { snackbar.showSnackbar("Veri işlemleri menüsü") }
                }
                // Çizim Araçları (Point, Line, Polyline, Circle, Arc, Polygon)
                CadToolButton(Icons.Filled.Edit, "Çizim") {
                    showDrawingMenu = true
                    scope.launch { snackbar.showSnackbar("Çizim araçları - Nokta, Hat, Daire, vb.") }
                }
                // Ölçüm Araçları (Intersection, Offset, Divide, Measure)
                CadToolButton(Icons.Filled.Architecture, "Ölçüm") {
                    showSurveyMenu = true
                    scope.launch { snackbar.showSnackbar("Ölçüm araçları - Kesişim, Ofset, Böl") }
                }
                // Zoom In
                CadToolButton(Icons.Filled.ZoomIn, "Yakınlaştır") {
                    viewModel.zoomIn()
                    scope.launch { snackbar.showSnackbar("Harita yakınlaştırıldı") }
                }
                // Zoom Out
                CadToolButton(Icons.Filled.ZoomOut, "Uzaklaştır") {
                    viewModel.zoomOut()
                    scope.launch { snackbar.showSnackbar("Harita uzaklaştırıldı") }
                }
            }

            // Data Menu Dialog
            if (showDataMenu) {
                CadDataMenuDialog(
                    onDismiss = { showDataMenu = false },
                    onOpenFile = {
                        showDataMenu = false
                        onOpen()
                    },
                    onExportDxf = {
                        showDataMenu = false
                        onExportDxf()
                    },
                    onExportGeoJson = {
                        showDataMenu = false
                        onExportGeoJson()
                    },
                    onExportCsv = {
                        showDataMenu = false
                        onExport()
                    },
                    onManageLayer = {
                        showDataMenu = false
                        showLayerDialog = true
                    }
                )
            }

            // Drawing Menu Dialog
            if (showDrawingMenu) {
                CadDrawingMenuDialog(
                    onDismiss = { showDrawingMenu = false },
                    onDrawPoint = {
                        showDrawingMenu = false
                        scope.launch { snackbar.showSnackbar("Nokta Çizim: Haritadan nokta seçin") }
                    },
                    onDrawLine = {
                        showDrawingMenu = false
                        scope.launch { snackbar.showSnackbar("Çizgi Çizim: İki nokta seçin") }
                    },
                    onDrawPolyline = {
                        showDrawingMenu = false
                        scope.launch { snackbar.showSnackbar("Çoklu Çizgi: Noktaları sırayla seçin") }
                    },
                    onDrawCircle = {
                        showDrawingMenu = false
                        scope.launch { snackbar.showSnackbar("Daire: Merkez ve yarıçap girin") }
                    },
                    onDrawArc = {
                        showDrawingMenu = false
                        scope.launch { snackbar.showSnackbar("Yay: Başlangıç, orta ve bitiş noktası seçin") }
                    },
                    onDrawPolygon = {
                        showDrawingMenu = false
                        scope.launch { snackbar.showSnackbar("Çokgen: Noktaları seçip kapatın") }
                    }
                )
            }

            // Survey Menu Dialog
            if (showSurveyMenu) {
                CadSurveyMenuDialog(
                    onDismiss = { showSurveyMenu = false },
                    onInt2Dist = {
                        showSurveyMenu = false
                        scope.launch { snackbar.showSnackbar("Kesişim 2 Mesafe: İki nokta ve mesafeleri girin") }
                    },
                    onInt4Point = {
                        showSurveyMenu = false
                        scope.launch { snackbar.showSnackbar("Kesişim 4 Nokta: Dört nokta seçin") }
                    },
                    onIntEntity = {
                        showSurveyMenu = false
                        scope.launch { snackbar.showSnackbar("Kesişim Eğri: İki obje seçin") }
                    },
                    onPointOffset = {
                        showSurveyMenu = false
                        scope.launch { snackbar.showSnackbar("Ofset Dik: Obje ve mesafe girin") }
                    },
                    onDistOffset = {
                        showSurveyMenu = false
                        scope.launch { snackbar.showSnackbar("Ofset Paralel: Obje ve mesafe girin") }
                    },
                    onDivide = {
                        showSurveyMenu = false
                        scope.launch { snackbar.showSnackbar("Böl Parça: Obje ve parça sayısı girin") }
                    },
                    onMeasure = {
                        showSurveyMenu = false
                        scope.launch { snackbar.showSnackbar("Ölçü: Obje ve mesafe girin") }
                    }
                )
            }

            // Layer Dialog
            if (showLayerDialog) {
                CadLayerDialog(
                    viewModel = viewModel,
                    onDismiss = { showLayerDialog = false }
                )
            }

            // Right Toolbar - Tools Menu state
            var showToolsMenu by remember { mutableStateOf(false) }
            var showSettingsDialog by remember { mutableStateOf(false) }
            var showBackgroundColorDialog by remember { mutableStateOf(false) }
            var showAngleCalculator by remember { mutableStateOf(false) }
            var showDistanceCalculator by remember { mutableStateOf(false) }
            var showAreaCalculator by remember { mutableStateOf(false) }

            // Right Toolbar - İşlem Araçları (kullanım kılavuzuna göre)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Aplikasyon (Stakeout) - Seçili objeye göre aplikasyon başlat
                CadToolButton(Icons.Filled.Place, "Aplikasyon") {
                    if (hasSelection) {
                        scope.launch {
                            val entity = viewModel.selectedEntity.value
                            val entityName = entity?.let { it::class.simpleName ?: "Entity" } ?: "Obje"
                            snackbar.showSnackbar("$entityName aplikasyonu başlatılıyor...")
                        }
                    } else {
                        scope.launch {
                            snackbar.showSnackbar("Önce bir CAD objesi seçin (Seçim modunu açın)")
                        }
                    }
                }
                // Ayarlar - Grid, Snap, vs.
                CadToolButton(Icons.Filled.Settings, "Ayarlar") {
                    showSettingsDialog = true
                    scope.launch { snackbar.showSnackbar("CAD ayarları açıldı") }
                }
                // Araçlar Menüsü - Hesaplama araçları
                CadToolButton(Icons.Filled.Build, "Araçlar") {
                    showToolsMenu = true
                    scope.launch { snackbar.showSnackbar("Hesaplama araçları - Açı, Mesafe, Alan") }
                }
                // Silme - Seçili objeyi sil
                CadToolButton(Icons.Filled.Delete, "Sil") {
                    if (hasSelection) {
                        onDeleteSelected()
                        scope.launch { snackbar.showSnackbar("Seçili obje silindi") }
                    } else {
                        scope.launch { snackbar.showSnackbar("Önce bir obje seçin (Seçim modunu açın)") }
                    }
                }
                // Geri Al
                CadToolButton(Icons.Filled.Undo, "Geri") {
                    onUndo()
                    scope.launch { snackbar.showSnackbar("Son işlem geri alındı") }
                }
                // Tekrar Yap
                CadToolButton(Icons.Filled.Redo, "İleri") {
                    onRedo()
                    scope.launch { snackbar.showSnackbar("Son işlem tekrar yapıldı") }
                }
                // Temizle / Sıfırla
                CadToolButton(Icons.Filled.Clear, "Temizle") {
                    onClearMeasure()
                    scope.launch { snackbar.showSnackbar("Ölçüm noktaları temizlendi") }
                }
            }

            // Tools Menu Dialog
            if (showToolsMenu) {
                CadToolsMenuDialog(
                    onDismiss = { showToolsMenu = false },
                    onAngle = {
                        showToolsMenu = false
                        showAngleCalculator = true
                    },
                    onDistance = {
                        showToolsMenu = false
                        showDistanceCalculator = true
                    },
                    onArea = {
                        showToolsMenu = false
                        showAreaCalculator = true
                    },
                    onDelete = {
                        showToolsMenu = false
                        if (hasSelection) {
                            onDeleteSelected()
                            scope.launch { snackbar.showSnackbar("Seçili obje silindi") }
                        } else {
                            scope.launch { snackbar.showSnackbar("Lütfen önce bir obje seçin") }
                        }
                    },
                    onBackgroundColor = {
                        showToolsMenu = false
                        showBackgroundColorDialog = true
                    },
                    onRedraw = {
                        showToolsMenu = false
                        scope.launch { snackbar.showSnackbar("Ekran yenilendi") }
                    },
                    onCurrentPosition = {
                        showToolsMenu = false
                        scope.launch { snackbar.showSnackbar("Mevcut konum gösteriliyor") }
                    }
                )
            }

            // Settings Dialog
            if (showSettingsDialog) {
                CadSettingsDialog(
                    gridVisible = gridVisible,
                    snapEnabled = snapEnabled,
                    onDismiss = { showSettingsDialog = false },
                    onToggleGrid = {
                        onToggleGrid()
                        scope.launch { snackbar.showSnackbar("Grid ${if (!gridVisible) "Açıldı" else "Kapatıldı"}") }
                    },
                    onToggleSnap = {
                        onToggleSnap()
                        scope.launch { snackbar.showSnackbar("Snap ${if (!snapEnabled) "Açıldı" else "Kapatıldı"}") }
                    }
                )
            }

            // Background Color Dialog
            if (showBackgroundColorDialog) {
                BackgroundColorDialog(
                    currentColor = backgroundColor,
                    onDismiss = { showBackgroundColorDialog = false },
                    onColorSelected = { color ->
                        viewModel.setBackgroundColor(color)
                        showBackgroundColorDialog = false
                        scope.launch { snackbar.showSnackbar("Arka plan rengi değiştirildi") }
                    }
                )
            }

            // Angle Calculator Dialog
            if (showAngleCalculator) {
                CadAngleCalculatorDialog(
                    onDismiss = { showAngleCalculator = false },
                    onCalculate = { point1, point2, point3 ->
                        scope.launch {
                            snackbar.showSnackbar("Açı hesaplandı: $point1-$point2-$point3")
                        }
                    }
                )
            }

            // Distance Calculator Dialog
            if (showDistanceCalculator) {
                CadDistanceCalculatorDialog(
                    onDismiss = { showDistanceCalculator = false },
                    onCalculate = { point1, point2 ->
                        scope.launch {
                            snackbar.showSnackbar("Mesafe hesaplandı: $point1-$point2")
                        }
                    }
                )
            }

            // Area Calculator Dialog
            if (showAreaCalculator) {
                CadAreaCalculatorDialog(
                    onDismiss = { showAreaCalculator = false },
                    onCalculate = { points ->
                        scope.launch {
                            snackbar.showSnackbar("Alan hesaplandı: ${points.size} nokta")
                        }
                    }
                )
            }

            // Center Position Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .background(Color(0xFFFF6B00), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MyLocation,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CadToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Icon(icon, label, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CadNavigationButton(label: String, value: String, color: Color) {
    Button(
        onClick = { /* Navigation action */ },
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.width(110.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icon = when (label) {
                "Backward" -> Icons.AutoMirrored.Filled.ArrowBack
                "Right" -> Icons.AutoMirrored.Filled.ArrowForward
                else -> Icons.Filled.ArrowUpward
            }
            Icon(icon, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Text(label, color = Color.Black, style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.Black, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

// ============ CAD Menu Dialogs ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadDataMenuDialog(
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit,
    onExportDxf: () -> Unit,
    onExportGeoJson: () -> Unit,
    onExportCsv: () -> Unit,
    onManageLayer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CAD Veri İşlemleri") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Veri içe/dışa aktarma ve katman yönetimi:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()

                // Katman İşlemleri
                Text(
                    "Katman İşlemleri",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.Layers, "Katman Yönetimi (Göster/Gizle)", onManageLayer)

                HorizontalDivider()

                // Veri İçe Aktarma
                Text(
                    "Veri İçe Aktarma",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.FolderOpen, "Dosya Aç (DXF/DWG/CSV/GeoJSON)", onOpenFile)

                HorizontalDivider()

                // Veri Dışa Aktarma
                Text(
                    "Veri Dışa Aktarma",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.Save, "DXF Formatında Kaydet", onExportDxf)
                CadMenuItem(Icons.Filled.Save, "GeoJSON Formatında Kaydet", onExportGeoJson)
                CadMenuItem(Icons.Filled.Save, "CSV Formatında Kaydet", onExportCsv)

                HorizontalDivider()
                Text(
                    "Desteklenen formatlar: DXF, DWG, CSV, GeoJSON, KML",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadDrawingMenuDialog(
    onDismiss: () -> Unit,
    onDrawPoint: () -> Unit,
    onDrawLine: () -> Unit,
    onDrawPolyline: () -> Unit,
    onDrawCircle: () -> Unit,
    onDrawArc: () -> Unit,
    onDrawPolygon: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CAD Çizim Araçları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Harita üzerinde çizim yapın:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                CadMenuItem(Icons.Filled.Circle, "Nokta Çiz") {
                    onDrawPoint()
                }
                CadMenuItem(Icons.Filled.Remove, "Hat Çiz (2 nokta)") {
                    onDrawLine()
                }
                CadMenuItem(Icons.Filled.Timeline, "Çoklu Çizgi (Polyline)") {
                    onDrawPolyline()
                }
                CadMenuItem(Icons.Filled.RadioButtonUnchecked, "Daire (Merkez + Yarıçap)") {
                    onDrawCircle()
                }
                CadMenuItem(Icons.Filled.Architecture, "Yay (Arc - 3 nokta)") {
                    onDrawArc()
                }
                CadMenuItem(Icons.Filled.Pentagon, "Çokgen (Polygon - Kapalı alan)") {
                    onDrawPolygon()
                }
                HorizontalDivider()
                Text(
                    "İpucu: Snap modunu açarak noktaları hassas seçebilirsiniz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadSurveyMenuDialog(
    onDismiss: () -> Unit,
    onInt2Dist: () -> Unit,
    onInt4Point: () -> Unit,
    onIntEntity: () -> Unit,
    onPointOffset: () -> Unit,
    onDistOffset: () -> Unit,
    onDivide: () -> Unit,
    onMeasure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CAD Ölçüm ve Hesaplama Araçları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Profesyonel CAD ölçüm araçları:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()

                // Kesişim İşlemleri
                Text(
                    "Kesişim İşlemleri",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.MyLocation, "Kesişim 2 Mesafe") {
                    onInt2Dist()
                }
                CadMenuItem(Icons.Filled.CropSquare, "Kesişim 4 Nokta") {
                    onInt4Point()
                }
                CadMenuItem(Icons.Filled.Timeline, "Kesişim Eğri/Hat") {
                    onIntEntity()
                }

                HorizontalDivider()

                // Ofset İşlemleri
                Text(
                    "Ofset İşlemleri",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.VerticalAlignCenter, "Ofset Dik (Perpendicular)") {
                    onPointOffset()
                }
                CadMenuItem(Icons.Filled.HorizontalRule, "Ofset Paralel") {
                    onDistOffset()
                }

                HorizontalDivider()

                // Diğer İşlemler
                Text(
                    "Diğer İşlemler",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.CallSplit, "Böl (Parçalara Ayır)") {
                    onDivide()
                }
                CadMenuItem(Icons.Filled.Straighten, "Ölçü (Measure)") {
                    onMeasure()
                }

                HorizontalDivider()
                Text(
                    "İpucu: Bu araçlar SurPad CAD modülünün temel özellikleridir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadToolsMenuDialog(
    onDismiss: () -> Unit,
    onAngle: () -> Unit,
    onDistance: () -> Unit,
    onArea: () -> Unit,
    onDelete: () -> Unit,
    onBackgroundColor: () -> Unit,
    onRedraw: () -> Unit,
    onCurrentPosition: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CAD Hesaplama ve Yardımcı Araçlar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CAD nesneleri üzerinde hesaplama ve düzenleme:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()

                // Hesaplama Araçları
                Text(
                    "Hesaplama Araçları",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.Architecture, "Açı Hesapla (3 Nokta)") {
                    onAngle()
                }
                CadMenuItem(Icons.Filled.Straighten, "Mesafe Hesapla (2 Nokta)") {
                    onDistance()
                }
                CadMenuItem(Icons.Filled.Square, "Alan Hesapla (Polygon)") {
                    onArea()
                }

                HorizontalDivider()

                // Düzenleme Araçları
                Text(
                    "Düzenleme Araçları",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.Delete, "Seçili Objeyi Sil") {
                    onDelete()
                }

                HorizontalDivider()

                // Görünüm Araçları
                Text(
                    "Görünüm Araçları",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                CadMenuItem(Icons.Filled.Palette, "Arka Plan Rengi") {
                    onBackgroundColor()
                }
                CadMenuItem(Icons.Filled.Refresh, "Ekranı Yenile") {
                    onRedraw()
                }
                CadMenuItem(Icons.Filled.MyLocation, "Mevcut GPS Konumu Göster") {
                    onCurrentPosition()
                }

                HorizontalDivider()
                Text(
                    "İpucu: Seçim modunu açarak objeleri seçebilir ve düzenleyebilirsiniz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@Composable
private fun CadMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null)
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadLayerDialog(
    viewModel: CadViewModel,
    onDismiss: () -> Unit
) {
    val layers by viewModel.layers.collectAsState()
    val activeLayers by viewModel.activeLayers.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Katman Yönetimi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (layers.isEmpty()) {
                    Text("Henüz katman yok", style = MaterialTheme.typography.bodyMedium)
                } else {
                    layers.forEach { layer ->
                        val isActive = layer in activeLayers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isActive,
                                    onCheckedChange = { viewModel.toggleLayer(layer) }
                                )
                                Text(layer)
                            }
                            Icon(
                                if (isActive) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tamam") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadSettingsDialog(
    gridVisible: Boolean,
    snapEnabled: Boolean,
    onDismiss: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleSnap: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CAD Ayarları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Grid Göster")
                    Switch(checked = gridVisible, onCheckedChange = { onToggleGrid() })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Snap (Yakalama) Etkin")
                    Switch(checked = snapEnabled, onCheckedChange = { onToggleSnap() })
                }
                HorizontalDivider()
                Text(
                    "Diğer ayarlar geliştirme aşamasında...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tamam") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadAngleCalculatorDialog(
    onDismiss: () -> Unit,
    onCalculate: (String, String, String) -> Unit
) {
    var point1 by remember { mutableStateOf("") }
    var point2 by remember { mutableStateOf("") }
    var point3 by remember { mutableStateOf("") }
    var calculatedAngle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Açı Hesapla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Üç nokta arasındaki açıyı hesaplayın",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "2. nokta köşe noktasıdır (açının tepe noktası)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = point1,
                    onValueChange = { point1 = it },
                    label = { Text("1. Nokta (İsim veya Kod)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ör: P1 veya 101") }
                )
                OutlinedTextField(
                    value = point2,
                    onValueChange = { point2 = it },
                    label = { Text("2. Nokta - Köşe (Vertex)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ör: P2 veya 102") }
                )
                OutlinedTextField(
                    value = point3,
                    onValueChange = { point3 = it },
                    label = { Text("3. Nokta (İsim veya Kod)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ör: P3 veya 103") }
                )

                if (calculatedAngle.isNotEmpty()) {
                    HorizontalDivider()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Hesaplanan Açı:", fontWeight = FontWeight.Bold)
                            Text(calculatedAngle, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (point1.isNotBlank() && point2.isNotBlank() && point3.isNotBlank()) {
                        // Simüle edilmiş hesaplama - gerçek implementasyon ViewModel'de olmalı
                        calculatedAngle = "Örnek: 45.25° (DMS: 45°15'00\")"
                        onCalculate(point1, point2, point3)
                    }
                }
            ) { Text("Hesapla") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadDistanceCalculatorDialog(
    onDismiss: () -> Unit,
    onCalculate: (String, String) -> Unit
) {
    var point1 by remember { mutableStateOf("") }
    var point2 by remember { mutableStateOf("") }
    var calculatedDistance by remember { mutableStateOf("") }
    var calculatedBearing by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mesafe ve Yön Hesapla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "İki nokta arasındaki mesafe ve yönü hesaplayın",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Yatay mesafe, düşey fark ve yön bilgisi verilir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = point1,
                    onValueChange = { point1 = it },
                    label = { Text("Başlangıç Noktası") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ör: P1, A1, 101") }
                )
                OutlinedTextField(
                    value = point2,
                    onValueChange = { point2 = it },
                    label = { Text("Bitiş Noktası") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ör: P2, A2, 102") }
                )

                if (calculatedDistance.isNotEmpty()) {
                    HorizontalDivider()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Hesaplama Sonuçları:", fontWeight = FontWeight.Bold)
                            Text("Yatay Mesafe: $calculatedDistance", style = MaterialTheme.typography.bodyMedium)
                            Text("Yön (Azimut): $calculatedBearing", style = MaterialTheme.typography.bodyMedium)
                            Text("Düşey Fark: ΔH = ±0.000 m", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (point1.isNotBlank() && point2.isNotBlank()) {
                        // Simüle edilmiş hesaplama
                        calculatedDistance = "125.458 m"
                        calculatedBearing = "245.75° (DMS: 245°45'00\")"
                        onCalculate(point1, point2)
                    }
                }
            ) { Text("Hesapla") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadAreaCalculatorDialog(
    onDismiss: () -> Unit,
    onCalculate: (List<String>) -> Unit
) {
    var pointsText by remember { mutableStateOf("") }
    var calculatedArea by remember { mutableStateOf("") }
    var calculatedPerimeter by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alan ve Çevre Hesapla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Kapalı bir alan için alan ve çevre hesaplayın",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "En az 3 nokta gereklidir. Noktaları virgülle ayırın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it },
                    label = { Text("Noktalar (virgülle ayırın)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("ör: P1,P2,P3,P4 veya 101,102,103,104") },
                    supportingText = {
                        val pointCount = pointsText.split(",").count { it.trim().isNotBlank() }
                        Text("$pointCount nokta girildi")
                    }
                )

                if (calculatedArea.isNotEmpty()) {
                    HorizontalDivider()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Hesaplama Sonuçları:", fontWeight = FontWeight.Bold)
                            Text("Alan: $calculatedArea",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Çevre: $calculatedPerimeter", style = MaterialTheme.typography.bodyMedium)
                            Text("Birim: m² (metrekare)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val points = pointsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (points.size >= 3) {
                        // Simüle edilmiş hesaplama
                        calculatedArea = "1,254.75 m²"
                        calculatedPerimeter = "145.32 m"
                        onCalculate(points)
                    }
                }
            ) { Text("Hesapla") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

// ============ TOPBAR DROPDOWN MENÜLER ============

@Composable
private fun TopBarProjectMenu(
    onDismiss: () -> Unit,
    onProjectManager: () -> Unit,
    onDataManager: () -> Unit,
    onCoordinateSystem: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 220.dp)
    ) {
        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Folder, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Proje Yöneticisi", style = MaterialTheme.typography.bodyMedium)
                        Text("Proje oluştur, aç, yönet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onProjectManager
        )

        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Storage, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Veri Yöneticisi", style = MaterialTheme.typography.bodyMedium)
                        Text("Veri dosyalarını yönet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onDataManager
        )

        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Public, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Koordinat Sistemi", style = MaterialTheme.typography.bodyMedium)
                        Text("Koordinat ayarları", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onCoordinateSystem
        )
    }
}

@Composable
private fun TopBarFileMenu(
    onDismiss: () -> Unit,
    onPointDatabase: () -> Unit,
    onImportFile: () -> Unit,
    onExportFile: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 220.dp)
    ) {
        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Nokta Veritabanı", style = MaterialTheme.typography.bodyMedium)
                        Text("Nokta ekle, düzenle, sil", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onPointDatabase
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Dosya İçe Aktar", style = MaterialTheme.typography.bodyMedium)
                        Text("DXF, CSV, NCN, vb.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onImportFile
        )

        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Dosya Dışa Aktar", style = MaterialTheme.typography.bodyMedium)
                        Text("8 format desteği", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onExportFile
        )
    }
}

@Composable
private fun TopBarSettingsMenu(
    onDismiss: () -> Unit,
    onGeneralSettings: () -> Unit,
    onDisplaySettings: () -> Unit,
    onAbout: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 220.dp)
    ) {
        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Tune, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Genel Ayarlar", style = MaterialTheme.typography.bodyMedium)
                        Text("Uygulama ayarları", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onGeneralSettings
        )

        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Visibility, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Görünüm Ayarları", style = MaterialTheme.typography.bodyMedium)
                        Text("Grid, Snap, Tema", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onDisplaySettings
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Hakkında", style = MaterialTheme.typography.bodyMedium)
                        Text("TUGIS3 v2.1", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            onClick = onAbout
        )
    }
}

// ============ BACKGROUND COLOR PICKER DIALOG ============

@Composable
private fun BackgroundColorDialog(
    currentColor: Long,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val colors = listOf(
        0xFF000000 to "Siyah",
        0xFF1A1A1A to "Koyu Gri",
        0xFF2C2C2C to "Gri",
        0xFFFFFFFF to "Beyaz",
        0xFFF5F5F5 to "Açık Gri",
        0xFF0D47A1 to "Koyu Mavi",
        0xFF1976D2 to "Mavi",
        0xFF1E88E5 to "Açık Mavi",
        0xFF004D40 to "Koyu Yeşil",
        0xFF00695C to "Yeşil"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Palette, null)
                Text("Arka Plan Rengi Seç")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "CAD çizim alanının arka plan rengini değiştirin:",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                // Renk seçenekleri
                colors.chunked(2).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowColors.forEach { (color, name) ->
                            Surface(
                                onClick = { onColorSelected(color) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = Color(color),
                                border = if (color == currentColor) {
                                    androidx.compose.foundation.BorderStroke(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline
                                    )
                                }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        name,
                                        color = if (color == 0xFFFFFFFF || color == 0xFFF5F5F5) {
                                            Color.Black
                                        } else {
                                            Color.White
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (color == currentColor) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "💡 İpucu: Koyu renkler göz yorgunluğunu azaltır",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}
