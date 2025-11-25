package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CalibrationActivity : ComponentActivity() {
    private val vm: CalibrationViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CalibrationScreen(vm = vm, onBack = { finish() })
            }
        }
    }
}

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val pointRepo: SurveyPointRepository
) : ViewModel() {

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating

    private val _calibrationData = MutableStateFlow<CalibrationData?>(null)
    val calibrationData: StateFlow<CalibrationData?> = _calibrationData

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _message

    fun startCalibration() {
        _isCalibrating.value = true
        _message.tryEmit("📡 GNSS CORS bağlantısı kuruluyor...")
        // Burada gerçek GNSS bağlantısı yapılacak
    }

    fun stopCalibration() {
        _isCalibrating.value = false
        _message.tryEmit("⏹️ Kalibrasyon durduruldu")
    }

    fun updateCalibrationData(data: CalibrationData) {
        _calibrationData.value = data
    }

    fun saveCalibrationPoint(
        pointName: String,
        knownNorthing: Double,
        knownEasting: Double,
        knownElevation: Double
    ) {
        viewModelScope.launch {
            val project = activeProject.value ?: return@launch
            val data = _calibrationData.value ?: return@launch

            runCatching {
                // GNSS'den okunan değerler ile bilinen değerler arasındaki farkları hesapla
                val deltaNorthing = knownNorthing - (data.northing ?: 0.0)
                val deltaEasting = knownEasting - (data.easting ?: 0.0)
                val deltaElevation = knownElevation - (data.elevation ?: 0.0)

                // Kalibrasyon noktasını kaydet
                pointRepo.saveBasePoint(
                    name = pointName,
                    code = "CAL",
                    latitude = data.latitude,
                    longitude = data.longitude,
                    elevation = knownElevation,
                    northing = knownNorthing,
                    easting = knownEasting,
                    zone = data.zone,
                    hrms = data.hrms,
                    vrms = data.vrms,
                    pdop = data.pdop,
                    satellites = data.satellites,
                    fixType = data.fixType,
                    antennaHeight = data.antennaHeight,
                    projectId = project.id
                )

                _message.tryEmit(
                    "✓ Kalibrasyon kaydedildi\n" +
                    "ΔN: ${String.format("%.3f", deltaNorthing)}m\n" +
                    "ΔE: ${String.format("%.3f", deltaEasting)}m\n" +
                    "ΔH: ${String.format("%.3f", deltaElevation)}m"
                )
            }.onFailure {
                _message.tryEmit("✗ Hata: ${it.message}")
            }
        }
    }
}

