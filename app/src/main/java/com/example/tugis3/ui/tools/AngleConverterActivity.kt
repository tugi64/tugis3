package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor

// NOT: Eski AngleConverterActivity yeniden adlandırıldı -> AngleConversionActivity (redeclaration hatasını çözmek için)
class AngleConversionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AngleConverterScreen(onBack = { finish() }) }
    }
}

private enum class AngleInputMode(val label: String) { DECIMAL("Ondalık Derece"), DMS("DMS"), RAD("Radyan"), GON("Gon") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AngleConverterScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(AngleInputMode.DECIMAL) }
    var rawInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val decimalDeg: Double? = remember(mode, rawInput) {
        if (rawInput.isBlank()) return@remember null
        when (mode) {
            AngleInputMode.DECIMAL -> rawInput.toSmartDouble()
            AngleInputMode.RAD -> rawInput.toSmartDouble()?.let { it * 180.0 / PI }
            AngleInputMode.GON -> rawInput.toSmartDouble()?.let { it * 0.9 }
            AngleInputMode.DMS -> parseDms(rawInput)
        }
    }

    val rad = decimalDeg?.let { it * PI / 180.0 }
    val gon = decimalDeg?.let { it / 0.9 }
    val dms = decimalDeg?.let { toDms(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Açı Dönüştürme") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Girdi Formatı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AngleInputMode.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m; rawInput = "" }, label = { Text(m.label) })
                }
            }

            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                label = { Text("${mode.label} Girişi") },
                supportingText = {
                    Text(
                        when (mode) {
                            AngleInputMode.DECIMAL -> "Örn: 23.456 veya -10,25"
                            AngleInputMode.RAD -> "Örn: 1.0471975512"
                            AngleInputMode.GON -> "Örn: 120.5 (200 gon = 180°)"
                            AngleInputMode.DMS -> "Örn: 30°15'20.5\" / 30 15 20.5 / 30:15:20.5"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()
            Text("Sonuçlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (decimalDeg == null) {
                Text("Geçerli bir açı giriniz", color = MaterialTheme.colorScheme.error)
            } else {
                ResultRow("Ondalık Derece", String.format(Locale.US, "%.10f°", decimalDeg))
                ResultRow("DMS", dms ?: "-")
                ResultRow("Radyan", String.format(Locale.US, "%.12f", rad))
                ResultRow("Gon", String.format(Locale.US, "%.10f gon", gon))
            }

            AssistChip(onClick = { focusManager.clearFocus() }, label = { Text("Klavyeyi Kapat") })
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun String.toSmartDouble(): Double? = this.trim().replace(',', '.').toDoubleOrNull()

private fun parseDms(input: String): Double? {
    val cleaned = input.lowercase()
        .replace("d", " ")
        .replace("°", " ")
        .replace("'", " ")
        .replace("\"", " ")
        .replace("m", " ")
        .replace("s", " ")
        .replace(":", " ")
        .trim()
        .split(Regex("\\s+"))
    if (cleaned.isEmpty()) return null
    val deg = cleaned.getOrNull(0)?.toSmartDouble() ?: return null
    val min = cleaned.getOrNull(1)?.toSmartDouble() ?: 0.0
    val sec = cleaned.getOrNull(2)?.toSmartDouble() ?: 0.0
    val sign = if (deg < 0) -1 else if (deg == 0.0 && (input.trim().startsWith('-'))) -1 else 1
    val absDeg = abs(deg)
    if (min < 0 || sec < 0) return null
    return sign * (absDeg + (min / 60.0) + (sec / 3600.0))
}

private fun toDms(decimal: Double): String {
    val sign = if (decimal < 0) -1 else 1
    var absVal = abs(decimal)
    val d = floor(absVal)
    absVal = (absVal - d) * 60.0
    val m = floor(absVal)
    val s = (absVal - m) * 60.0
    val prefix = if (sign < 0) "-" else ""
    return String.format(Locale.US, "%s%.0f°%.0f'%.5f\"", prefix, d, m, s)
}
