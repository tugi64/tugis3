package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExternalRadioConfigActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { ExternalRadioConfigScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalRadioConfigScreen(onBack: () -> Unit) {
    var connectionType by remember { mutableStateOf("Bluetooth") }
    var baudRate by remember { mutableStateOf("115200") }
    var frequency by remember { mutableStateOf("450.0") }
    var powerLevel by remember { mutableStateOf("Medium") }
    var workMode by remember { mutableStateOf("Base") }
    var channelNumber by remember { mutableStateOf("1") }
    var rssiValue by remember { mutableStateOf(-65) }
    var isConnected by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // RSSI simülasyonu
    LaunchedEffect(isConnected) {
        while (isConnected) {
            delay(1000)
            rssiValue = (-85..-55).random()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dış Radyo Yapılandırması") },
                navigationIcon = {
                    IconButton(onClick = onBack){
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            snackbar.showSnackbar("Ayarlar kaydedildi")
                        }
                    }) {
                        Icon(Icons.Default.Save, "Kaydet")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bağlantı Durumu
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isConnected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                null,
                                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (isConnected) "Radyo Bağlı" else "Bağlantı Yok",
                                    fontWeight = FontWeight.Bold
                                )
                                if (isConnected) {
                                    Text(
                                        "RSSI: $rssiValue dBm",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                isConnected = !isConnected
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (isConnected) "Radyo bağlandı" else "Bağlantı kesildi"
                                    )
                                }
                            }
                        ) {
                            Text(if (isConnected) "Kes" else "Bağlan")
                        }
                    }
                }
            }

            // Port Tipi
            item {
                Text("Bağlantı Tipi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Bluetooth", "USB", "TCP/IP").forEach { type ->
                                FilterChip(
                                    selected = connectionType == type,
                                    onClick = { connectionType = type },
                                    label = { Text(type) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (connectionType == "Bluetooth") {
                            OutlinedTextField(
                                value = "HC-05 Radio Module",
                                onValueChange = {},
                                label = { Text("Bluetooth Cihaz") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        isScanning = true
                                        scope.launch {
                                            delay(2000)
                                            isScanning = false
                                            snackbar.showSnackbar("3 cihaz bulundu")
                                        }
                                    }) {
                                        if (isScanning) {
                                            CircularProgressIndicator(Modifier.size(20.dp))
                                        } else {
                                            Icon(Icons.Default.Search, "Ara")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Baud Rate
            item {
                Text("İletişim Hızı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("9600", "19200", "38400", "57600", "115200").forEach { rate ->
                                FilterChip(
                                    selected = baudRate == rate,
                                    onClick = { baudRate = rate },
                                    label = { Text(rate) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Frekans Ayarları
            item {
                Text("Frekans Yapılandırması", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = frequency,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) frequency = it },
                            label = { Text("Frekans (MHz)") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Sensors, null) },
                            trailingIcon = {
                                IconButton(onClick = { showFrequencyDialog = true }) {
                                    Icon(Icons.AutoMirrored.Filled.List, "Ön Ayarlar")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = channelNumber,
                            onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) channelNumber = it },
                            label = { Text("Kanal Numarası") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Tune, null) }
                        )

                        Text("Modülasyon: FSK", style = MaterialTheme.typography.bodyMedium)
                        Text("Bant Genişliği: 25 kHz", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Güç Seviyesi
            item {
                Text("Güç Seviyesi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Low" to "10mW", "Medium" to "100mW", "High" to "1W").forEach { (level, power) ->
                                FilterChip(
                                    selected = powerLevel == level,
                                    onClick = { powerLevel = level },
                                    label = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(level)
                                            Text(power, style = MaterialTheme.typography.labelSmall)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Çalışma Modu
            item {
                Text("Çalışma Rolü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = workMode == "Base",
                                onClick = { workMode = "Base" },
                                label = {
                                    Column {
                                        Text("Base Station")
                                        Text("Baz İstasyonu", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.SignalCellularAlt, null) }
                            )
                            FilterChip(
                                selected = workMode == "Rover",
                                onClick = { workMode = "Rover" },
                                label = {
                                    Column {
                                        Text("Rover")
                                        Text("Gezgin", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.MyLocation, null) }
                            )
                        }
                    }
                }
            }

            // Sinyal Kalitesi
            if (isConnected) {
                item {
                    Text("Sinyal Kalitesi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RSSI (Received Signal Strength)")
                                Text(
                                    "$rssiValue dBm",
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        rssiValue > -70 -> MaterialTheme.colorScheme.primary
                                        rssiValue > -80 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }

                            LinearProgressIndicator(
                                progress = { ((rssiValue + 100) / 50f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = when {
                                    rssiValue > -70 -> MaterialTheme.colorScheme.primary
                                    rssiValue > -80 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )

                            Text(
                                when {
                                    rssiValue > -70 -> "Mükemmel Sinyal"
                                    rssiValue > -80 -> "İyi Sinyal"
                                    else -> "Zayıf Sinyal"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Test ve Tanılama
            item {
                Text("Test ve Tanılama", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    snackbar.showSnackbar("Test verisi gönderiliyor...")
                                    delay(1000)
                                    snackbar.showSnackbar("✓ Test başarılı")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Test Verisi Gönder")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    snackbar.showSnackbar("Tanılama başlatıldı...")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Troubleshoot, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Radyo Tanılaması Yap")
                        }
                    }
                }
            }
        }
    }

    // Frekans Ön Ayarları Dialog
    if (showFrequencyDialog) {
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = { Text("Frekans Ön Ayarları") },
            text = {
                Column(Modifier.height(300.dp)) {
                    (1..5).forEach { index ->
                        val freq = 450.0 + (index - 1) * 5
                        ListItem(
                            headlineContent = { Text("Kanal $index") },
                            supportingContent = { Text("$freq MHz") },
                            modifier = Modifier.clickable {
                                frequency = freq.toString()
                                channelNumber = index.toString()
                                showFrequencyDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFrequencyDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}
