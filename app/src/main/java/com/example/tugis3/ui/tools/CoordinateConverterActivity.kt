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
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.coord.transform.UtmTransformer
import com.example.tugis3.prefs.CoordPrefs

private fun parseDouble(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

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
    val context = LocalContext.current
    val coordOrder by CoordPrefs.orderFlow(context).collectAsState(initial = CoordPrefs.CoordOrder.Y_FIRST)
    val axisMap by CoordPrefs.axisMapFlow(context).collectAsState(initial = CoordPrefs.AxisMap.EASTING_TO_Y_NORTHING_TO_X)
    val eLabel = if (axisMap == CoordPrefs.AxisMap.EASTING_TO_Y_NORTHING_TO_X) "Y (Doğu/Easting)" else "X (Doğu/Easting)"
    val nLabel = if (axisMap == CoordPrefs.AxisMap.EASTING_TO_Y_NORTHING_TO_X) "X (Kuzey/Northing)" else "Y (Kuzey/Northing)"

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
                        if (coordOrder == CoordPrefs.CoordOrder.X_FIRST) {
                            OutlinedTextField(
                                value = inputEasting,
                                onValueChange = { inputEasting = it },
                                label = { Text(eLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = { Text("355000.00") }
                            )
                            OutlinedTextField(
                                value = inputNorthing,
                                onValueChange = { inputNorthing = it },
                                label = { Text(nLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = { Text("4330000.00") }
                            )
                        } else {
                            OutlinedTextField(
                                value = inputNorthing,
                                onValueChange = { inputNorthing = it },
                                label = { Text(nLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = { Text("4330000.00") }
                            )
                            OutlinedTextField(
                                value = inputEasting,
                                onValueChange = { inputEasting = it },
                                label = { Text(eLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = { Text("355000.00") }
                            )
                        }
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
                        if (coordOrder == CoordPrefs.CoordOrder.X_FIRST) {
                            OutlinedTextField(
                                value = outputEasting,
                                onValueChange = { },
                                label = { Text(eLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true
                            )
                            OutlinedTextField(
                                value = outputNorthing,
                                onValueChange = { },
                                label = { Text(nLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true
                            )
                        } else {
                            OutlinedTextField(
                                value = outputNorthing,
                                onValueChange = { },
                                label = { Text(nLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true
                            )
                            OutlinedTextField(
                                value = outputEasting,
                                onValueChange = { },
                                label = { Text(eLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true
                            )
                        }
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
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
        // Ellipsoid: ITRF96 / GRS80 varsayılanı
        val a = 6378137.0
        val invF = 298.257222101

        fun tm30Transformer() = ProjectionEngine.createTransverseMercator(
            semiMajor = a,
            invFlattening = invF,
            centralMeridianDeg = 30.0,
            latOriginDeg = 0.0,
            scaleFactor = 1.0,
            falseE = 500000.0,
            falseN = 0.0
        )

        when {
            inputType.contains("Geographic") && outputType.contains("UTM") -> {
                val latVal = parseDouble(lat) ?: error("Geçersiz enlem")
                val lonVal = parseDouble(lon) ?: error("Geçersiz boylam")
                val zoneInt = if (zone.isNotBlank()) zone.trim().toIntOrNull() else null
                val utmZone = zoneInt ?: (((lonVal + 180.0) / 6.0).toInt() + 1)
                val tf = UtmTransformer(utmZone, true, a, invF)
                val (easting, northing) = tf.forward(latVal, lonVal)
                onResult("", "", northing.toString(), easting.toString(), utmZone.toString())
            }
            inputType.contains("UTM") && outputType.contains("Geographic") -> {
                val northVal = parseDouble(north) ?: error("Geçersiz northing")
                val eastVal = parseDouble(east) ?: error("Geçersiz easting")
                val utmZone = zone.trim().toIntOrNull() ?: error("UTM zone gerekli")
                val tf = UtmTransformer(utmZone, true, a, invF)
                val (latDeg, lonDeg) = tf.inverse(eastVal, northVal)
                onResult(latDeg.toString(), lonDeg.toString(), "", "", "")
            }
            inputType.contains("Geographic") && outputType.contains("TM30") -> {
                val latVal = parseDouble(lat) ?: error("Geçersiz enlem")
                val lonVal = parseDouble(lon) ?: error("Geçersiz boylam")
                val tf = tm30Transformer()
                val (easting, northing) = tf.forward(latVal, lonVal)
                onResult("", "", northing.toString(), easting.toString(), "")
            }
            inputType.contains("TM30") && outputType.contains("Geographic") -> {
                val northVal = parseDouble(north) ?: error("Geçersiz northing")
                val eastVal = parseDouble(east) ?: error("Geçersiz easting")
                val tf = tm30Transformer()
                val (latDeg, lonDeg) = tf.inverse(eastVal, northVal)
                onResult(latDeg.toString(), lonDeg.toString(), "", "", "")
            }
            inputType.contains("TM30") && outputType.contains("UTM") -> {
                val northVal = parseDouble(north) ?: error("Geçersiz northing")
                val eastVal = parseDouble(east) ?: error("Geçersiz easting")
                val tm = tm30Transformer()
                val (latDeg, lonDeg) = tm.inverse(eastVal, northVal)
                val zoneInt = if (zone.isNotBlank()) zone.trim().toIntOrNull() else null
                val utmZone = zoneInt ?: (((lonDeg + 180.0) / 6.0).toInt() + 1)
                val utm = UtmTransformer(utmZone, true, a, invF)
                val (easting, northing) = utm.forward(latDeg, lonDeg)
                onResult("", "", northing.toString(), easting.toString(), utmZone.toString())
            }
            inputType.contains("UTM") && outputType.contains("TM30") -> {
                val northVal = parseDouble(north) ?: error("Geçersiz northing")
                val eastVal = parseDouble(east) ?: error("Geçersiz easting")
                val utmZone = zone.trim().toIntOrNull() ?: error("UTM zone gerekli")
                val utm = UtmTransformer(utmZone, true, a, invF)
                val (latDeg, lonDeg) = utm.inverse(eastVal, northVal)
                val tm = tm30Transformer()
                val (easting, northing) = tm.forward(latDeg, lonDeg)
                onResult("", "", northing.toString(), easting.toString(), "")
            }
            else -> {
                onResult(lat, lon, north, east, zone)
            }
        }
    } catch (e: Exception) {
        onResult("Hata: ${e.message}", "Hata", "Hata", "Hata", "Hata")
    }
}
