package com.example.tugis3.ui.survey

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class StaticSurveyActivity : ComponentActivity() {
    private val vm: StaticSurveyViewModel by viewModels()
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) vm.startEngine() else finish()
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensurePermissions()
        setContent { Tugis3Theme { StaticSurveyScreen(onBack = { finish() }) } }
    }
    private fun ensurePermissions() {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        }
        val ask = needed.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (ask.isNotEmpty()) permissionLauncher.launch(ask.toTypedArray()) else vm.startEngine()
    }
    override fun onDestroy() { vm.stopEngine(); super.onDestroy() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaticSurveyScreen(onBack: () -> Unit, vm: StaticSurveyViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val st by vm.state.collectAsState()
    var pdopText by remember(st.config.pdopLimit) { mutableStateOf(String.format(Locale.US, "%.1f", st.config.pdopLimit)) }
    var elevMaskText by remember(st.config.elevationMask) { mutableStateOf(st.config.elevationMask.toString()) }
    var antHeightText by remember(st.config.antennaHeight) { mutableStateOf(String.format(Locale.US, "%.2f", st.config.antennaHeight)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statik Ölçüm") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // GNSS Durum
                Card(colors = CardDefaults.cardColors(
                    containerColor = when {
                        st.observation?.fixType?.isRtk() == true -> MaterialTheme.colorScheme.primaryContainer
                        st.observation?.fixType?.isDifferential() == true -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )) {
                    Column(Modifier.padding(16.dp)) {
                        Text("GNSS Durumu", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fix:"); Text(st.observation?.fixType?.name ?: "-")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Uydu:"); Text("${st.observation?.satellitesInUse ?: 0}/${st.observation?.satellitesVisible ?: 0}")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PDOP:"); Text(st.observation?.pdop?.let { String.format(Locale.US, "%.1f", it) } ?: "-")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("HRMS:"); Text(st.observation?.hrms?.let { String.format(Locale.US, "%.02f", it) } ?: "-")
                        }
                    }
                }
            }
            item {
                // Konfigürasyon
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ayarlar", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = st.config.pointName,
                        onValueChange = vm::setPointName,
                        label = { Text("Nokta Adı") },
                        singleLine = true,
                        enabled = !st.isRecording
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = pdopText,
                            onValueChange = {
                                pdopText = it; it.toDoubleOrNull()?.let(vm::setPdopLimit)
                            },
                            label = { Text("PDOP Limit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            enabled = !st.isRecording
                        )
                        OutlinedTextField(
                            value = elevMaskText,
                            onValueChange = {
                                elevMaskText = it; it.toIntOrNull()?.let(vm::setElevationMask)
                            },
                            label = { Text("Mask (°)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            enabled = !st.isRecording
                        )
                    }
                    OutlinedTextField(
                        value = antHeightText,
                        onValueChange = { antHeightText = it; it.toDoubleOrNull()?.let(vm::setAntennaHeight) },
                        label = { Text("Anten Yükseği (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !st.isRecording,
                        modifier = Modifier.fillMaxWidth()
                    )
                } }
            }
            item {
                // Kayıt Durumu
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (st.isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Statik Kayıt", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Süre:"); Text("${st.elapsedSec}s")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Epoch:"); Text(st.epochs.size.toString())
                        }
                        Spacer(Modifier.height(8.dp))
                        if (st.isRecording) {
                            Button(onClick = vm::stopRecording, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Default.Stop, null); Spacer(Modifier.width(4.dp)); Text("Durdur")
                            }
                        } else {
                            Button(onClick = vm::startRecording, enabled = st.canStart, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Başlat")
                            }
                            if (!st.canStart) {
                                Text(st.status, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item {
                if (st.epochs.isNotEmpty()) {
                    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("İstatistikler", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Avg Lat:"); Text(st.avgLat?.let { String.format(Locale.US, "%.7f", it) } ?: "-") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Avg Lon:"); Text(st.avgLon?.let { String.format(Locale.US, "%.7f", it) } ?: "-") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Avg EllH:"); Text(st.avgEllH?.let { String.format(Locale.US, "%.3f", it) } ?: "-") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("HRMS Avg:"); Text(st.hrmsAvg?.let { String.format(Locale.US, "%.02f", it) } ?: "-") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("HRMS RMS:"); Text(st.hrmsRms?.let { String.format(Locale.US, "%.02f", it) } ?: "-") }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.stopRecording(); }, enabled = st.isRecording, modifier = Modifier.weight(1f)) { Text("Dur") }
                            Button(onClick = vm::saveAveragedPoint, enabled = !st.isRecording && st.epochs.isNotEmpty() && !st.saved, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text(if (st.saved) "Kaydedildi" else "Kaydet") }
                        }
                        if (st.saved) Text("✅ Nokta kaydedildi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    } }
                }
            }
        }
    }
}
