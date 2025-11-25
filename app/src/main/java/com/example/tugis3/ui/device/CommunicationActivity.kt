package com.example.tugis3.ui.device

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommunicationActivity : ComponentActivity() {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }

    private val discoveredDevices = mutableStateListOf<BluetoothDevice>()
    private val isScanning = mutableStateOf(false)

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (hasBluetoothPermissions()) {
                            try {
                                if (!discoveredDevices.any { d -> d.address == it.address }) {
                                    discoveredDevices.add(it)
                                    Log.d("CommunicationActivity", "Cihaz bulundu: ${it.name ?: "İsimsiz"}")
                                }
                            } catch (_: SecurityException) {
                                Log.e("CommunicationActivity", "Cihaz bilgisi okunamadı")
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    isScanning.value = true
                    Toast.makeText(context, "🔍 Tarama başladı...", Toast.LENGTH_SHORT).show()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isScanning.value = false
                    Toast.makeText(context, "✅ Tarama tamamlandı (${discoveredDevices.size} cihaz)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bluetooth receiver kaydet
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(bluetoothReceiver, filter)

        setContent {
            Tugis3Theme {
                CommunicationScreen(
                    onBack = { finish() },
                    onScanDevices = { startBluetoothScan() },
                    discoveredDevices = discoveredDevices,
                    isScanning = isScanning.value,
                    onConnectDevice = { device -> connectToDevice(device) },
                    bluetoothAdapter = bluetoothAdapter
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (hasBluetoothPermissions()) {
                try {
                    bluetoothAdapter?.cancelDiscovery()
                } catch (_: SecurityException) {
                    // İzin sorunu
                }
            }
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e("CommunicationActivity", "onDestroy error: ${e.message}")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        ActivityCompat.requestPermissions(this, permissions, 1001)
    }

    private fun startBluetoothScan() {
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this, "⚠️ Bluetooth izinleri gerekli!", Toast.LENGTH_LONG).show()
            requestBluetoothPermissions()
            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "⚠️ Önce Bluetooth'u açın!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            discoveredDevices.clear()
            bluetoothAdapter?.cancelDiscovery()

            val started = bluetoothAdapter?.startDiscovery() ?: false
            if (!started) {
                Toast.makeText(this, "❌ Tarama başlatılamadı!", Toast.LENGTH_SHORT).show()
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, "❌ İzin hatası!", Toast.LENGTH_LONG).show()
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            val deviceName = device.name ?: device.address
            Toast.makeText(this, "🔄 $deviceName'e bağlanılıyor...", Toast.LENGTH_SHORT).show()

            // BluetoothGnssManager ile bağlan
            Thread {
                try {
                    val gnssManager = com.example.tugis3.bluetooth.BluetoothGnssManager(this)

                    // NMEA data listener ekle - GNSS verilerini GnssPositionRepository'ye gönder
                    gnssManager.setNmeaDataListener(object : com.example.tugis3.bluetooth.BluetoothGnssManager.NmeaDataListener {
                        override fun onNmeaReceived(nmea: String) {
                            // NMEA cümlesini parse et
                            try {
                                val parser = com.example.tugis3.gnss.nmea.NmeaParser()
                                val parsed = parser.parse(nmea)

                                if (parsed != null) {
                                    // GnssPositionRepository'yi güncelle
                                    com.example.tugis3.gnss.GnssPositionRepository.update(
                                        lat = parsed.latDeg,
                                        lon = parsed.lonDeg,
                                        alt = parsed.heightEllipsoidal,
                                        hdop = parsed.hdop,
                                        vdop = parsed.vdop,
                                        pdop = parsed.pdop,
                                        satsUse = parsed.satellitesInUse,
                                        satsVis = parsed.satellitesVisible,
                                        fix = parsed.fixType,
                                        raw = nmea
                                    )

                                    Log.d("CommunicationActivity", "GNSS data updated: sats=${parsed.satellitesInUse}, fix=${parsed.fixType}")
                                }
                            } catch (e: Exception) {
                                Log.e("CommunicationActivity", "NMEA parse error: ${e.message}")
                            }
                        }

                        override fun onConnectionLost() {
                            runOnUiThread {
                                Toast.makeText(this@CommunicationActivity, "❌ GNSS bağlantısı koptu!", Toast.LENGTH_LONG).show()
                            }
                        }
                    })

                    // Cihaza bağlan
                    gnssManager.connectToDevice(device)

                    runOnUiThread {
                        Toast.makeText(this, "✅ $deviceName bağlandı! GNSS verileri alınıyor...", Toast.LENGTH_LONG).show()

                        // GnssService'i başlat - Bildirim gösterir ve veri akışını takip eder
                        com.example.tugis3.service.GnssService.start(this)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "❌ Bağlantı hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()

        } catch (_: SecurityException) {
            Toast.makeText(this, "❌ İzin hatası", Toast.LENGTH_SHORT).show()
            requestBluetoothPermissions()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunicationScreen(
    onBack: () -> Unit,
    onScanDevices: () -> Unit,
    discoveredDevices: List<BluetoothDevice>,
    isScanning: Boolean,
    onConnectDevice: (BluetoothDevice) -> Unit,
    bluetoothAdapter: BluetoothAdapter?
) {
    var connectionType by remember { mutableStateOf("Bluetooth") }
    var isConnected by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("") }
    var baudRate by remember { mutableStateOf("115200") }
    var showDeviceDialog by remember { mutableStateOf(false) }

    val connectionTypes = listOf("Bluetooth", "USB", "Serial", "TCP/IP", "Wi-Fi", "NTRIP/CORS")
    val baudRates = listOf("9600", "19200", "38400", "57600", "115200", "230400")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cihaz İletişimi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                                text = "Bağlantı Durumu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isConnected) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (isConnected) "Bağlı" else "Bağlantısız",
                                    color = if (isConnected) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isConnected && deviceName.isNotEmpty()) {
                            Text(
                                text = "Bağlı Cihaz: $deviceName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (connectionType == "Bluetooth") {
                                        showDeviceDialog = true
                                        onScanDevices()
                                    } else {
                                        isConnected = !isConnected
                                        if (isConnected) deviceName = "GNSS Receiver X1"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = bluetoothAdapter?.isEnabled == true || connectionType != "Bluetooth"
                            ) {
                                Icon(
                                    if (isScanning) Icons.Default.Refresh else Icons.Default.Search,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isScanning) "Taranıyor..." else "Tara")
                            }

                            if (isConnected) {
                                OutlinedButton(
                                    onClick = {
                                        isConnected = false
                                        deviceName = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Bağlantıyı Kes")
                                }
                            }
                        }
                    }
                }
            }

            // Bluetooth Ayarları
            if (connectionType == "Bluetooth") {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Bluetooth Ayarları",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bluetooth Durumu:")
                                Text(
                                    text = if (bluetoothAdapter?.isEnabled == true) "Açık ✓" else "Kapalı ✗",
                                    color = if (bluetoothAdapter?.isEnabled == true)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (discoveredDevices.isNotEmpty()) {
                                Text(
                                    text = "${discoveredDevices.size} cihaz bulundu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Bağlantı Ayarları
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Bağlantı Ayarları",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Bağlantı Türü
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = connectionType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bağlantı Türü") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                connectionTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            connectionType = type
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Baud Rate
                        var baudExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = baudExpanded,
                            onExpandedChange = { baudExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = baudRate,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Baud Rate") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = baudExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = baudExpanded,
                                onDismissRequest = { baudExpanded = false }
                            ) {
                                baudRates.forEach { rate ->
                                    DropdownMenuItem(
                                        text = { Text(rate) },
                                        onClick = {
                                            baudRate = rate
                                            baudExpanded = false
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

    // Bluetooth Cihaz Seçim Dialog'u
    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Bluetooth Cihazları")
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            text = {
                Column {
                    when {
                        discoveredDevices.isEmpty() && !isScanning -> {
                            Text(
                                "Hiç cihaz bulunamadı. 'Tara' butonuna basın.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        discoveredDevices.isEmpty() -> {
                            Text(
                                "Taranıyor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 400.dp)
                            ) {
                                items(discoveredDevices) { device ->
                                    val deviceNameSafe = try { device.name } catch (_: SecurityException) { null }
                                    val deviceAddress = device.address

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                deviceName = deviceNameSafe ?: "İsimsiz Cihaz"
                                                isConnected = true
                                                showDeviceDialog = false
                                                onConnectDevice(device)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = deviceNameSafe ?: "İsimsiz Cihaz",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = deviceAddress,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("Kapat")
                }
            },
            dismissButton = {
                TextButton(onClick = { onScanDevices() }) {
                    Text("Yeniden Tara")
                }
            }
        )
    }
}

