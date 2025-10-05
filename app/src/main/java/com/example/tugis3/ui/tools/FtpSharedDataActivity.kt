package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme

/**
 * 6.10 FTP Shared Data (kılavuz) özelliği için placeholder.
 * Planlanan işlevler:
 *  - FTP sunucu parametreleri (host, port, kullanıcı, şifre, uzak dizin)
 *  - Bağlan / Listele
 *  - Yerel proje nokta / CAD / log dosyalarını seçip yükleme
 *  - Uzak dosya indir, ad çatışması çözümü
 *  - Oturum logu ve basit ilerleme çubuğu
 */
class FtpSharedDataActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { FtpSharedDataScreen(onBack = { finish() }) } }
    }
}

private data class RemoteFile(val name: String, val size: Long, val modified: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FtpSharedDataScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("ftp.example.com") }
    var port by remember { mutableStateOf("21") }
    var user by remember { mutableStateOf("anonymous") }
    var pass by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("/") }
    var connected by remember { mutableStateOf(false) }
    var logLines by remember { mutableStateOf(listOf("Hazır.")) }
    var remoteFiles by remember { mutableStateOf(listOf<RemoteFile>()) }
    var isListing by remember { mutableStateOf(false) }

    fun appendLog(m: String){ logLines = (logLines + m).takeLast(200) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FTP Paylaşım") },
                navigationIcon = { IconButton(onClick = onBack){ Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            Text("Bağlantı", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(host, { host = it }, label = { Text("Host") })
            OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("Port") }, singleLine = true)
            OutlinedTextField(user, { user = it }, label = { Text("Kullanıcı") })
            OutlinedTextField(pass, { pass = it }, label = { Text("Şifre") }, singleLine = true)
            OutlinedTextField(path, { path = it }, label = { Text("Dizin") })
            RowButtons(connected = connected, isListing = isListing,
                onConnect = {
                    connected = true
                    appendLog("Bağlandı (simülasyon)")
                },
                onList = {
                    isListing = true
                    appendLog("Liste alınıyor...")
                    // Simüle edilmiş sonuç
                    remoteFiles = listOf(
                        RemoteFile("points_20241010.csv", 12450, "2024-10-10 12:41"),
                        RemoteFile("cad_plan.dxf", 98512, "2024-09-02 09:10")
                    )
                    isListing = false
                    appendLog("${remoteFiles.size} dosya geldi.")
                }
            )
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Uzak Dosyalar", style = MaterialTheme.typography.titleSmall)
            if (!connected) Text("Önce bağlanın.", color = MaterialTheme.colorScheme.secondary)
            else if (remoteFiles.isEmpty()) Text(if (isListing) "Listeleniyor..." else "Dosya yok.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(180.dp)) {
                items(remoteFiles){ rf ->
                    ElevatedCard(onClick = { appendLog("İndirilecek: ${rf.name} (TODO)") }) {
                        Column(Modifier.padding(8.dp)) {
                            Text(rf.name, fontWeight = FontWeight.Medium)
                            Text("${rf.size} B • ${rf.modified}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Oturum Logu", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.height(120.dp)) {
                items(logLines){ l -> Text(l, style = MaterialTheme.typography.bodySmall) }
            }
            Text("NOT: Gerçek FTP bağlantısı henüz uygulanmadı.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun RowButtons(connected: Boolean, isListing: Boolean, onConnect: () -> Unit, onList: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onConnect, enabled = !connected) { Text("Bağlan") }
        OutlinedButton(onClick = onList, enabled = connected && !isListing) { Text(if (isListing) "Bekleyin" else "Listele") }
        OutlinedButton(onClick = { /* TODO: Upload */ }, enabled = connected) { Text("Yükle (TODO)") }
    }
}
