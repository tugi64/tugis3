package com.example.tugis3.ui.device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoverSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                RoverSettingsScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoverSettingsScreen(onBackPressed: () -> Unit) {
    var elevationAngle by remember { mutableStateOf(10) }
    var recordRawData by remember { mutableStateOf(false) }
    var enableARTK by remember { mutableStateOf(true) }
    var artkAgeLimit by remember { mutableStateOf(120) }
    var selectedDataLink by remember { mutableStateOf("NTRIP") }
    var ntripServer by remember { mutableStateOf("cors.harita.gov.tr") }
    var ntripPort by remember { mutableStateOf(2101) }
    var ntripUsername by remember { mutableStateOf("") }
    var ntripPassword by remember { mutableStateOf("") }
    var mountPoint by remember { mutableStateOf("VRS_RTCM31") }
    var isConnected by remember { mutableStateOf(false) }

    val dataLinkOptions = listOf("NTRIP", "Dahili Radyo", "Harici Radyo", "Wi-Fi Harici Radyo")
    val mountPoints = listOf("VRS_RTCM31", "VRSCMRP", "FKP_RTCM31", "RTCM3NET")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gezici Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Konfigürasyonlar
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Konfigürasyonlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedButton(
                            onClick = { /* Konfigürasyon seç */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Varsayılan Konfigürasyon")
                        }
                    }
                }
            }

            item {
                // Temel Ayarlar
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Temel Ayarlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = elevationAngle.toString(),
                            onValueChange = { it.toIntOrNull()?.let { v -> if (v in 0..90) elevationAngle = v } },
                            label = { Text("Yükseklik Açısı (°)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = recordRawData,
                                onCheckedChange = { recordRawData = it }
                            )
                            Column {
                                Text("Ham Veri Kaydet")
                                Text("Post-processing için ham GNSS verisi kaydeder", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item {
                // aRTK Ayarları
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("aRTK Teknolojisi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Düzeltme verisi kesildiğinde fix çözümü korur", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = enableARTK,
                                onCheckedChange = { enableARTK = it }
                            )
                        }

                        if (enableARTK) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = artkAgeLimit.toString(),
                                onValueChange = { it.toIntOrNull()?.let { v -> if (v > 0) artkAgeLimit = v } },
                                label = { Text("aRTK Gecikme Limiti (saniye)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                // Veri Bağlantısı
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Veri Bağlantısı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        var dataLinkExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = dataLinkExpanded,
                            onExpandedChange = { dataLinkExpanded = !dataLinkExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDataLink,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Bağlantı Modu") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dataLinkExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = dataLinkExpanded,
                                onDismissRequest = { dataLinkExpanded = false }
                            ) {
                                dataLinkOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedDataLink = option
                                            dataLinkExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedDataLink == "NTRIP") {
                item {
                    // NTRIP Ayarları
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("NTRIP Ayarları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = ntripServer,
                                onValueChange = { ntripServer = it },
                                label = { Text("Sunucu Adı") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ntripPort.toString(),
                                    onValueChange = { it.toIntOrNull()?.let { v -> if (v > 0) ntripPort = v } },
                                    label = { Text("Port") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )

                                var mountExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = mountExpanded,
                                    onExpandedChange = { mountExpanded = !mountExpanded },
                                    modifier = Modifier.weight(2f)
                                ) {
                                    OutlinedTextField(
                                        value = mountPoint,
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text("Mount Point") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mountExpanded) },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = mountExpanded,
                                        onDismissRequest = { mountExpanded = false }
                                    ) {
                                        mountPoints.forEach { point ->
                                            DropdownMenuItem(
                                                text = { Text(point) },
                                                onClick = {
                                                    mountPoint = point
                                                    mountExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = ntripUsername,
                                onValueChange = { ntripUsername = it },
                                label = { Text("Kullanıcı Adı") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = ntripPassword,
                                onValueChange = { ntripPassword = it },
                                label = { Text("Şifre") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = true, onCheckedChange = { })
                                Text("Otomatik Olarak Bağlanma")
                            }
                        }
                    }
                }
            }

            if (selectedDataLink.contains("Radyo")) {
                item {
                    // Radyo Ayarları
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Radyo Ayarları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = "1",
                                    onValueChange = { },
                                    label = { Text("Kanal") },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = "462.500",
                                    onValueChange = { },
                                    label = { Text("Frekans (MHz)") },
                                    modifier = Modifier.weight(2f)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text("RTK veri formatı otomatik tanınır", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                // Bağlantı Durumu ve Kontrol
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer
                                       else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isConnected) "Bağlantı Aktif" else "Bağlantı Yok",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Icon(
                                if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        if (isConnected) {
                            Text("Düzeltme verisi alınıyor", style = MaterialTheme.typography.bodyMedium)
                            Text("Veri hızı: 1.2 KB/s", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Düzeltme verisi alınamıyor", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isConnected) {
                                Button(
                                    onClick = { isConnected = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Stop, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Durdur")
                                }
                            } else {
                                Button(
                                    onClick = { isConnected = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Başla")
                                }
                            }

                            OutlinedButton(
                                onClick = { /* Gelişmiş ayarlar */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Gelişmiş")
                            }
                        }
                    }
                }
            }

            item {
                // Kaydet ve Uygula
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Kaydet & Uygula */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kaydet & Uygula")
                    }

                    Button(
                        onClick = { /* Uygula */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Uygula")
                    }
                }
            }
        }
    }
}
