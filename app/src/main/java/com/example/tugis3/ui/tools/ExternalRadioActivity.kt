package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExternalRadioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                ExternalRadioScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalRadioScreen(onBack: () -> Unit) {
    var isConnected by remember { mutableStateOf(false) }
    var radioModel by remember { mutableStateOf("Pacific Crest") }
    var portType by remember { mutableStateOf("RS232") }
    var baudRate by remember { mutableStateOf("9600") }
    var frequency by remember { mutableStateOf("450.000") }
    var power by remember { mutableStateOf(1f) }
    var channelSpacing by remember { mutableStateOf("12.5 KHz") }
    var networkId by remember { mutableStateOf("00") }

    // Radyo Konfigürasyonu Türü
    var configType by remember { mutableStateOf("Transmitter") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Harici Radyo Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Yardım */ }) {
                        Icon(Icons.Default.Help, "Yardım")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bağlantı Durumu", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isConnected) "Radyo Bağlı" else "Radyo Bağlı Değil",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = isConnected,
                            onCheckedChange = { isConnected = it }
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Radyo Modeli", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val radioModels = listOf(
                            "Pacific Crest",
                            "Satel",
                            "TrimTalk",
                            "HPT Series",
                            "Transparent Mode"
                        )

                        radioModels.forEach { model ->
                            FilterChip(
                                selected = radioModel == model,
                                onClick = { radioModel = model },
                                label = { Text(model) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Port Ayarları", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = portType == "RS232",
                                onClick = { portType = "RS232" },
                                label = { Text("RS232") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = portType == "RS485",
                                onClick = { portType = "RS485" },
                                label = { Text("RS485") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Baud Rate", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("4800", "9600", "19200", "38400", "57600").forEach { rate ->
                                FilterChip(
                                    selected = baudRate == rate,
                                    onClick = { baudRate = rate },
                                    label = { Text(rate, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Konfigürasyon Tipi", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = configType == "Transmitter",
                                onClick = { configType = "Transmitter" },
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Transmitter", fontWeight = FontWeight.Bold)
                                        Text("(Base)", style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = configType == "Receiver",
                                onClick = { configType = "Receiver" },
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Receiver", fontWeight = FontWeight.Bold)
                                        Text("(Rover)", style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Radyo Parametreleri", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = frequency,
                            onValueChange = { frequency = it },
                            label = { Text("Frekans (MHz)") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Radio, null) },
                            supportingText = { Text("400-470 MHz arası") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Güç Seviyesi: ${power.toInt()} W", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = power,
                            onValueChange = { power = it },
                            valueRange = 0f..5f,
                            steps = 4
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0W", style = MaterialTheme.typography.bodySmall)
                            Text("5W", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Kanal Aralığı", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = channelSpacing == "12.5 KHz",
                                onClick = { channelSpacing = "12.5 KHz" },
                                label = { Text("12.5 KHz") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = channelSpacing == "25 KHz",
                                onClick = { channelSpacing = "25 KHz" },
                                label = { Text("25 KHz") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = networkId,
                            onValueChange = { networkId = it },
                            label = { Text("Network ID") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("00-FF arası hex değer") }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sinyal Kalitesi", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("RSSI", style = MaterialTheme.typography.labelMedium)
                                Text("-72 dBm", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SNR", style = MaterialTheme.typography.labelMedium)
                                Text("28 dB", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = 0.7f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sinyal Kalitesi: İyi", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Test */ },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected
                    ) {
                        Icon(Icons.Default.Speed, "Test")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bağlantı Testi")
                    }

                    Button(
                        onClick = { /* Kaydet */ },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected
                    ) {
                        Icon(Icons.Default.Save, "Kaydet")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ayarları Kaydet")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Icon(Icons.Default.Info, "Bilgi", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Harici radyo, RTK çalışmasında baz istasyonu ve gezici arasında düzeltme verisi iletimi için kullanılır. Frekans seçimi bölgenizin yasal düzenlemelerine uygun olmalıdır.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
