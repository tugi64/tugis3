@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.tugis3.ui.device

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.Tugis3Application
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

class DeviceCommunicationViewModel(application: Application) : AndroidViewModel(application) {
    private val btManager = BluetoothGnssManager(application.applicationContext)
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
        // btManager.isBluetoothSupported() ve isEnabled() yerine adapter kontrolü yapalım
        val adapter = btManager.bluetoothAdapter
        if (adapter == null) { appendLog("[HATA] Cihaz Bluetooth desteklemiyor"); return false }
        if (!adapter.isEnabled) { appendLog("[UYARI] Bluetooth kapalı, lütfen açın"); return false }
        return true
    }

    fun scan(context: Context) {
        if (isScanning) return
        if (!ensureBluetoothReady()) return
        isScanning = true
        devices = emptyList()
        discoveredDevices.clear()
        appendLog("Tarama başlatılıyor...")
        // startScan metodunu çağıralım - callback parametresi yok, receiver ile çalışıyor
        try {
            btManager.startDiscovery()
            appendLog("Tarama başlatıldı")
        } catch (e: Exception) {
            isScanning = false
            appendLog("Tarama başlatılamadı: ${e.message}")
        }
    }

    fun stopScan(context: Context) {
        if (isScanning) {
            try {
                btManager.stopDiscovery()
                isScanning = false
                appendLog("Tarama iptal edildi")
            } catch (e: Exception) {
                appendLog("Tarama durdurma hatası: ${e.message}")
            }
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
        if (_connected.value) disconnect(context) else connect(dev, context)
    }

    fun pairSelected() {
        val dev = _selected.value ?: return
        val real = discoveredDevices[dev.mac] ?: return
        if (real.bondState == BluetoothDevice.BOND_BONDED) { appendLog("Zaten eşleşmiş") ; return }
        try {
            val ok = real.createBond()
            appendLog(if (ok) "Eşleştirme isteği gönderildi" else "Eşleştirme isteği başarısız")
        } catch (e: SecurityException) {
            appendLog("Eşleştirme izin hatası: ${e.message}")
        } catch (e: Exception) {
            appendLog("Eşleştirme hatası: ${e.message}")
        }
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
                GnssService.start(context)
                startReaderLoop()
            } catch (e: Exception) {
                appendLog("Bağlantı hatası: ${e.message}")
                _connected.value = false
            }
        }
    }

    private fun disconnect(context: Context) {
        _connected.value = false
        appendLog("Bağlantı kesiliyor...")
        readerJob?.cancel(); readerJob = null
        streamJob?.cancel(); streamJob = null
        btManager.disconnect()
        appendLog("Bağlantı kesildi")
        // GNSS service durdur
        GnssService.stop(context)
    }

    private fun startReaderLoop() {
        readerJob?.cancel()
        readerJob = viewModelScope.launch(Dispatchers.IO) {
            val buf = ByteArray(4096)
            while (_connected.value) {
                try {
                    // BluetoothGnssManager'dan inputStream alıp okuyoruz
                    val stream = btManager.inputStream
                    if (stream != null && stream.available() > 0) {
                        val bytesRead = stream.read(buf, 0, buf.size)
                        if (bytesRead > 0) {
                            val text = String(buf, 0, bytesRead)
                            withContext(Dispatchers.Main) {
                                // NMEA satırlarını böl
                                text.split('\n', '\r').filter { it.isNotBlank() }.forEach { appendNmea(it.trim()) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        appendLog("Okuma hatası: ${e.message}")
                        _connected.value = false
                    }
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
                val outputStream = btManager.outputStream
                if (outputStream != null) {
                    outputStream.write(line.toByteArray())
                    outputStream.flush()
                    appendNmea("> $cmd")
                } else {
                    appendLog("Komut gönderilemedi: output stream yok")
                }
            } catch (e: Exception) { appendLog("Komut hatası: ${e.message}") }
        }
    }

    fun clearLog() { _nmea.value = emptyList() }

    override fun onCleared() {
        readerJob?.cancel(); streamJob?.cancel()
        try { btManager.disconnect() } catch (_: Exception) {}
        // ViewModel temizlenirken servisi de durdur (bağlantı kalmadı varsayımı)
        GnssService.stop(Tugis3Application.appContext)
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCommunicationScreen(vm: DeviceCommunicationViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
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

    val tabs = listOf("İletişim", "Gezici", "Sabit", "Statik", "Durum", "Ayarlar", "Cihaz Bilgi", "Doğruluk", "Sensör", "Radyo")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cihaz İletişim") },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.exportNmea(context) }) {
                        Icon(Icons.Default.FileDownload, "NMEA Dışa Aktar")
                    }
                    IconButton(onClick = { debugMode = !debugMode }) {
                        Icon(
                            Icons.Default.BugReport,
                            "Debug",
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
        ) {
            // Tab seçimi
            PrimaryScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab içeriği
            when (selectedTab) {
                0 -> CommunicationTab(vm, context, selectedManufacturer, selectedMode,
                    { selectedManufacturer = it }, { selectedMode = it },
                    connected, selected, nmea, command, { command = it },
                    permissionDenied, ::requestOrScan, debugMode)
                1 -> RoverSettingsTab(context)
                2 -> BaseSettingsTab(context)
                3 -> StaticSettingsTab(context)
                4 -> StatusTab(connected, nmea)
                5 -> DeviceSettingsTab(context)
                6 -> DeviceInfoTab()
                7 -> AccuracyInspectionTab()
                8 -> SensorCalibrationTab()
                9 -> RadioSettingsTab()
            }
        }
    }
}

@Composable
private fun CommunicationTab(
    vm: DeviceCommunicationViewModel,
    context: Context,
    selectedManufacturer: String,
    selectedMode: String,
    onManufacturerChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    connected: Boolean,
    selected: BTDeviceUi?,
    nmea: List<String>,
    command: String,
    onCommandChange: (String) -> Unit,
    permissionDenied: Boolean,
    requestOrScan: () -> Unit,
    debugMode: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Manufacturer & Model Selection
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Cihaz Markası", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    var manuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = manuExpanded,
                        onExpandedChange = { manuExpanded = !manuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedManufacturer,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Marka") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = manuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = manuExpanded, onDismissRequest = { manuExpanded = false }) {
                            listOf("SOUTH", "Trimble", "Leica", "Topcon", "CHC", "Hi-Target").forEach { manufacturer ->
                                DropdownMenuItem(
                                    text = { Text(manufacturer) },
                                    onClick = { onManufacturerChange(manufacturer); manuExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Communication Mode
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("İletişim Modu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            onClick = { onModeChange("Bluetooth") },
                            label = { Text("Bluetooth") },
                            selected = selectedMode == "Bluetooth",
                            leadingIcon = { Icon(Icons.Default.Bluetooth, null) }
                        )
                        FilterChip(
                            onClick = { onModeChange("WiFi") },
                            label = { Text("WiFi") },
                            selected = selectedMode == "WiFi",
                            leadingIcon = { Icon(Icons.Default.Wifi, null) }
                        )
                        FilterChip(
                            onClick = { onModeChange("USB") },
                            label = { Text("USB") },
                            selected = selectedMode == "USB",
                            leadingIcon = { Icon(Icons.Default.Usb, null) }
                        )
                    }
                }
            }
        }

        // Bluetooth Device List
        if (selectedMode == "Bluetooth") {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bluetooth Cihazlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (vm.isScanning) {
                                    OutlinedButton(onClick = { vm.stopScan(context) }) { Text("Durdur") }
                                }
                                Button(onClick = { requestOrScan() }, enabled = !vm.isScanning) {
                                    if (vm.isScanning) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text(if (vm.isScanning) "Taranıyor..." else "Tara")
                                }
                            }
                        }
                        if (permissionDenied) {
                            Text("Bluetooth izinleri gerekli", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (vm.devices.isEmpty() && !vm.isScanning) {
                            Text("Cihaz bulunamadı. Tarama başlatın.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            vm.devices.forEach { device ->
                                BluetoothDeviceCard(
                                    device = device,
                                    onConnect = { vm.selectDevice(device); vm.toggleConnection(context) },
                                    isConnected = connected && selected?.mac == device.mac,
                                    onSelectOnly = { vm.selectDevice(device) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        selected?.let { d ->
                            Text("Seçili: ${d.name} (${d.mac})", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.toggleConnection(context) }) {
                                    Text(if (connected) "Bağlantıyı Kes" else "Bağlan")
                                }
                                if (d.status != "Paired") {
                                    OutlinedButton(onClick = { vm.pairSelected() }) { Text("Eşleştir") }
                                }
                            }
                        }
                    }
                }
            }

            // NMEA Stream
            if (connected) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("NMEA Veri Akışı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { vm.clearLog() }) { Text("Temizle") }
                                    TextButton(onClick = { vm.exportNmea(context) }) { Text("Dışa Aktar") }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            ElevatedCard(Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                LazyColumn(reverseLayout = true, modifier = Modifier.padding(8.dp)) {
                                    items(nmea.reversed()) { line ->
                                        Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = command,
                                    onValueChange = onCommandChange,
                                    label = { Text("Komut Gönder") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { vm.sendCommand(command); onCommandChange("") },
                                    enabled = command.isNotBlank()
                                ) {
                                    Text("Gönder")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Debug Log
        if (debugMode) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Debug Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(vm.log, style = MaterialTheme.typography.bodySmall, modifier = Modifier.heightIn(max = 150.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoverSettingsTab(context: Context) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gezici (Rover) Ayarları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("GNSS alıcısını gezici mod için yapılandırın", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()

                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(context, RoverSettingsActivity::class.java))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gezici ayarları henüz hazır değil", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Router, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gezici Yapılandırma")
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "CORS ayarları gezici yapılandırma içinde bulunur", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Satellite, null)
                        Spacer(Modifier.width(8.dp))
                        Text("CORS Bağlantı Ayarları")
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "RTK radyo ayarları gezici yapılandırma içinde bulunur", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Wifi, null)
                        Spacer(Modifier.width(8.dp))
                        Text("RTK Radyo Ayarları")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Gezici Mod Hakkında", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Gezici mod, GNSS alıcısının düzeltme verisi alarak RTK ölçüm yapmasını sağlar.\n\n" +
                        "• CORS: İnternet üzerinden sanal referans istasyonu\n" +
                        "• RTK Radyo: Radyo modem ile sabit istasyondan düzeltme\n" +
                        "• Uydu yükseklik açısı: 10-15° önerilir\n" +
                        "• Ağ rölesi: Düzeltmeyi radyo ile paylaşma\n" +
                        "• Veri bağlantısı: NTRIP/Radio/Network\n" +
                        "• Gelişmiş ayarlar: Uydu sistemi, kesme açısı, diferansiyel veri formatı",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun BaseSettingsTab(context: Context) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sabit İstasyon Ayarları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("GNSS alıcısını sabit istasyon modu için yapılandırın", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()

                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(context, BaseSettingsActivity::class.java))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Sabit istasyon ayarları henüz hazır değil", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Router, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sabit İstasyon Yapılandırma")
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Radyo ayarları sabit yapılandırma içinde bulunur", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Radio, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Radyo Modem Ayarları")
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Anten ayarları sabit yapılandırma içinde bulunur", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Adjust, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Anten Yüksekliği Ayarları")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Sabit İstasyon Hakkında", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sabit istasyon, bilinen bir noktada RTK düzeltme verisi yayını yapar.\n\n" +
                        "• Başlangıç Modu: Bilinen nokta / Ortalama konum / Buraya Taşı\n" +
                        "• Radyo Frekansı: 430-470 MHz arası\n" +
                        "• Veri formatı: RTCM3.0/RTCM3.2/CMR/CMRx önerilir\n" +
                        "• Anten yüksekliği: Hassas ölçülmeli\n" +
                        "• Veri bağlantısı: Radio/Network/External Radio\n" +
                        "• Gelişmiş ayarlar: Mesaj tipi, yayın aralığı",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StaticSettingsTab(context: Context) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Statik Ölçüm Ayarları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Statik GNSS ölçümü için yapılandırma", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()

                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(context, StaticSettingsActivity::class.java))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Statik ayarlar henüz hazır değil", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Timer, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Statik Yapılandırma")
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Kayıt ayarları statik yapılandırma içinde bulunur", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Veri Kayıt Ayarları")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Statik Ölçüm Hakkında", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Statik GNSS ölçümü, yüksek hassasiyetli konum belirleme için kullanılır.\n\n" +
                        "• Kayıt aralığı: 1-30 saniye (varsayılan 15s)\n" +
                        "• Minimum süre: 20-60 dakika\n" +
                        "• Uydu sistemi: GPS+GLONASS+GALILEO+BEIDOU\n" +
                        "• Anten parametreleri: Tip ve yükseklik\n" +
                        "• Kesme açısı: 10-15° önerilir\n" +
                        "• Post-processing ile cm hassasiyet",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTab(connected: Boolean, nmea: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Çalışma Modu Durumu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bağlantı:")
                        Text(
                            if (connected) "Bağlı" else "Bağlı Değil",
                            color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Çalışma Modu:")
                        Text("Hazır", fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Veri Akışı:")
                        Text("${nmea.size} satır", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSettingsTab(context: Context) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cihaz Ayarları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HorizontalDivider()

                    OutlinedButton(
                        onClick = { /* Yapılandırma Profilleri */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Yapılandırma Profilleri")
                    }

                    OutlinedButton(
                        onClick = { /* Varsayılan Ayarlar */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Varsayılan Ayarlara Dön")
                    }

                    OutlinedButton(
                        onClick = { /* Cihaz Güncellemesi */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Update, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Firmware Güncelleme")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cihaz Bilgileri", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    InfoRow("Marka:", "SOUTH")
                    InfoRow("Model:", "ALPS2")
                    InfoRow("Seri No:", "E600351920028")
                    InfoRow("Firmware:", "v4.2.1")
                    InfoRow("Donanım:", "v2.0")
                    InfoRow("Anten Tipi:", "Helix")
                    InfoRow("IMU:", "Aktif")
                    InfoRow("Radyo:", "430-470 MHz")
                }
            }
        }
    }
}

@Composable
private fun AccuracyInspectionTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Doğruluk Kontrolü", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Eğim ölçümü ve direk kalibrasyonu", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()

                    Button(
                        onClick = { /* Eğim Doğruluğu Testi */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Straighten, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Eğim Ölçümü Doğruluğu Testi")
                    }

                    OutlinedButton(
                        onClick = { /* Direk Kalibrasyonu */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Height, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Direk Kalibrasyonu")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Kalibrasyon İpuçları", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Eğim ölçümü testi: 0-60° arası 4 yönde test\n" +
                        "• Direk kalibrasyonu: Düz zemin üzerinde yapılmalı\n" +
                        "• IMU kalibrasyonu sonrası kontrol önerilir\n" +
                        "• Her 6 ayda bir kontrol yapın",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorCalibrationTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sensör Kalibrasyonu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("IMU ve manyetik sensör kalibrasyonu", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()

                    Button(
                        onClick = { /* IMU Kalibrasyonu */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Explore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("IMU Sensör Kalibrasyonu")
                    }

                    OutlinedButton(
                        onClick = { /* Manyetik Tarama */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Explore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manyetik Alan Taraması")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Önemli Uyarı", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Kalibrasyon sırasında cihazı manyetik alanlardan uzak tutun\n" +
                        "• Metal objelerden ve elektrik hatlarından uzaklaşın\n" +
                        "• Kalibrasyonu açık alanda yapın\n" +
                        "• Ekran talimatlarını dikkatle takip edin",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioSettingsTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Radyo Ayarları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("İç radyo modem yapılandırması", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()

                    var frequency by remember { mutableStateOf("450.000") }
                    var power by remember { mutableStateOf("1W") }
                    var dataRate by remember { mutableStateOf("19200") }

                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frekans (MHz)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    var powerExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = powerExpanded,
                        onExpandedChange = { powerExpanded = !powerExpanded }
                    ) {
                        OutlinedTextField(
                            value = power,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Güç") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = powerExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = powerExpanded, onDismissRequest = { powerExpanded = false }) {
                            listOf("0.1W", "0.5W", "1W", "2W").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = { power = p; powerExpanded = false }
                                )
                            }
                        }
                    }

                    var dataRateExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dataRateExpanded,
                        onExpandedChange = { dataRateExpanded = !dataRateExpanded }
                    ) {
                        OutlinedTextField(
                            value = dataRate,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Veri Hızı (bps)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dataRateExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = dataRateExpanded, onDismissRequest = { dataRateExpanded = false }) {
                            listOf("4800", "9600", "19200", "38400").forEach { rate ->
                                DropdownMenuItem(
                                    text = { Text(rate) },
                                    onClick = { dataRate = rate; dataRateExpanded = false }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { /* Ayarları Uygula */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ayarları Uygula")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Radyo Ayarları Hakkında", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Frekans aralığı: 430-470 MHz\n" +
                        "• Sabit ve gezici aynı frekansta olmalı\n" +
                        "• Menzil: 1W için ~3km, 2W için ~5km\n" +
                        "• RTCM3.2 için 19200 bps önerilir",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = device.mac,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (device.status == "Paired") {
                        AssistChip(
                            onClick = { },
                            label = { Text("Eşleştirilmiş", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            if (isConnected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Bağlı",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(onClick = onConnect) {
                    Icon(Icons.Default.Link, "Bağlan")
                }
            }
        }
    }
}
