package com.example.tugis3.ui.device

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.bluetooth.BluetoothGnssManager
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.gnss.NmeaLogRepository
import com.example.tugis3.gnss.nmea.NmeaParser
import com.example.tugis3.gnss.TrackRepository
import com.example.tugis3.service.GnssService
import com.example.tugis3.gnss.model.FixType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeviceCommunicationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.example.tugis3.ui.theme.Tugis3Theme {
                val vm: DeviceCommunicationViewModel = viewModel()
                DeviceCommunicationScreen(vm)
            }
        }
    }
}

// Basit UI modeli
data class BTDeviceUi(val mac: String, val name: String, val status: String)

class DeviceCommunicationViewModel : androidx.lifecycle.ViewModel() {
    private val btManager = BluetoothGnssManager()
    internal val discoveredDevices = mutableMapOf<String, BluetoothDevice>()

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
    private var readerJob: kotlinx.coroutines.Job? = null

    private val nmeaParser = NmeaParser()

    fun appendLog(line: String) { log += line + "\n" }

    fun ensureBluetoothReady(): Boolean {
        if (!btManager.isBluetoothSupported()) { appendLog("[HATA] Cihaz Bluetooth desteklemiyor"); return false }
        if (!btManager.isEnabled()) { appendLog("[UYARI] Bluetooth kapalı, lütfen açın"); return false }
        return true
    }

    fun scan(context: Context) {
        if (isScanning) return
        if (!ensureBluetoothReady()) return
        isScanning = true
        devices = emptyList()
        discoveredDevices.clear()
        appendLog("Tarama başlatılıyor...")
        val ok = btManager.startScan(context, object : BluetoothGnssManager.ScanCallback {
            override fun onDeviceFound(device: BluetoothDevice) { addOrUpdateDevice(device) }
            override fun onFinished() { isScanning = false; appendLog("Tarama bitti (${devices.size} cihaz)") }
            override fun onError(message: String) { isScanning = false; appendLog("[Tarama Hatası] $message") }
        })
        if (!ok) { isScanning = false; appendLog("Tarama başlatılamadı") }
    }

    fun stopScan(context: Context) {
        if (isScanning) {
            btManager.stopScan(context)
            isScanning = false
            appendLog("Tarama iptal edildi")
        }
    }

    private fun addOrUpdateDevice(device: BluetoothDevice) {
        val mac = device.address ?: return
        val bonded = device.bondState == BluetoothDevice.BOND_BONDED
        val ui = BTDeviceUi(mac, device.name ?: "(Adsız)", if (bonded) "Paired" else "Discovered")
        discoveredDevices[mac] = device
        val existing = devices.associateBy { it.mac }
        devices = (existing + (mac to ui)).values.sortedBy { it.name }
        // Seçili ise status güncelle
        if (_selected.value?.mac == mac) _selected.value = ui
    }

    fun onBondStateChanged(device: BluetoothDevice) {
        addOrUpdateDevice(device)
        when (device.bondState) {
            BluetoothDevice.BOND_BONDING -> appendLog("Eşleştiriliyor: ${device.name ?: device.address}")
            BluetoothDevice.BOND_BONDED -> appendLog("Eşleştirildi: ${device.name ?: device.address}")
            BluetoothDevice.BOND_NONE -> appendLog("Eşleşme yok: ${device.name ?: device.address}")
        }
    }

    fun selectDevice(d: BTDeviceUi) { _selected.value = d }

    fun toggleConnection(context: Context) {
        val dev = _selected.value ?: return
        if (_connected.value) disconnect() else connect(dev, context)
    }

    fun pairSelected() {
        val dev = _selected.value ?: return
        val real = discoveredDevices[dev.mac] ?: return
        if (real.bondState == BluetoothDevice.BOND_BONDED) { appendLog("Zaten eşleşmiş") ; return }
        val ok = btManager.pairDevice(real)
        appendLog(if (ok) "Eşleştirme isteği gönderildi" else "Eşleştirme isteği başarısız")
    }

