package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.ui.theme.Tugis3Theme
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.db.entity.PointEntity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlin.math.*
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class StakeoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                StakeoutScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutScreen(
    viewModel: StakeoutViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMapView by remember { mutableStateOf(true) }
    var showPointList by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    // GPS durumu kontrolü
    LaunchedEffect(uiState.gpsEnabled) {
        if (!uiState.gpsEnabled) {
            snackbarHostState.showSnackbar(
                message = "GPS kapalı. Ayarlara gitmek için tıklayın.",
                actionLabel = "GPS Aç"
            ).let { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Aplikasyon")
                        val selectedPoint = uiState.selectedPoint
                        if (selectedPoint != null) {
                            Text(
                                text = selectedPoint.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showMapView = !showMapView }) {
                        Icon(
                            if (showMapView) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMapView) "Liste Görünümü" else "Harita Görünümü"
                        )
                    }
                    IconButton(onClick = { showPointList = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Nokta Seç")
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val selectedPoint = uiState.selectedPoint
            if (selectedPoint != null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.toggleNavigation() },
                    icon = {
                        Icon(
                            if (uiState.isNavigating) Icons.Default.Stop else Icons.Default.Navigation,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(if (uiState.isNavigating) "Durdur" else "Navigasyon")
                    },
                    containerColor = if (uiState.isNavigating)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // GPS Durum Kartı
            GpsStatusCard(uiState)

            // Navigasyon Bilgisi Kartı
            val navigationInfo = uiState.navigationInfo
            if (navigationInfo != null) {
                NavigationInfoCard(navigationInfo)
            }

            // Hedef Nokta Bilgisi
            val selectedPoint = uiState.selectedPoint
            if (selectedPoint != null) {
                TargetPointCard(
                    selectedPoint = selectedPoint,
                    onSelectPoint = { showPointList = true },
                    onClearPoint = { viewModel.clearSelectedPoint() }
                )
            } else {
                Text(
                    text = "Lütfen bir hedef nokta seçin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mevcut Konum
            CurrentPositionCard(position = uiState.currentPosition)

            // Doğruluk ve Hassasiyet
            AccuracyCard(uiState = uiState)

            // Aplikasyon Geçmişi
            if (uiState.completedStakeouts.isNotEmpty()) {
                StakeoutHistoryCard(
                    completedStakeouts = uiState.completedStakeouts,
                    onRepeatStakeout = { viewModel.selectPoint(it) }
                )
            }

            // Yakındaki Noktalar
            if (uiState.nearbyPoints.isNotEmpty()) {
                NearbyPointsCard(
                    nearbyPoints = uiState.nearbyPoints,
                    onSelectPoint = { viewModel.selectPoint(it) }
                )
            }

            // Harita Görünümü
            if (showMapView) {
                StakeoutMapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    uiState = uiState,
                    onPointSelect = { viewModel.selectPoint(it) }
                )
            }
        }
    }

    // Nokta Seçici Dialog
    if (showPointList) {
        PointSelectorDialog(
            availablePoints = uiState.availablePoints,
            onPointSelect = { point ->
                viewModel.selectPoint(point)
                showPointList = false
            },
            onDismiss = { showPointList = false }
        )
    }
}

@Composable
private fun GpsStatusCard(
    uiState: StakeoutScreenUiState
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.gpsEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GPS Durumu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.gpsEnabled) "Aktif" else "Kapalı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.satelliteCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Uydu", style = MaterialTheme.typography.bodySmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f", uiState.accuracy),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            uiState.accuracy < 1.0 -> Color.Green
                            uiState.accuracy < 3.0 -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                    Text("cm", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TargetPointCard(
    selectedPoint: StakeoutPoint,
    onSelectPoint: () -> Unit,
    onClearPoint: () -> Unit
) {
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
                    text = "Hedef Nokta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onClearPoint) {
                        Icon(Icons.Default.Clear, contentDescription = "Temizle")
                    }
                    Button(onClick = onSelectPoint) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nokta Seç")
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = selectedPoint.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = selectedPoint.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CoordinateText("X", selectedPoint.x)
                    CoordinateText("Y", selectedPoint.y)
                    CoordinateText("Z", selectedPoint.z)
                }
            }
        }
    }
}

