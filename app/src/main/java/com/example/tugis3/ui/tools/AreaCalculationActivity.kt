package com.example.tugis3.ui.tools

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

class AreaCalculationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AreaCalcScreen() }
    }
}

data class PolyPoint(var x: String = "", var y: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaCalcScreen() {
    val points: SnapshotStateList<PolyPoint> = remember { mutableStateListOf(PolyPoint(), PolyPoint(), PolyPoint()) }
    var area by remember { mutableStateOf<Double?>(null) }
    var perimeter by remember { mutableStateOf<Double?>(null) }
    val ctx = LocalContext.current

    fun compute() {
        val numeric = points.mapNotNull { p ->
            val x = p.x.replace(',', '.').toDoubleOrNull()
            val y = p.y.replace(',', '.').toDoubleOrNull()
            if (x != null && y != null) x to y else null
        }
        if (numeric.size < 3) { area = null; perimeter = null; return }
        var sum = 0.0
        var peri = 0.0
        for (i in numeric.indices) {
            val (x1, y1) = numeric[i]
            val (x2, y2) = numeric[(i + 1) % numeric.size]
            sum += (x1 * y2) - (x2 * y1)
            peri += hypot(x2 - x1, y2 - y1)
        }
        area = abs(sum) / 2.0
        perimeter = peri
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alan / Çevre Hesabı") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { points.add(PolyPoint()) }) { Icon(Icons.Default.Add, contentDescription = "Add") }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Poligon Noktaları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, false)) {
                items(points) { point ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = point.x,
                                onValueChange = { point.x = it },
                                label = { Text("X") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = point.y,
                                onValueChange = { point.y = it },
                                label = { Text("Y") },
                                modifier = Modifier.weight(1f)
                            )
                            if (points.size > 3) {
                                IconButton(onClick = {
                                    points.remove(point)
                                    compute()
                                }) { Icon(Icons.Default.Delete, contentDescription = "Sil") }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { compute() }, modifier = Modifier.weight(1f)) { Text("Hesapla") }
                OutlinedButton(onClick = {
                    points.clear()
                    repeat(3) { points.add(PolyPoint()) }
                    area = null
                    perimeter = null
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Temizle")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sonuç", fontWeight = FontWeight.Bold)
                    when (area) {
                        null -> Text("En az 3 geçerli nokta giriniz")
                        else -> {
                            Text(String.format(Locale.US, "Alan: %.4f m²", area))
                            perimeter?.let { Text(String.format(Locale.US, "Çevre: %.4f m", it)) }
                        }
                    }
                    val shareEnabled = area != null
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (area != null) {
                                val shareText = buildString {
                                    append("Alan Hesabı Sonuçları\n")
                                    append("Nokta Sayısı: ${points.size}\n")
                                    append(String.format(Locale.US, "Alan: %.4f m²\n", area))
                                    perimeter?.let { append(String.format(Locale.US, "Çevre: %.4f m\n", it)) }
                                }
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                ctx.startActivity(Intent.createChooser(send, "Sonuçları Paylaş"))
                            }
                        }, enabled = shareEnabled) { Text("Paylaş") }
                    }
                }
            }
        }
    }
}
