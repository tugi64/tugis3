package com.example.tugis3.ui.device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeviceCommunicationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                val vm: DeviceCommunicationViewModel = viewModel()
                DeviceCommunicationScreen(vm)
            }
        }
    }
}

// Basit UI modeli (Android BluetoothDevice sınıfından ayrışması için)
data class BTDeviceUi(val mac: String, val name: String, val status: String)

class DeviceCommunicationViewModel : androidx.lifecycle.ViewModel() {
    var isScanning by mutableStateOf(false)
        private set
    var devices by mutableStateOf(listOf<BTDeviceUi>())
        private set
    var log by mutableStateOf("")
        private set

    private val _selected = MutableStateFlow<BTDeviceUi?>(null)
    val selected = _selected.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    private val _nmea = MutableStateFlow<List<String>>(emptyList())
    val nmea = _nmea.asStateFlow()

    private var streamJob: kotlinx.coroutines.Job? = null

    fun scan() {
        if (isScanning) return
        isScanning = true
        log = "Taramaya başlandı...\n"
        viewModelScope.launch {
            devices = emptyList()
            // TODO: Gerçek Bluetooth taraması ile değiştirilecek
            val sample = listOf(
                BTDeviceUi("00:11:22:AA:BB:01", "SOUTH N80T", "Available"),
                BTDeviceUi("00:11:22:AA:BB:02", "SOUTH S86", "Available"),
                BTDeviceUi("00:11:22:AA:BB:03", "Trimble R12", "Available")
            )
            sample.forEach {
                delay(400)
                devices = devices + it
                log += "Bulundu: ${it.name}\n"
            }
            isScanning = false
            log += "Tarama bitti (${devices.size} cihaz).\n"
        }
    }

    fun selectDevice(d: BTDeviceUi) {
        _selected.value = d
    }

    fun toggleConnection() {
        val dev = _selected.value ?: return
        if (_connected.value) disconnect() else connect(dev)
    }

    private fun connect(dev: BTDeviceUi) {
        _connected.value = true
        log += "Bağlanıldı: ${dev.name}\n"
        startStream()
    }

    private fun disconnect() {
        _connected.value = false
        log += "Bağlantı kesildi.\n"
        streamJob?.cancel()
        streamJob = null
    }