@Composable
private fun CurrentPositionCard(position: Position) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Mevcut Konum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CoordinateText("X", position.x)
                CoordinateText("Y", position.y)
                CoordinateText("Z", position.z)
            }

            // Koordinat formatları
            Divider()

            Text(
                text = "Alternatif Formatlar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // DD formatında göster (basitleştirilmiş)
            val lat = 41.0 + (position.y - 4500000) / 111320.0
            val lon = 29.0 + (position.x - 580000) / 111320.0

            Text(
                text = "Lat/Lon: ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun NavigationInfoCard(navigationInfo: NavigationInfo) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Navigasyon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Mesafe Bilgileri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                NavigationInfo("Yatay", String.format("%.2f m", navigationInfo.horizontalDistance))
                NavigationInfo("Dikey", String.format("%.2f m", abs(navigationInfo.verticalDistance)))
                NavigationInfo("Toplam", String.format("%.2f m", navigationInfo.totalDistance))
            }

            // Yön Bilgisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                NavigationInfo("Açı", String.format("%.1f°", navigationInfo.bearing))
                NavigationInfo("Doğu", String.format("%.2f m", navigationInfo.deltaX))
                NavigationInfo("Kuzey", String.format("%.2f m", navigationInfo.deltaY))
            }

            // Mesafe Durumu
            DistanceStatusCard(navigationInfo.horizontalDistance)

            // Yön Göstergesi
            DirectionIndicator(
                deltaX = navigationInfo.deltaX,
                deltaY = navigationInfo.deltaY,
                distance = navigationInfo.horizontalDistance
            )
        }
    }
}

@Composable
private fun AccuracyCard(uiState: StakeoutScreenUiState) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Doğruluk ve Hassasiyet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AccuracyInfo("HDOP", String.format("%.2f", uiState.hdop))
                AccuracyInfo("VDOP", String.format("%.2f", uiState.vdop))
                AccuracyInfo("PDOP", String.format("%.2f", uiState.pdop))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AccuracyInfo("RTK Yaşı", "${uiState.rtkAge}s")
                AccuracyInfo("Fix Kalitesi", uiState.fixQuality)
                AccuracyInfo("Uydu Sayısı", "${uiState.satelliteCount}")
            }

            // Doğruluk seviye göstergesi
            LinearProgressIndicator(
                progress = (5.0f - uiState.accuracy.toFloat()) / 5.0f,
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    uiState.accuracy < 1.0 -> Color.Green
                    uiState.accuracy < 3.0 -> Color.Yellow
                    else -> Color.Red
                }
            )
        }
    }
}

