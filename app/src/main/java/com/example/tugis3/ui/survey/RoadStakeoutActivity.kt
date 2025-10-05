package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.abs

// Lokal slope point veri sınıfı (yalnızca UI listesi için)
data class SlopePoint(
    val description: String,
    val northing: Double,
    val easting: Double,
    val elevation: Double
)

@AndroidEntryPoint
class RoadStakeoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                RoadStakeoutScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadStakeoutScreen(onBackPressed: () -> Unit, vm: RoadStakeoutViewModel = hiltViewModel()) {
    val uiState = vm.uiState.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yol Aplikasyonu") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Yol Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = uiState.roadName,
                            onValueChange = { vm.updateRoadName(it) },
                            label = { Text("Yol Adı") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text("Mod: ${uiState.selectedMode}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tasarım Parametreleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Text("Profil Aralığı: ${uiState.stationInterval} m")
                        Text("Yol Genişliği: ${uiState.roadWidth} m")
                        Text("Kazı Şevi: 1:${uiState.cutSlope}")
                        Text("Dolgu Şevi: 1:${uiState.fillSlope}")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if ((uiState.lateralOffset?.let { kotlin.math.abs(it) } ?: 999.0) < 0.5) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mevcut Konum Durumu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        uiState.nearestStation?.let { station ->
                            Text("En Yakın KM: ${String.format(Locale.US, "%.3f", station.km)}")
                            uiState.lateralOffset?.let { Text("Yola Lateral: ${String.format(Locale.US, "%.3f m", it)}") }
                            uiState.deltaElevation?.let { Text("Kot Δ: ${String.format(Locale.US, "%.3f m", it)}") }
                            Text("Tasarım Kotu: ${String.format(Locale.US, "%.3f m", station.elevation)}")

                            val within = (uiState.lateralOffset?.let { kotlin.math.abs(it) } ?: 9.0) < 0.1 && (uiState.deltaElevation?.let { kotlin.math.abs(it) } ?: 9.0) < 0.05
                            if (within) {
                                Spacer(Modifier.height(8.dp))
                                Text("✅ TASARIM ÜZERİNDESİNİZ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Yol Profili", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        uiState.roadPoints.forEach { station ->
                            val isNearby = station == uiState.nearestStation
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isNearby) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(String.format(Locale.US, "KM %.3f", station.km), fontWeight = FontWeight.Medium)
                                    Text(String.format(Locale.US, "N: %.1f", station.northing))
                                    Text(String.format(Locale.US, "E: %.1f", station.easting))
                                    Text(String.format(Locale.US, "Z: %.3f", station.elevation))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.generateSyntheticAlignment() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Upload, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Tasarım Üret")
                    }

                    Button(
                        onClick = { vm.saveStakePoint() },
                        modifier = Modifier.weight(1f),
                        enabled = (uiState.lateralOffset?.let { kotlin.math.abs(it) } ?: 9.0) < 0.1
                    ) {
                        Icon(Icons.Filled.Save, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

// Not: Geometri fonksiyonları ViewModel içine entegre edilmiştir.
