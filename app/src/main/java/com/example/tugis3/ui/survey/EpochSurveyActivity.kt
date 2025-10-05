package com.example.tugis3.ui.survey

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.sqrt

@AndroidEntryPoint
class EpochSurveyActivity : ComponentActivity() {
    private val vm: EpochSurveyViewModel by viewModels()
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) vm.startEngine() else finish()
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensurePermissions()
        setContent { Tugis3Theme { EpochSurveyScreen(onBack = { finish() }) } }
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
private fun EpochSurveyScreen(onBack: () -> Unit, vm: EpochSurveyViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val st by vm.state.collectAsState()
    var epochCountText by remember(st.config.targetEpochCount) { mutableStateOf(st.config.targetEpochCount.toString()) }
    var pointNameText by remember(st.config.pointName) { mutableStateOf(st.config.pointName) }
    var pointCodeText by remember(st.config.pointCode) { mutableStateOf(st.config.pointCode ?: "") }
    var pdopText by remember(st.config.maxPdop) { mutableStateOf(st.config.maxPdop?.let { String.format(Locale.US, "%.1f", it) } ?: "") }
    var hrmsText by remember(st.config.maxHrms) { mutableStateOf(st.config.maxHrms?.let { String.format(Locale.US, "%.03f", it) } ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Epoch Ölçüm") },
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
                // GNSS Status
                Card(colors = CardDefaults.cardColors(
                    containerColor = when {
                        st.observation?.fixType?.isRtk() == true -> MaterialTheme.colorScheme.primaryContainer
                        st.observation?.fixType?.isDifferential() == true -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(st.observation?.fixType?.name ?: "-", fontWeight = FontWeight.Bold); Text("Uydu: ${st.observation?.satellitesInUse ?: 0}") }
                        Column { Text("PDOP: ${st.observation?.pdop?.let { String.format(Locale.US, "%.1f", it) } ?: "-"}")
                            Text("HRMS: ${st.observation?.hrms?.let { String.format(Locale.US, "%.02f", it) } ?: "-"}") }
                    }
                }
            }
            item {
                // Point Setup
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nokta Bilgileri", fontWeight = FontWeight.Bold)
                    OutlinedTextField(pointNameText, {
                        pointNameText = it; vm.setPointName(it)
                    }, label = { Text("Nokta Adı") }, singleLine = true, enabled = !st.isRecording && !st.isCompleted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(pointCodeText, {
                            pointCodeText = it; vm.setPointCode(it)
                        }, label = { Text("Kod") }, singleLine = true, modifier = Modifier.weight(1f), enabled = !st.isRecording && !st.isCompleted)
                        OutlinedTextField(epochCountText, {
                            epochCountText = it; it.toIntOrNull()?.let(vm::setTargetEpochCount)
                        }, label = { Text("Epoch Sayısı") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f), enabled = !st.isRecording && !st.isCompleted)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(pdopText, {
                            pdopText = it; vm.setMaxPdop(it.toDoubleOrNull())
                        }, label = { Text("Max PDOP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f), enabled = !st.isRecording && !st.isCompleted)
                        OutlinedTextField(hrmsText, {
                            hrmsText = it; vm.setMaxHrms(it.toDoubleOrNull())
                        }, label = { Text("Max HRMS (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f), enabled = !st.isRecording && !st.isCompleted)
                    }
                } }
            }
            item {
                // Progress & Control
                Card(colors = CardDefaults.cardColors(
                    containerColor = when {
                        st.isCompleted -> MaterialTheme.colorScheme.primaryContainer
                        st.isRecording -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    st.isCompleted -> "Ölçüm Tamamlandı"
                                    st.isRecording -> "Epoch Alımı"
                                    else -> st.status
                                }, fontWeight = FontWeight.Bold
                            )
                            Text("${st.currentEpoch}/${st.config.targetEpochCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = if (st.config.targetEpochCount>0) st.currentEpoch.toFloat()/st.config.targetEpochCount else 0f, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        when {
                            st.isRecording -> Button(onClick = vm::stop, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Stop,null); Spacer(Modifier.width(4.dp)); Text("Durdur") }
                            st.isCompleted -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { vm.setTargetEpochCount(st.config.targetEpochCount); /* reset by new start */ vm.start() }, modifier = Modifier.weight(1f)) { Text("Tekrar") }
                                Button(onClick = vm::saveAveragedPoint, enabled = !st.saved, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save,null); Spacer(Modifier.width(4.dp)); Text(if (st.saved) "Kaydedildi" else "Kaydet") }
                            }
                            else -> Button(onClick = vm::start, enabled = st.canStart, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow,null); Spacer(Modifier.width(4.dp)); Text("Başla") }
                        }
                        if (!st.canStart && !st.isRecording && !st.isCompleted) {
                            Text(st.status, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                if (st.samples.isNotEmpty()) {
                    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("İstatistikler", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Avg Lat:"); Text(st.avgLat?.let { String.format(Locale.US, "%.7f", it) } ?: "-") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Avg Lon:"); Text(st.avgLon?.let { String.format(Locale.US, "%.7f", it) } ?: "-") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Avg EllH:"); Text(st.avgEllH?.let { String.format(Locale.US, "%.3f", it) } ?: "-") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Horiz RMS:"); Text(st.horizontalRms?.let { String.format(Locale.US, "%.5f°", it) } ?: "-") }
                    } }
                }
            }
            item {
                if (st.samples.size >= 2) {
                    Card { Column(Modifier.padding(16.dp)) {
                        Text("Epoch Sapmalar (derece bazında)", fontWeight = FontWeight.Bold)
                        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                            val margin = 20f
                            val w = size.width - 2*margin
                            val h = size.height - 2*margin
                            val sx = if (st.samples.size>1) w/(st.samples.size-1) else w
                            val avgLat = st.avgLat ?: return@Canvas
                            val avgLon = st.avgLon ?: return@Canvas
                            val deviations = st.samples.map { sample ->
                                val dLat = sample.lat - avgLat
                                val dLon = sample.lon - avgLon
                                sqrt(dLat*dLat + dLon*dLon)
                            }
                            val maxDev = deviations.maxOrNull() ?: 1e-6
                            for (i in 1 until deviations.size) {
                                val x1 = margin + (i-1)*sx
                                val x2 = margin + i*sx
                                val y1 = size.height - margin - (deviations[i-1]/maxDev * h).toFloat()
                                val y2 = size.height - margin - (deviations[i]/maxDev * h).toFloat()
                                drawLine(Color.Blue, Offset(x1,y1), Offset(x2,y2), strokeWidth = 3f)
                                drawCircle(if (deviations[i] <= maxDev*0.2) Color.Green else Color.Yellow, 4f, Offset(x2,y2))
                            }
                            // first point
                            val y0 = size.height - margin - (deviations.first()/maxDev * h).toFloat()
                            drawCircle(if (deviations.first() <= maxDev*0.2) Color.Green else Color.Yellow, 4f, Offset(margin,y0))
                        }
                        Text("🟢 düşük sapma  🟡 yüksek sapma", style = MaterialTheme.typography.bodySmall)
                    } }
                }
            }
        }
    }
}
