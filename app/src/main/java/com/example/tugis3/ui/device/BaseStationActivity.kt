package com.example.tugis3.ui.device

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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BaseStationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                BaseStationScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaseStationScreen(onBack: () -> Unit) {
    var startupMode by remember { mutableStateOf("Statik Nokta") }
    var basePointName by remember { mutableStateOf("BASE01") }
    var coordinates by remember { mutableStateOf(BaseCoordinates()) }
    var dataLink by remember { mutableStateOf("Internal Radio") }
    var radioChannel by remember { mutableStateOf("1") }
    var radioPower by remember { mutableStateOf("5W") }
    var dataFormat by remember { mutableStateOf("RTCM3.3") }
    var isTransmitting by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val startupModes = listOf("Statik Nokta", "Ortalama Nokta", "Son Konum", "Manuel Giriş")
    val dataLinks = listOf("Internal Radio", "External Radio", "Network", "Serial")
    val radioChannels = (1..20).map { it.toString() }
    val radioPowers = listOf("1W", "2W", "5W", "10W", "20W")
    val dataFormats = listOf("RTCM3.3", "RTCM3.2", "RTCM2.3", "CMR+")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Base Station Yapılandırma") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Base Station Durumu
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Base Station Durumu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (isTransmitting) Icons.Default.Radio else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isTransmitting) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isTransmitting) "Yayın Yapıyor" else "Durduruldu",
                                    color = if (isTransmitting) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isTransmitting = !isTransmitting
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isTransmitting) "Base station başlatıldı"
                                            else "Base station durduruldu"
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (isTransmitting) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isTransmitting) "Durdur" else "Başlat")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Konfigürasyon kaydedildi")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kaydet")
                            }
                        }
                    }
                }
            }

            // Startup Mode Ayarları
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Başlangıç Modu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        var startupExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = startupExpanded,
                            onExpandedChange = { startupExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = startupMode,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Başlangıç Modu") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = startupExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = startupExpanded,
                                onDismissRequest = { startupExpanded = false }
                            ) {
                                startupModes.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode) },
                                        onClick = {
                                            startupMode = mode
                                            startupExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = basePointName,
                            onValueChange = { basePointName = it },
                            label = { Text("Base Nokta Adı") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Koordinat Ayarları
            if (startupMode == "Manuel Giriş" || startupMode == "Statik Nokta") {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Koordinat Ayarları",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = coordinates.latitude,
                                onValueChange = { coordinates = coordinates.copy(latitude = it) },
                                label = { Text("Enlem (°)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = coordinates.longitude,
                                onValueChange = { coordinates = coordinates.copy(longitude = it) },
                                label = { Text("Boylam (°)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = coordinates.height,
                                onValueChange = { coordinates = coordinates.copy(height = it) },
                                label = { Text("Yükseklik (m)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Mevcut konum alındı")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mevcut Konum")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Nokta seçildi")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Nokta Seç")
                                }
                            }
                        }
                    }
                }
            }

            // Veri Bağlantısı Ayarları
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Veri Bağlantısı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        var dataLinkExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = dataLinkExpanded,
                            onExpandedChange = { dataLinkExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = dataLink,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Veri Bağlantısı") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dataLinkExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = dataLinkExpanded,
                                onDismissRequest = { dataLinkExpanded = false }
                            ) {
                                dataLinks.forEach { link ->
                                    DropdownMenuItem(
                                        text = { Text(link) },
                                        onClick = {
                                            dataLink = link
                                            dataLinkExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (dataLink.contains("Radio")) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var channelExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = channelExpanded,
                                    onExpandedChange = { channelExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = radioChannel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Radyo Kanalı") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = channelExpanded,
                                        onDismissRequest = { channelExpanded = false }
                                    ) {
                                        radioChannels.forEach { channel ->
                                            DropdownMenuItem(
                                                text = { Text("Kanal $channel") },
                                                onClick = {
                                                    radioChannel = channel
                                                    channelExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                var powerExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = powerExpanded,
                                    onExpandedChange = { powerExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = radioPower,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Radyo Gücü") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = powerExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = powerExpanded,
                                        onDismissRequest = { powerExpanded = false }
                                    ) {
                                        radioPowers.forEach { power ->
                                            DropdownMenuItem(
                                                text = { Text(power) },
                                                onClick = {
                                                    radioPower = power
                                                    powerExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        var formatExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = formatExpanded,
                            onExpandedChange = { formatExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = dataFormat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Veri Formatı") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = formatExpanded,
                                onDismissRequest = { formatExpanded = false }
                            ) {
                                dataFormats.forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format) },
                                        onClick = {
                                            dataFormat = format
                                            formatExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Gelişmiş Ayarlar
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Gelişmiş Ayarlar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        var messageInterval by remember { mutableStateOf("1") }
                        OutlinedTextField(
                            value = messageInterval,
                            onValueChange = { messageInterval = it },
                            label = { Text("Mesaj Aralığı (s)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Anten Yüksekliği Ölçümü")
                            Switch(
                                checked = true,
                                onCheckedChange = { }
                            )
                        }

                        var antennaHeight by remember { mutableStateOf("2.000") }
                        OutlinedTextField(
                            value = antennaHeight,
                            onValueChange = { antennaHeight = it },
                            label = { Text("Anten Yüksekliği (m)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Base Station İstatistikleri
            if (isTransmitting) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Base Station İstatistikleri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Uptime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "02:15:30",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Uydu Sayısı",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "12",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Mesaj Sayısı",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "8,234",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BaseCoordinates(
    val latitude: String = "41.008238",
    val longitude: String = "28.978359",
    val height: String = "100.000"
)
