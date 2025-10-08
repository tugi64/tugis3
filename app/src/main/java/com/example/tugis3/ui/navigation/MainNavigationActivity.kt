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
// Ortak
import com.example.tugis3.ui.common.PlaceholderActivity
import com.example.tugis3.util.crash.CrashLogger
import com.example.tugis3.util.crash.CrashLogActivity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel // (deprecated uyarısı kalabilir, sürüm güncellenince yeni pakete taşınır)
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

@AndroidEntryPoint
class MainNavigationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Önceki crash tespit edilirse log ekranını aç
        runCatching {
            val ts = CrashLogger.consumePendingCrash(this)
            if (ts != null) startActivity(Intent(this, CrashLogActivity::class.java))
        }
        try {
            setContent {
                Tugis3Theme { ErrorBoundary { AppRoot() } }
            }
        } catch (t: Throwable) {
            runCatching { CrashLogger.logException(this, t) }
            setContent {
                Tugis3Theme { FatalErrorScreen(error = t, onShowCrashLog = {
                    runCatching { startActivity(Intent(this, CrashLogActivity::class.java)) }
                }) }
            }
        }
    }
}

// Basit Compose Error Boundary - içerik composable'ı çalıştırırken oluşan hataları yakalayıp fallback gösterir
@Composable
private fun ErrorBoundary(content: @Composable () -> Unit) {
    var error by remember { mutableStateOf<Throwable?>(null) }
    if (error == null) {
        // İlk compose sırasında hata olursa try-catch yakalar.
        try {
            content()
        } catch (t: Throwable) {
            error = t
            // CrashLogger'a yaz (UI thread'te hızlıca) - silent
            runCatching { CrashLogger.logException(LocalContext.current, t) }
        }
    } else {
        FatalErrorScreen(error = error!!, onShowCrashLog = {
            val ctx = LocalContext.current
            runCatching { ctx.startActivity(Intent(ctx, CrashLogActivity::class.java)) }
        })
    }
}

