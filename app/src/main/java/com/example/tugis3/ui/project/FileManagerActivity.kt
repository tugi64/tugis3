package com.example.tugis3.ui.project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.ui.theme.Tugis3Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

@AndroidEntryPoint
class FileManagerActivity : ComponentActivity() {
    private val vm: FileManagerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                FileManagerScreen(vm = vm, onBack = { finish() })
            }
        }
    }
}

private const val EXPORT_DIR_NAME = "exports"
private const val DATE_PATTERN = "yyyyMMdd_HHmmss"

data class ExportFileInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val format: String
)

enum class FileFormat(val displayName: String, val extension: String) {
    CSV("CSV Formatı", "csv"),
    JSON("JSON Formatı", "json"),
    TXT("Metin Dosyası", "txt"),
    NCN("Nikon NCN Formatı", "ncn"),
    DAT("CASS Formatı", "dat"),
    DXF("AutoCAD DXF", "dxf"),
    KML("Google Earth KML", "kml"),
    KMZ("Google Earth KMZ", "kmz"),
    XLS("Excel Formatı", "xls")
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FileManagerViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val pointsFlow: Flow<List<PointEntity>> = activeProject
        .filterNotNull()
        .flatMapLatest { p -> pointRepo.observePoints(p.id) }

    val points = pointsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val exportDir: File by lazy { File(appContext.filesDir, EXPORT_DIR_NAME).apply { mkdirs() } }

    private val _exports = MutableStateFlow<List<ExportFileInfo>>(emptyList())
    val exports: StateFlow<List<ExportFileInfo>> = _exports.asStateFlow()

    private val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _message

    init { refreshExports() }

    fun refreshExports() = viewModelScope.launch(Dispatchers.IO) {
        val list = exportDir.listFiles()?.sortedByDescending { it.lastModified() }?.map {
            ExportFileInfo(
                name = it.name,
                path = it.absolutePath,
                sizeBytes = it.length(),
                lastModified = it.lastModified(),
                format = it.extension.uppercase()
            )
        } ?: emptyList()
        _exports.value = list
    }

    fun exportFile(format: FileFormat) = exportGeneric(
        "points_${sdf.format(Date())}.${format.extension}"
    ) { file, pts ->
        when (format) {
            FileFormat.CSV -> exportCsvFormat(file, pts)
            FileFormat.JSON -> exportJsonFormat(file, pts)
            FileFormat.TXT -> exportTxtFormat(file, pts)
            FileFormat.NCN -> exportNcnFormat(file, pts)
            FileFormat.DAT -> exportDatFormat(file, pts)
            FileFormat.DXF -> exportDxfFormat(file, pts)
            FileFormat.KML -> exportKmlFormat(file, pts)
            FileFormat.KMZ -> _message.tryEmit("KMZ formatı henüz desteklenmiyor")
            FileFormat.XLS -> _message.tryEmit("XLS formatı henüz desteklenmiyor")
        }
    }

    private fun exportCsvFormat(file: File, pts: List<PointEntity>) {
        file.appendText("id,name,northing,easting,height,code,desc\n")
        pts.forEach { p ->
            file.appendText(
                listOf(
                    p.id,
                    escCsv(p.name),
                    p.northing,
                    p.easting,
                    p.ellipsoidalHeight ?: "",
                    escCsv(p.featureCode ?: ""),
                    escCsv(p.description ?: "")
                ).joinToString(",") + "\n"
            )
        }
    }

    private fun exportJsonFormat(file: File, pts: List<PointEntity>) {
        file.appendText("[\n")
        pts.forEachIndexed { i, p ->
            val obj = buildString {
                append("  {\n")
                append("    \"id\": ${p.id},\n")
                append("    \"name\": \"${jsonEsc(p.name)}\",\n")
                append("    \"n\": ${p.northing},\n")
                append("    \"e\": ${p.easting},\n")
                p.ellipsoidalHeight?.let { append("    \"h\": $it,\n") }
                p.featureCode?.let { append("    \"code\": \"${jsonEsc(it)}\",\n") }
                p.description?.let { append("    \"desc\": \"${jsonEsc(it)}\",\n") }
                if (endsWith(",\n")) deleteRange(length - 2, length)
                append("\n  }")
            }
            file.appendText(obj)
            if (i < pts.lastIndex) file.appendText(",")
            file.appendText("\n")
        }
        file.appendText("]")
    }