    private fun connect(dev: BTDeviceUi, context: Context) {
        val real = discoveredDevices[dev.mac]
        if (real == null) { appendLog("Gerçek cihaz referansı bulunamadı: ${dev.mac}"); return }
        stopScan(context)
        viewModelScope.launch {
            appendLog("Bağlanılıyor: ${dev.name}")
            try {
                btManager.connectToDevice(real)
                _connected.value = true
                appendLog("Bağlandı: ${dev.name}")
                // GNSS service başlat
                try { context.startService(Intent(context, GnssService::class.java).setAction(GnssService.ACTION_START)) } catch (_: Exception) {}
                startReaderLoop()
            } catch (e: Exception) {
                appendLog("Bağlantı hatası: ${e.message}")
                _connected.value = false
            }
        }
    }

    private fun disconnect() {
        _connected.value = false
        appendLog("Bağlantı kesiliyor...")
        readerJob?.cancel(); readerJob = null
        streamJob?.cancel(); streamJob = null
        btManager.disconnect()
        appendLog("Bağlantı kesildi")
        // GNSS service durdur (cihaz bağlantısı yoksa)
        // Not: Birden fazla sağlayıcı düşünülüyorsa referans sayacı eklenebilir.
    }

    private fun startReaderLoop() {
        readerJob?.cancel()
        readerJob = viewModelScope.launch {
            val buf = ByteArray(4096)
            while (_connected.value) {
                try {
                    val r = btManager.readAvailable(buf)
                    if (r > 0) {
                        val text = String(buf, 0, r)
                        // NMEA satırlarını böl
                        text.split('\n', '\r').filter { it.isNotBlank() }.forEach { appendNmea(it.trim()) }
                    }
                } catch (e: Exception) {
                    appendLog("Okuma hatası: ${e.message}")
                    _connected.value = false
                }
                delay(150)
            }
        }
    }

    private fun appendNmea(line: String) {
        _nmea.update { (it + line).takeLast(400) }
        // Log repository'ye ekle
        viewModelScope.launch { NmeaLogRepository.add(line) }
        if (line.startsWith("$") && !line.startsWith(">$")) {
            try {
                val parsed = nmeaParser.parse(line.trim())
                if (parsed != null) {
                    GnssPositionRepository.update(
                        lat = parsed.latDeg,
                        lon = parsed.lonDeg,
                        alt = parsed.heightEllipsoidal,
                        hdop = parsed.hdop,
                        vdop = parsed.vdop,
                        pdop = parsed.pdop,
                        satsUse = parsed.satellitesInUse,
                        satsVis = parsed.satellitesVisible,
                        fix = parsed.fixType,
                        raw = parsed.raw
                    )
                    // Track kaydı açıksa nokta ekle (fix tipini geçir)
                    if (parsed.latDeg != null && parsed.lonDeg != null) {
                        viewModelScope.launch { TrackRepository.addPoint(parsed.latDeg, parsed.lonDeg, parsed.fixType) }
                    }
                }
            } catch (e: Exception) {
                appendLog("NMEA parse hata: ${e.message}")
            }
        }
    }

