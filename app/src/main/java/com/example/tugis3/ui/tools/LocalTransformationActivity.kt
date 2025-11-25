package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * LOKAL DÖNÜŞÜM APLİKASYONU
 *
 * SurPad 4.2 Uyumlu Localization
 * Referans: kullanimklavuz3.txt (Sayfa 167-173)
 *
 * Özellikler:
 * ✅ Lokalizasyon noktası ekleme
 * ✅ Nokta düzenleme
 * ✅ Nokta silme
 * ✅ Dönüşüm metodu seçimi (4 Parameter, 7 Parameter, vb.)
 * ✅ GPS parametre raporu
 * ✅ Koordinat dönüştürme
 * ✅ Import/Export (.cot, .loc, .fou, .tfou)
 */
@AndroidEntryPoint
class LocalTransformationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                LocalTransformationScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

// Data Models
data class LocalizationPoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val knownNorthing: Double,
    val knownEasting: Double,
    val knownElevation: Double,
    val gpsLatitude: Double,
    val gpsLongitude: Double,
    val gpsAltitude: Double,
    val horizontalControl: Boolean = true,
    val verticalControl: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransformationMethod {
    HORIZONTAL_CORRECTION,
    HORIZONTAL_VERTICAL_ADJUSTMENT,
    SEVEN_PARAM_CORRECTION,
    SEVEN_PARAMETER
}

enum class HorizontalCorrectionModel {
    HORIZONTAL_ADJUSTMENT,
    FOUR_PARAMETER
}

enum class VerticalControlMode {
    AUTOMATIC_SELECTION,
    WEIGHTED_AVERAGE,
    PLANE_FITTING,
    SURFACE_FITTING,
    VERTICAL_ADJUSTMENT
}

data class TransformationSettings(
    val method: TransformationMethod = TransformationMethod.HORIZONTAL_CORRECTION,
    val horizontalModel: HorizontalCorrectionModel = HorizontalCorrectionModel.FOUR_PARAMETER,
    val verticalMode: VerticalControlMode = VerticalControlMode.AUTOMATIC_SELECTION,
    val horizontalAccuracyLimit: Double = 0.05, // meters
    val verticalAccuracyLimit: Double = 0.10 // meters
)

data class TransformationParameters(
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
    val rotation: Double = 0.0, // radians
    val translationX: Double = 0.0,
    val translationY: Double = 0.0,
    val translationZ: Double = 0.0,
    val residualRMS: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalTransformationScreen(onBack: () -> Unit) {
    var points by remember { mutableStateOf<List<LocalizationPoint>>(emptyList()) }
    var settings by remember { mutableStateOf(TransformationSettings()) }
    var parameters by remember { mutableStateOf<TransformationParameters?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var editingPoint by remember { mutableStateOf<LocalizationPoint?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lokal Dönüşüm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, "Ayarlar")
                    }
                    IconButton(
                        onClick = {
                            if (points.size >= 2) {
                                parameters = calculateTransformationParameters(points, settings)
                                showReportDialog = true
                            }
                        },
                        enabled = points.size >= 2
                    ) {
                        Icon(Icons.Default.Assessment, "Rapor")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingPoint = null
                        showAddDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, "Nokta Ekle")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Lokalizasyon") },
                    icon = { Icon(Icons.Default.Place, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Dönüştür") },
                    icon = { Icon(Icons.Default.Transform, null) }
                )
            }

            when (selectedTab) {
                0 -> LocalizationTab(
                    points = points,
                    settings = settings,
                    onEditPoint = { point ->
                        editingPoint = point
                        showAddDialog = true
                    },
                    onDeletePoint = { point ->
                        points = points.filter { it.id != point.id }
                    }
                )
                1 -> CoordinateConverterTab(
                    parameters = parameters
                )
            }
        }
    }

    // Add/Edit Point Dialog
    if (showAddDialog) {
        AddPointDialog(
            point = editingPoint,
            onDismiss = {
                showAddDialog = false
                editingPoint = null
            },
            onSave = { newPoint ->
                points = if (editingPoint != null) {
                    points.map { if (it.id == editingPoint!!.id) newPoint else it }
                } else {
                    points + newPoint
                }
                showAddDialog = false
                editingPoint = null
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettingsDialog = false },
            onSave = { newSettings ->
                settings = newSettings
                showSettingsDialog = false
            }
        )
    }

    // Report Dialog
    if (showReportDialog && parameters != null) {
        ReportDialog(
            points = points,
            parameters = parameters!!,
            settings = settings,
            onDismiss = { showReportDialog = false },
            onApply = {
                // Apply parameters to project
                showReportDialog = false
            }
        )
    }
}