    private fun exportTxtFormat(file: File, pts: List<PointEntity>) {
        file.appendText("Nokta Listesi\n")
        file.appendText("=".repeat(60) + "\n\n")
        pts.forEach { p ->
            file.appendText("Nokta: ${p.name}\n")
            file.appendText("Kuzey: ${p.northing}\n")
            file.appendText("Doğu: ${p.easting}\n")
            p.ellipsoidalHeight?.let { file.appendText("Yükseklik: $it\n") }
            p.featureCode?.let { file.appendText("Kod: $it\n") }
            p.description?.let { file.appendText("Açıklama: $it\n") }
            file.appendText("\n")
        }
    }

    private fun exportNcnFormat(file: File, pts: List<PointEntity>) {
        // NCN formatı: nokta_adı doğu kuzey yükseklik
        // Örnek: m1 446055.966 4286420.511 1072.775
        pts.forEach { p ->
            val name = p.name.replace(" ", "_") // Boşlukları kaldır
            val easting = String.format(Locale.US, "%.3f", p.easting)
            val northing = String.format(Locale.US, "%.3f", p.northing)
            val height = String.format(Locale.US, "%.3f", p.ellipsoidalHeight ?: 0.0)
            file.appendText("$name $easting $northing $height\n")
        }
    }

    private fun exportDatFormat(file: File, pts: List<PointEntity>) {
        // CASS formatı
        pts.forEach { p ->
            file.appendText("${p.name},${p.easting},${p.northing},${p.ellipsoidalHeight ?: 0.0},${p.featureCode ?: ""}\n")
        }
    }

    private fun exportDxfFormat(file: File, pts: List<PointEntity>) {
        // Basit DXF formatı - sadece POINT entity'leri
        file.appendText("0\nSECTION\n2\nENTITIES\n")
        pts.forEach { p ->
            file.appendText("0\nPOINT\n8\n0\n")
            file.appendText("10\n${p.easting}\n20\n${p.northing}\n")
            p.ellipsoidalHeight?.let { file.appendText("30\n$it\n") }
        }
        file.appendText("0\nENDSEC\n0\nEOF\n")
    }

    private fun exportKmlFormat(file: File, pts: List<PointEntity>) {
        // Basit KML formatı
        file.appendText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        file.appendText("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        file.appendText("<Document>\n")
        file.appendText("<name>Nokta Listesi</name>\n")
        pts.forEach { p ->
            file.appendText("  <Placemark>\n")
            file.appendText("    <name>${xmlEsc(p.name)}</name>\n")
            p.description?.let { file.appendText("    <description>${xmlEsc(it)}</description>\n") }
            file.appendText("    <Point>\n")
            file.appendText("      <coordinates>${p.easting},${p.northing},${p.ellipsoidalHeight ?: 0.0}</coordinates>\n")
            file.appendText("    </Point>\n")
            file.appendText("  </Placemark>\n")
        }
        file.appendText("</Document>\n</kml>\n")
    }

    private fun exportGeneric(filename: String, writer: (File, List<PointEntity>) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            if (_busy.value) return@launch
            _busy.value = true
            runCatching {
                val pts = points.value
                if (pts.isEmpty()) throw IllegalStateException("Nokta listesi boş")
                val file = File(exportDir, filename)
                writer(file, pts)
                _message.tryEmit("Dışa aktarıldı: ${file.name}")
            }.onFailure { e ->
                _message.tryEmit("Hata: ${e.message}")
            }
            _busy.value = false
            refreshExports()
        }

    private fun escCsv(s: String): String =
        if (s.contains(',') || s.contains('"')) '"' + s.replace("\"", "\"\"") + '"' else s

    private fun jsonEsc(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun xmlEsc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun deleteExport(name: String) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            File(exportDir, name).takeIf { it.exists() }?.delete()
            _message.tryEmit("Dosya silindi: $name")
        }
        refreshExports()
    }

