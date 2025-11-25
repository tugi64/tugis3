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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class AngleConverterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                AngleConverterScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AngleConverterScreen(onBackPressed: () -> Unit) {
    var inputAngle by remember { mutableStateOf("") }
    var degrees by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var radians by remember { mutableStateOf("") }
    var grads by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Açı Dönüştürücü") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Açı Dönüştürme",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Decimal Degrees Input
            OutlinedTextField(
                value = inputAngle,
                onValueChange = {
                    inputAngle = it
                    val angle = it.toDoubleOrNull()
                    if (angle != null) {
                        // Convert to DMS
                        val deg = floor(abs(angle)).toInt()
                        val minDecimal = (abs(angle) - deg) * 60
                        val min = floor(minDecimal).toInt()
                        val sec = (minDecimal - min) * 60

                        degrees = if (angle < 0) "-$deg" else deg.toString()
                        minutes = min.toString()
                        seconds = String.format("%.4f", sec)

                        // Convert to radians
                        radians = String.format("%.6f", Math.toRadians(angle))

                        // Convert to grads
                        grads = String.format("%.4f", angle * 200.0 / 180.0)
                    }
                },
                label = { Text("Ondalık Derece") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // DMS Input
            Text(
                text = "Derece-Dakika-Saniye (DMS)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = degrees,
                    onValueChange = {
                        degrees = it
                        updateFromDMS(degrees, minutes, seconds) { result ->
                            inputAngle = result.inputAngle
                            radians = result.radians
                            grads = result.grads
                        }
                    },
                    label = { Text("Derece") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = minutes,
                    onValueChange = {
                        minutes = it
                        updateFromDMS(degrees, minutes, seconds) { result ->
                            inputAngle = result.inputAngle
                            radians = result.radians
                            grads = result.grads
                        }
                    },
                    label = { Text("Dakika") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = seconds,
                    onValueChange = {
                        seconds = it
                        updateFromDMS(degrees, minutes, seconds) { result ->
                            inputAngle = result.inputAngle
                            radians = result.radians
                            grads = result.grads
                        }
                    },
                    label = { Text("Saniye") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Radians
            OutlinedTextField(
                value = radians,
                onValueChange = {
                    radians = it
                    val rad = it.toDoubleOrNull()
                    if (rad != null) {
                        val angle = Math.toDegrees(rad)
                        inputAngle = String.format("%.6f", angle)

                        // Convert to DMS
                        val deg = floor(abs(angle)).toInt()
                        val minDecimal = (abs(angle) - deg) * 60
                        val min = floor(minDecimal).toInt()
                        val sec = (minDecimal - min) * 60

                        degrees = if (angle < 0) "-$deg" else deg.toString()
                        minutes = min.toString()
                        seconds = String.format("%.4f", sec)

                        // Convert to grads
                        grads = String.format("%.4f", angle * 200.0 / 180.0)
                    }
                },
                label = { Text("Radyan") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Grads
            OutlinedTextField(
                value = grads,
                onValueChange = {
                    grads = it
                    val grad = it.toDoubleOrNull()
                    if (grad != null) {
                        val angle = grad * 180.0 / 200.0
                        inputAngle = String.format("%.6f", angle)

                        // Convert to DMS
                        val deg = floor(abs(angle)).toInt()
                        val minDecimal = (abs(angle) - deg) * 60
                        val min = floor(minDecimal).toInt()
                        val sec = (minDecimal - min) * 60

                        degrees = if (angle < 0) "-$deg" else deg.toString()
                        minutes = min.toString()
                        seconds = String.format("%.4f", sec)

                        // Convert to radians
                        radians = String.format("%.6f", Math.toRadians(angle))
                    }
                },
                label = { Text("Grad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Bilgi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("• 1 tam daire = 360 derece")
                    Text("• 1 tam daire = 2π radyan")
                    Text("• 1 tam daire = 400 grad")
                    Text("• 1 derece = 60 dakika = 3600 saniye")
                }
            }
        }
    }
}

data class DMSResult(
    val inputAngle: String,
    val radians: String,
    val grads: String
)

private fun updateFromDMS(
    degrees: String,
    minutes: String,
    seconds: String,
    onResult: (DMSResult) -> Unit
) {
    val deg = degrees.toIntOrNull() ?: return
    val min = minutes.toIntOrNull() ?: return
    val sec = seconds.toDoubleOrNull() ?: return

    val sign = if (deg < 0) -1.0 else 1.0
    val angle = sign * (abs(deg) + min / 60.0 + sec / 3600.0)

    val result = DMSResult(
        inputAngle = String.format("%.6f", angle),
        radians = String.format("%.6f", Math.toRadians(angle)),
        grads = String.format("%.4f", angle * 200.0 / 180.0)
    )

    onResult(result)
}

