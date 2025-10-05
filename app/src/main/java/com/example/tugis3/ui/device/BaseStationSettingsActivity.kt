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
class BaseStationSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                BaseStationSettingsScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseStationSettingsScreen(onBackPressed: () -> Unit) {
    var baseId by remember { mutableStateOf("1000") }
    var selectedStartupMode by remember { mutableStateOf("Bilinen Noktada Kurulum") }
    var rtkDataFormat by remember { mutableStateOf("RTCM3.2") }
    var autoStart by remember { mutableStateOf(false) }
    var recordStaticData by remember { mutableStateOf(false) }
    var staticPointName by remember { mutableStateOf("BASE_001") }
    var staticInterval by remember { mutableStateOf("1HZ") }

    // Koordinat bilgileri
    var coordinateType by remember { mutableStateOf("Grid Koordinat") }
    var northing by remember { mutableStateOf("485068.1578") }
    var easting by remember { mutableStateOf("4415705.6843") }
    var elevation by remember { mutableStateOf("1127.3931") }

    // Anten ayarları
    var antennaHeight by remember { mutableStateOf("1.560") }
    var antennaMeasureType by remember { mutableStateOf("Eğik Yükseklik Çizgisi") }
    var antennaPhaseHeight by remember { mutableStateOf("1.5734") }

    // Veri bağlantısı
    var selectedDataLink by remember { mutableStateOf("Dahili Radyo") }
    var radioChannel by remember { mutableStateOf("1") }
    var radioFrequency by remember { mutableStateOf("462.5") }

    var isTransmitting by remember { mutableStateOf(false) }

