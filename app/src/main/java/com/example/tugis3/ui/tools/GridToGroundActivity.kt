package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import kotlin.math.roundToInt
import java.util.Locale

/**
 * 6.12 Grid to Ground (Izgara -> Arazi) placeholder.
 * Hesaplama Yaklaşımı (temel):
 *   Ground Distance = Grid Distance * (1 + Elevation Factor) * (1 + Scale Factor)
 * veya birleşik faktör (Combined Factor) doğrudan girilebilir.
 * Gelecek adımlar:
 *   - EPSG / Proje parametrelerinden proje ölçek faktörü otomatik çekme
 *   - Seçili proje ortalama elipsoidal yükseklik / ortometrik yükseklikten yükseklik faktörü hesaplama
 *   - Çoklu satır (batch) dönüşüm ve dosya dışa aktarımı
 */
class GridToGroundActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { GridToGroundScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridToGroundScreen(onBack: () -> Unit) {
    var gridDist by remember { mutableStateOf("100.000") }
    var scaleFactor by remember { mutableStateOf("0.000120") } // proje ölçek faktörü (k * 1e-6 yerine doğrudan) ~ 120 ppm
    var elevationFactor by remember { mutableStateOf("0.000180") } // ~180 ppm
    var combinedOverride by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Double?>(null) }
    var modeCombined by remember { mutableStateOf(false) }

    fun compute(){
        val gd = gridDist.toDoubleOrNull() ?: return
        if (modeCombined) {
            val cf = combinedOverride.toDoubleOrNull() ?: return
            result = gd * (1.0 + cf)
        } else {
            val sf = scaleFactor.toDoubleOrNull() ?: return
            val ef = elevationFactor.toDoubleOrNull() ?: return
            result = gd * (1.0 + sf) * (1.0 + ef)
        }
    }

    LaunchedEffect(gridDist, scaleFactor, elevationFactor, combinedOverride, modeCombined){ compute() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Grid -> Arazi") }, navigationIcon = { IconButton(onClick = onBack){ Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Bu ekran, grid (projeksiyon) düzleminde ölçülen mesafenin arazi mesafesine dönüştürülmesi için kullanılacaktır.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(gridDist, { gridDist = it.filter { c -> c.isDigit() || c=='.' } }, label = { Text("Grid Mesafe (m)") }, singleLine = true)
            AssistChip(onClick = { modeCombined = !modeCombined }, label = { Text(if (modeCombined) "Birleşik Faktör Modu" else "Ayrık Faktör Modu") })
            if (modeCombined) {
                OutlinedTextField(combinedOverride, { combinedOverride = it }, label = { Text("Birleşik Faktör (ör: 0.000300)") }, singleLine = true)
            } else {
                OutlinedTextField(scaleFactor, { scaleFactor = it }, label = { Text("Ölçek Faktörü (k)") }, supportingText = { Text("Projeksiyon ölçek düzeltmesi") })
                OutlinedTextField(elevationFactor, { elevationFactor = it }, label = { Text("Yükseklik Faktörü") }, supportingText = { Text("(R/(R+H)) yaklaşıklığı farkı)") })
            }
            Divider()
            Text("Sonuç", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Card { Row(Modifier.padding(12.dp)) {
                Text(
                    result?.let { "Arazi Mesafe: ${String.format(Locale.US, "%.4f", it)} m" } ?: "Geçersiz giriş",
                    fontWeight = FontWeight.Medium
                )
            }}
            result?.let { r ->
                val ppm = ((r / (gridDist.toDoubleOrNull() ?: r)) - 1.0) * 1_000_000.0
                Text("Toplam Faktör ≈ ${ppm.roundToInt()} ppm", style = MaterialTheme.typography.labelSmall)
            }
            ElevatedButton(onClick = { compute() }, enabled = true) { Icon(Icons.Outlined.Calculate, contentDescription = null); Text("  Yeniden Hesapla") }
            Text("NOT: Nihai sürümde proje ve nokta veritabanıyla entegrasyon yapılacak.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
