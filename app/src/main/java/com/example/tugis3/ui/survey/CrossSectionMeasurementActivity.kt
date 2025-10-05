package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

@AndroidEntryPoint
class CrossSectionMeasurementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { CrossSectionScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrossSectionScreen(onBack: () -> Unit) {
    var samples by remember { mutableStateOf(emptyList<Pair<Double,Double>>()) }

    LaunchedEffect(Unit) {
        // örnek kesit verisi
        samples = List(25) { i ->
            val chain = i * 2.0
            val elev = 100.0 + Random.nextDouble(-2.0,2.0) + if (i in 10..15) 3 else 0
            chain to elev
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kesit Ölçümü") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Örnek Kesit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Örnek veri simülasyonu gösterilir.") } }
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                if (samples.isNotEmpty()) {
                    val minC = samples.minOf { it.first }
                    val maxC = samples.maxOf { it.first }
                    val minE = samples.minOf { it.second }
                    val maxE = samples.maxOf { it.second }
                    val path = Path()
                    samples.forEachIndexed { index, (c,e) ->
                        val x = ((c - minC)/(maxC-minC)) * size.width
                        val y = size.height - ((e - minE)/(maxE-minE)) * size.height
                        if (index==0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
                    }
                    drawPath(path, Color(0xFF2196F3))
                    samples.forEach { (c,e) ->
                        val x = ((c - minC)/(maxC-minC)) * size.width
                        val y = size.height - ((e - minE)/(maxE-minE)) * size.height
                        drawCircle(Color.Red, radius = 4.dp.toPx(), center = Offset(x.toFloat(), y.toFloat()))
                    }
                }
            }
        }
    }
}