data class CalibrationData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    val northing: Double? = null,
    val easting: Double? = null,
    val zone: String? = null,
    val hrms: Double? = null,
    val vrms: Double? = null,
    val pdop: Double? = null,
    val satellites: Int? = null,
    val fixType: String? = null,
    val antennaHeight: Double? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalibrationScreen(vm: CalibrationViewModel, onBack: () -> Unit) {
    val activeProject by vm.activeProject.collectAsState()
    val isCalibrating by vm.isCalibrating.collectAsState()
    val calibrationData by vm.calibrationData.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nokta Kalibrasyonu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Proje Bilgisi
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aktif Proje", style = MaterialTheme.typography.labelMedium)
                    Text(activeProject?.name ?: "Yok", fontWeight = FontWeight.Bold)
                }
            }

            // Kalibrasyon Açıklaması
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Kalibrasyon Nasıl Yapılır?", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "1. GNSS CORS alıcınızı bilinen bir noktaya yerleştirin\n" +
                        "2. 'Kalibrasyon Başlat' butonuna basın\n" +
                        "3. GNSS verilerini bekleyin (RTK FIX önerilir)\n" +
                        "4. Bilinen nokta koordinatlarını girin\n" +
                        "5. 'Kaydet' ile kalibrasyon parametrelerini kaydedin",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Kalibrasyon Kontrolü
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GNSS CORS Durumu", fontWeight = FontWeight.Bold)
                    
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isCalibrating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("Kalibrasyon Aktif", color = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Default.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Bekleniyor", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (isCalibrating) {
                            Button(onClick = { vm.stopCalibration() }) {
                                Icon(Icons.Default.Stop, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Durdur")
                            }
                        } else {
                            Button(onClick = { vm.startCalibration() }) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Başlat")
                            }
                        }
                    }
                }
            }

            // GNSS Verileri
            if (calibrationData != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("GNSS Ölçüm Verileri", fontWeight = FontWeight.Bold)

                        calibrationData?.let { data ->
                            CalibrationDataRow("Enlem", data.latitude?.let { String.format("%.8f°", it) } ?: "—")
                            CalibrationDataRow("Boylam", data.longitude?.let { String.format("%.8f°", it) } ?: "—")
                            CalibrationDataRow("Yükseklik", data.elevation?.let { String.format("%.3f m", it) } ?: "—")
                            CalibrationDataRow("Kuzey (UTM)", data.northing?.let { String.format("%.3f m", it) } ?: "—")
                            CalibrationDataRow("Doğu (UTM)", data.easting?.let { String.format("%.3f m", it) } ?: "—")
                            CalibrationDataRow("Bölge", data.zone ?: "—")

                            HorizontalDivider()

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("HRMS", style = MaterialTheme.typography.labelSmall)
                                    Text(data.hrms?.let { String.format("%.3f", it) } ?: "—", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("VRMS", style = MaterialTheme.typography.labelSmall)
                                    Text(data.vrms?.let { String.format("%.3f", it) } ?: "—", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PDOP", style = MaterialTheme.typography.labelSmall)
                                    Text(data.pdop?.let { String.format("%.1f", it) } ?: "—", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Uydu", style = MaterialTheme.typography.labelSmall)
                                    Text(data.satellites?.toString() ?: "—", fontWeight = FontWeight.Bold)
                                }
                            }

                            data.fixType?.let {
                                Spacer(Modifier.height(4.dp))
                                AssistChip(
                                    onClick = { },
                                    label = { Text("Fix: $it") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = when (it) {
                                                "RTK_FIXED" -> MaterialTheme.colorScheme.primary
                                                "RTK_FLOAT" -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = calibrationData != null
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Kalibrasyon Noktası Kaydet")
                        }
                    }
                }
            }

            // Yardım Notları
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, null)
                        Text("İpuçları", fontWeight = FontWeight.Bold)
                    }
                    Text("• RTK FIX durumunda kalibrasyon yapmanız önerilir", style = MaterialTheme.typography.bodySmall)
                    Text("• En az 3 kalibrasyon noktası kullanın", style = MaterialTheme.typography.bodySmall)
                    Text("• Kalibrasyon noktaları çalışma alanını kapsamalı", style = MaterialTheme.typography.bodySmall)
                    Text("• HRMS ve VRMS değerleri <0.05 olmalı", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // Kalibrasyon Kaydet Dialog
    if (showSaveDialog && calibrationData != null) {
        SaveCalibrationDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name, n, e, h ->
                vm.saveCalibrationPoint(name, n, e, h)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun CalibrationDataRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SaveCalibrationDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double) -> Unit
) {
    var pointName by remember { mutableStateOf("") }
    var knownNorthing by remember { mutableStateOf("") }
    var knownEasting by remember { mutableStateOf("") }
    var knownElevation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kalibrasyon Noktası Kaydet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Bilinen nokta koordinatlarını girin:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pointName,
                    onValueChange = { pointName = it },
                    label = { Text("Nokta Adı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = knownNorthing,
                    onValueChange = { knownNorthing = it },
                    label = { Text("Bilinen Kuzey (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = knownEasting,
                    onValueChange = { knownEasting = it },
                    label = { Text("Bilinen Doğu (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = knownElevation,
                    onValueChange = { knownElevation = it },
                    label = { Text("Bilinen Yükseklik (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        pointName,
                        knownNorthing.toDoubleOrNull() ?: 0.0,
                        knownEasting.toDoubleOrNull() ?: 0.0,
                        knownElevation.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = pointName.isNotBlank() &&
                        knownNorthing.toDoubleOrNull() != null &&
                        knownEasting.toDoubleOrNull() != null &&
                        knownElevation.toDoubleOrNull() != null
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

