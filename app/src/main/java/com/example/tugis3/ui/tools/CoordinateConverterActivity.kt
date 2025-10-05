package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack as _Ignored // to avoid unused removal if wildcard present
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.cos

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class CoordinateConverterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CoordinateConverterScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinateConverterScreen(onBackPressed: () -> Unit) {
    var inputLat by remember { mutableStateOf("") }
    var inputLon by remember { mutableStateOf("") }
    var inputNorthing by remember { mutableStateOf("") }
    var inputEasting by remember { mutableStateOf("") }
    var inputZone by remember { mutableStateOf("35") }
    var selectedInputType by remember { mutableStateOf("Geographic") }
    var selectedOutputType by remember { mutableStateOf("UTM") }

    var outputLat by remember { mutableStateOf("") }
    var outputLon by remember { mutableStateOf("") }
    var outputNorthing by remember { mutableStateOf("") }
    var outputEasting by remember { mutableStateOf("") }
    var outputZone by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koordinat Dönüşümü") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Koordinat Sistemi Seçimi
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Koordinat Sistemi Seçimi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Girdi:")
                        CoordinateSystemDropdown(
                            selectedValue = selectedInputType,
                            options = listOf("Geographic (Lat/Lon)", "UTM", "TM30", "TM3", "ED50"),
                            onValueChange = { selectedInputType = it }
                        )
                    }

                    IconButton(
                        onClick = {
                            val temp = selectedInputType
                            selectedInputType = selectedOutputType
                            selectedOutputType = temp
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Değiştir")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Çıktı:")
                        CoordinateSystemDropdown(
                            selectedValue = selectedOutputType,
                            options = listOf("Geographic (Lat/Lon)", "UTM", "TM30", "TM3", "ED50"),
                            onValueChange = { selectedOutputType = it }
                        )
                    }
                }
            }

            // Girdi Koordinatları
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Girdi Koordinatları ($selectedInputType)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedInputType.contains("Geographic")) {
                        OutlinedTextField(
                            value = inputLat,
                            onValueChange = { inputLat = it },
                            label = { Text("Enlem (Latitude)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("39.123456") }
                        )
                        OutlinedTextField(
                            value = inputLon,
                            onValueChange = { inputLon = it },
                            label = { Text("Boylam (Longitude)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("32.123456") }
                        )
                    } else {
                        OutlinedTextField(
                            value = inputNorthing,
                            onValueChange = { inputNorthing = it },
                            label = { Text("Kuzey (Northing)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("4330000.00") }
                        )
                        OutlinedTextField(
                            value = inputEasting,
                            onValueChange = { inputEasting = it },
                            label = { Text("Doğu (Easting)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("355000.00") }
                        )
                        if (selectedInputType.contains("UTM")) {
                            OutlinedTextField(
                                value = inputZone,
                                onValueChange = { inputZone = it },
                                label = { Text("UTM Zone") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("35") }
                            )
                        }
                    }
                }
            }

            // Dönüştür Butonu
            Button(
                onClick = {
                    // Koordinat dönüşüm algoritması burada çalışacak
                    performCoordinateConversion(
                        selectedInputType, selectedOutputType,
                        inputLat, inputLon, inputNorthing, inputEasting, inputZone
                    ) { lat, lon, north, east, zone ->
                        outputLat = lat
                        outputLon = lon
                        outputNorthing = north
                        outputEasting = east
                        outputZone = zone
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dönüştür")
            }

            // Çıktı Koordinatları
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Çıktı Koordinatları ($selectedOutputType)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedOutputType.contains("Geographic")) {
                        OutlinedTextField(
                            value = outputLat,
                            onValueChange = { },
                            label = { Text("Enlem (Latitude)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                        OutlinedTextField(
                            value = outputLon,
                            onValueChange = { },
                            label = { Text("Boylam (Longitude)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                    } else {
                        OutlinedTextField(
                            value = outputNorthing,
                            onValueChange = { },
                            label = { Text("Kuzey (Northing)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                        OutlinedTextField(
                            value = outputEasting,
                            onValueChange = { },
                            label = { Text("Doğu (Easting)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                        )
                        if (selectedOutputType.contains("UTM") && outputZone.isNotEmpty()) {
                            OutlinedTextField(
                                value = outputZone,
                                onValueChange = { },
                                label = { Text("UTM Zone") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoordinateSystemDropdown(
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            label = { Text("Koordinat Sistemi") }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Basit koordinat dönüşüm algoritması (gerçek implementasyon için geodesy kütüphanesi kullanılmalı)
private fun Double.toRadians(): Double = this * Math.PI / 180.0

private fun performCoordinateConversion(
    inputType: String,
    outputType: String,
    lat: String,
    lon: String,
    north: String,
    east: String,
    zone: String,
    onResult: (String, String, String, String, String) -> Unit
) {
    try {
        if (inputType.contains("Geographic") && outputType.contains("UTM")) {
            val latVal = lat.toDoubleOrNull() ?: 0.0
            val lonVal = lon.toDoubleOrNull() ?: 0.0

            // Basit UTM dönüşümü (yaklaşık)
            val utmZone = ((lonVal + 180) / 6).toInt() + 1
            val northing = (latVal * 111320).toString()
            val easting = (lonVal * 111320 * cos(Math.toRadians(latVal))).toString()

            onResult("", "", northing, easting, utmZone.toString())
        } else if (inputType.contains("UTM") && outputType.contains("Geographic")) {
            val northVal = north.toDoubleOrNull() ?: 0.0
            val eastVal = east.toDoubleOrNull() ?: 0.0

            // Basit Geographic dönüşümü (yaklaşık)
            val latitude = (northVal / 111320).toString()
            val longitude = (eastVal / (111320 * cos(Math.toRadians(northVal / 111320)))).toString()

            onResult(latitude, longitude, "", "", "")
        } else {
            // Aynı sistem - kopyala
            onResult(lat, lon, north, east, zone)
        }
    } catch (e: Exception) {
        onResult("Hata", "Hata", "Hata", "Hata", "Hata")
    }
}