@Composable
private fun FatalErrorScreen(error: Throwable, onShowCrashLog: () -> Unit) {
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
                OutlinedButton(onClick = { /* ileride tekrar deneme opsiyonu eklenebilir */ }) { Text("Tekrar Dene") }
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
    var selectedRoute by rememberSaveable { mutableStateOf(NavRoutes.PROJECT) }
    LaunchedEffect(selectedRoute) {
        if (selectedRoute != NavRoutes.PROJECT) {
            val current = navController.currentDestination?.route
            if (current != selectedRoute) {
                runCatching {
                    navController.navigate(selectedRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
    Scaffold(
        topBar = { GeneralInformationSection() },
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
                            if (selectedRoute != item.route) {
                                selectedRoute = item.route
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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

@Immutable
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
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
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Project:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("(yükleniyor)", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fix:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("0/0", style = MaterialTheme.typography.bodySmall)
                    Text("H:- V:-", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                AssistChip(onClick = {}, label = { Text("NTRIP: -") })
            }
        }
        return
    }

    val header by vm.header.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Project Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Project:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = header.projectName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GNSS Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // GNSS Status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Satellite,
                        contentDescription = "GNSS Status",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = header.fixLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Satellite Count
                Text(
                    text = header.satCounts,
                    style = MaterialTheme.typography.bodySmall
                )

                // HRMS/VRMS
                Text(
                    text = header.rmsLabel,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Work Mode and Datalink Row (NTRIP durumu dahil)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rover",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "UHF",
                    style = MaterialTheme.typography.bodySmall
                )
                val (chipColor, labelColor) = when {
                    header.ntripStatus.startsWith("Bağlandı") -> MaterialTheme.colorScheme.primaryContainer to contentColorFor(MaterialTheme.colorScheme.primaryContainer)
                    header.ntripStatus.startsWith("Bağlanıyor") -> MaterialTheme.colorScheme.tertiaryContainer to contentColorFor(MaterialTheme.colorScheme.tertiaryContainer)
                    header.ntripStatus.startsWith("Hata") -> MaterialTheme.colorScheme.errorContainer to contentColorFor(MaterialTheme.colorScheme.errorContainer)
                    else -> MaterialTheme.colorScheme.surfaceVariant to contentColorFor(MaterialTheme.colorScheme.surfaceVariant)
                }
                AssistChip(
                    onClick = {},
                    label = { Text("NTRIP: ${header.ntripStatus}") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = chipColor, labelColor = labelColor)
                )
                IconButton(
                    onClick = { /* SAT Information */ },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "SAT Info",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// SurvStar benzeri ana ekranlar

// Eski basit MenuItem tanımı yerine id içeren gelişmiş sürüm
@Immutable
data class MenuItem(val title: String, val icon: ImageVector, val route: String? = null, val id: String = title.lowercase().replace(" ", "_"))

// --- Menü durum yönetimi: favoriler & son kullanılanlar ---
@Composable
private fun rememberMenuState(viewModel: MenuStateViewModel = hiltViewModel()): MenuStateViewModel = viewModel

// GridMenu önceki gelişmiş sürümünü kolon parametresi destekleyecek şekilde güncelliyoruz
@Composable
fun GridMenu(
    menuItems: List<MenuItem>,
    favorites: Set<String>,
    recents: List<String>,
    columns: Int,
    onMenuItemClick: (MenuItem) -> Unit,
    onLongPress: (MenuItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(menuItems, key = { it.id }) { item ->
            val isFav = favorites.contains(item.id)
            val isRecent = recents.contains(item.id)
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMenuItemClick(item)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress(item)
                        }
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isRecent) 8.dp else 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFav) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                        if (isFav) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favori",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = if (isRecent) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isRecent) {
                        Text(
                            text = "SON",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

// Menü üst iskeleti güncellendi: sekmeler, bottom sheet, temizleme butonları
@Composable
fun MenuScreenScaffold(
    rawItems: List<MenuItem>,
    onLaunch: (MenuItem) -> Unit
) {
    val ms = rememberMenuState()
    val favorites by ms.favorites.collectAsState()
    val recents by ms.recents.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Tümü, 1: Favoriler
    val configuration = LocalConfiguration.current
    val maxWidth = configuration.screenWidthDp
    val columns = remember(maxWidth) {
        when {
            maxWidth >= 1000 -> 6
            maxWidth >= 800 -> 5
            maxWidth >= 600 -> 4
            maxWidth >= 400 -> 4
            else -> 3
        }
    }

    // Bottom sheet state
    var sheetItem by remember { mutableStateOf<MenuItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val baseFiltered = remember(rawItems, query) {
        if (query.isBlank()) rawItems else rawItems.filter { it.title.contains(query, true) }
    }

    val filtered = remember(baseFiltered, favorites, selectedTab) {
        val base = if (selectedTab == 1) baseFiltered.filter { favorites.contains(it.id) } else baseFiltered
        base.sortedWith(compareByDescending<MenuItem> { favorites.contains(it.id) }.thenBy { it.title })
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val inProgress by ms.inProgress.collectAsState()
            if (inProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tümü") },
                    icon = { Icon(Icons.Outlined.Apps, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Favoriler") },
                    icon = { Icon(Icons.Outlined.Star, contentDescription = null) }
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Ara / filtrele") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = query.isNotBlank(),
                    onClick = { if (query.isNotBlank()) query = "" },
                    label = { Text(if (query.isBlank()) "Filtre Yok" else "Filtreyi Temizle") },
                    leadingIcon = if (query.isNotBlank()) ({ Icon(Icons.Outlined.Close, contentDescription = null) }) else null
                )
                if (favorites.isNotEmpty()) {
                    AssistChip(onClick = { selectedTab = 1 }, label = { Text("Favoriler: ${favorites.size}") }, leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) })
                    TextButton(onClick = { ms.clearFavorites() }) { Text("Favorileri Temizle") }
                }
                if (recents.isNotEmpty()) {
                    TextButton(onClick = { ms.clearRecents() }) { Text("Geçmişi Temizle") }
                }
            }
            if (selectedTab == 1 && favorites.isEmpty()) {
                Text(
                    "Henüz favori yok. Bir kartı uzun basarak favoriye ekleyebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (recents.isNotEmpty()) {
                Text(
                    "Son Kullanılanlar",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val recentItems = recents.mapNotNull { id -> rawItems.find { it.id == id } }
                    items(recentItems, key = { it.id }) { mi ->
                        AssistChip(
                            onClick = {
                                onLaunch(mi)
                                ms.recordUsage(mi.id)
                            },
                            label = { Text(mi.title, maxLines = 1) },
                            leadingIcon = { Icon(mi.icon, contentDescription = null) }
                        )
                    }
                }
            }
            HorizontalDivider()
            Box(Modifier.weight(1f)) {
                if (filtered.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (query.isNotBlank()) "Filtreye uygun sonuç yok" else if (selectedTab==1) "Favori yok" else "Gösterilecek öğe yok",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        if (selectedTab==1 && favorites.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Kartı uzun basarak favori ekleyin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                } else {
                    GridMenu(
                        menuItems = filtered,
                        favorites = favorites,
                        recents = recents,
                        columns = columns,
                        onMenuItemClick = {
                            onLaunch(it)
                            ms.recordUsage(it.id)
                        },
                        onLongPress = { sheetItem = it }
                    )
                }
            }
        }
        if (sheetItem != null) {
            val mi = sheetItem!!
            ModalBottomSheet(
                onDismissRequest = { sheetItem = null },
                sheetState = sheetState
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(mi.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val isFav = favorites.contains(mi.id)
                    OutlinedButton(onClick = {
                        ms.toggleFavorite(mi.id)
                        sheetItem = null
                    }) { Text(if (isFav) "Favoriden Çıkar" else "Favoriye Ekle") }
                    OutlinedButton(onClick = {
                        onLaunch(mi)
                        ms.recordUsage(mi.id)
                        sheetItem = null
                    }) { Text("Aç") }
                    if (recents.contains(mi.id)) {
                        OutlinedButton(onClick = {
                            ms.removeRecent(mi.id)
                            sheetItem = null
                        }) { Text("Geçmişten Kaldır") }
                    }
                    TextButton(onClick = { sheetItem = null }) { Text("Kapat") }
                }
            }
        }
    }
}

// ProjectMainScreen vb. fonksiyonlar yeni GridMenu imzasını kullanacak şekilde güncelleniyor
@Composable
fun ProjectMainScreen() {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val vm = hiltViewModel<ProjectMenuViewModel>()
    val uiState by vm.ui.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    // Eski grid menü öğeleri (operasyonel menü) aynı kaldı
    val operationMenuItems = listOf(
        MenuItem("Proje Bilgisi", Icons.Outlined.Folder),
        MenuItem("Dosya Yönet", Icons.Outlined.FileCopy),
        MenuItem("Koordinat Sistemi", Icons.Outlined.Public),
        MenuItem("Nokta Kalibre", Icons.Outlined.GpsFixed),
        MenuItem("Nokta Listesi", Icons.AutoMirrored.Outlined.List),
        MenuItem("Nokta Verisi Dışa Aktarımı", Icons.Outlined.Upload),
        MenuItem("Nokta Verisi İçe Aktarımı", Icons.Outlined.Download),
        MenuItem("Barkod", Icons.Outlined.QrCodeScanner),
        MenuItem("Bulut Ayarları", Icons.Outlined.Cloud),
        MenuItem("Yazılım Ayarları", Icons.Outlined.Settings),
        MenuItem("Yazılım Güncelleme", Icons.Outlined.SystemUpdate)
    )

    // Hata snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            // --- Aktif Proje Kartı + Aksiyonlar ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aktif Proje", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val ap = uiState.activeProject
                    if (ap == null) {
                        Text("Aktif proje yok", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { showCreate = true }) { Icon(Icons.Outlined.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Yeni Proje Oluştur") }
                    } else {
                        Text(ap.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        ap.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = { showCreate = true }, label = { Text("Yeni") }, leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) })
                            AssistChip(onClick = { context.safeStart(ProjectManagerActivity::class.java, "Proje Bilgisi") }, label = { Text("Yönet") }, leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) })
                            AssistChip(onClick = { context.safeStart(FileManagerActivity::class.java, "Dosya Yönet") }, label = { Text("Dosyalar") }, leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) })
                        }
                    }
                }
            }

            // --- Proje Listesi ---
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .weight(0.5f, fill = false)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Projeler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showCreate = true }) { Icon(Icons.Outlined.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Oluştur") }
                    }
                    if (uiState.loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (uiState.projects.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Henüz proje yok. Yeni proje oluşturun.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.projects, key = { it.id }) { p ->
                                ProjectRow(
                                    model = p,
                                    onActivate = { vm.setActive(p.id) },
                                    onDelete = { vm.delete(p.id) },
                                    onOpen = { context.safeStart(ProjectManagerActivity::class.java, p.name) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Text(
                "Operasyon Menüsü",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            Box(Modifier.weight(1f)) {
                MenuScreenScaffold(rawItems = operationMenuItems) { menuItem ->
                    when (menuItem.title) {
                        "Proje Bilgisi" -> context.safeStart(ProjectManagerActivity::class.java, "Proje Bilgisi")
                        "Dosya Yönet" -> context.safeStart(FileManagerActivity::class.java, "Dosya Yönet")
                        "Koordinat Sistemi" -> context.safeStart(CoordinateSystemActivity::class.java, "Koordinat Sistemi")
                        "Nokta Kalibre" -> context.safeStart(EllipsoidParametersActivity::class.java, "Nokta Kalibre")
                        "Nokta Listesi" -> context.safeStart(PointListActivity::class.java, "Nokta Listesi")
                        "Nokta Verisi Dışa Aktarımı" -> context.safeStart(DataUploadActivity::class.java, "Veri Gönder")
                        "Nokta Verisi İçe Aktarımı" -> context.safeStart(FileManagerActivity::class.java, "Dosya Yönet")
                        "Barkod" -> context.safeStart(BarcodeActivity::class.java, "Barkod")
                        "Bulut Ayarları" -> context.safeStart(CloudSettingsActivity::class.java, "Bulut Ayarları")
                        "Yazılım Ayarları" -> context.safeStart(SoftwareSettingsActivity::class.java, "Yazılım Ayarları")
                        "Yazılım Güncelleme" -> context.safeStart(SoftwareUpdateActivity::class.java, "Yazılım Güncelleme")
                        else -> context.safeStart(PlaceholderActivity::class.java, menuItem.title)
                    }
                }
            }
        }
    }

    // --- Yeni Proje Dialog ---
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        vm.createProject(newName, newDesc.ifBlank { null })
                        newName = ""; newDesc = ""; showCreate = false
                    }
                ) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("İptal") } },
            title = { Text("Yeni Proje") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Proje Adı") }, singleLine = true)
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Açıklama (opsiyonel)") })
                }
            }
        )
    }
}

