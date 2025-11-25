@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.tugis3.ui.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.tugis3.gnss.GnssPositionRepository
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Proje aktiviteleri
import com.example.tugis3.ui.project.*
// Cihaz aktiviteleri
import com.example.tugis3.ui.device.*
// Survey aktiviteleri
import com.example.tugis3.ui.survey.*
// CAD
import com.example.tugis3.ui.cad.CadApplicationActivity
// Tools aktiviteleri
import com.example.tugis3.ui.tools.*
// Settings aktiviteleri
import com.example.tugis3.ui.settings.CrosshairSettingsActivity as SettingsCrosshairActivity
// Analysis aktiviteleri
import com.example.tugis3.ui.analysis.MeasurementAnalysisActivity as AnalysisMeasurementActivity
// Ortak
import com.example.tugis3.ui.common.PlaceholderActivity
import com.example.tugis3.util.crash.CrashLogger
import com.example.tugis3.util.crash.CrashLogActivity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // updated import
// Eksik özel paket aktiviteleri
import com.example.tugis3.gnss.ui.GnssMonitorActivity
import com.example.tugis3.ntrip.NtripProfilesActivity

// Merkezi route sabitleri (ileride deeplink vb. için tek noktadan yönetim)
private object NavRoutes {
    const val PROJECT = "project"
    const val DEVICE = "device"
    const val SURVEY = "survey"
    const val TOOLS = "tools"
}

// MenuItem data class for menu items
data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val action: String
)

// NavigationItem data class for bottom navigation
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainNavigationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crash logger'ı background thread'de çalıştır
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val ts = CrashLogger.consumePendingCrash(this@MainNavigationActivity)
                if (ts != null) {
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@MainNavigationActivity, CrashLogActivity::class.java))
                    }
                }
            }
        }

        try {
            setContent {
                Tugis3Theme {
                    RootErrorHost {
                        AppRoot()
                    }
                }
            }
        } catch (t: Throwable) {
            runCatching { CrashLogger.logException(this, t) }
            setContent {
                Tugis3Theme {
                    FatalErrorScreen(error = t, onShowCrashLog = {
                        runCatching { startActivity(Intent(this, CrashLogActivity::class.java)) }
                    })
                }
            }
        }
    }
}

// --- Top-level UI Error Propagation ---
interface UiErrorReporter { fun report(t: Throwable) }
private val LocalUiErrorReporter = staticCompositionLocalOf<UiErrorReporter?> { null }

@Composable
fun rememberUiErrorReporter(): UiErrorReporter = LocalUiErrorReporter.current ?: remember {
    object : UiErrorReporter { override fun report(t: Throwable) { /* no-op (out of host) */ } }
}

@Composable
private fun RootErrorHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val errorState = remember { mutableStateOf<Throwable?>(null) }
    val reporter = remember {
        object : UiErrorReporter {
            override fun report(t: Throwable) {
                if (errorState.value == null) errorState.value = t
            }
        }
    }
    // Log errors when they appear
    LaunchedEffect(Unit) {
        snapshotFlow { errorState.value }.collect { err ->
            if (err != null) runCatching { CrashLogger.logException(context, err) }
        }
    }
    if (errorState.value != null) {
        FatalErrorScreen(
            error = errorState.value!!,
            onShowCrashLog = {
                runCatching { context.startActivity(Intent(context, CrashLogActivity::class.java)) }
            },
            onRetry = { errorState.value = null }
        )
    } else {
        CompositionLocalProvider(LocalUiErrorReporter provides reporter) {
            content()
        }
    }
}

@Composable
private fun ErrorBoundary(content: @Composable () -> Unit) { content() }

@Composable
private fun FatalErrorScreen(
    error: Throwable,
    onShowCrashLog: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Kritik Hata") }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ana ekran yüklenirken bir hata oluştu.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = (error.message ?: error::class.qualifiedName ?: "Bilinmeyen hata"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Uygulama kapanmadı; Crash Log ekranını açarak detayları inceleyebilirsiniz.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onShowCrashLog) { Text("Crash Log") }
                if (onRetry != null) {
                    OutlinedButton(onClick = onRetry) { Text("Tekrar Dene") }
                }
            }
        }
    }
}