    fun createNewDataFile(fileName: String) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val file = File(exportDir, "$fileName.pd")
            if (file.exists()) throw IllegalStateException("Dosya zaten mevcut")
            file.createNewFile()
            file.appendText("PD Veri Dosyası - ${Date()}\n")
            _message.tryEmit("Yeni dosya oluşturuldu: ${file.name}")
        }.onFailure { e ->
            _message.tryEmit("Hata: ${e.message}")
        }
        refreshExports()
    }

    fun openFile(path: String): String {
        return runCatching {
            File(path).takeIf { it.exists() }?.readText() ?: "Dosya bulunamadı"
        }.getOrElse { "Dosya okunamadı: ${it.message}" }
    }

    fun importNcnFile(fileContent: String) {
        // NCN formatını parse et ve nokta olarak kaydet
        val proj = activeProject.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            var importedCount = 0
            runCatching {
                fileContent.trim().lines().forEach { line ->
                    if (line.isBlank()) return@forEach

                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        val pointName = parts[0]
                        val easting = parts[1].toDoubleOrNull() ?: return@forEach
                        val northing = parts[2].toDoubleOrNull() ?: return@forEach
                        val height = parts[3].toDoubleOrNull() ?: 0.0

                        val point = PointEntity(
                            projectId = proj.id,
                            name = pointName,
                            northing = northing,
                            easting = easting,
                            ellipsoidalHeight = height,
                            featureCode = null,
                            description = "NCN import"
                        )

                        pointRepo.upsert(point)
                        importedCount++
                    }
                }
                _message.tryEmit("$importedCount nokta içe aktarıldı (NCN)")
            }.onFailure { e ->
                _message.tryEmit("NCN import hatası: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileManagerScreen(vm: FileManagerViewModel, onBack: () -> Unit) {
    val active by vm.activeProject.collectAsState()
    val points by vm.points.collectAsState()
    val exports by vm.exports.collectAsState()
    val busy by vm.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showFileContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dosya Yönetimi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshExports() }) {
                        Icon(Icons.Filled.Refresh, "Yenile")
                    }
                    IconButton(onClick = { showNewFileDialog = true }) {
                        Icon(Icons.Filled.Add, "Yeni Dosya")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Proje Bilgisi Kartı
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Aktif Proje",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        active?.name ?: "(Proje Seçilmedi)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Toplam Nokta:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${points.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dosya İşlemleri Butonları
            Text("Dosya İşlemleri", style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showNewFileDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !busy
                ) {
                    Icon(Icons.Filled.CreateNewFolder, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Yeni")
                }
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !busy && points.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Upload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dışa Aktar")
                }
            }

            // İçe Aktarma Butonu
            var showImportDialog by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && active != null
            ) {
                Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("İçe Aktar (.ncn, .csv, .txt)")
            }

            if (showImportDialog) {
                ImportDialog(
                    onDismiss = { showImportDialog = false },
                    onImport = { fileContent ->
                        vm.importNcnFile(fileContent)
                        showImportDialog = false
                    }
                )
            }

            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Dosya Listesi
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dosyalar", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${exports.size} dosya",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (exports.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Henüz dosya yok",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Yeni dosya oluşturun veya veri dışa aktarın",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exports, key = { it.name }) { f ->
                        FileRow(
                            info = f,
                            onOpen = {
                                showFileContent = vm.openFile(f.path)
                            },
                            onShare = { shareFile(ctx, f.path) },
                            onDelete = { vm.deleteExport(f.name) }
                        )
                    }
                }
            }

            // Dizin Bilgisi
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "Dizin: ${ctx.filesDir.name}/$EXPORT_DIR_NAME",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Folder, null, Modifier.size(18.dp)) }
            )
        }
    }

    // Yeni Dosya Dialog
    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onCreate = { fileName ->
                vm.createNewDataFile(fileName)
                showNewFileDialog = false
            }
        )
    }

    // Dışa Aktar Dialog
    if (showExportDialog) {
        ExportFormatDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                vm.exportFile(format)
                showExportDialog = false
            }
        )
    }

    // Dosya İçeriği Dialog
    showFileContent?.let { content ->
        FileContentDialog(
            content = content,
            onDismiss = { showFileContent = null }
        )
    }
}