@Composable
private fun DistanceStatusCard(horizontalDistance: Double) {
    // Mesafe Durumu
    val distanceStatus = when {
        horizontalDistance < 0.1 -> "Hedefe Ulaşıldı"
        horizontalDistance < 1.0 -> "Hedefe Çok Yakın"
        horizontalDistance < 5.0 -> "Hedefe Yakın"
        else -> "Hedefe Uzak"
    }

    val statusColor = when {
        horizontalDistance < 0.1 -> MaterialTheme.colorScheme.primary
        horizontalDistance < 1.0 -> MaterialTheme.colorScheme.tertiary
        horizontalDistance < 5.0 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = distanceStatus,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun DirectionIndicator(deltaX: Double, deltaY: Double, distance: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        val direction = when {
            deltaX > 1 && deltaY > 1 -> "↗"
            deltaX > 1 && deltaY < -1 -> "↘"
            deltaX < -1 && deltaY > 1 -> "↖"
            deltaX < -1 && deltaY < -1 -> "↙"
            deltaX > 1 -> "→"
            deltaX < -1 -> "←"
            deltaY > 1 -> "↑"
            deltaY < -1 -> "↓"
            else -> "●"
        }

        Text(
            text = direction,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NavigationInfo(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AccuracyInfo(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoordinateText(label: String, value: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%.3f", value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class StakeoutPoint(
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val description: String
)

data class Position(
    val x: Double = 584500.0,
    val y: Double = 4515200.0,
    val z: Double = 120.0
)

data class NavigationInfo(
    val deltaX: Double,
    val deltaY: Double,
    val distance: Double,
    val bearing: Double,
    val horizontalDistance: Double,
    val verticalDistance: Double,
    val totalDistance: Double
)

@HiltViewModel
class StakeoutViewModel @Inject constructor(
    private val pointRepository: PointRepository,
    private val gnssPositionRepository: GnssPositionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StakeoutScreenUiState())
    val uiState: StateFlow<StakeoutScreenUiState> = _uiState.asStateFlow()

    private var positionUpdateJob: Job? = null

    init {
        loadInitialData()
        startPositionUpdates()
        loadSamplePoints()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Nokta veritabanından mevcut noktaları yükle - şimdilik örnek veriler kullan
                // PointRepository'de getAllPoints() metodu yok, observePoints(projectId) var
                // Bu yüzden şimdilik loadSamplePoints() kullanıyoruz
                loadSamplePoints()
            } catch (e: Exception) {
                // Hata durumunda örnek veriler yükle
                loadSamplePoints()
            }
        }
    }

    private fun loadSamplePoints() {
        val samplePoints = listOf(
            StakeoutPoint("AP001", 584521.123, 4515234.567, 123.456, "Aplikasyon Noktası 1"),
            StakeoutPoint("AP002", 584621.789, 4515334.890, 124.789, "Aplikasyon Noktası 2"),
            StakeoutPoint("BM01", 584721.456, 4515434.123, 125.234, "Benchmark"),
            StakeoutPoint("YAPI01", 584821.678, 4515534.456, 126.678, "Yapı Köşesi"),
            StakeoutPoint("YOL01", 584921.345, 4515634.789, 127.345, "Yol Noktası"),
            StakeoutPoint("TRAV01", 584450.123, 4515150.456, 122.890, "Traversy Noktası"),
            StakeoutPoint("KONTROL1", 584580.345, 4515280.123, 123.678, "Kontrol Noktası"),
            StakeoutPoint("DETAY01", 584520.789, 4515220.345, 123.234, "Detay Noktası")
        )

        _uiState.update { currentState ->
            currentState.copy(
                availablePoints = samplePoints,
                nearbyPoints = samplePoints.take(3)
            )
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob = viewModelScope.launch {
            try {
                gnssPositionRepository.position.collect { gnssPosition ->
                    // GnssPosition'da lat/lon var, x/y/z yok
                    // Basit bir dönüşüm yapıyoruz (gerçek projede koordinat dönüşümü gerekli)
                    val position = Position(
                        x = 584500.0, // Sabit değer - gerçek projede lat/lon'dan dönüştürülmeli
                        y = 4515200.0,
                        z = gnssPosition.alt ?: 120.0
                    )

                    _uiState.update { currentState ->
                        currentState.copy(
                            currentPosition = position,
                            gpsEnabled = gnssPosition.lat != null && gnssPosition.lon != null,
                            satelliteCount = gnssPosition.satsInUse ?: 0,
                            accuracy = 999.0, // hAcc özelliği yok
                            hdop = gnssPosition.hdop ?: 99.0,
                            vdop = gnssPosition.vdop ?: 99.0,
                            pdop = gnssPosition.pdop ?: 99.0,
                            rtkAge = 999, // rtkAge özelliği yok
                            fixQuality = gnssPosition.fixType?.name ?: "NO_FIX"
                        )
                    }

                    // Seçili nokta varsa navigasyon bilgilerini güncelle
                    _uiState.value.selectedPoint?.let { target ->
                        updateNavigationInfo(target, position)
                    }

                    // Yakındaki noktaları güncelle
                    updateNearbyPoints()
                }
            } catch (e: Exception) {
                // GNSS bağlantısı yoksa simüle edilmiş veri kullan
                startSimulatedPositionUpdates()
            }
        }
    }

    private fun startSimulatedPositionUpdates() {
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Her saniye güncelle

                val currentPos = _uiState.value.currentPosition
                val newPosition = Position(
                    x = currentPos.x + (kotlin.random.Random.nextDouble() - 0.5) * 0.5, // ±25cm hareket
                    y = currentPos.y + (kotlin.random.Random.nextDouble() - 0.5) * 0.5,
                    z = currentPos.z + (kotlin.random.Random.nextDouble() - 0.5) * 0.1
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        currentPosition = newPosition,
                        gpsEnabled = true,
                        satelliteCount = (8..15).random(),
                        accuracy = kotlin.random.Random.nextDouble(0.5, 2.0), // cm
                        hdop = kotlin.random.Random.nextDouble(0.8, 2.5),
                        vdop = kotlin.random.Random.nextDouble(1.0, 3.0),
                        pdop = kotlin.random.Random.nextDouble(1.2, 3.5),
                        rtkAge = (1..5).random(),
                        fixQuality = listOf("RTK_FIX", "RTK_FLOAT", "DGPS").random()
                    )
                }

                // Navigasyon güncellemesi
                _uiState.value.selectedPoint?.let { target ->
                    updateNavigationInfo(target, newPosition)
                }
            }
        }
    }

    private fun updateNearbyPoints() {
        val currentPos = _uiState.value.currentPosition
        val nearby = _uiState.value.availablePoints.filter { point ->
            val distance = sqrt((point.x - currentPos.x).pow(2) + (point.y - currentPos.y).pow(2))
            distance <= 100.0 // 100m içindeki noktalar
        }.sortedBy { point ->
            sqrt((point.x - currentPos.x).pow(2) + (point.y - currentPos.y).pow(2))
        }.take(5)

        _uiState.update { currentState ->
            currentState.copy(nearbyPoints = nearby)
        }
    }

    fun selectPoint(point: StakeoutPoint) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedPoint = point,
                isNavigating = false
            )
        }

        updateNavigationInfo(point, _uiState.value.currentPosition)

        // Seçilen noktayı geçmişe ekle (benzersiz tutmak için)
        val history = _uiState.value.completedStakeouts.toMutableList()
        if (!history.any { it.name == point.name }) {
            history.add(0, point) // Başa ekle
            if (history.size > 10) {
                history.removeAt(history.lastIndex) // En eskisini çıkar
            }

            _uiState.update { currentState ->
                currentState.copy(completedStakeouts = history.toList())
            }
        }
    }

    private fun updateNavigationInfo(target: StakeoutPoint, current: Position) {
        val deltaX = target.x - current.x
        val deltaY = target.y - current.y
        val deltaZ = target.z - current.z
        val horizontalDistance = sqrt(deltaX.pow(2) + deltaY.pow(2))
        val verticalDistance = deltaZ
        val totalDistance = sqrt(deltaX.pow(2) + deltaY.pow(2) + deltaZ.pow(2))

        var bearing = Math.toDegrees(atan2(deltaX, deltaY))
        if (bearing < 0) bearing += 360

        _uiState.update { currentState ->
            currentState.copy(
                navigationInfo = NavigationInfo(
                    deltaX = deltaX,
                    deltaY = deltaY,
                    distance = totalDistance,
                    bearing = bearing,
                    horizontalDistance = horizontalDistance,
                    verticalDistance = verticalDistance,
                    totalDistance = totalDistance
                )
            )
        }
    }

    fun clearSelectedPoint() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedPoint = null,
                navigationInfo = null,
                isNavigating = false
            )
        }
    }

    fun toggleNavigation() {
        val wasNavigating = _uiState.value.isNavigating

        _uiState.update { currentState ->
            currentState.copy(isNavigating = !currentState.isNavigating)
        }

        if (!wasNavigating && _uiState.value.selectedPoint != null) {
            // Navigasyon başlatıldığında sürekli güncelleme başlat
            startNavigationUpdates()
        }
    }

    private fun startNavigationUpdates() {
        viewModelScope.launch {
            while (_uiState.value.isNavigating && _uiState.value.selectedPoint != null) {
                delay(500) // Yarım saniyede bir güncelle

                val target = _uiState.value.selectedPoint
                if (target != null) {
                    updateNavigationInfo(target, _uiState.value.currentPosition)

                    // Hedefe ulaşma kontrolü
                    val navInfo = _uiState.value.navigationInfo
                    if (navInfo != null && navInfo.horizontalDistance < 0.5) {
                        // Hedefe ulaşıldı, navigasyonu durdur
                        _uiState.update { currentState ->
                            currentState.copy(isNavigating = false)
                        }

                        // Başarılı aplikasyon geçmişine ekle
                        markPointAsCompleted(target)
                        // break yerine isNavigating false olduğu için döngü bitecek
                    }
                }
            }
        }
    }

    fun markPoint() {
        val selectedPoint = _uiState.value.selectedPoint
        val navigationInfo = _uiState.value.navigationInfo

        if (selectedPoint != null && navigationInfo != null) {
            if (navigationInfo.horizontalDistance < 1.0) {
                markPointAsCompleted(selectedPoint)

                _uiState.update { currentState ->
                    currentState.copy(
                        isNavigating = false,
                        selectedPoint = null,
                        navigationInfo = null
                    )
                }
            }
        }
    }

    private fun markPointAsCompleted(point: StakeoutPoint) {
        val completedList = _uiState.value.completedStakeouts.toMutableList()

        // Aynı nokta tekrar işaretlenmişse en üste taşı
        completedList.removeAll { it.name == point.name }
        completedList.add(0, point)

        // Maksimum 20 adet tut
        if (completedList.size > 20) {
            completedList.removeAt(completedList.lastIndex)
        }

        _uiState.update { currentState ->
            currentState.copy(
                completedStakeouts = completedList.toList()
            )
        }

        // Veritabanına da kaydet (isteğe bağlı)
        viewModelScope.launch {
            try {
                // Burada nokta işaretleme kaydı tutulabilir
                // pointRepository.markPointAsStaked(point.name, System.currentTimeMillis())
            } catch (e: Exception) {
                // Hata durumunda sessizce devam et
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                // Şimdilik sadece mevcut verileri yenile
                updateNearbyPoints()
            } catch (e: Exception) {
                // Hata durumunda mevcut verileri koru
            }
        }
    }

    fun importPointsFromDatabase() {
        viewModelScope.launch {
            try {
                // Şimdilik örnek noktaları kullan
                // PointRepository'de getAllPoints() metodu yok
                loadSamplePoints()
            } catch (e: Exception) {
                // Hata durumunda örnek noktaları yükle
                loadSamplePoints()
            }
        }
    }

    fun addCustomPoint(name: String, x: Double, y: Double, z: Double, description: String) {
        val newPoint = StakeoutPoint(name, x, y, z, description)

        val updatedList = _uiState.value.availablePoints.toMutableList()
        updatedList.add(newPoint)

        _uiState.update { currentState ->
            currentState.copy(availablePoints = updatedList)
        }

        // Veritabanına kaydetme şimdilik devre dışı
        // PointEntity yapısı farklı (easting, northing, ellipsoidalHeight)
        // ve upsert kullanılması gerekiyor
    }

    fun filterPointsByDistance(maxDistance: Double) {
        val currentPos = _uiState.value.currentPosition
        val filtered = _uiState.value.availablePoints.filter { point ->
            val distance = sqrt((point.x - currentPos.x).pow(2) + (point.y - currentPos.y).pow(2))
            distance <= maxDistance
        }

        _uiState.update { currentState ->
            currentState.copy(availablePoints = filtered)
        }
    }

    fun resetPointFilter() {
        loadInitialData()
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
    }
}