@Composable
private fun AppRoot() { // showMain varsayılanı artık true
    var showMain by rememberSaveable { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    if (errorMessage != null) {
        Scaffold { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Hata: " + errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }
    LaunchedEffect(showMain) {
        try { /* gelecekte bootstrap kontrolleri */ } catch (e: Exception) { errorMessage = e.message ?: "Bilinmeyen hata" }
    }
    if (showMain) MainNavigationScreen() else SafeLanding(onOpenMain = { showMain = true })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SafeLanding(onOpenMain: () -> Unit) {
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("TUGIS 3") }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Uygulama güvenli modda açıldı. Ana ekranı başlatabilir veya Crash Log’a gidebilirsiniz.")
            Button(onClick = onOpenMain) { Text("Ana Ekranı Aç") }
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(context, CrashLogActivity::class.java)) }
            }) { Text("Crash Log’u Aç") }
        }
    }
}

@Composable
fun MainNavigationScreen() {
    val navController = rememberNavController()
    val navItems = listOf(
        NavigationItem("Proje", Icons.Outlined.Folder, NavRoutes.PROJECT),
        NavigationItem("Cihaz", Icons.Outlined.PhoneAndroid, NavRoutes.DEVICE),
        NavigationItem("Ölçüm", Icons.Outlined.LocationOn, NavRoutes.SURVEY),
        NavigationItem("Araçlar", Icons.Outlined.Build, NavRoutes.TOOLS)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tugis",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        // Sağ üst durum göstergeleri
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Uydu durumu
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Satellite,
                                    contentDescription = "Uydu",
                                    tint = androidx.compose.ui.graphics.Color.Green,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("12", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Green)
                            }
                            // Wi-Fi durumu
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = "Wi-Fi",
                                tint = androidx.compose.ui.graphics.Color.Blue,
                                modifier = Modifier.size(16.dp)
                            )
                            // Batarya durumu
                            Icon(
                                Icons.Default.BatteryFull,
                                contentDescription = "Batarya",
                                tint = androidx.compose.ui.graphics.Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                            // Gerçek zamanlı saat
                            RealTimeClock()
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.PROJECT,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.PROJECT) { ProjectMainScreen() }
            composable(NavRoutes.DEVICE) { DeviceMainScreen() }
            composable(NavRoutes.SURVEY) { SurveyMainScreen() }
            composable(NavRoutes.TOOLS) { ToolsMainScreen() }
        }
    }
}

// Context extension fonksiyonları
fun Context.safeStart(target: Class<*>, title: String? = null) {
    try {
        val intent = Intent(this, target)
        if (title != null) intent.putExtra(PlaceholderActivity.EXTRA_TITLE, title)
        startActivity(intent)
    } catch (e: Exception) {
        runCatching { CrashLogger.logException(this, e) }
        val pi = Intent(this, PlaceholderActivity::class.java)
        pi.putExtra(PlaceholderActivity.EXTRA_TITLE, title ?: target.simpleName)
        pi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(pi)
    }
}

