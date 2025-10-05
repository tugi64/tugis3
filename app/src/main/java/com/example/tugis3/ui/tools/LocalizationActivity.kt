package com.example.tugis3.ui.tools

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class LocalizationActivity : ComponentActivity() {
    private val vm: LocalizationViewModel by viewModels()
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) vm.startEngine() else finish()
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensurePermissions()
        setContent { LocalizationScreen(onBack = { finish() }) }
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
private fun LocalizationScreen(onBack: () -> Unit, vm: LocalizationViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val st by vm.state.collectAsState()
    var northText by remember(st.dstNorthText) { mutableStateOf(st.dstNorthText) }
    var eastText by remember(st.dstEastText) { mutableStateOf(st.dstEastText) }
    var weightText by remember(st.weightText) { mutableStateOf(st.weightText) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lokalizasyon") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (!st.solving) {
                        IconButton(onClick = vm::solve, enabled = st.points.count { it.entity.include == 1 } >= 2) {
                            Icon(Icons.Default.Refresh, contentDescription = "Çöz")
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
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
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GNSS Durumu", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fix:"); Text(st.observationFix ?: "-") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PDOP:"); Text(st.pdop?.let { String.format(Locale.US, "%.1f", it) } ?: "-") }
                    Text(st.status, color = when {
                        st.status.contains("Hata") -> MaterialTheme.colorScheme.error
                        st.status == "Hazır" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }, style = MaterialTheme.typography.bodySmall)
                } }
            }
            item {
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kontrol Noktası Girişi", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = northText,
                            onValueChange = { northText = it; vm.setDstNorth(it) },
                            label = { Text("Hedef Northing") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = eastText,
                            onValueChange = { eastText = it; vm.setDstEast(it) },
                            label = { Text("Hedef Easting") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it; vm.setWeight(it) },
                            label = { Text("Ağırlık") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { vm.addPoint() }, enabled = st.canAdd, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Ekle") }
                    }
                } }
            }
            if (st.solveResult != null) {
                item {
                    val r = st.solveResult!!
                    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Çözüm", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Scale:"); Text(String.format(Locale.US, "%.8f", r.scale)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Rot (°):"); Text(String.format(Locale.US, "%.6f", r.rotDeg)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tx:"); Text(String.format(Locale.US, "%.3f", r.tx)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Ty:"); Text(String.format(Locale.US, "%.3f", r.ty)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("RMS:"); Text(String.format(Locale.US, "%.3f", r.rms)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Nokta:"); Text(r.pointCount.toString()) }
                    } }
                }
            }
            item {
                Text("Kalibrasyon Noktaları", style = MaterialTheme.typography.titleMedium)
            }
            items(st.points, key = { it.entity.id }) { p ->
                Card { Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "SRC N: %.3f E: %.3f".format(Locale.US, p.entity.srcNorth, p.entity.srcEast),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "DST N: %.3f E: %.3f".format(Locale.US, p.entity.dstNorth, p.entity.dstEast),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (p.residualE != null && p.residualN != null) {
                            Text(
                                "dE=%.003f dN=%.003f".format(Locale.US, p.residualE, p.residualN),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = p.entity.include == 1, onCheckedChange = { vm.toggleInclude(p.entity) })
                            IconButton(onClick = { vm.deletePoint(p.entity) }) { Icon(Icons.Default.Delete, contentDescription = null) }
                        }
                        Text("w=%.2f".format(Locale.US, p.entity.weight), style = MaterialTheme.typography.labelSmall)
                    }
                } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
