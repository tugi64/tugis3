package com.example.tugis3.ui.device

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// GNSS Cihaz Veri Modelleri
data class GnssManufacturer(
    val id: String,
    val name: String,
    val models: List<GnssModel>
)

data class GnssModel(
    val id: String,
    val name: String,
    val type: String, // RTK, DGPS, PPK
    val features: List<String>
)

@AndroidEntryPoint
class RoverSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                RoverSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoverSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }

    // Bluetooth permission check
    val hasBluetoothPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val isBluetoothConnected = remember {
        mutableStateOf(
            hasBluetoothPermission &&
            bluetoothAdapter?.isEnabled == true &&
            try {
                bluetoothAdapter.bondedDevices?.isNotEmpty() == true
            } catch (_: SecurityException) {
                false
            }
        )
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // GNSS Üreticileri ve Modelleri
    val manufacturers = remember { getGnssManufacturers() }
    var selectedManufacturer by remember { mutableStateOf<GnssManufacturer?>(null) }
    var selectedModel by remember { mutableStateOf<GnssModel?>(null) }

    // Rover Ayarları
    var cutOffAngle by remember { mutableStateOf("10") }
    var recordRawData by remember { mutableStateOf(true) }
    var enableARTK by remember { mutableStateOf(false) }
    var antennaHeight by remember { mutableStateOf("2.000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gezici (Rover) Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bluetooth Bağlantı Durumu
            item {
                BluetoothStatusCard(
                    isConnected = isBluetoothConnected.value,
                    onCheckConnection = {
                        val enabled = bluetoothAdapter?.isEnabled == true
                        val hasBonded = try {
                            hasBluetoothPermission && bluetoothAdapter?.bondedDevices?.isNotEmpty() == true
                        } catch (_: SecurityException) {
                            false
                        }
                        isBluetoothConnected.value = enabled && hasBonded

                        scope.launch {
                            if (isBluetoothConnected.value) {
                                snackbar.showSnackbar("✓ Bluetooth bağlı - RTK cihazı aktif")
                            } else {
                                snackbar.showSnackbar("⚠ RTK Bağlı Değil! Lütfen Bluetooth bağlantısı yapın")
                            }
                        }
                    }
                )
            }

            if (!isBluetoothConnected.value) {
                // Bluetooth Bağlı Değil Uyarısı
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Filled.BluetoothDisabled,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "RTK Cihazı Bağlı Değil",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Gezici modu kullanabilmek için önce RTK cihazını Bluetooth ile bağlayın",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        snackbar.showSnackbar("Bluetooth ayarlarına yönlendiriliyor...")
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Bluetooth, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Bluetooth Ayarları")
                            }
                        }
                    }
                }
            } else {
                // Bluetooth Bağlı - Ayarları Göster

                // GNSS Cihaz Seçimi
                item {
                    Text(
                        "GNSS Cihaz Seçimi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Üretici ve Model",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )

                            // Üretici Seçimi
                            manufacturers.forEach { manufacturer ->
                                ManufacturerItem(
                                    manufacturer = manufacturer,
                                    isSelected = selectedManufacturer?.id == manufacturer.id,
                                    selectedModel = if (selectedManufacturer?.id == manufacturer.id) selectedModel else null,
                                    onSelectManufacturer = {
                                        selectedManufacturer = manufacturer
                                    },
                                    onSelectModel = { model ->
                                        selectedModel = model
                                        scope.launch {
                                            snackbar.showSnackbar("${manufacturer.name} ${model.name} seçildi")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Seçili Cihaz Bilgisi
                if (selectedModel != null && selectedManufacturer != null) {
                    item {
                        SelectedDeviceCard(
                            manufacturer = selectedManufacturer!!,
                            model = selectedModel!!
                        )
                    }
                }

                // Rover Ayarları
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Rover Mod Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Cut-off Angle
                            OutlinedTextField(
                                value = cutOffAngle,
                                onValueChange = { cutOffAngle = it },
                                label = { Text("Cut-off Angle (derece)") },
                                leadingIcon = { Icon(Icons.Filled.Terrain, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = { Text("Minimum uydu yükseklik açısı") }
                            )

                            HorizontalDivider()

                            // Record Raw Data
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Ham Veri Kaydet", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Post-processing için ham veri",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = recordRawData,
                                    onCheckedChange = { recordRawData = it }
                                )
                            }

                            HorizontalDivider()

                            // Enable aRTK
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("aRTK Etkinleştir", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Düzeltme kaynağı kesildiğinde RTK",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = enableARTK,
                                    onCheckedChange = { enableARTK = it }
                                )
                            }

                            HorizontalDivider()

                            // Antenna Height
                            OutlinedTextField(
                                value = antennaHeight,
                                onValueChange = { antennaHeight = it },
                                label = { Text("Anten Yüksekliği (m)") },
                                leadingIcon = { Icon(Icons.Filled.Height, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = { Text("Yerden anten merkez noktasına") }
                            )
                        }
                    }
                }

                // Bağlan Butonu
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                if (selectedModel != null) {
                                    snackbar.showSnackbar(
                                        "${selectedManufacturer?.name} ${selectedModel?.name} ile bağlantı kuruluyor..."
                                    )
                                } else {
                                    snackbar.showSnackbar("Lütfen önce bir cihaz seçin")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedModel != null
                    ) {
                        Icon(Icons.Filled.Sync, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gezici Olarak Bağlan")
                    }
                }

                // Durum Bilgisi
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Column {
                                Text(
                                    "Rover Mod Bilgisi",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "Gezici mod, RTK düzeltmelerini alarak yüksek hassasiyetli konum sağlar. " +
                                    "Base station veya NTRIP bağlantısı gereklidir.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothStatusCard(
    isConnected: Boolean,
    onCheckConnection: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                    null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(
                        if (isConnected) "Bluetooth Bağlı" else "Bluetooth Bağlı Değil",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isConnected) "RTK cihazı kullanılabilir" else "RTK cihazı bağlanmamış",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onCheckConnection) {
                Icon(Icons.Filled.Refresh, "Yenile")
            }
        }
    }
}

@Composable
private fun ManufacturerItem(
    manufacturer: GnssManufacturer,
    isSelected: Boolean,
    selectedModel: GnssModel?,
    onSelectManufacturer: () -> Unit,
    onSelectModel: (GnssModel) -> Unit
) {
    var expanded by remember { mutableStateOf(isSelected) }

    Column {
        Surface(
            onClick = {
                expanded = !expanded
                if (!expanded) onSelectManufacturer()
            },
            color = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Satellite,
                        null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        manufacturer.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null
                )
            }
        }

        if (expanded) {
            manufacturer.models.forEach { model ->
                ModelItem(
                    model = model,
                    isSelected = selectedModel?.id == model.id,
                    onSelect = { onSelectModel(model) }
                )
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun ModelItem(
    model: GnssModel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        color = if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp)
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
                    model.name,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    model.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SelectedDeviceCard(
    manufacturer: GnssManufacturer,
    model: GnssModel
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        "Seçili Cihaz",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${manufacturer.name} ${model.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            Text(
                "Özellikler:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            model.features.forEach { feature ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Done,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        feature,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// GNSS Üreticileri ve Modelleri
private fun getGnssManufacturers(): List<GnssManufacturer> {
    return listOf(
        GnssManufacturer(
            id = "south",
            name = "South",
            models = listOf(
                GnssModel("south_alps2", "Alps2", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS")),
                GnssModel("south_g1", "Galaxy G1", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "IMU", "Tilt Compensation")),
                GnssModel("south_g2", "Galaxy G2", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("south_g3", "Galaxy G3", "RTK", listOf("GPS", "GLONASS", "BeiDou")),
                GnssModel("south_s82v", "S82V", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("south_s86t", "S86T", "RTK/PPK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "Tilt 60°")),
                GnssModel("south_s82", "S82", "RTK", listOf("GPS", "GLONASS", "BeiDou"))
            )
        ),
        GnssManufacturer(
            id = "trimble",
            name = "Trimble",
            models = listOf(
                GnssModel("trimble_r12", "R12", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS", "IMU")),
                GnssModel("trimble_r10", "R10", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("trimble_r8s", "R8s", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("trimble_r2", "R2", "RTK", listOf("GPS", "GLONASS"))
            )
        ),
        GnssManufacturer(
            id = "chcnav",
            name = "CHC Navigation",
            models = listOf(
                GnssModel("chc_i80", "i80", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "IRNSS", "QZSS", "IMU")),
                GnssModel("chc_i73", "i73", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("chc_i50", "i50", "RTK", listOf("GPS", "GLONASS", "BeiDou")),
                GnssModel("chc_x91", "X91+", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "SBAS"))
            )
        ),
        GnssManufacturer(
            id = "topcon",
            name = "Topcon",
            models = listOf(
                GnssModel("topcon_hiper_vr", "HiPer VR", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS")),
                GnssModel("topcon_hiper_v", "HiPer V", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("topcon_gr5", "GR-5", "RTK", listOf("GPS", "GLONASS", "BeiDou")),
                GnssModel("topcon_gr3", "GR-3", "RTK", listOf("GPS", "GLONASS"))
            )
        ),
        GnssManufacturer(
            id = "leica",
            name = "Leica Geosystems",
            models = listOf(
                GnssModel("leica_gs18t", "GS18 T", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS", "Tilt Compensation", "IMU")),
                GnssModel("leica_gs18i", "GS18 I", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "Visual Positioning")),
                GnssModel("leica_gs16", "GS16", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("leica_gs14", "GS14", "RTK", listOf("GPS", "GLONASS", "BeiDou"))
            )
        ),
        GnssManufacturer(
            id = "stonex",
            name = "Stonex",
            models = listOf(
                GnssModel("stonex_s900t", "S900T", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "Tilt")),
                GnssModel("stonex_s900a", "S900A", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("stonex_s850a", "S850A", "RTK", listOf("GPS", "GLONASS", "BeiDou"))
            )
        ),
        GnssManufacturer(
            id = "hi_target",
            name = "Hi-Target",
            models = listOf(
                GnssModel("hitarget_v200", "V200", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "IMU")),
                GnssModel("hitarget_v100", "V100", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("hitarget_v90", "V90 Plus", "RTK", listOf("GPS", "GLONASS", "BeiDou"))
            )
        ),
        GnssManufacturer(
            id = "kolida",
            name = "Kolida",
            models = listOf(
                GnssModel("kolida_k9t", "K9T", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "Tilt")),
                GnssModel("kolida_k5plus", "K5 Plus", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("kolida_k3", "K3", "RTK", listOf("GPS", "GLONASS", "BeiDou"))
            )
        ),
        GnssManufacturer(
            id = "hemisphere",
            name = "Hemisphere GNSS",
            models = listOf(
                GnssModel("hemisphere_s321", "S321", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "L-Band")),
                GnssModel("hemisphere_v320", "V320", "RTK", listOf("GPS", "GLONASS", "BeiDou")),
                GnssModel("hemisphere_v200", "V200", "RTK", listOf("GPS", "GLONASS"))
            )
        ),
        GnssManufacturer(
            id = "sokkia",
            name = "Sokkia",
            models = listOf(
                GnssModel("sokkia_gr5", "GRX3", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS")),
                GnssModel("sokkia_grx2", "GRX2", "RTK", listOf("GPS", "GLONASS", "BeiDou")),
                GnssModel("sokkia_gr1", "GR1", "RTK", listOf("GPS", "GLONASS"))
            )
        ),
        GnssManufacturer(
            id = "emlid",
            name = "Emlid",
            models = listOf(
                GnssModel("emlid_rs3", "Reach RS3", "RTK/PPK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS", "IMU")),
                GnssModel("emlid_rs2", "Reach RS2+", "RTK/PPK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS")),
                GnssModel("emlid_rs2", "Reach RS2", "RTK/PPK", listOf("GPS", "GLONASS", "BeiDou", "Galileo"))
            )
        ),
        GnssManufacturer(
            id = "septentrio",
            name = "Septentrio",
            models = listOf(
                GnssModel("sept_altus_nr3", "AsteRx SBi3", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo", "QZSS", "NavIC", "SBAS")),
                GnssModel("sept_altus_nr2", "Altus NR2", "RTK", listOf("GPS", "GLONASS", "BeiDou", "Galileo")),
                GnssModel("sept_altus_nr1", "Altus NR1", "RTK", listOf("GPS", "GLONASS", "BeiDou"))
            )
        )
    )
}