// Proje ana ekranı - Resimde sol üst panel - güncellenmiş
@Composable
private fun ProjectMainScreen() {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(getProjectMenuItems()) { item ->
            MenuCard(
                title = item.title,
                icon = item.icon,
                color = item.color,
                onClick = {
                    when (item.action) {
                        "project_manager" -> context.safeStart(ProjectManagerActivity::class.java, "Proje Yöneticisi")
                        "coordinate_system" -> context.safeStart(CoordinateSystemActivity::class.java, "Koordinat Sistemi")
                        "point_database" -> context.safeStart(PointDatabaseActivity::class.java, "Nokta Veritabanı")
                        "project_info" -> context.safeStart(PlaceholderActivity::class.java, "Proje Bilgisi")
                        "survey_management" -> context.safeStart(FileManagerActivity::class.java, "Dosya Yönetimi")
                        "calibration" -> context.safeStart(CalibrationActivity::class.java, "Nokta Kalibrasyonu")
                        "send_data" -> context.safeStart(PlaceholderActivity::class.java, "Veri Gönder")
                        "barcode" -> context.safeStart(PlaceholderActivity::class.java, "QR Kod")
                        "cloud_settings" -> context.safeStart(CloudSettingsActivity::class.java, "Bulut Ayarları")
                        "software_settings" -> context.safeStart(SoftwareSettingsActivity::class.java, "Yazılım Ayarları")
                        else -> Toast.makeText(context, "${item.title} özelliği", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// Cihaz ana ekranı - Resimde sol alt panel - güncellenmiş
@Composable
private fun DeviceMainScreen() {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(getDeviceMenuItems()) { item ->
            MenuCard(
                title = item.title,
                icon = item.icon,
                color = item.color,
                onClick = {
                    when (item.action) {
                        "communication" -> context.safeStart(com.example.tugis3.ui.device.CommunicationActivity::class.java, "İletişim")
                        "bluetooth" -> context.safeStart(com.example.tugis3.ui.device.BluetoothSettingsActivityNew::class.java, "Bluetooth Ayarları")
                        "base_station" -> context.safeStart(BaseStationActivity::class.java, "Base Station")
                        "rover_station" -> context.safeStart(RoverSettingsActivity::class.java, "Gezici (Rover)")
                        "measuring_device" -> context.safeStart(MeasuringDeviceActivity::class.java, "Ölçü Cihazı")
                        "fixed" -> context.safeStart(FixedPointActivity::class.java, "Sabit")
                        "static_status" -> context.safeStart(StaticStatusActivity::class.java, "Statik Durum")
                        "current_location" -> context.safeStart(CurrentLocationActivity::class.java, "Mevcut Konum")
                        "quick_module" -> context.safeStart(QuickModuleActivity::class.java, "Hızlı Modül")
                        "device_info" -> context.safeStart(DeviceInfoActivity::class.java, "Cihaz Bilgisi")
                        "sensor_calibration" -> context.safeStart(SensorCalibrationActivity::class.java, "Sensör Kalibre")
                        else -> Toast.makeText(context, "${item.title} özelliği", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// Ölçüm ana ekranı - Resimde sağ üst panel - güncellenmiş
@Composable
private fun SurveyMainScreen() {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(getSurveyMenuItems()) { item ->
            MenuCard(
                title = item.title,
                icon = item.icon,
                color = item.color,
                onClick = {
                    when (item.action) {
                        "graphic_survey" -> context.safeStart(GraphicalSurveyActivity::class.java, "Grafik Alım")
                        "quick_survey" -> context.safeStart(QuickPointSurveyActivity::class.java, "Hızlı Alım")
                        "cad_application" -> context.safeStart(CadApplicationActivity::class.java, "Cad Aplikasyon")
                        "point_application" -> context.safeStart(PointApplicationActivity::class.java, "Nokta Aplikasyon")
                        "line_application" -> context.safeStart(LineStakeoutActivity::class.java, "Hat Aplikasyon")
                        "precision_plan_application" -> context.safeStart(CrossSectionMeasurementActivity::class.java, "Kesit Plan Aplikasyon")
                        "crosshair_settings" -> context.safeStart(LayerSettingsActivity::class.java, "Katman Ayarları")
                        "measurement_analysis" -> context.safeStart(AnalysisMeasurementActivity::class.java, "Ölçüm Analizi")
                        else -> Toast.makeText(context, "${item.title} özelliği", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// Araçlar ana ekranı - Resimde sağ alt panel
@Composable
private fun ToolsMainScreen() {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(getToolsMenuItems()) { item ->
            MenuCard(
                title = item.title,
                icon = item.icon,
                color = item.color,
                onClick = {
                    when (item.action) {
                        "local_transformation" -> context.safeStart(LocalTransformationActivity::class.java, "Lokal Dönüşüm")
                        "coordinate_transformation" -> context.safeStart(CoordinateConverterActivity::class.java, "Koordinat Dönüştür")
                        "angle_transformation" -> context.safeStart(AngleConverterActivity::class.java, "Açı Dönüştür")
                        "conversion_area" -> context.safeStart(ConversionAreaActivity::class.java, "Çevre Ve Alan")
                        "cogo" -> context.safeStart(CogoActivity::class.java, "COGO")
                        "calculation_machine" -> context.safeStart(CalculationActivity::class.java, "Hesap Makinesi")
                        "external_radio" -> context.safeStart(ExternalRadioActivity::class.java, "Harici Radyo")
                        "volume_calculation" -> context.safeStart(VolumeCalculationActivity::class.java, "Hacim Hesabı")
                        "station_renewal" -> context.safeStart(StationRenewalActivity::class.java, "İstasyon Yenile")
                        "user_manual" -> context.safeStart(com.example.tugis3.ui.help.HelpActivity::class.java, "Kullanım Kılavuzu")
                        else -> Toast.makeText(context, "${item.title} özelliği", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// Proje menü öğeleri - güncellenmiş ve yeni modüllere uygun
private fun getProjectMenuItems() = listOf(
    MenuItem("Proje Yöneticisi", Icons.Default.AccountTree, Color(0xFF4CAF50), "project_manager"),
    MenuItem("Koordinat Sistemi", Icons.Default.Language, Color(0xFF2196F3), "coordinate_system"),
    MenuItem("Nokta Veritabanı", Icons.AutoMirrored.Outlined.List, Color(0xFF9C27B0), "point_database"),
    MenuItem("Proje Bilgisi", Icons.Default.Info, Color(0xFFFF9800), "project_info"),
    MenuItem("Dosya Yönet", Icons.Default.Folder, Color(0xFF4CAF50), "survey_management"),
    MenuItem("Nokta Kalibre", Icons.Default.Tune, Color(0xFFE91E63), "calibration"),
    MenuItem("Veri Gönder", Icons.Default.Send, Color(0xFF00BCD4), "send_data"),
    MenuItem("QR Kod", Icons.Default.QrCode, Color(0xFFFF5722), "barcode"),
    MenuItem("Bulut Ayarları", Icons.Default.Cloud, Color(0xFF607D8B), "cloud_settings"),
    MenuItem("Yazılım Ayarları", Icons.Default.Settings, Color(0xFF795548), "software_settings")
)

// Cihaz menü öğeleri - güncellenmiş ve yeni modüllere uygun
private fun getDeviceMenuItems() = listOf(
    MenuItem("İletişim", Icons.Default.Wifi, Color(0xFF4CAF50), "communication"),
    MenuItem("Bluetooth", Icons.Default.Bluetooth, Color(0xFF2196F3), "bluetooth"),
    MenuItem("Base Station", Icons.Default.Wifi, Color(0xFF9C27B0), "base_station"),
    MenuItem("Rover Station", Icons.Default.GpsFixed, Color(0xFFFF9800), "rover_station"),
    MenuItem("Ölçü Cihazı", Icons.Default.DeviceHub, Color(0xFFE91E63), "measuring_device"),
    MenuItem("Sabit", Icons.Default.Place, Color(0xFF00BCD4), "fixed"),
    MenuItem("Statik Durum", Icons.Default.Satellite, Color(0xFFFF5722), "static_status"),
    MenuItem("Mevcut Konum", Icons.Default.LocationOn, Color(0xFF607D8B), "current_location"),
    MenuItem("Hızlı Modül", Icons.Default.Speed, Color(0xFF795548), "quick_module"),
    MenuItem("Cihaz Bilgisi", Icons.Default.Info, Color(0xFF3F51B5), "device_info"),
    MenuItem("Sensör Kalibre", Icons.Default.Tune, Color(0xFF009688), "sensor_calibration")
)

// Ölçüm menü öğeleri - Lazy loading ile optimize edildi
private fun getSurveyMenuItems() = listOf(
    MenuItem("Grafik Alım", Icons.Default.Map, Color(0xFF2196F3), "graphic_survey"),
    MenuItem("Hızlı Alım", Icons.Default.Speed, Color(0xFF4CAF50), "quick_survey"),
    MenuItem("Cad Aplikasyon", Icons.Default.Architecture, Color(0xFFFF9800), "cad_application"),
    MenuItem("Nokta Aplikasyon", Icons.Default.LocationOn, Color(0xFF9C27B0), "point_application"),
    MenuItem("Hat Aplikasyon", Icons.Default.Timeline, Color(0xFFE91E63), "line_application"),
    MenuItem("Kesit Plan Aplikasyon", Icons.Default.Layers, Color(0xFF00BCD4), "precision_plan_application"),
    MenuItem("Katman Ayarları", Icons.Default.Layers, Color(0xFFFF5722), "crosshair_settings"),
    MenuItem("Ölçüm Analizi", Icons.Default.Analytics, Color(0xFF607D8B), "measurement_analysis")
)

// Araçlar menü öğeleri - Lazy loading ile optimize edildi
private fun getToolsMenuItems() = listOf(
    MenuItem("Lokal Dönüşüm", Icons.Default.Transform, Color(0xFF4CAF50), "local_transformation"),
    MenuItem("Koordinat Dönüştür", Icons.Default.CompareArrows, Color(0xFF2196F3), "coordinate_transformation"),
    MenuItem("Açı Dönüştür", Icons.Default.Rotate90DegreesCw, Color(0xFFFF9800), "angle_transformation"),
    MenuItem("Çevre Ve Alan", Icons.Default.CropFree, Color(0xFF9C27B0), "conversion_area"),
    MenuItem("COGO", Icons.Default.Calculate, Color(0xFFE91E63), "cogo"),
    MenuItem("Hesap Makinesi", Icons.Default.Calculate, Color(0xFF00BCD4), "calculation_machine"),
    MenuItem("Harici Radyo", Icons.Default.Radio, Color(0xFFFF5722), "external_radio"),
    MenuItem("Hacim Hesabı", Icons.Default.ViewInAr, Color(0xFF607D8B), "volume_calculation"),
    MenuItem("İstasyon Yenile", Icons.Default.Refresh, Color(0xFF795548), "station_renewal"),
    MenuItem("Kullanım Kılavuzu", Icons.Default.MenuBook, Color(0xFF3F51B5), "user_manual")
)

// General Information Section - SurvStar benzeri
@Suppress("DEPRECATION")
@Composable
fun GeneralInformationSection() { // uyarı bastırıldı
    val vm = runCatching { hiltViewModel<NavInfoViewModel>() }.getOrNull()

    if (vm == null) {
        // Fallback (VM oluşturulamadıysa crash yerine basit bilgi kartı göster)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TUGIS 3.0",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Satellite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("GPS: --", fontSize = 12.sp)
                }
            }
        }
        return
    }

    val uiState by vm.uiState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TUGIS 3.0 - ${uiState.projectName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Satellite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when (uiState.gpsStatus) {
                            "RTK_FIX" -> MaterialTheme.colorScheme.primary
                            "RTK_FLOAT" -> Color(0xFFFFC107)
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                    Text("GPS: ${uiState.satelliteCount}", fontSize = 12.sp)
                }
            }

            if (uiState.showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Konum: ${uiState.currentLocation}", fontSize = 10.sp)
                    Text("Durum: ${uiState.gpsStatus}", fontSize = 10.sp)
                }
            }
        }
    }
}

// NavInfoViewModel - GNSS ve proje bilgilerini yönetir
@HiltViewModel
class NavInfoViewModel @Inject constructor(
    private val gnssRepository: GnssPositionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavInfoUiState())
    val uiState: StateFlow<NavInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gnssRepository.position.collect { position ->
                _uiState.update { currentState ->
                    currentState.copy(
                        satelliteCount = position.satsInUse ?: 0,
                        gpsStatus = position.fixType?.name ?: "NO_FIX",
                        currentLocation = if (position.lat != null && position.lon != null) {
                            "${String.format("%.6f", position.lat)}, ${String.format("%.6f", position.lon)}"
                        } else {
                            "Konum alınamadı"
                        }
                    )
                }
            }
        }
    }

    fun toggleDetails() {
        _uiState.update { it.copy(showDetails = !it.showDetails) }
    }
}

data class NavInfoUiState(
    val projectName: String = "Varsayılan Proje",
    val satelliteCount: Int = 0,
    val gpsStatus: String = "NO_FIX",
    val currentLocation: String = "Konum alınamadı",
    val showDetails: Boolean = false
)

// MenuCard composable for displaying menu items in grid
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Gerçek zamanlı saat gösterimi - her saniye güncellenir
 */
@Composable
fun RealTimeClock() {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    // Her saniye güncelleme
    LaunchedEffect(key1 = Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L) // 1 saniye bekle
            currentTime = getCurrentTime() // Saati güncelle
        }
    }

    Text(
        text = currentTime,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    )
}

private fun getCurrentTime(): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}