    private fun startStream() {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            while (_connected.value) {
                delay(1000)
                val fixTypes = listOf("RTKFIX","RTKFLT","DGPS","SINGLE")
                val ft = fixTypes.random()
                val lat = 39 + kotlin.random.Random.nextDouble(0.0, 0.001)
                val lon = 32 + kotlin.random.Random.nextDouble(0.0, 0.001)
                val gga = "\$GPGGA,${System.currentTimeMillis()/1000},$lat,$lon,1,12,1.0,100.5,M,0.0,M,,,"
                val txt = "[$ft] $gga"
                appendNmea(txt)
            }
        }
    }

    private fun appendNmea(line: String) {
        _nmea.update { (it + line).takeLast(200) }
    }

    fun sendCommand(cmd: String) {
        if (cmd.isBlank()) return
        appendNmea("> $cmd")
    }

    fun clearLog() {
        _nmea.value = emptyList()
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCommunicationScreen(vm: DeviceCommunicationViewModel) {
    var selectedManufacturer by remember { mutableStateOf("SOUTH") }
    var selectedMode by remember { mutableStateOf("Bluetooth") }
    var debugMode by remember { mutableStateOf(false) }

    val selected by vm.selected.collectAsState()
    val connected by vm.connected.collectAsState()
    val nmea by vm.nmea.collectAsState()
    var command by remember { mutableStateOf("") }

    // Üretici -> Modeller haritası (South’a ALPS2 ve S82 eklendi)
    val manufacturerModels = remember {
        mapOf(
            "SOUTH" to listOf("ALPS2", "S82", "N80T", "S86"),
            "Trimble" to listOf("R10", "R12"),
            "Leica" to listOf("GS18i", "GS16"),
            "Topcon" to listOf("HiPer VR", "HiPer HR")
        )
    }
    var selectedModel by remember { mutableStateOf(manufacturerModels[selectedManufacturer]?.firstOrNull() ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Communication") },
                actions = {
                    IconButton(onClick = { debugMode = !debugMode }) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Debug Mode",
                            tint = if (debugMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Manufacturer & Model Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select Model",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    // Manufacturer
                    var manuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = manuExpanded,
                        onExpandedChange = { manuExpanded = !manuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedManufacturer,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Manufacturer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = manuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = manuExpanded,
                            onDismissRequest = { manuExpanded = false }
                        ) {
                            listOf("SOUTH", "Trimble", "Leica", "Topcon").forEach { manufacturer ->
                                DropdownMenuItem(
                                    text = { Text(manufacturer) },
                                    onClick = {
                                        selectedManufacturer = manufacturer
                                        selectedModel = manufacturerModels[manufacturer]?.firstOrNull() ?: ""
                                        manuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Model
                    var modelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            val models = manufacturerModels[selectedManufacturer].orEmpty()
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        selectedModel = model
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Communication Mode Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Communication Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { selectedMode = "Bluetooth" },
                            label = { Text("Bluetooth") },
                            selected = selectedMode == "Bluetooth",
                            leadingIcon = {
                                Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth")
                            }
                        )

                        FilterChip(
                            onClick = { selectedMode = "WLAN" },
                            label = { Text("WLAN") },
                            selected = selectedMode == "WLAN",
                            leadingIcon = {
                                Icon(Icons.Default.Wifi, contentDescription = "WLAN")
                            }
                        )

                        FilterChip(
                            onClick = { selectedMode = "Demo" },
                            label = { Text("Demo") },
                            selected = selectedMode == "Demo",
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Demo")
                            }
                        )
                    }
                }
            }

            // Device List (Bluetooth Mode)
            if (selectedMode == "Bluetooth") {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Available Devices",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = { vm.scan() },
                                enabled = !vm.isScanning
                            ) {
                                if (vm.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (vm.isScanning) "Scanning..." else "Scan")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (vm.devices.isEmpty() && !vm.isScanning) {
                            Text("Cihaz yok. Tarama başlatın.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(vm.devices) { device ->
                                    BluetoothDeviceCard(
                                        device = device,
                                        onConnect = {
                                            vm.selectDevice(device); vm.toggleConnection()
                                        },
                                        isConnected = connected && selected?.mac == device.mac,
                                        onSelectOnly = { vm.selectDevice(device) }
                                    )
                                }
                            }
                        }

                        Divider(Modifier.padding(vertical = 8.dp))
                        // Seçili cihaz durumu
                        selected?.let { d ->
                            Text("Seçili: ${d.name} (${d.mac})", fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.toggleConnection() }) { Text(if (connected) "Disconnect" else "Connect") }
                                OutlinedButton(onClick = { vm.clearLog() }, enabled = nmea.isNotEmpty()) { Text("Temizle") }
                            }
                        } ?: Text("Cihaz seçin")

                        if (connected) {
                            Spacer(Modifier.height(12.dp))
                            Text("NMEA / Data Stream", style = MaterialTheme.typography.titleMedium)
                            ElevatedCard(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                                Column(Modifier.padding(8.dp)) {
                                    LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                                        items(nmea.reversed()) { line ->
                                            Text(line, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = command,
                                            onValueChange = { command = it },
                                            label = { Text("Komut") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(onClick = { vm.sendCommand(command); command = "" }, enabled = command.isNotBlank()) { Text("Gönder") }
                                    }
                                }
                            }
                        }
                        if (debugMode) {
                            Divider(Modifier.padding(vertical = 8.dp))
                            Text(
                                vm.log,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.heightIn(min = 0.dp, max = 140.dp)
                            )
                        }
                    }
                }
            }

            // WLAN Mode
            if (selectedMode == "WLAN") {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "WLAN Connection",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Connect to receiver's WiFi hotspot")

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* Connect to WLAN */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = "WLAN")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect to Receiver WLAN")
                        }
                    }
                }
            }

            // Demo Mode
            if (selectedMode == "Demo") {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Demo Mode",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Use SurvStar without connecting to real receiver")

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("Starting Point Coordinates") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = { },
                                label = { Text("Direction") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "",
                                onValueChange = { },
                                label = { Text("Speed") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Debug Mode
            if (debugMode) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Debug Mode",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* Start/Stop data stream */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Start/Stop")
                            }

                            Button(
                                onClick = { /* Send command */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Send")
                            }

                            Button(
                                onClick = { /* Clear */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("Send Command") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Command List:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "• \$GPGGA - Position data\n• \$GPGSA - Satellite status\n• \$GPGSV - Satellite visibility",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothDeviceCard(
    device: BTDeviceUi,
    onConnect: () -> Unit,
    isConnected: Boolean,
    onSelectOnly: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isConnected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold)
                Text(device.mac, style = MaterialTheme.typography.bodySmall)
                Text(device.status, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSelectOnly) { Text("Seç") }
                    TextButton(onClick = onConnect) { Text(if (isConnected) "Kes" else "Bağlan") }
                }
            }
        }
    }
}
