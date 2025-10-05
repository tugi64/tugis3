@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tugis3.ntrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max

@AndroidEntryPoint
class NtripProfilesActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NtripProfilesScreen() }
    }
}

@Composable
private fun NtripProfilesScreen(vm: NtripProfilesViewModel = hiltViewModel()) {
    val profiles by vm.profiles.collectAsState()
    val connectedId by vm.connectedProfileId.collectAsState()
    val status by vm.connectionStatusFlow.collectAsState()
    // Yeni istatistik akışları
    val rtcmBytes by vm.rtcmBytes.collectAsState()
    val nmeaBytes by vm.nmeaBytes.collectAsState()
    val dataRate by vm.dataRateBps.collectAsState()
    val isSim by vm.isSimulated.collectAsState()
    val lastRtcmTs by vm.lastRtcmTimestamp.collectAsState()
    val diffAge by vm.diffAgeSec.collectAsState()
    val rtcmTypeCounts by vm.rtcmTypeCounts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<NtripProfileUi?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    // Durum değişiminde snackbar
    LaunchedEffect(status) {
        if (status.startsWith("Hata") || status.startsWith("Bağlandı") || status.startsWith("Durduruldu")) {
            snackbarHost.showSnackbar(status)
        }
    }

    if (showDialog) {
        NtripProfileDialog(
            initial = editing,
            onDismiss = { showDialog = false; editing = null },
            onSave = { ui -> vm.save(ui); showDialog = false; editing = null }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NTRIP Profilleri") }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true; editing = null }) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (profiles.isEmpty()) {
                Text("Henüz profil yok. Sağ alttan ekleyin.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profiles) { p ->
                        val isConnected = p.id == connectedId && status.startsWith("Bağ")
                        ElevatedCard(
                            onClick = { editing = p; showDialog = true },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        Text("${p.host}:${p.port}/${p.mountPoint}", style = MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (p.autoConnect) Text("Auto", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                            if (isConnected) Text("Bağlı", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                            if (isConnected && isSim) Text("Simülasyon", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { vm.toggleAuto(p) }) { Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto") }
                                        IconButton(onClick = { vm.delete(p) }, enabled = !isConnected) { Icon(Icons.Default.Delete, contentDescription = "Sil") }
                                        IconButton(onClick = { vm.connect(p) }) {
                                            if (isConnected) Icon(Icons.Default.Close, contentDescription = "Durdur") else Icon(Icons.Default.Wifi, contentDescription = "Bağlan")
                                        }
                                    }
                                }
                                if (isConnected) {
                                    // İstatistik chip satırı
                                    val rtcmKb = rtcmBytes / 1024.0
                                    val nmeaKb = nmeaBytes / 1024.0
                                    val rateKbps = dataRate / 1024.0
                                    val age = diffAge
                                    val ageLabel = age?.let { if (it < 60) "${it}s" else ">${it}" } ?: "-"
                                    val ageStale = age != null && age > 15
                                    val ageColor = when {
                                        age == null -> MaterialTheme.colorScheme.surfaceVariant
                                        age <= 5 -> MaterialTheme.colorScheme.primaryContainer
                                        age <= 15 -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    }
                                    val topTypes = rtcmTypeCounts.entries.sortedByDescending { it.value }.take(3)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(onClick = {}, label = { Text("RTCM %.1f KB".format(rtcmKb)) })
                                        AssistChip(onClick = {}, label = { Text("NMEA %.1f KB".format(nmeaKb)) })
                                        AssistChip(onClick = {}, label = { Text("~%.1f KB/s".format(rateKbps)) })
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Diff $ageLabel") },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = ageColor)
                                        )
                                    }
                                    if (topTypes.isNotEmpty()) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            topTypes.forEach { (t, c) ->
                                                AssistChip(onClick = {}, label = { Text("T$t:$c") })
                                            }
                                            if (rtcmTypeCounts.size > topTypes.size) {
                                                val others = rtcmTypeCounts.entries.sortedByDescending { it.value }.drop(3).sumOf { it.value }
                                                AssistChip(onClick = {}, label = { Text("Diğ.:$others") })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val statusColor = when {
                status.startsWith("Bağlandı") -> MaterialTheme.colorScheme.primary
                status.startsWith("Bağlanıyor") -> MaterialTheme.colorScheme.tertiary
                status.startsWith("Hata") -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
                if (connectedId != null) {
                    Divider(Modifier.height(16.dp).width(1.dp))
                    Text(if (isSim) "Simülasyon Modu" else "Gerçek Akış", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun NtripProfileDialog(initial: NtripProfileUi?, onDismiss:()->Unit, onSave:(NtripProfileUi)->Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf((initial?.port ?: 2101).toString()) }
    var mp by remember { mutableStateOf(initial?.mountPoint ?: "") }
    var user by remember { mutableStateOf(initial?.username ?: "") }
    var pass by remember { mutableStateOf(initial?.password ?: "") }
    val valid = host.isNotBlank() && mp.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial==null) "Yeni Profil" else "Profili Düzenle") },
        confirmButton = {
            TextButton(onClick = {
                val ui = NtripProfileUi(
                    id = initial?.id ?: 0,
                    name = name.ifBlank { "Profil" },
                    host = host.trim(),
                    port = port.toIntOrNull() ?: 2101,
                    mountPoint = mp.trim(),
                    username = user.ifBlank { null },
                    password = pass.ifBlank { null },
                    autoConnect = initial?.autoConnect ?: false
                )
                onSave(ui)
            }, enabled = valid) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label={Text("Ad")}, singleLine = true)
                OutlinedTextField(host, { host = it }, label={Text("Host")}, singleLine = true, isError = host.isBlank())
                OutlinedTextField(port, { port = it.filter { ch -> ch.isDigit() }.take(5) }, label={Text("Port")}, singleLine = true)
                OutlinedTextField(mp, { mp = it.trim('/') }, label={Text("Mount Point")}, singleLine = true, isError = mp.isBlank())
                OutlinedTextField(user, { user = it }, label={Text("Kullanıcı (ops)")}, singleLine = true)
                OutlinedTextField(pass, { pass = it }, label={Text("Şifre (ops)")}, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (!valid) Text("Host ve Mount Point zorunlu", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}
