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
        setContent { FileManagerScreen(vm = vm, onBack = { finish() }) }
    }
}

private const val EXPORT_DIR_NAME = "exports"
private const val DATE_PATTERN = "yyyyMMdd_HHmmss"

data class ExportFileInfo(val name: String, val path: String, val sizeBytes: Long, val lastModified: Long)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FileManagerViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    @Suppress("unused") private val pointRepo: PointRepository,
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
                lastModified = it.lastModified()
            )
        } ?: emptyList()
        _exports.value = list
    }

    fun exportCsv() = exportGeneric("points_" + sdf.format(Date()) + ".csv") { file, pts ->
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

    fun exportJson() = exportGeneric("points_" + sdf.format(Date()) + ".json") { file, pts ->
        // Minimal JSON builder (Üretimde kotlinx.serialization tercih edilmeli)
        file.appendText("[\n")
        pts.forEachIndexed { i, p ->
            val obj = buildString {
                append("  {\n")
                append("    \"id\": ${p.id},\n")
                append("    \"name\": \"" + jsonEsc(p.name) + "\",\n")
                append("    \"n\": ${p.northing},\n")
                append("    \"e\": ${p.easting},\n")
                p.ellipsoidalHeight?.let { append("    \"h\": $it,\n") }
                p.featureCode?.let { append("    \"code\": \"" + jsonEsc(it) + "\",\n") }
                p.description?.let { append("    \"desc\": \"" + jsonEsc(it) + "\",\n") }
                // trailing comma temizliği
                if (endsWith(",\n")) deleteRange(length - 2, length)
                append("\n  }")
            }
            file.appendText(obj)
            if (i < pts.lastIndex) file.appendText(",")
            file.appendText("\n")
        }
        file.appendText("]")
    }

    private fun exportGeneric(filename: String, writer: (File, List<PointEntity>) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            if (_busy.value) return@launch
            _busy.value = true
            runCatching {
                val pts = points.value
                if (pts.isEmpty()) throw IllegalStateException("Nokta yok")
                val file = File(exportDir, filename)
                writer(file, pts)
                _message.tryEmit("Dışa aktarıldı: ${file.name}")
            }.onFailure { e ->
                _message.tryEmit("Hata: ${e.message}")
            }
            _busy.value = false
            refreshExports()
        }

    private fun escCsv(s: String): String = if (s.contains(',') || s.contains('"')) '"' + s.replace("\"", "\"\"") + '"' else s
    private fun jsonEsc(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

    fun deleteExport(name: String) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { File(exportDir, name).takeIf { it.exists() }?.delete() }
        refreshExports()
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

    LaunchedEffect(Unit) {
        vm.messages.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dosya Yöneticisi") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.refreshExports() }) { Icon(Icons.Filled.Refresh, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card { // Proje & Nokta Bilgisi
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aktif Proje", style = MaterialTheme.typography.labelMedium)
                    Text(active?.name ?: "(yok)", fontWeight = FontWeight.Bold)
                    Text("Nokta sayısı: ${points.size}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.exportCsv() }, enabled = !busy && points.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CSV")
                }
                OutlinedButton(onClick = { vm.exportJson() }, enabled = !busy && points.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DataObject, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("JSON")
                }
            }
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Dışa Aktarılan Dosyalar", style = MaterialTheme.typography.titleMedium)
            if (exports.isEmpty()) {
                Text("Henüz dosya yok", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(exports, key = { it.name }) { f ->
                        ExportRow(info = f,
                            onShare = { shareFile(ctx, f.path) },
                            onDelete = { vm.deleteExport(f.name) }
                        )
                    }
                }
            }
            AssistChip(onClick = {}, label = { Text("Dizin: ${ctx.filesDir.name}/$EXPORT_DIR_NAME") })
        }
    }
}

@Composable
private fun ExportRow(info: ExportFileInfo, onShare: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Description, null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(info.name, fontWeight = FontWeight.Medium)
                Text(prettySize(info.sizeBytes), style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onShare) { Icon(Icons.Filled.Share, null) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) }
        }
    }
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
    // Basit paylaşım (FileProvider eklenmemişse ACTION_SEND ile path metin olarak)
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, f.name)
        .putExtra(Intent.EXTRA_TEXT, "Export dosyası: ${f.name}\n${f.absolutePath}")
    ctx.startActivity(Intent.createChooser(intent, "Paylaş"))
}