    fun exportNmea(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = NmeaLogRepository.exportToAppExternal(context)
            withContext(Dispatchers.Main) {
                result.onSuccess { file -> appendLog("NMEA export: ${file.absolutePath}") }
                    .onFailure { e -> appendLog("NMEA export hata: ${e.message}") }
            }
        }
    }

    fun sendCommand(cmd: String) {
        if (cmd.isBlank()) return
        viewModelScope.launch {
            if (!_connected.value) { appendLog("Komut gönderilemedi: bağlantı yok"); return@launch }
            try {
                val line = if (cmd.endsWith("\r\n")) cmd else cmd + "\r\n"
                btManager.write(line.toByteArray())
                appendNmea("> $cmd")
            } catch (e: Exception) { appendLog("Komut hatası: ${e.message}") }
        }
    }

    fun clearLog() { _nmea.value = emptyList() }

    override fun onCleared() {
        readerJob?.cancel(); streamJob?.cancel()
        try { btManager.disconnect() } catch (_: Exception) {}
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCommunicationScreen(vm: DeviceCommunicationViewModel) {
    val context = LocalContext.current
    var selectedManufacturer by remember { mutableStateOf("SOUTH") }
    var selectedMode by remember { mutableStateOf("Bluetooth") }
    var debugMode by remember { mutableStateOf(false) }

    val selected by vm.selected.collectAsState()
    val connected by vm.connected.collectAsState()
    val nmea by vm.nmea.collectAsState()
    var command by remember { mutableStateOf("") }

    // Bond state receiver (lifecycle aware)
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) vm.onBondStateChanged(device)
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val neededPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val allGranted = neededPermissions.all { result[it] == true }
        permissionDenied = !allGranted
        if (allGranted) vm.scan(context) else vm.appendLog("İzinler reddedildi")
    }

    fun requestOrScan() {
        val missing = neededPermissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing) permissionLauncher.launch(neededPermissions.toTypedArray()) else vm.scan(context)
    }

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
                    IconButton(onClick = { vm.exportNmea(context) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export NMEA")
                    }
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
                            leadingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth") }
                        )
                        FilterChip(
                            onClick = { selectedMode = "WLAN" },
                            label = { Text("WLAN") },
                            selected = selectedMode == "WLAN",
                            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = "WLAN") }
                        )
                        FilterChip(
                            onClick = { selectedMode = "Demo" },
                            label = { Text("Demo") },
                            selected = selectedMode == "Demo",
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = "Demo") }
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { vm.stopScan(context) }, enabled = vm.isScanning) { Text("Stop") }
                                Button(
                                    onClick = { requestOrScan() },
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
                        }
                        if (permissionDenied) {
                            Text("Bluetooth tarama izinleri gerekli", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (vm.devices.isEmpty() && !vm.isScanning) {
                            Text("Cihaz yok. Tarama başlatın.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(vm.devices) { device ->
                                    BluetoothDeviceCard(
                                        device = device,
                                        onConnect = { vm.selectDevice(device); vm.toggleConnection(context) },
                                        isConnected = connected && selected?.mac == device.mac,
                                        onSelectOnly = { vm.selectDevice(device) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        selected?.let { d ->
                            Text("Seçili: ${d.name} (${d.mac})", fontWeight = FontWeight.Medium)
                            val isPaired = d.status == "Paired"
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.toggleConnection(context) }) { Text(if (connected) "Disconnect" else "Connect") }
                                if (!isPaired) {
                                    OutlinedButton(onClick = { vm.pairSelected() }) { Text("Pair") }
                                }
                                OutlinedButton(onClick = { vm.clearLog() }, enabled = nmea.isNotEmpty()) { Text("Temizle") }
                            }
                        } ?: Text("Cihaz seçin")
                        if (connected) {
                            Spacer(Modifier.height(12.dp))
                            Text("NMEA / Data Stream", style = MaterialTheme.typography.titleMedium)
                            ElevatedCard(Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Son ${nmea.size} satır", style = MaterialTheme.typography.labelSmall)
                                        TextButton(onClick = { vm.exportNmea(context) }) { Text("Dışa Aktar") }
                                    }
                                    LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                                        items(nmea.reversed()) { line -> Text(line, style = MaterialTheme.typography.bodySmall) }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text("Komut") }, modifier = Modifier.weight(1f))
                                        Button(onClick = { vm.sendCommand(command); command = "" }, enabled = command.isNotBlank()) { Text("Gönder") }
                                    }
                                }
                            }
                        }
                        if (debugMode) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text(vm.log, style = MaterialTheme.typography.bodySmall, modifier = Modifier.heightIn(min = 0.dp, max = 140.dp))
                        }
                    }
                }
            }

            // WLAN Mode
            if (selectedMode == "WLAN") {
                // Basit placeholder
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("WLAN Connection (TODO)") } }
            }

            // Demo Mode
            if (selectedMode == "Demo") {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Demo Mode (Simülasyon)") } }
            }

            // Debug Mode
            if (debugMode && selectedMode != "Bluetooth") {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Debug Log Boş: Bluetooth dışı mod") } }
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
            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
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
