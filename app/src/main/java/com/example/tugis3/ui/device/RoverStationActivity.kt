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
class RoverStationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                RoverStationScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoverStationScreen(onBack: () -> Unit) {
    var dataLink by remember { mutableStateOf("Internal Radio") }
    var radioChannel by remember { mutableStateOf("1") }
    var correctionSource by remember { mutableStateOf("Radio RTK") }
    var isReceivingCorrections by remember { mutableStateOf(false) }
    var rtkStatus by remember { mutableStateOf("Float") }
    var accuracy by remember { mutableStateOf("1.2") }
    var antennaHeight by remember { mutableStateOf("2.000") }
    var imuEnabled by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val dataLinks = listOf("Internal Radio", "External Radio", "NTRIP", "Serial", "Bluetooth")
    val radioChannels = (1..20).map { it.toString() }
    val correctionSources = listOf("Radio RTK", "NTRIP", "CORS", "Base Station")
    val rtkStatuses = listOf("Fixed", "Float", "DGPS", "Single")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rover Station Yapılandırma") },
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
            // RTK Durumu
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "RTK Durumu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Düzeltme Durumu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (isReceivingCorrections) Icons.Default.SignalWifi4Bar else Icons.Default.SignalWifiOff,
                                        contentDescription = null,
                                        tint = if (isReceivingCorrections) MaterialTheme.colorScheme.primary
                                              else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = if (isReceivingCorrections) "Alınıyor" else "Alınamıyor",
                                        fontWeight = FontWeight.Medium,
                                        color = if (isReceivingCorrections) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "RTK Durumu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = rtkStatus,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when(rtkStatus) {
                                        "Fixed" -> MaterialTheme.colorScheme.primary
                                        "Float" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            Column {
                                Text(
                                    text = "Doğruluk",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${accuracy} cm",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isReceivingCorrections = !isReceivingCorrections
                                rtkStatus = if (isReceivingCorrections) "Fixed" else "Single"
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isReceivingCorrections) "RTK başlatıldı" else "RTK durduruldu"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (isReceivingCorrections) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isReceivingCorrections) "RTK'yi Durdur" else "RTK'yi Başlat")
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
                            var channelExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = channelExpanded,
                                onExpandedChange = { channelExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = radioChannel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Radio Kanalı") },
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
                        }

                        if (dataLink == "NTRIP") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                var ntripHost by remember { mutableStateOf("cors.tusaga-aktif.gov.tr") }
                                var ntripPort by remember { mutableStateOf("2101") }
                                var ntripMountpoint by remember { mutableStateOf("RTCM33_NEAR") }
                                var ntripUsername by remember { mutableStateOf("") }
                                var ntripPassword by remember { mutableStateOf("") }

                                OutlinedTextField(
                                    value = ntripHost,
                                    onValueChange = { ntripHost = it },
                                    label = { Text("NTRIP Sunucusu") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ntripPort,
                                        onValueChange = { ntripPort = it },
                                        label = { Text("Port") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = ntripMountpoint,
                                        onValueChange = { ntripMountpoint = it },
                                        label = { Text("Mountpoint") },
                                        modifier = Modifier.weight(2f)
                                    )
                                }

                                OutlinedTextField(
                                    value = ntripUsername,
                                    onValueChange = { ntripUsername = it },
                                    label = { Text("Kullanıcı Adı") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = ntripPassword,
                                    onValueChange = { ntripPassword = it },
                                    label = { Text("Şifre") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        var correctionExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = correctionExpanded,
                            onExpandedChange = { correctionExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = correctionSource,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Düzeltme Kaynağı") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = correctionExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = correctionExpanded,
                                onDismissRequest = { correctionExpanded = false }
                            ) {
                                correctionSources.forEach { source ->
                                    DropdownMenuItem(
                                        text = { Text(source) },
                                        onClick = {
                                            correctionSource = source
                                            correctionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Anten Ayarları
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Anten Ayarları",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = antennaHeight,
                            onValueChange = { antennaHeight = it },
                            label = { Text("Anten Yüksekliği (m)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("IMU Eğim Ölçümü")
                            Switch(
                                checked = imuEnabled,
                                onCheckedChange = { imuEnabled = it }
                            )
                        }

                        if (imuEnabled) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "IMU Durumu",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Eğim X: 2.3°")
                                        Text("Eğim Y: -1.1°")
                                        Text("Kalibre: ✓")
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Anten kalibre edildi")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kalibre Et")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Yükseklik ölçüldü")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Height, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ölç")
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

                        var elevationMask by remember { mutableStateOf("15") }
                        OutlinedTextField(
                            value = elevationMask,
                            onValueChange = { elevationMask = it },
                            label = { Text("Yükseklik Maskesi (°)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        var pdopMask by remember { mutableStateOf("6.0") }
                        OutlinedTextField(
                            value = pdopMask,
                            onValueChange = { pdopMask = it },
                            label = { Text("PDOP Maskesi") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Otomatik Yeniden Başlatma")
                            Switch(
                                checked = true,
                                onCheckedChange = { }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Koordinat Loglama")
                            Switch(
                                checked = false,
                                onCheckedChange = { }
                            )
                        }
                    }
                }
            }

            // Rover İstatistikleri
            if (isReceivingCorrections) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Rover İstatistikleri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Uydu Sayısı",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "14",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Column {
                                    Text(
                                        text = "PDOP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "1.8",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Age",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "2.1s",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Ratio",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "35.2",
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
