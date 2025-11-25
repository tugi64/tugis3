@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FtpSharedDataActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { FtpSharedDataScreen(onBack = { finish() }) } }
    }
}

data class RemoteFile(
    val name: String,
    val size: Long,
    val modified: String,
    val type: String
)

@Composable
private fun FtpSharedDataScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("ftp.example.com") }
    var port by remember { mutableStateOf("21") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("/survey_data") }
    var connected by remember { mutableStateOf(false) }
    var logLines by remember { mutableStateOf(listOf("FTP Paylaşım sistemi hazır.")) }
    var remoteFiles by remember { mutableStateOf<List<RemoteFile>>(emptyList()) }
    var isListing by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var selectedFile by remember { mutableStateOf<RemoteFile?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    fun appendLog(m: String){
        val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logLines = (logLines + "[$currentTime] $m").takeLast(100)
    }

    fun simulateConnection() {
        scope.launch {
            connected = false
            appendLog("Bağlanıyor: $user@$host:$port")
            delay(1000)
            if (user.isNotEmpty() && host.isNotEmpty()) {
                connected = true
                appendLog("✓ Bağlantı başarılı")
                snackbar.showSnackbar("FTP sunucusuna bağlandı")
            } else {
                appendLog("✗ Bağlantı hatası: Kullanıcı adı ve host gerekli")
                snackbar.showSnackbar("Bağlantı hatası")
            }
        }
    }

    fun simulateList() {
        scope.launch {
            isListing = true
            appendLog("Dizin listeleniyor: $path")
            delay(800)
            remoteFiles = listOf(
                RemoteFile("project_2024_001.csv", 45120, "2024-10-15 14:23", "CSV"),
                RemoteFile("survey_points.txt", 12450, "2024-10-14 09:15", "TXT"),
                RemoteFile("cad_drawing.dxf", 198512, "2024-10-10 16:42", "DXF"),
                RemoteFile("backup_20241016.zip", 2450000, "2024-10-16 08:30", "ZIP"),
                RemoteFile("coordinates.json", 8920, "2024-10-12 11:05", "JSON")
            )
            appendLog("✓ ${remoteFiles.size} dosya listelendi")
            isListing = false
        }
    }

    fun simulateUpload(fileName: String) {
        scope.launch {
            isUploading = true
            uploadProgress = 0f
            appendLog("Yükleniyor: $fileName")
            for (i in 1..10) {
                delay(200)
                uploadProgress = i / 10f
            }
            appendLog("✓ Yükleme tamamlandı: $fileName")
            snackbar.showSnackbar("Dosya yüklendi")
            isUploading = false
            uploadProgress = 0f
        }
    }

    fun simulateDownload(file: RemoteFile) {
        scope.launch {
            appendLog("İndiriliyor: ${file.name}")
            delay(1000)
            appendLog("✓ İndirildi: ${file.name} (${file.size} bytes)")
            snackbar.showSnackbar("Dosya indirildi")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FTP Veri Paylaşımı") },
                navigationIcon = {
                    IconButton(onClick = onBack){
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (connected) simulateList()
                        else snackbar.let { scope.launch { it.showSnackbar("Önce bağlanın") } }
                    }) {
                        Icon(Icons.Default.Refresh, "Yenile")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bağlantı Bilgileri
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (connected) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                                null,
                                tint = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (connected) "Bağlı" else "Bağlantı Yok",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            host,
                            { host = it },
                            label = { Text("FTP Sunucu") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !connected,
                            leadingIcon = { Icon(Icons.Default.Cloud, null) }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                port,
                                { port = it.filter { c -> c.isDigit() } },
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f),
                                enabled = !connected
                            )
                            OutlinedTextField(
                                path,
                                { path = it },
                                label = { Text("Dizin") },
                                modifier = Modifier.weight(2f),
                                enabled = !connected
                            )
                        }
                        OutlinedTextField(
                            user,
                            { user = it },
                            label = { Text("Kullanıcı") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !connected,
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                        OutlinedTextField(
                            pass,
                            { pass = it },
                            label = { Text("Şifre") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !connected,
                            leadingIcon = { Icon(Icons.Default.Lock, null) }
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (connected) {
                                Button(
                                    onClick = {
                                        connected = false
                                        remoteFiles = emptyList()
                                        appendLog("Bağlantı kesildi")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Bağlantıyı Kes")
                                }
                            } else {
                                Button(
                                    onClick = { simulateConnection() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Bağlan")
                                }
                            }
                        }
                    }
                }
            }

            // Upload Progress
            if (isUploading) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("Dosya yükleniyor...", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "${(uploadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Upload Buttons
            if (connected && !isUploading) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Yerel Dosyaları Yükle", fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { simulateUpload("current_project.csv") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Upload, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("CSV")
                                }
                                OutlinedButton(
                                    onClick = { simulateUpload("drawing.dxf") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Upload, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("DXF")
                                }
                                OutlinedButton(
                                    onClick = { simulateUpload("backup.zip") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Upload, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Yedek")
                                }
                            }
                        }
                    }
                }
            }

            // Uzak Dosyalar
            if (connected) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Uzak Dosyalar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isListing) CircularProgressIndicator(Modifier.size(20.dp))
                    }
                }

                if (remoteFiles.isEmpty() && !isListing) {
                    item {
                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.FolderOpen, null, Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Dosya listesi boş")
                                TextButton(onClick = { simulateList() }) {
                                    Text("Listeyi Yenile")
                                }
                            }
                        }
                    }
                } else {
                    items(remoteFiles) { file ->
                        ElevatedCard(
                            onClick = { selectedFile = file }
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(Modifier.weight(1f)) {
                                    Icon(
                                        when(file.type) {
                                            "CSV", "TXT" -> Icons.Default.Description
                                            "DXF" -> Icons.Default.Draw
                                            "ZIP" -> Icons.Default.Folder
                                            else -> Icons.Default.Article
                                        },
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(file.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${file.size / 1024} KB • ${file.modified}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { simulateDownload(file) }) {
                                    Icon(Icons.Default.Download, "İndir")
                                }
                            }
                        }
                    }
                }
            }

            // Log
            item {
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("Oturum Logu", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            logLines.takeLast(10).forEach { line ->
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