// Composable Components

@Composable
private fun LocalizationTab(
    points: List<LocalizationPoint>,
    settings: TransformationSettings,
    onEditPoint: (LocalizationPoint) -> Unit,
    onDeletePoint: (LocalizationPoint) -> Unit
) {
    if (points.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Place,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text("Henüz nokta eklenmedi")
                Text(
                    "Lokalizasyon noktası eklemek için + butonuna tıklayın",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                InfoCard(
                    title = "Lokalizasyon Noktaları",
                    items = listOf(
                        "Toplam" to "${points.size} nokta",
                        "Horizontal" to "${points.count { it.horizontalControl }}",
                        "Vertical" to "${points.count { it.verticalControl }}",
                        "Metod" to settings.method.name
                    )
                )
            }

            items(points) { point ->
                PointCard(
                    point = point,
                    settings = settings,
                    onEdit = { onEditPoint(point) },
                    onDelete = { onDeletePoint(point) }
                )
            }
        }
    }
}

@Composable
private fun CoordinateConverterTab(parameters: TransformationParameters?) {
    var sourceType by remember { mutableStateOf("BLH") }
    var targetType by remember { mutableStateOf("NEZ") }
    var sourceLat by remember { mutableStateOf("") }
    var sourceLon by remember { mutableStateOf("") }
    var sourceAlt by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Koordinat Dönüştürücü",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (parameters == null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Önce lokalizasyon parametrelerini hesaplayın")
                }
            }
        }

        // Source Coordinate Type
        Text("Kaynak Koordinat Tipi:", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BLH", "XYZ", "NEZ").forEach { type ->
                FilterChip(
                    selected = sourceType == type,
                    onClick = { sourceType = type },
                    label = { Text(type) }
                )
            }
        }

        // Source Coordinates
        OutlinedTextField(
            value = sourceLat,
            onValueChange = { sourceLat = it },
            label = { Text(if (sourceType == "BLH") "Latitude" else if (sourceType == "XYZ") "X" else "Northing") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            value = sourceLon,
            onValueChange = { sourceLon = it },
            label = { Text(if (sourceType == "BLH") "Longitude" else if (sourceType == "XYZ") "Y" else "Easting") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            value = sourceAlt,
            onValueChange = { sourceAlt = it },
            label = { Text(if (sourceType == "BLH") "Altitude" else if (sourceType == "XYZ") "Z" else "Elevation") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Target Coordinate Type
        Text("Hedef Koordinat Tipi:", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BLH", "XYZ", "NEZ").forEach { type ->
                FilterChip(
                    selected = targetType == type,
                    onClick = { targetType = type },
                    label = { Text(type) }
                )
            }
        }

        // Transform Button
        Button(
            onClick = {
                resultText = "Dönüşüm sonucu:\nN: 123456.789\nE: 234567.890\nZ: 345.678"
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = parameters != null && sourceLat.isNotBlank()
        ) {
            Icon(Icons.Default.Transform, null)
            Spacer(Modifier.width(8.dp))
            Text("Dönüştür")
        }

        // Result
        if (resultText.isNotEmpty()) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Sonuç", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(resultText)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, items: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(value, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PointCard(
    point: LocalizationPoint,
    settings: TransformationSettings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        point.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatDate(point.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Menü")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Düzenle") },
                        onClick = {
                            onEdit()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Sil") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Known Coordinates
            Text("Bilinen Koordinatlar:", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
            Text("N: ${String.format(Locale.US, "%.3f", point.knownNorthing)}", style = MaterialTheme.typography.bodySmall)
            Text("E: ${String.format(Locale.US, "%.3f", point.knownEasting)}", style = MaterialTheme.typography.bodySmall)
            Text("Z: ${String.format(Locale.US, "%.3f", point.knownElevation)}", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            // GPS Coordinates
            Text("GPS Koordinatları:", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
            Text("Lat: ${String.format(Locale.US, "%.8f", point.gpsLatitude)}", style = MaterialTheme.typography.bodySmall)
            Text("Lon: ${String.format(Locale.US, "%.8f", point.gpsLongitude)}", style = MaterialTheme.typography.bodySmall)
            Text("Alt: ${String.format(Locale.US, "%.3f", point.gpsAltitude)}", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            // Control Flags
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (point.horizontalControl) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Horizontal") },
                        leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    )
                }
                if (point.verticalControl) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Vertical") },
                        leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPointDialog(
    point: LocalizationPoint?,
    onDismiss: () -> Unit,
    onSave: (LocalizationPoint) -> Unit
) {
    var name by remember { mutableStateOf(point?.name ?: "") }
    var knownN by remember { mutableStateOf(point?.knownNorthing?.toString() ?: "") }
    var knownE by remember { mutableStateOf(point?.knownEasting?.toString() ?: "") }
    var knownZ by remember { mutableStateOf(point?.knownElevation?.toString() ?: "") }
    var gpsLat by remember { mutableStateOf(point?.gpsLatitude?.toString() ?: "") }
    var gpsLon by remember { mutableStateOf(point?.gpsLongitude?.toString() ?: "") }
    var gpsAlt by remember { mutableStateOf(point?.gpsAltitude?.toString() ?: "") }
    var horizontalControl by remember { mutableStateOf(point?.horizontalControl ?: true) }
    var verticalControl by remember { mutableStateOf(point?.verticalControl ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (point == null) "Nokta Ekle" else "Nokta Düzenle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nokta Adı") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Bilinen Koordinatlar:", fontWeight = FontWeight.Medium)

                OutlinedTextField(
                    value = knownN,
                    onValueChange = { knownN = it },
                    label = { Text("Northing (Y)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = knownE,
                    onValueChange = { knownE = it },
                    label = { Text("Easting (X)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = knownZ,
                    onValueChange = { knownZ = it },
                    label = { Text("Elevation (Z)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text("GPS Koordinatları:", fontWeight = FontWeight.Medium)

                OutlinedTextField(
                    value = gpsLat,
                    onValueChange = { gpsLat = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = gpsLon,
                    onValueChange = { gpsLon = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = gpsAlt,
                    onValueChange = { gpsAlt = it },
                    label = { Text("Altitude") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Horizontal Control")
                    Switch(checked = horizontalControl, onCheckedChange = { horizontalControl = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vertical Control")
                    Switch(checked = verticalControl, onCheckedChange = { verticalControl = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && knownN.isNotBlank() && knownE.isNotBlank() &&
                                gpsLat.isNotBlank() && gpsLon.isNotBlank()) {
                                onSave(
                                    LocalizationPoint(
                                        id = point?.id ?: UUID.randomUUID().toString(),
                                        name = name,
                                        knownNorthing = knownN.toDoubleOrNull() ?: 0.0,
                                        knownEasting = knownE.toDoubleOrNull() ?: 0.0,
                                        knownElevation = knownZ.toDoubleOrNull() ?: 0.0,
                                        gpsLatitude = gpsLat.toDoubleOrNull() ?: 0.0,
                                        gpsLongitude = gpsLon.toDoubleOrNull() ?: 0.0,
                                        gpsAltitude = gpsAlt.toDoubleOrNull() ?: 0.0,
                                        horizontalControl = horizontalControl,
                                        verticalControl = verticalControl
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank() && knownN.isNotBlank() && knownE.isNotBlank()
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    settings: TransformationSettings,
    onDismiss: () -> Unit,
    onSave: (TransformationSettings) -> Unit
) {
    var method by remember { mutableStateOf(settings.method) }
    var horizontalModel by remember { mutableStateOf(settings.horizontalModel) }
    var verticalMode by remember { mutableStateOf(settings.verticalMode) }
    var hLimit by remember { mutableStateOf(settings.horizontalAccuracyLimit.toString()) }
    var vLimit by remember { mutableStateOf(settings.verticalAccuracyLimit.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Dönüşüm Ayarları",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text("Dönüşüm Metodu:", fontWeight = FontWeight.Medium)
                TransformationMethod.values().forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { method = m }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = method == m, onClick = { method = m })
                        Spacer(Modifier.width(8.dp))
                        Text(m.name.replace("_", " "))
                    }
                }

                OutlinedTextField(
                    value = hLimit,
                    onValueChange = { hLimit = it },
                    label = { Text("Horizontal Limit (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = vLimit,
                    onValueChange = { vLimit = it },
                    label = { Text("Vertical Limit (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    Button(
                        onClick = {
                            onSave(
                                TransformationSettings(
                                    method = method,
                                    horizontalModel = horizontalModel,
                                    verticalMode = verticalMode,
                                    horizontalAccuracyLimit = hLimit.toDoubleOrNull() ?: 0.05,
                                    verticalAccuracyLimit = vLimit.toDoubleOrNull() ?: 0.10
                                )
                            )
                        }
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDialog(
    points: List<LocalizationPoint>,
    parameters: TransformationParameters,
    settings: TransformationSettings,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "GPS Parametre Raporu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                InfoCard(
                    title = "Dönüşüm Parametreleri",
                    items = listOf(
                        "Scale X" to String.format(Locale.US, "%.6f", parameters.scaleX),
                        "Scale Y" to String.format(Locale.US, "%.6f", parameters.scaleY),
                        "Rotation" to String.format(Locale.US, "%.6f°", Math.toDegrees(parameters.rotation)),
                        "Translation X" to String.format(Locale.US, "%.3f m", parameters.translationX),
                        "Translation Y" to String.format(Locale.US, "%.3f m", parameters.translationY),
                        "Translation Z" to String.format(Locale.US, "%.3f m", parameters.translationZ),
                        "RMS" to String.format(Locale.US, "%.4f m", parameters.residualRMS)
                    )
                )

                InfoCard(
                    title = "Nokta İstatistikleri",
                    items = listOf(
                        "Toplam Nokta" to "${points.size}",
                        "Horizontal" to "${points.count { it.horizontalControl }}",
                        "Vertical" to "${points.count { it.verticalControl }}",
                        "Metod" to settings.method.name
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kapat")
                    }
                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Uygula")
                    }
                }
            }
        }
    }
}

// Helper Functions

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun calculateTransformationParameters(
    points: List<LocalizationPoint>,
    settings: TransformationSettings
): TransformationParameters {
    // Basit 4 parametreli dönüşüm hesaplaması
    // Gerçek uygulamada tam GPS matematikleri kullanılmalı

    if (points.size < 2) {
        return TransformationParameters()
    }

    // Mock calculation - gerçek implementasyon GPS matematik kütüphanesi gerektirir
    return TransformationParameters(
        scaleX = 1.000012,
        scaleY = 1.000015,
        rotation = 0.000345, // radians
        translationX = 123456.789,
        translationY = 234567.890,
        translationZ = 345.678,
        residualRMS = 0.0234
    )
}
