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
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BluetoothSettingsActivityNew : ComponentActivity() {

    companion object {
        private const val TAG = "BluetoothSettings"
    }

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private var isScanning = mutableStateOf(false)
    private val discoveredDevices = mutableStateListOf<BluetoothDevice>()

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
                                    Log.d(TAG, "Cihaz bulundu: ${it.name ?: "İsimsiz"} - ${it.address}")
                                }
                            } catch (e: SecurityException) {
                                Log.e(TAG, "Cihaz bilgisi okunamadı: ${e.message}")
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    isScanning.value = true
                    Log.d(TAG, "Tarama başladı")
                    Toast.makeText(context, "🔍 Tarama başladı...", Toast.LENGTH_SHORT).show()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isScanning.value = false
                    val message = if (discoveredDevices.isEmpty()) {
                        "⚠️ Cihaz bulunamadı"
                    } else {
                        "✅ ${discoveredDevices.size} cihaz bulundu"
                    }
                    Log.d(TAG, "Tarama bitti: ${discoveredDevices.size} cihaz")
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "Bluetooth kapatıldı")
                            discoveredDevices.clear()
                            isScanning.value = false
                        }
                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG, "Bluetooth açıldı")
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                        val previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)

                        try {
                            val deviceName = it.name ?: it.address

                            when (bondState) {
                                BluetoothDevice.BOND_BONDING -> {
                                    Log.d(TAG, "Eşleştiriliyor: $deviceName")
                                    Toast.makeText(context, "🔄 Eşleştiriliyor: $deviceName", Toast.LENGTH_SHORT).show()
                                }
                                BluetoothDevice.BOND_BONDED -> {
                                    Log.d(TAG, "✅ Eşleştirildi: $deviceName")
                                    Toast.makeText(context, "✅ Bluetooth bağlandı!\n$deviceName ile eşleştirildi", Toast.LENGTH_LONG).show()
                                }
                                BluetoothDevice.BOND_NONE -> {
                                    if (previousBondState == BluetoothDevice.BOND_BONDING) {
                                        Log.d(TAG, "❌ Eşleştirme başarısız: $deviceName")
                                        Toast.makeText(context, "❌ Eşleştirme başarısız: $deviceName", Toast.LENGTH_LONG).show()
                                    } else {
                                        Log.d(TAG, "Eşleşme kaldırıldı: $deviceName")
                                        Toast.makeText(context, "Eşleşme kaldırıldı: $deviceName", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Bond state bilgisi okunamadı: ${e.message}")
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        try {
                            val deviceName = it.name ?: it.address
                            Log.d(TAG, "🔗 Cihaz bağlandı: $deviceName")
                            Toast.makeText(context, "🔗 Bağlandı: $deviceName", Toast.LENGTH_LONG).show()
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Bağlantı bilgisi okunamadı: ${e.message}")
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        try {
                            val deviceName = it.name ?: it.address
                            Log.d(TAG, "🔌 Bağlantı kesildi: $deviceName")
                            Toast.makeText(context, "🔌 Bağlantı kesildi: $deviceName", Toast.LENGTH_SHORT).show()
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Bağlantı kesme bilgisi okunamadı: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Bluetooth etkinleştirildi")
            Toast.makeText(this, "✅ Bluetooth açıldı", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Bluetooth açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "Tüm izinler verildi")
            Toast.makeText(this, "✅ İzinler verildi", Toast.LENGTH_SHORT).show()

            // İzinler verildiyse konum kontrolü yap
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                checkLocationEnabled()
            }
        } else {
            val denied = permissions.entries.filter { !it.value }.map { it.key }
            Log.w(TAG, "Reddedilen izinler: $denied")
            Toast.makeText(this, "⚠️ Bluetooth için izinler gerekli!\n${denied.joinToString()}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - Android SDK: ${Build.VERSION.SDK_INT}")

        // Bluetooth Receiver kaydet
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)

        // İzinleri kontrol et
        requestBluetoothPermissions()

        setContent {
            Tugis3Theme {
                BluetoothScreenNew(
                    onBack = { finish() },
                    onToggleBluetooth = { enabled -> toggleBluetooth(enabled) },
                    onScanDevices = { startDiscovery() },
                    onStopScan = { stopDiscovery() },
                    onPairDevice = { device -> pairDevice(device) },
                    onConnectDevice = { device -> connectDevice(device) },
                    getPairedDevices = { getPairedDevices() },
                    discoveredDevices = discoveredDevices,
                    bluetoothAdapter = bluetoothAdapter,
                    isScanning = isScanning.value
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopDiscovery()
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy error: ${e.message}")
        }
    }

    // İzin Kontrolleri
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 ve altı (API 30-)
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // Android 11 ve altı (API 30-)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            Log.d(TAG, "İzin isteniyor: $permissions")
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            Log.d(TAG, "Tüm izinler zaten var")
            // Android 11 altıysa konum açık mı kontrol et
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                checkLocationEnabled()
            }
        }
    }

    private fun checkLocationEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isLocationEnabled) {
            // Sadece bilgilendirme yap, uygulama içinde kal
            Toast.makeText(
                this,
                "⚠️ Bluetooth taraması için konum servisini açmanız gerekebilir\n(Android 11 ve altı için gerekli)",
                Toast.LENGTH_LONG
            ).show()

            Log.w(TAG, "Konum servisi kapalı - Android ${Build.VERSION.SDK_INT} için tarama etkilenebilir")

            // NOT: Konum ayarlarına yönlendirme yapMIyoruz - SurvX gibi uygulamada kalıyoruz
            // Kullanıcı gerekirse kendi gidip açacak
        }
    }


    // Bluetooth İşlemleri
    private fun toggleBluetooth(enable: Boolean) {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "❌ Bu cihaz Bluetooth desteklemiyor", Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        try {
            if (enable && bluetoothAdapter?.isEnabled == false) {
                // Android 13+ (API 33+) için sessizce açma artık mümkün değil
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Kullanıcıdan izin iste ama uygulama içinde kal
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                } else {
                    // Android 12 ve altı - direkt aç
                    @Suppress("DEPRECATION")
                    val success = bluetoothAdapter?.enable()
                    if (success == true) {
                        Toast.makeText(this, "✅ Bluetooth açılıyor...", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Bluetooth enable() çağrıldı")
                    } else {
                        Toast.makeText(this, "❌ Bluetooth açılamadı", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (!enable && bluetoothAdapter?.isEnabled == true) {
                // Bluetooth'u kapat
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ - Direkt kapatma artık mümkün değil
                    Toast.makeText(this, "ℹ️ Bluetooth'u kapatmak için sistem ayarlarını kullanın", Toast.LENGTH_LONG).show()
                } else {
                    // Android 12 ve altı - direkt kapat
                    @Suppress("DEPRECATION")
                    val success = bluetoothAdapter?.disable()
                    if (success == true) {
                        Toast.makeText(this, "Bluetooth kapatılıyor...", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Bluetooth disable() çağrıldı")
                    } else {
                        Toast.makeText(this, "❌ Bluetooth kapatılamadı", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (enable && bluetoothAdapter?.isEnabled == true) {
                Toast.makeText(this, "ℹ️ Bluetooth zaten açık", Toast.LENGTH_SHORT).show()
            } else if (!enable && bluetoothAdapter?.isEnabled == false) {
                Toast.makeText(this, "ℹ️ Bluetooth zaten kapalı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "toggleBluetooth SecurityException: ${e.message}")
            requestBluetoothPermissions()
        }
    }

    private fun startDiscovery() {
        Log.d(TAG, "startDiscovery() çağrıldı")

        // 1. Bluetooth adapter var mı?
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "❌ Bluetooth desteklenmiyor", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Bluetooth açık mı?
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "⚠️ Önce Bluetooth'u açın!", Toast.LENGTH_LONG).show()
            return
        }

        // 3. İzinler var mı?
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this, "⚠️ Bluetooth izinleri gerekli!", Toast.LENGTH_LONG).show()
            requestBluetoothPermissions()
            return
        }

        // 4. Android 11 ve altıysa konum açık mı? (Sadece uyar, engelleme)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isLocationEnabled) {
                // Sadece uyar, ama taramaya devam et (SurvX gibi)
                Toast.makeText(
                    this,
                    "⚠️ Konum kapalı - Bazı cihazlar görünmeyebilir\n(Android ${Build.VERSION.SDK_INT})",
                    Toast.LENGTH_LONG
                ).show()
                Log.w(TAG, "Konum kapalı ama taramaya devam ediyoruz")
                // return yapmıyoruz - taramaya devam
            }
        }

        // 5. Taramayı başlat
        try {
            discoveredDevices.clear()

            // Önceki tarama devam ediyorsa durdur
            if (bluetoothAdapter?.isDiscovering == true) {
                Log.d(TAG, "Önceki tarama durduruluyor...")
                bluetoothAdapter?.cancelDiscovery()
                Thread.sleep(500) // Biraz bekle
            }

            Log.d(TAG, "Tarama başlatılıyor...")
            val started = bluetoothAdapter?.startDiscovery()

            if (started == true) {
                isScanning.value = true
                Log.d(TAG, "✅ Tarama başarıyla başlatıldı")
            } else {
                Log.e(TAG, "❌ startDiscovery() false döndü")
                Toast.makeText(this, "❌ Tarama başlatılamadı. Bluetooth'u kapatıp açmayı deneyin.", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "startDiscovery SecurityException: ${e.message}")
            Toast.makeText(this, "❌ İzin hatası: ${e.message}", Toast.LENGTH_LONG).show()
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "startDiscovery Exception: ${e.message}", e)
            Toast.makeText(this, "❌ Hata: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
                isScanning.value = false
                Log.d(TAG, "Tarama durduruldu")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "stopDiscovery error: ${e.message}")
        }
    }

    private fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermissions()) {
            return emptyList()
        }

        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "getPairedDevices error: ${e.message}")
            emptyList()
        }
    }

    private fun pairDevice(device: BluetoothDevice) {
        try {
            if (!hasBluetoothPermissions()) {
                Toast.makeText(this, "⚠️ Bluetooth izinleri gerekli!", Toast.LENGTH_LONG).show()
                requestBluetoothPermissions()
                return
            }

            // Taramayı durdur
            stopDiscovery()

            val deviceName = device.name ?: device.address

            // Zaten eşleşmiş mi kontrol et
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(this, "ℹ️ $deviceName zaten eşleştirilmiş!", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Eşleştirme başlatılıyor: $deviceName")

            // createBond() metodunu çağır
            val success = device.createBond()

            if (success) {
                Toast.makeText(this, "🔄 Eşleştirme başlatıldı: $deviceName\nCihazda onay gerekebilir...", Toast.LENGTH_LONG).show()
                Log.d(TAG, "✅ createBond() başarılı: $deviceName")
            } else {
                Toast.makeText(this, "❌ Eşleştirme başlatılamadı: $deviceName", Toast.LENGTH_LONG).show()
                Log.e(TAG, "❌ createBond() false döndü: $deviceName")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "pairDevice SecurityException: ${e.message}")
            Toast.makeText(this, "❌ İzin hatası: ${e.message}", Toast.LENGTH_LONG).show()
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "pairDevice Exception: ${e.message}", e)
            Toast.makeText(this, "❌ Eşleştirme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectDevice(device: BluetoothDevice) {
        try {
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
                return
            }

            val deviceName = device.name ?: device.address
            Toast.makeText(this, "🔄 Bağlanıyor: $deviceName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Bağlantı başlatılıyor: $deviceName [${device.address}]")

            // Arka planda bağlan (BluetoothGnssManager birden fazla UUID deneyecek)
            Thread {
                try {
                    val gnssManager = com.example.tugis3.bluetooth.BluetoothGnssManager(this)

                    // NMEA data listener ekle
                    gnssManager.setNmeaDataListener(object : com.example.tugis3.bluetooth.BluetoothGnssManager.NmeaDataListener {
                        override fun onNmeaReceived(nmea: String) {
                            runOnUiThread {
                                Log.d(TAG, "NMEA: $nmea")
                                // TODO: NMEA verisini GnssService'e gönder
                            }
                        }

                        override fun onConnectionLost() {
                            runOnUiThread {
                                Toast.makeText(this@BluetoothSettingsActivityNew, "❌ Bağlantı kesildi", Toast.LENGTH_LONG).show()
                                Log.w(TAG, "Bluetooth bağlantısı kesildi")
                            }
                        }
                    })

                    // Bağlan (birden fazla UUID deneyecek)
                    gnssManager.connectToDevice(device)

                    runOnUiThread {
                        Toast.makeText(this@BluetoothSettingsActivityNew, "✅ $deviceName bağlandı!\n📡 NMEA verileri alınıyor...", Toast.LENGTH_LONG).show()
                        Log.i(TAG, "✅ Cihaz bağlandı: $deviceName")
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        val errorMsg = when {
                            e.message?.contains("socket") == true -> "Socket hatası"
                            e.message?.contains("refused") == true -> "Bağlantı reddedildi"
                            e.message?.contains("timeout") == true -> "Zaman aşımı"
                            else -> e.message ?: "Bilinmeyen hata"
                        }
                        Toast.makeText(this@BluetoothSettingsActivityNew, "❌ Bağlantı hatası: $errorMsg", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Bağlantı hatası: $deviceName", e)
                    }
                }
            }.start()

        } catch (e: SecurityException) {
            Toast.makeText(this, "❌ İzin hatası", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "connectDevice SecurityException: ${e.message}")
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "connectDevice Exception: ${e.message}", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreenNew(
    onBack: () -> Unit,
    onToggleBluetooth: (Boolean) -> Unit,
    onScanDevices: () -> Unit,
    onStopScan: () -> Unit,
    onPairDevice: (BluetoothDevice) -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    getPairedDevices: () -> List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    bluetoothAdapter: BluetoothAdapter?,
    isScanning: Boolean
) {
    val isBluetoothEnabled = remember(bluetoothAdapter) {
        derivedStateOf { bluetoothAdapter?.isEnabled == true }
    }

    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    LaunchedEffect(isBluetoothEnabled.value) {
        if (isBluetoothEnabled.value) {
            pairedDevices = getPairedDevices()
        } else {
            pairedDevices = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Bluetooth Durumu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBluetoothEnabled.value)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isBluetoothEnabled.value) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bluetooth",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBluetoothEnabled.value) "Açık" else "Kapalı",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Switch(
                        checked = isBluetoothEnabled.value,
                        onCheckedChange = onToggleBluetooth
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarama Butonu
            if (isBluetoothEnabled.value) {
                Button(
                    onClick = { if (isScanning) onStopScan() else onScanDevices() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isScanning) Icons.Default.Cancel else Icons.Default.Search,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isScanning) "Taramayı Durdur" else "Yeni Cihaz Tara")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarama Durumu
                if (isScanning) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "🔍 Cihazlar taranıyor...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bulunan Cihazlar
                if (discoveredDevices.isNotEmpty()) {
                    Text(
                        "Bulunan Cihazlar (${discoveredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(discoveredDevices) { device ->
                            DeviceCard(
                                device = device,
                                onPairClick = { onPairDevice(device) }
                            )
                        }
                    }
                } else if (isScanning) {
                    Text(
                        "Henüz cihaz bulunamadı...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Eşleşmiş Cihazlar
                if (pairedDevices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Eşleşmiş Cihazlar (${pairedDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(pairedDevices) { device ->
                            PairedDeviceCard(
                                device = device,
                                onConnectClick = { onConnectDevice(device) }
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Bluetooth kapalı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Cihazları taramak için Bluetooth'u açın",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: BluetoothDevice,
    onPairClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = try {
                        device.name ?: "İsimsiz Cihaz"
                    } catch (e: SecurityException) {
                        "İsimsiz Cihaz"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onPairClick) {
                Text("Eşleştir")
            }
        }
    }
}

@Composable
fun PairedDeviceCard(
    device: BluetoothDevice,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onConnectClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = try {
                            device.name ?: "İsimsiz Cihaz"
                        } catch (e: SecurityException) {
                            "İsimsiz Cihaz"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