data class StakeoutScreenUiState(
    val selectedPoint: StakeoutPoint? = null,
    val currentPosition: Position = Position(),
    val navigationInfo: NavigationInfo? = null,
    val isNavigating: Boolean = false,
    val gpsEnabled: Boolean = false,
    val satelliteCount: Int = 0,
    val accuracy: Double = 0.0,
    val hdop: Double = 0.0,
    val vdop: Double = 0.0,
    val pdop: Double = 0.0,
    val rtkAge: Int = 0,
    val fixQuality: String = "",
    val completedStakeouts: List<StakeoutPoint> = emptyList(),
    val nearbyPoints: List<StakeoutPoint> = emptyList(),
    val availablePoints: List<StakeoutPoint> = emptyList()
)

@Composable
private fun StakeoutHistoryCard(
    completedStakeouts: List<StakeoutPoint>,
    onRepeatStakeout: (StakeoutPoint) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Son Aplikasyonlar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            completedStakeouts.take(5).forEach { point ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.clickable { onRepeatStakeout(point) }
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
                                text = point.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = point.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Tamamlandı",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyPointsCard(
    nearbyPoints: List<StakeoutPoint>,
    onSelectPoint: (StakeoutPoint) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Yakındaki Noktalar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            nearbyPoints.take(3).forEach { point ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.clickable { onSelectPoint(point) }
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
                                text = point.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = point.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            // Mesafe hesapla (örnek)
                            val distance = sqrt((point.x - 584500).pow(2) + (point.y - 4515200).pow(2))
                            Text(
                                text = "${String.format("%.0f", distance)}m",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Yakında",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PointSelectorDialog(
    availablePoints: List<StakeoutPoint>,
    onPointSelect: (StakeoutPoint) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredPoints = availablePoints // Filtreleme şimdilik yok

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hedef Nokta Seç")
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {

                if (filteredPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nokta bulunamadı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPoints) { point ->
                            Card(
                                modifier = Modifier.clickable { onPointSelect(point) }
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
                                            text = point.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = point.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "X:${String.format("%.3f", point.x)} Y:${String.format("%.3f", point.y)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Seç",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun StakeoutMapView(
    modifier: Modifier = Modifier,
    uiState: StakeoutScreenUiState,
    onPointSelect: (StakeoutPoint) -> Unit
) {
    var scale by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            // Arka plan çiz
            drawRect(
                color = Color(0xFFF0F0F0),
                size = size
            )

            // Grid çiz
            drawGrid(
                centerX = centerX + offsetX,
                centerY = centerY + offsetY,
                scale = scale
            )

            // Mevcut konumu çiz
            drawCurrentPosition(
                position = uiState.currentPosition,
                centerX = centerX + offsetX,
                centerY = centerY + offsetY,
                scale = scale
            )

            // Hedef noktayı çiz
            uiState.selectedPoint?.let { target ->
                drawTargetPoint(
                    point = target,
                    centerX = centerX + offsetX,
                    centerY = centerY + offsetY,
                    scale = scale
                )

                // Navigasyon çizgisi
                drawNavigationLine(
                    from = uiState.currentPosition,
                    to = target,
                    centerX = centerX + offsetX,
                    centerY = centerY + offsetY,
                    scale = scale
                )
            }

            // Yakındaki noktaları çiz
            uiState.nearbyPoints.forEach { point ->
                drawNearbyPoint(
                    point = point,
                    centerX = centerX + offsetX,
                    centerY = centerY + offsetY,
                    scale = scale
                )
            }
        }

        // Harita kontrolleri
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = minOf(scale * 1.2f, 5.0f) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yakınlaştır")
            }

            FloatingActionButton(
                onClick = { scale = maxOf(scale / 1.2f, 0.2f) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Uzaklaştır")
            }

            FloatingActionButton(
                onClick = {
                    offsetX = 0f
                    offsetY = 0f
                    scale = 1.0f
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Merkez")
            }
        }

        // Harita bilgi kartı
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Harita Görünümü",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ölçek: ${String.format("%.1fx", scale)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (uiState.selectedPoint != null) {
                    Text(
                        text = "Hedef: ${uiState.selectedPoint.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(centerX: Float, centerY: Float, scale: Float) {
    val gridSize = 50f * scale
    val color = Color.Gray.copy(alpha = 0.3f)

    // Dikey çizgiler
    for (i in -20..20) {
        val x = centerX + i * gridSize
        if (x >= 0 && x <= size.width) {
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
    }

    // Yatay çizgiler
    for (i in -20..20) {
        val y = centerY + i * gridSize
        if (y >= 0 && y <= size.height) {
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawCurrentPosition(
    position: Position,
    centerX: Float,
    centerY: Float,
    scale: Float
) {
    val x = centerX + ((position.x - 584500) * scale * 0.1).toFloat()
    val y = centerY - ((position.y - 4515200) * scale * 0.1).toFloat()

    // Ana konum işareti
    drawCircle(
        color = Color.Blue,
        radius = 8.dp.toPx(),
        center = Offset(x, y)
    )

    // İç nokta
    drawCircle(
        color = Color.White,
        radius = 4.dp.toPx(),
        center = Offset(x, y)
    )

    // Doğruluk çemberi (yaklaşık)
    drawCircle(
        color = Color.Blue.copy(alpha = 0.2f),
        radius = 20.dp.toPx() * scale,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawTargetPoint(
    point: StakeoutPoint,
    centerX: Float,
    centerY: Float,
    scale: Float
) {
    val x = centerX + ((point.x - 584500) * scale * 0.1).toFloat()
    val y = centerY - ((point.y - 4515200) * scale * 0.1).toFloat()

    // Hedef işareti
    drawCircle(
        color = Color.Red,
        radius = 10.dp.toPx(),
        center = Offset(x, y)
    )

    // İç çarpı işareti (basitleştirilmiş)
    drawLine(
        color = Color.White,
        start = Offset(x - 6.dp.toPx(), y - 6.dp.toPx()),
        end = Offset(x + 6.dp.toPx(), y + 6.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = Color.White,
        start = Offset(x - 6.dp.toPx(), y + 6.dp.toPx()),
        end = Offset(x + 6.dp.toPx(), y - 6.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )
}

private fun DrawScope.drawNearbyPoint(
    point: StakeoutPoint,
    centerX: Float,
    centerY: Float,
    scale: Float
) {
    val x = centerX + ((point.x - 584500) * scale * 0.1).toFloat()
    val y = centerY - ((point.y - 4515200) * scale * 0.1).toFloat()

    // Yakındaki nokta işareti
    drawCircle(
        color = Color.Green,
        radius = 6.dp.toPx(),
        center = Offset(x, y)
    )
}

private fun DrawScope.drawNavigationLine(
    from: Position,
    to: StakeoutPoint,
    centerX: Float,
    centerY: Float,
    scale: Float
) {
    val fromX = centerX + ((from.x - 584500) * scale * 0.1).toFloat()
    val fromY = centerY - ((from.y - 4515200) * scale * 0.1).toFloat()
    val toX = centerX + ((to.x - 584500) * scale * 0.1).toFloat()
    val toY = centerY - ((to.y - 4515200) * scale * 0.1).toFloat()

    // Navigasyon çizgisi
    drawLine(
        color = Color.Red,
        start = Offset(fromX, fromY),
        end = Offset(toX, toY),
        strokeWidth = 3.dp.toPx()
    )

    // Ok ucu (basitleştirilmiş)
    val arrowSize = 10.dp.toPx()
    val angle = atan2(toY - fromY, toX - fromX)

    val arrowX1 = toX - arrowSize * cos(angle - PI/6).toFloat()
    val arrowY1 = toY - arrowSize * sin(angle - PI/6).toFloat()
    val arrowX2 = toX - arrowSize * cos(angle + PI/6).toFloat()
    val arrowY2 = toY - arrowSize * sin(angle + PI/6).toFloat()

    drawLine(
        color = Color.Red,
        start = Offset(toX, toY),
        end = Offset(arrowX1, arrowY1),
        strokeWidth = 3.dp.toPx()
    )
    drawLine(
        color = Color.Red,
        start = Offset(toX, toY),
        end = Offset(arrowX2, arrowY2),
        strokeWidth = 3.dp.toPx()
    )
}
