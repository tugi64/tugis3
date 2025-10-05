package com.example.tugis3.util.crash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

class CrashLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { CrashLogScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashLogScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var lines by remember { mutableStateOf(listOf<String>()) }
    var lastModified by remember { mutableStateOf<Long?>(null) }
    var exists by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    var reloadTrigger by remember { mutableStateOf(0) }
    val clipboard = LocalClipboardManager.current

    fun load() {
        val file = File(context.filesDir, "logs/crash.log")
        exists = file.exists()
        if (file.exists()) {
            lastModified = file.lastModified()
            runCatching { lines = file.readLines().takeLast(500) }
        } else {
            lines = emptyList()
        }
    }

    LaunchedEffect(reloadTrigger) { load() }

    fun shareLog() {
        val file = File(context.filesDir, "logs/crash.log")
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(context, "com.example.tugis3.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Log Paylaş"))
    }
    fun clearLog() {
        val file = File(context.filesDir, "logs/crash.log")
        if (file.exists()) {
            runCatching { file.writeText("") }
            lines = emptyList(); lastModified = System.currentTimeMillis(); exists = true
        }
    }
    fun refresh() { reloadTrigger++ }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    val shown = remember(lines, filter) {
        if (filter.isBlank()) lines else lines.filter { it.contains(filter, true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crash Log") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, contentDescription = "Yenile") }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(shown.joinToString("\n")))
                    }, enabled = exists && shown.isNotEmpty()) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala")
                    }
                    IconButton(onClick = { shareLog() }, enabled = exists) { Icon(Icons.Default.Share, contentDescription = "Paylaş") }
                    IconButton(onClick = { clearLog() }, enabled = exists && lines.isNotEmpty()) { Icon(Icons.Default.Delete, contentDescription = "Temizle") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (exists) {
                OutlinedTextField(filter, { filter = it }, label = { Text("Ara (case-insensitive)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (!exists) {
                Text("Crash log bulunamadı.")
            } else if (lines.isEmpty()) {
                Text("Log dosyası boş.")
            } else {
                lastModified?.let { Text("Son Güncelleme: ${sdf.format(Date(it))}", style = MaterialTheme.typography.labelSmall) }
                Divider()
                SelectionContainer {
                    LazyColumn(Modifier.weight(1f)) {
                        items(shown) { l -> Text(l, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (shown.size != lines.size) Text("Filtre: ${shown.size}/${lines.size}", style = MaterialTheme.typography.labelSmall)
            }
            Text("En fazla son 500 satır gösteriliyor.", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Light)
        }
    }
}
