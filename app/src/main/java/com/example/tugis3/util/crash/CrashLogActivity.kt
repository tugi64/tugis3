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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable

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
    val clipboard = LocalClipboardManager.current

    // State
    var mode by remember { mutableStateOf(ViewMode.Sectioned) } // Sectioned / Raw
    var filter by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    var sections by remember { mutableStateOf<List<CrashSection>>(emptyList()) }
    var rawLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var fileInfo by remember { mutableStateOf(CrashFileInfo(false,0,null)) }
    var expandedSectionTs by remember { mutableStateOf<String?>(null) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    fun load() {
        fileInfo = CrashLogger.fileInfo(context)
        sections = CrashLogger.readSections(context)
        rawLines = CrashLogger.readLines(context)
        if (expandedSectionTs != null && sections.none { it.timestamp == expandedSectionTs }) {
            expandedSectionTs = null
        }
    }
    LaunchedEffect(reloadKey) { load() }

    fun refresh() { reloadKey++ }

    fun clearAll() {
        CrashLogger.clear(context)
        refresh()
    }

    fun shareFull() {
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

    fun shareLastSection() {
        val last = sections.lastOrNull() ?: return
        val temp = File(context.cacheDir, "last_crash.txt").apply {
            writeText(buildString {
                append("Crash: ").append(last.timestamp).append('\n')
                last.lines.forEach { append(it).append('\n') }
            })
        }
        val uri: Uri = FileProvider.getUriForFile(context, "com.example.tugis3.fileprovider", temp)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Son Crash Paylaş"))
    }

    val filteredSections = remember(sections, filter) {
        if (filter.isBlank()) sections else sections.filter { s ->
            s.timestamp.contains(filter, true) || s.lines.any { it.contains(filter, true) }
        }
    }
    val filteredRaw = remember(rawLines, filter) {
        if (filter.isBlank()) rawLines else rawLines.filter { it.contains(filter, true) }
    }

    val exists = fileInfo.exists
    val totalCrashes = sections.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crash Log") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, contentDescription = "Yenile") }
                    // Mode toggle
                    IconButton(onClick = { mode = if (mode == ViewMode.Sectioned) ViewMode.Raw else ViewMode.Sectioned }, enabled = exists) {
                        Icon(if (mode == ViewMode.Sectioned) Icons.Default.ViewList else Icons.Default.Splitscreen, contentDescription = "Görünüm")
                    }
                    IconButton(onClick = {
                        val txt = if (mode == ViewMode.Sectioned) filteredSections.joinToString("\n\n") { it.timestamp + "\n" + it.lines.joinToString("\n") } else filteredRaw.joinToString("\n")
                        clipboard.setText(AnnotatedString(txt))
                    }, enabled = exists && (filteredSections.isNotEmpty() || filteredRaw.isNotEmpty())) { Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala") }
                    IconButton(onClick = { shareLastSection() }, enabled = exists && sections.isNotEmpty()) { Icon(Icons.Default.Send, contentDescription = "Son Crash Paylaş") }
                    IconButton(onClick = { shareFull() }, enabled = exists) { Icon(Icons.Default.Share, contentDescription = "Tamamı Paylaş") }
                    IconButton(onClick = { clearAll() }, enabled = exists) { Icon(Icons.Default.Delete, contentDescription = "Temizle") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (exists) {
                OutlinedTextField(filter, { filter = it }, label = { Text("Ara / filtrele") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (!exists) {
                Text("Crash log bulunamadı.")
            } else if (mode == ViewMode.Sectioned && sections.isEmpty()) {
                Text("Hiç crash kaydı yok.")
            } else if (mode == ViewMode.Raw && rawLines.isEmpty()) {
                Text("Log dosyası boş.")
            } else {
                // Üst bilgi
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dosya: ${formatSize(fileInfo.sizeBytes)}", style = MaterialTheme.typography.labelSmall)
                    val lm = fileInfo.lastModified?.let { sdf.format(Date(it)) } ?: "-"
                    Text("Güncellenme: $lm", style = MaterialTheme.typography.labelSmall)
                }
                if (mode == ViewMode.Sectioned) {
                    Text("Crash sayısı: $totalCrashes (gösterilen: ${filteredSections.size})", style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("Satır: ${filteredRaw.size}", style = MaterialTheme.typography.labelSmall)
                }
                Divider()
                if (mode == ViewMode.Sectioned) {
                    LazyColumn(Modifier.weight(1f)) {
                        items(filteredSections, key = { it.timestamp }) { sec ->
                            CrashSectionCard(
                                section = sec,
                                expanded = expandedSectionTs == sec.timestamp,
                                onToggle = { expandedSectionTs = if (expandedSectionTs == sec.timestamp) null else sec.timestamp },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(sec.timestamp + "\n" + sec.lines.joinToString("\n")))
                                }
                            )
                        }
                    }
                } else {
                    SelectionContainer {
                        LazyColumn(Modifier.weight(1f)) {
                            items(filteredRaw) { l -> Text(l, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                if (filter.isNotBlank()) {
                    Text("Filtre aktif", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Light)
                }
            }
            Text("Sectioned görünüm: son ~20 crash; Raw görünüm: son ~1000 satır.", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Light)
        }
    }
}

private enum class ViewMode { Sectioned, Raw }

@Composable
private fun CrashSectionCard(section: CrashSection, expanded: Boolean, onToggle: () -> Unit, onCopy: () -> Unit) {
    val severity = remember(section) { detectSeverity(section) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggle() }
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(containerColor = when (severity) {
            Severity.ERROR -> MaterialTheme.colorScheme.errorContainer
            Severity.WARN -> MaterialTheme.colorScheme.tertiaryContainer
            Severity.INFO -> MaterialTheme.colorScheme.secondaryContainer
        })
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(section.timestamp, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(onClick = onCopy, label = { Text("Kopyala") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    AssistChip(onClick = onToggle, label = { Text(if (expanded) "Kapat" else "Aç") })
                }
            }
            if (expanded) {
                Divider()
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        section.lines.forEach { ln -> Text(ln, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            } else {
                // İlk problem satırı önizleme
                val preview = section.lines.firstOrNull { it.contains("Exception") || it.contains("Error") } ?: section.lines.firstOrNull() ?: "(boş)"
                Text(preview, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}

enum class Severity { INFO, WARN, ERROR }

private fun detectSeverity(section: CrashSection): Severity {
    val text = section.lines.joinToString("\n")
    return when {
        text.contains("Exception") || text.contains("Error") || text.contains("Fatal", true) -> Severity.ERROR
        text.contains("warn", true) -> Severity.WARN
        else -> Severity.INFO
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B","KB","MB","GB")
    var v = bytes.toDouble(); var idx = 0
    while (v >= 1024 && idx < units.lastIndex) { v /= 1024; idx++ }
    return String.format(Locale.getDefault(), "%.1f %s", v, units[idx])
}