@Composable
private fun ProjectRow(
    model: ProjectUiModel,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (model.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (model.isActive) Icons.Outlined.Star else Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = if (model.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(model.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (model.isActive) FontWeight.Bold else FontWeight.Medium)
                }
                model.description?.takeIf { it.isNotBlank() }?.let { d ->
                    Spacer(Modifier.height(4.dp))
                    Text(d, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!model.isActive) {
                    IconButton(onClick = onActivate) { Icon(Icons.Outlined.CheckCircle, contentDescription = "Aktif Et") }
                } else {
                    IconButton(onClick = onOpen) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Aç") }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun DeviceMainScreen() {
    val context = LocalContext.current
    val menuItems = listOf(
        MenuItem("Haberleşme", Icons.Outlined.Bluetooth),
        MenuItem("GNSS İzleme", Icons.Outlined.GpsFixed),
        MenuItem("NTRIP Profilleri", Icons.Outlined.Cloud),
        MenuItem("Gezici Ayarları", Icons.Outlined.Navigation),
        MenuItem("Sabit Ayarları", Icons.Outlined.Radio),
        MenuItem("Cihaz Bilgisi", Icons.Outlined.Info),
        MenuItem("Cihaz Kaydı", Icons.Outlined.AppRegistration),
        MenuItem("Manyetik Tarama", Icons.Outlined.Search)
    )
    MenuScreenScaffold(rawItems = menuItems) { menuItem ->
        when (menuItem.title) {
            "Haberleşme" -> context.safeStart(DeviceCommunicationActivity::class.java, "Haberleşme")
            "GNSS İzleme" -> context.safeStart(GnssMonitorActivity::class.java, "GNSS İzleme")
            "NTRIP Profilleri" -> context.safeStart(NtripProfilesActivity::class.java, "NTRIP Profilleri")
            "Gezici Ayarları" -> context.safeStart(RoverSettingsActivity::class.java, "Gezici Ayarları")
            "Sabit Ayarları" -> context.safeStart(BaseStationSettingsActivity::class.java, "Sabit Ayarları")
            "Cihaz Bilgisi" -> context.safeStart(DeviceInfoActivity::class.java, "Cihaz Bilgisi")
            "Cihaz Kaydı" -> context.safeStart(DeviceRegistrationActivity::class.java, "Cihaz Kaydı")
            "Manyetik Tarama" -> context.safeStart(MagneticScanActivity::class.java, "Manyetik Tarama")
            else -> context.safeStart(PlaceholderActivity::class.java, menuItem.title)
        }
    }
}

// Bölümlenmiş Survey menü veri modeli
data class SurveySection(val title: String, val items: List<MenuItem>)

@Composable
private fun SectionedSurveyMenu(
    sections: List<SurveySection>,
    onLaunch: (MenuItem) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sections.forEach { section ->
            item(key = section.title + "_header") {
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                HorizontalDivider()
            }
            item(key = section.title + "_items") {
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    section.items.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { mi ->
                                ElevatedCard(
                                    onClick = { onLaunch(mi) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Icon(mi.icon, contentDescription = mi.title)
                                        Text(
                                            mi.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun SurveyMainScreen() {
    val context = LocalContext.current
    val sections = listOf(
        SurveySection(
            title = "Ölçüm Menüsü",
            items = listOf(MenuItem("Nokta Aplikasyonu", Icons.Outlined.GpsFixed))
        ),
        SurveySection(
            title = "Veri & Yapı",
            items = listOf(MenuItem("GIS Veri", Icons.Outlined.Public), MenuItem("Katman Ayarları", Icons.Outlined.Layers))
        ),
        SurveySection(
            title = "Gelişmiş Ölçüm",
            items = listOf(
                MenuItem("AR Aplikasyon", Icons.Outlined.Visibility),
                MenuItem("Fotogrametri", Icons.Outlined.CameraAlt),
                MenuItem("Statik Ölçüm", Icons.Outlined.Panorama),
                MenuItem("Epoch Ölçüm", Icons.Outlined.HourglassEmpty),
                MenuItem("Yol Aplikasyonu", Icons.Outlined.DirectionsCar),
                MenuItem("CAD", Icons.Outlined.Layers)
            )
        )
    )
    SectionedSurveyMenu(sections = sections) { menuItem ->
        when (menuItem.title) {
            "Nokta Aplikasyonu" -> context.safeStart(PointApplicationActivity::class.java, "Nokta Aplikasyonu")
            "GIS Veri" -> context.safeStart(GisDataCollectionActivity::class.java, "GIS Veri")
            "Katman Ayarları" -> context.safeStart(LayerSettingsActivity::class.java, "Katman Ayarları")
            "AR Aplikasyon" -> context.safeStart(ARApplicationActivity::class.java, "AR Aplikasyon")
            "Fotogrametri" -> context.safeStart(PhotogrammetryActivity::class.java, "Fotogrametri")
            "Statik Ölçüm" -> context.safeStart(StaticSurveyActivity::class.java, "Statik Ölçüm")
            "Epoch Ölçüm" -> context.safeStart(EpochSurveyActivity::class.java, "Epoch Ölçüm")
            "Yol Aplikasyonu" -> context.safeStart(RoadStakeoutActivity::class.java, "Yol Aplikasyonu")
            "CAD" -> context.safeStart(CadApplicationActivity::class.java, "CAD")
            else -> context.safeStart(PlaceholderActivity::class.java, menuItem.title)
        }
    }
}

@Composable
fun ToolsMainScreen() {
    val context = LocalContext.current
    val menuItems = listOf(
        MenuItem("Lokalizasyon", Icons.Outlined.MyLocation),
        MenuItem("Koordinat Dönüşümü", Icons.Outlined.Public),
        MenuItem("Açı Dönüşümü", Icons.Outlined.Explore),
        MenuItem("Alan / Çevre", Icons.Outlined.CropSquare),
        MenuItem("Mesafe Hesabı", Icons.Outlined.Straighten),
        MenuItem("COGO", Icons.Outlined.Calculate),
        MenuItem("Hacim Hesabı", Icons.Outlined.Assessment),
        MenuItem("Hesap Makinesi", Icons.Outlined.Dialpad),
        MenuItem("Dış Radyo", Icons.Outlined.Radio),
        MenuItem("Periyodik Offset", Icons.Outlined.Schedule),
        MenuItem("FTP Paylaşım", Icons.Outlined.Cloud),
        MenuItem("Grid -> Arazi", Icons.Outlined.GridOn),
        MenuItem("Veri Paylaşımı", Icons.Outlined.Share)
    )
    MenuScreenScaffold(rawItems = menuItems) { mi ->
        when (mi.title) {
            "Lokalizasyon" -> context.safeStart(LocalizationActivity::class.java, mi.title)
            "Koordinat Dönüşümü" -> context.safeStart(CoordinateConverterActivity::class.java, mi.title)
            "Açı Dönüşümü" -> context.safeStart(AngleConversionActivity::class.java, mi.title)
            "Alan / Çevre" -> context.safeStart(AreaCalculationActivity::class.java, mi.title)
            "Mesafe Hesabı" -> context.safeStart(DistanceCalculationActivity::class.java, mi.title)
            "COGO" -> context.safeStart(CogoToolsActivity::class.java, mi.title)
            "Hacim Hesabı" -> context.safeStart(VolumeCalculationActivity::class.java, mi.title)
            "Hesap Makinesi" -> context.safeStart(CalculatorActivity::class.java, mi.title)
            "Dış Radyo" -> context.safeStart(ExternalRadioConfigActivity::class.java, mi.title)
            "Periyodik Offset" -> context.safeStart(PeriodicOffsetActivity::class.java, mi.title)
            "FTP Paylaşım" -> context.safeStart(FtpSharedDataActivity::class.java, mi.title)
            "Grid -> Arazi" -> context.safeStart(GridToGroundActivity::class.java, mi.title)
            "Veri Paylaşımı" -> context.safeStart(DataSharingActivity::class.java, mi.title)
            else -> context.safeStart(PlaceholderActivity::class.java, mi.title)
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

@Suppress("unused")
fun Context.safeStartByName(className: String, title: String? = null) {
    try {
        val cls = Class.forName(className)
        safeStart(cls, title)
    } catch (_: Exception) {
        safeStart(PlaceholderActivity::class.java, title ?: className)
    }
}