@Composable
private fun FileRow(
    info: ExportFileInfo,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (info.format) {
                    "CSV", "TXT", "DAT" -> Icons.Filled.Description
                    "JSON" -> Icons.Filled.DataObject
                    "DXF", "KML", "KMZ" -> Icons.Filled.Map
                    else -> Icons.Filled.InsertDriveFile
                },
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(info.name, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        info.format,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    Text(
                        prettySize(info.sizeBytes),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Filled.Visibility, "Aç")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, "Paylaş")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NewFileDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var fileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Dosya Oluştur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Yeni veri dosyası için bir isim girin:")
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Dosya Adı") },
                    placeholder = { Text("ornek_dosya") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(fileName) },
                enabled = fileName.isNotBlank()
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun ExportFormatDialog(onDismiss: () -> Unit, onExport: (FileFormat) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dışa Aktarma Formatı") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(FileFormat.values()) { format ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onExport(format) }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (format) {
                                    FileFormat.CSV, FileFormat.TXT, FileFormat.DAT, FileFormat.NCN -> Icons.Filled.Description
                                    FileFormat.JSON -> Icons.Filled.DataObject
                                    FileFormat.DXF, FileFormat.KML, FileFormat.KMZ -> Icons.Filled.Map
                                    FileFormat.XLS -> Icons.Filled.TableChart
                                },
                                null
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(format.displayName, fontWeight = FontWeight.Medium)
                                Text(
                                    ".${format.extension}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun FileContentDialog(content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dosya İçeriği") },
        text = {
            LazyColumn {
                item {
                    Text(
                        content.take(5000), // İlk 5000 karakter
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    if (content.length > 5000) {
                        Text(
                            "\n... (${content.length - 5000} karakter daha)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var fileContent by remember { mutableStateOf("") }
    var sampleDataShown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NCN/CSV/TXT İçe Aktar") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Dosya içeriğini buraya yapıştırın:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Örnek Format Butonu
                if (!sampleDataShown) {
                    OutlinedButton(
                        onClick = {
                            fileContent = """
                                m1 446055.966 4286420.511 1072.775
                                k1 446058.958 4286420.017 1073.039
                                k2 446058.659 4286412.615 1072.708
                                k3 446058.498 4286406.089 1072.444
                                yol1 446059.797 4286401.519 1072.427
                            """.trimIndent()
                            sampleDataShown = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Info, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Örnek NCN Formatı Göster")
                    }
                }

                OutlinedTextField(
                    value = fileContent,
                    onValueChange = { fileContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    placeholder = {
                        Text(
                            "nokta1 doğu kuzey yükseklik\nm1 446055.966 4286420.511 1072.775\nk1 446058.958 4286420.017 1073.039",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )

                Text(
                    "Format: nokta_adı doğu kuzey yükseklik",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Her satırda bir nokta, boşluklarla ayrılmış",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(fileContent) },
                enabled = fileContent.isNotBlank()
            ) {
                Text("İçe Aktar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

private fun prettySize(size: Long): String {
    if (size < 1024) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}

private fun shareFile(ctx: Context, path: String) {
    val f = File(path)
    if (!f.exists()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Nokta Verisi")
        putExtra(Intent.EXTRA_TEXT, "Dosya: ${f.name}")
    }
    ctx.startActivity(Intent.createChooser(intent, "Dosyayı Paylaş"))
}