    val startupModes = listOf("Bilinen Noktada Kurulum", "Herhangi Bir Noktada Kurulum")
    val dataFormats = listOf("RTCM3.2", "RTCM3", "CMR", "CMR+", "DGPS")
    val dataLinks = listOf("Dahili Radyo", "Harici Radyo", "Alıcı İnternet", "Wi-Fi Harici Radyo")
    val measureTypes = listOf("Eğik Yükseklik Çizgisi", "Jalon Yüksekliği")
    val intervals = listOf("60S", "30S", "15S", "10S", "5S", "2S", "1HZ")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sabit Ayarları") },
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
                // Hazır Modlar
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hazır Modlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                // Baz Koordinat Ayarları
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Baz Koordinat Ayarları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = baseId,
                                onValueChange = { baseId = it },
                                label = { Text("Baz Kimliği") },
                                modifier = Modifier.weight(1f)
                            )

                            var startupExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = startupExpanded,
                                onExpandedChange = { startupExpanded = !startupExpanded },
                                modifier = Modifier.weight(2f)
                            ) {
                                OutlinedTextField(
                                    value = selectedStartupMode,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Başlangıç Modu") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startupExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = startupExpanded,
                                    onDismissRequest = { startupExpanded = false }
                                ) {
                                    startupModes.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode) },
                                            onClick = {
                                                selectedStartupMode = mode
                                                startupExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        var formatExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = formatExpanded,
                            onExpandedChange = { formatExpanded = !formatExpanded }
                        ) {
                            OutlinedTextField(
                                value = rtkDataFormat,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("RTK Veri Formatı") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = formatExpanded,
                                onDismissRequest = { formatExpanded = false }
                            ) {
                                dataFormats.forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format) },
                                        onClick = {
                                            rtkDataFormat = format
                                            formatExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoStart,
                                onCheckedChange = { autoStart = it }
                            )
                            Text("Otomatik Başlat")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = recordStaticData,
                                onCheckedChange = { recordStaticData = it }
                            )
                            Text("Statik Veri Kaydet")
                        }

                        if (recordStaticData) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = staticPointName,
                                    onValueChange = { staticPointName = it },
                                    label = { Text("Nokta Adı") },
                                    modifier = Modifier.weight(1f)
                                )

                                var intervalExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = intervalExpanded,
                                    onExpandedChange = { intervalExpanded = !intervalExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = staticInterval,
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text("Toplama Aralığı") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = intervalExpanded,
                                        onDismissRequest = { intervalExpanded = false }
                                    ) {
                                        intervals.forEach { interval ->
                                            DropdownMenuItem(
                                                text = { Text(interval) },
                                                onClick = {
                                                    staticInterval = interval
                                                    intervalExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedStartupMode == "Bilinen Noktada Kurulum") {
                item {
                    // Koordinat Girişi
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bilinen Nokta Koordinatları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            var coordTypeExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = coordTypeExpanded,
                                onExpandedChange = { coordTypeExpanded = !coordTypeExpanded }
                            ) {
                                OutlinedTextField(
                                    value = coordinateType,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Koordinat Türü") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = coordTypeExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = coordTypeExpanded,
                                    onDismissRequest = { coordTypeExpanded = false }
                                ) {
                                    listOf("Grid Koordinat", "Coğrafi Koordinat").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                coordinateType = type
                                                coordTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = northing,
                                onValueChange = { northing = it },
                                label = { Text(if (coordinateType == "Grid Koordinat") "Kuzey (N)" else "Enlem") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = easting,
                                onValueChange = { easting = it },
                                label = { Text(if (coordinateType == "Grid Koordinat") "Doğu (E)" else "Boylam") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = elevation,
                                onValueChange = { elevation = it },
                                label = { Text("Yükseklik (Z)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { /* Konum oku */ },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.MyLocation, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Konum Oku")
                                }

                                OutlinedButton(
                                    onClick = { /* Nokta listesinden seç */ },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.List, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Nokta Listesi")
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Anten Ayarları
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Anten Ayarları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = antennaHeight,
                            onValueChange = { antennaHeight = it },
                            label = { Text("Anten Yüksekliği (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        var measureTypeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = measureTypeExpanded,
                            onExpandedChange = { measureTypeExpanded = !measureTypeExpanded }
                        ) {
                            OutlinedTextField(
                                value = antennaMeasureType,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Anten Ölçüm Türü") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = measureTypeExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = measureTypeExpanded,
                                onDismissRequest = { measureTypeExpanded = false }
                            ) {
                                measureTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            antennaMeasureType = type
                                            measureTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = antennaPhaseHeight,
                            onValueChange = { antennaPhaseHeight = it },
                            label = { Text("Faz Merkezi Yüksekliği (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )

                        Text(
                            "Not: Sehpa kurulumu için çentik uçlarından ölçüm yapın",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                label = { Text("Veri Bağlantısı") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dataLinkExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = dataLinkExpanded,
                                onDismissRequest = { dataLinkExpanded = false }
                            ) {
                                dataLinks.forEach { link ->
                                    DropdownMenuItem(
                                        text = { Text(link) },
                                        onClick = {
                                            selectedDataLink = link
                                            dataLinkExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedDataLink.contains("Radyo")) {
                            Spacer(Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = radioChannel,
                                    onValueChange = { radioChannel = it },
                                    label = { Text("Kanal") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = radioFrequency,
                                    onValueChange = { radioFrequency = it },
                                    label = { Text("Frekans (MHz)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(2f)
                                )
                            }

                            Text(
                                "8 kanal mevcuttur (7 sabit + 1 ayarlanabilir)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                // Transmit Status ve Control
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTransmitting) MaterialTheme.colorScheme.primaryContainer
                                       else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isTransmitting) "RTK Yayını Aktif" else "RTK Yayını Durmuş",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Icon(
                                if (isTransmitting) Icons.Default.Radio else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isTransmitting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isTransmitting) {
                            Text("Düzeltme verisi yayınlanıyor", style = MaterialTheme.typography.bodyMedium)
                            Text("Baz ID: $baseId | Format: $rtkDataFormat", style = MaterialTheme.typography.bodySmall)
                            if (recordStaticData) {
                                Text("Statik veri kaydediliyor: $staticPointName", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text("Düzeltme verisi yayınlanmıyor", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isTransmitting) {
                                Button(
                                    onClick = { isTransmitting = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Stop, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Durdur")
                                }
                            } else {
                                Button(
                                    onClick = { isTransmitting = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Başlat")
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
