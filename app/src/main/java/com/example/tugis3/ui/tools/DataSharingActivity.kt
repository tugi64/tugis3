package com.example.tugis3.ui.tools

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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

private const val EXPORT_DIR_NAME = "exports"

@AndroidEntryPoint
class DataSharingActivity : ComponentActivity() {
    private val vm: DataSharingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DataSharingScreen(vm = vm, onBack = { finish() }) }
    }
}

data class ShareFileInfo(
    val file: File,
    val sizeBytes: Long,
    val modified: Long
)

@HiltViewModel
class DataSharingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val exportDir = File(appContext.filesDir, EXPORT_DIR_NAME).apply { mkdirs() }
    private val _files = MutableStateFlow<List<ShareFileInfo>>(emptyList())
    val files: StateFlow<List<ShareFileInfo>> = _files.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _snacks = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snacks: SharedFlow<String> = _snacks

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    init { refresh() }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val list = exportDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { ShareFileInfo(it, it.length(), it.lastModified()) }
            ?: emptyList()
        _files.value = list
    }

    fun delete(name: String) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { File(exportDir, name).takeIf { it.exists() }?.delete() }
        refresh()
    }

    fun createZipAll(): File? {
        if (_busy.value) return null
        val sources = _files.value
        if (sources.isEmpty()) return null
        _busy.value = true
        return try {
            val zipName = "bundle_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".zip"
            val outFile = File(exportDir, zipName)
            ZipOutputStream(FileOutputStream(outFile)).use { zos ->
                sources.forEach { info ->
                    runCatching {
                        FileInputStream(info.file).use { fis ->
                            val entry = ZipEntry(info.file.name)
                            zos.putNextEntry(entry)
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
            _snacks.tryEmit("ZIP oluşturuldu: ${outFile.name}")
            outFile
        } catch (t: Throwable) {
            _snacks.tryEmit("ZIP hata: ${t.message}")
            null
        } finally {
            _busy.value = false
            refresh()
        }
    }

    fun format(ts: Long): String = sdf.format(Date(ts))
    fun pretty(size: Long): String {
        if (size < 1024) return "$size B"
        val kb = size / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataSharingScreen(vm: DataSharingViewModel, onBack: () -> Unit) {
    val files by vm.files.collectAsState()
    val busy by vm.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { vm.snacks.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Veri Paylaşımı") },
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
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Export Dizini", style = MaterialTheme.typography.labelMedium)
                    Text("Dosya sayısı: ${files.size}")
                    if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { shareMulti(ctx, files) }, enabled = files.isNotEmpty()) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tümü Paylaş")
                        }
                        OutlinedButton(onClick = {
                            val z = vm.createZipAll()
                            if (z != null) shareSingle(ctx, z)
                        }, enabled = files.size > 1 && !busy) {
                            Icon(Icons.Filled.Archive, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ZIP")
                        }
                    }
                }
            }
            if (files.isEmpty()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Paylaşılacak dosya yok", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("Önce Dosya Yöneticisi üzerinden dışa aktarım yapın.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files, key = { it.file.name }) { f ->
                        ShareRow(
                            info = f,
                            onShare = { shareSingle(ctx, f.file) },
                            onDelete = { vm.delete(f.file.name) },
                            prettySize = vm.pretty(f.sizeBytes),
                            date = vm.format(f.modified)
                        )
                    }
                }
            }
            AssistChip(onClick = {}, label = { Text("Dizin: ${ctx.filesDir.name}/$EXPORT_DIR_NAME") })
            Text(
                "Not: Güvenlik gereksinimleri nedeniyle dışa aktarılan dosyalar yalnızca uygulama içinden erişilebilir. Kurumsal entegrasyon için özel paylaşım servisleri eklenebilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShareRow(info: ShareFileInfo, onShare: () -> Unit, onDelete: () -> Unit, prettySize: String, date: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Description, null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(info.file.name, fontWeight = FontWeight.Medium)
                    Text("$prettySize • $date", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onShare) { Icon(Icons.Filled.Share, null) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) }
            }
        }
    }
}

private fun shareSingle(ctx: Context, f: File) {
    if (!f.exists()) return
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, f.name)
        .putExtra(Intent.EXTRA_TEXT, "Dosya: ${f.name}\n${f.absolutePath}")
    ctx.startActivity(Intent.createChooser(intent, "Paylaş"))
}

private fun shareMulti(ctx: Context, files: List<ShareFileInfo>) {
    if (files.isEmpty()) return
    val text = buildString {
        append("Dosyalar:\n")
        files.forEach { append("- ${it.file.name} (${it.sizeBytes} B)\n") }
    }
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "Export Paket Listesi")
        .putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(intent, "Paylaş"))
}
