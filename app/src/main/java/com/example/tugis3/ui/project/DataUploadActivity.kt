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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

private const val EXPORT_DIR_NAME = "exports"

private const val DEFAULT_MIN_UPLOAD_MS = 800
private const val UPLOAD_SIZE_DIVISOR = 3000L
private const val MAX_UPLOAD_MS = 8_000
private const val UPLOAD_STEPS = 25

data class UploadEntry(
    val file: File,
    val sizeBytes: Long,
    val lastModified: Long,
    val progress: Float = 0f,
    val status: UploadStatus = UploadStatus.IDLE,
    val message: String? = null
)

enum class UploadStatus { IDLE, UPLOADING, SUCCESS, ERROR }

@AndroidEntryPoint
class DataUploadActivity : ComponentActivity() {
    private val vm: DataUploadViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DataUploadScreen(vm = vm, onBack = { finish() }) }
    }
}

@HiltViewModel
class DataUploadViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val exportDir = File(appContext.filesDir, EXPORT_DIR_NAME).apply { mkdirs() }

    private val _items = MutableStateFlow<List<UploadEntry>>(emptyList())
    val items: StateFlow<List<UploadEntry>> = _items.asStateFlow()

    private val _snacks = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snacks: SharedFlow<String> = _snacks

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    init { refresh() }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val list = exportDir.listFiles()?.sortedByDescending { it.lastModified() }?.map { f ->
            UploadEntry(file = f, sizeBytes = f.length(), lastModified = f.lastModified())
        } ?: emptyList()
        _items.value = list
    }

    fun delete(name: String) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { File(exportDir, name).takeIf { it.exists() }?.delete() }
        refresh()
    }

    fun startUpload(name: String) {
        val current = _items.value.firstOrNull { it.file.name == name } ?: return
        if (current.status == UploadStatus.UPLOADING) return
        updateEntry(name) { it.copy(progress = 0f, status = UploadStatus.UPLOADING, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val durationMs = (DEFAULT_MIN_UPLOAD_MS + (current.sizeBytes / UPLOAD_SIZE_DIVISOR)).coerceAtMost(MAX_UPLOAD_MS.toLong()).toInt()
                val steps = UPLOAD_STEPS
                repeat(steps) { i ->
                    delay((durationMs / steps).toLong())
                    val p = (i + 1) / steps.toFloat()
                    updateEntry(name) { e -> e.copy(progress = p) }
                }
                updateEntry(name) { it.copy(progress = 1f, status = UploadStatus.SUCCESS, message = "Yüklendi") }
                _snacks.tryEmit("Yüklendi: $name")
            } catch (t: Throwable) {
                updateEntry(name) { it.copy(status = UploadStatus.ERROR, message = t.message ?: "Hata") }
                _snacks.tryEmit("Hata: ${t.message}")
            }
        }
    }

    private fun updateEntry(name: String, transform: (UploadEntry) -> UploadEntry) {
        _items.update { list -> list.map { if (it.file.name == name) transform(it) else it } }
    }

    fun formatDate(ts: Long): String = sdf.format(Date(ts))
    fun prettySize(size: Long): String {
        if (size < 1024) return "$size B"
        val kb = size / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataUploadScreen(vm: DataUploadViewModel, onBack: () -> Unit) {
    val items by vm.items.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { vm.snacks.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Veri Gönder") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, null) }
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
            Text("Export edilen dosyaları seçerek buluta yükleyin veya paylaşın.", style = MaterialTheme.typography.bodySmall)
            if (items.isEmpty()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Export dizininde dosya yok", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("Önce 'Dosya Yönet' ekranından CSV/JSON oluşturun.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.file.name }) { entry ->
                        UploadRow(
                            entry = entry,
                            onUpload = { vm.startUpload(entry.file.name) },
                            onShare = { shareFile(ctx, entry.file) },
                            onDelete = { vm.delete(entry.file.name) },
                            prettyDate = { vm.formatDate(entry.lastModified) },
                            prettySize = { vm.prettySize(entry.sizeBytes) }
                        )
                    }
                }
            }
            AssistChip(onClick = {}, label = { Text("Dizin: ${ctx.filesDir.name}/$EXPORT_DIR_NAME") })
        }
    }
}

@Composable
private fun UploadRow(
    entry: UploadEntry,
    onUpload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    prettyDate: () -> String,
    prettySize: () -> String
) {
    val statusColor = when (entry.status) {
        UploadStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        UploadStatus.ERROR -> MaterialTheme.colorScheme.error
        UploadStatus.UPLOADING -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Description, null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.file.name, fontWeight = FontWeight.Medium)
                    Text("${prettySize()} • ${prettyDate()}", style = MaterialTheme.typography.labelSmall)
                }
                if (entry.status == UploadStatus.SUCCESS) Icon(Icons.Filled.CheckCircle, null, tint = statusColor)
                if (entry.status == UploadStatus.ERROR) Icon(Icons.Filled.Error, null, tint = statusColor)
            }
            if (entry.status == UploadStatus.UPLOADING) {
                LinearProgressIndicator(progress = { entry.progress }, modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUpload, enabled = entry.status != UploadStatus.UPLOADING) {
                    Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when (entry.status) {
                            UploadStatus.SUCCESS -> "Tekrar"
                            UploadStatus.ERROR -> "Yeniden"
                            UploadStatus.UPLOADING -> "Yükleniyor"
                            else -> "Yükle"
                        }
                    )
                }
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paylaş")
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) }
            }
            entry.message?.let { msg -> Text(msg, style = MaterialTheme.typography.labelSmall, color = statusColor) }
        }
    }
}

private fun shareFile(ctx: Context, f: File) {
    if (!f.exists()) return
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, f.name)
        .putExtra(Intent.EXTRA_TEXT, "Dosya: ${f.name}\n${f.absolutePath}")
    ctx.startActivity(Intent.createChooser(intent, "Paylaş"))
}
