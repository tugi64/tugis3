package com.example.tugis3.ui.tools

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class VolumeCalculationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                VolumeCalculationScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeCalculationScreen(onBackPressed: () -> Unit) {
    var calculationMethod by remember { mutableStateOf("Üçgen Prizma") }
    var surveyPoints by remember { mutableStateOf<List<VolumePoint>>(emptyList()) }
    var designLevel by remember { mutableStateOf(850.0) }
    var calculatedVolume by remember { mutableStateOf<VolumeResult?>(null) }
    var selectedArea by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Sample survey points
    LaunchedEffect(Unit) {
        surveyPoints = listOf(
            VolumePoint("P001", 450100.0, 4330100.0, 852.5, "Mevcut Zemin"),
            VolumePoint("P002", 450120.0, 4330100.0, 851.8, "Mevcut Zemin"),
            VolumePoint("P003", 450140.0, 4330100.0, 853.2, "Mevcut Zemin"),
            VolumePoint("P004", 450100.0, 4330120.0, 851.9, "Mevcut Zemin"),
            VolumePoint("P005", 450120.0, 4330120.0, 852.3, "Mevcut Zemin"),
            VolumePoint("P006", 450140.0, 4330120.0, 852.7, "Mevcut Zemin")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hacim Hesaplama") },
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
            // Calculation Method Selection
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Hesaplama Yöntemi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val methods = listOf("Üçgen Prizma", "Kare Grid", "TIN (Triangulation)", "Kontur Çizgileri")
                    methods.forEach { method ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = calculationMethod == method,
                                onClick = { calculationMethod = method }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(method)
                        }
                    }
                }
            }

            // Design Parameters
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Tasarım Parametreleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = designLevel.toString(),
                        onValueChange = {
                            it.toDoubleOrNull()?.let { value ->
                                designLevel = value
                            }
                        },
                        label = { Text("Tasarım Kotu (m)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.Height, null) }
                    )

                    OutlinedTextField(
                        value = selectedArea,
                        onValueChange = { selectedArea = it },
                        label = { Text("Alan Tanımı") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Örn: Temel Kazısı, Dolgu Alanı") },
                        leadingIcon = { Icon(Icons.Default.Crop, null) }
                    )
                }
            }

            // Survey Points
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ölçü Noktaları (${surveyPoints.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            IconButton(onClick = { /* Add point */ }) {
                                Icon(Icons.Default.Add, null)
                            }
                            IconButton(onClick = { /* Import points */ }) {
                                Icon(Icons.Default.FileOpen, null)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(surveyPoints) { point ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            point.name,
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Z: ${String.format(Locale.US, "%.3f", point.z)}m",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    val heightDiff = point.z - designLevel
                                    Text(
                                        "${if (heightDiff >= 0) "+" else ""}${String.format(Locale.US, "%.3f", heightDiff)}m",
                                        fontWeight = FontWeight.Bold,
                                        color = if (heightDiff >= 0)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3D Visualization
            if (surveyPoints.isNotEmpty()) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "3D Görünüm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            draw3DVolume(
                                points = surveyPoints,
                                designLevel = designLevel,
                                canvasSize = size
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(color = Color.Red)
                                }
                                Text("Kazı", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(color = Color.Blue)
                                }
                                Text("Dolgu", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(color = Color.Green)
                                }
                                Text("Tasarım", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Calculate Button
            Button(
                onClick = {
                    calculatedVolume = if (calculationMethod.contains("TIN")) {
                        calculateTinVolume(surveyPoints, designLevel)
                    } else {
                        calculateVolume(surveyPoints, designLevel, calculationMethod)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = surveyPoints.isNotEmpty()
            ) {
                Icon(Icons.Default.Calculate, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hacim Hesapla")
            }

            // Results
            calculatedVolume?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Hacim Hesaplama Sonuçları",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Alan:")
                            Text("${String.format(Locale.US, "%.2f", result.area)} m²")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kazı Hacmi:")
                            Text(
                                "${String.format(Locale.US, "%.2f", result.cutVolume)} m³",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dolgu Hacmi:")
                            Text(
                                "${String.format(Locale.US, "%.2f", result.fillVolume)} m³",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Net Hacim:")
                            Text(
                                "${String.format(Locale.US, "%.2f", result.netVolume)} m³",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider()

                        val dateStr = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date()) }
                        Text("Yöntem: $calculationMethod", style = MaterialTheme.typography.bodySmall)
                        Text("Hesaplama Tarihi: $dateStr", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Export Options
            calculatedVolume?.let { result ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Dışa Aktarma",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Export report */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rapor")
                            }

                            OutlinedButton(
                                onClick = { /* Export CAD */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Architecture, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CAD")
                            }

                            OutlinedButton(
                                onClick = { /* Export Excel */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Excel")
                            }

                            OutlinedButton(
                                onClick = {
                                    shareVolumeCsv(
                                        context = context,
                                        result = result,
                                        points = surveyPoints,
                                        designLevel = designLevel,
                                        method = calculationMethod
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CSV")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun DrawScope.draw3DVolume(
    points: List<VolumePoint>,
    designLevel: Double,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    if (points.isEmpty()) return

    val margin = 20.dp.toPx()
    val width = canvasSize.width - 2 * margin
    val height = canvasSize.height - 2 * margin

    // Calculate bounds
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val minZ = minOf(points.minOf { it.z }, designLevel) - 1.0
    val maxZ = maxOf(points.maxOf { it.z }, designLevel) + 1.0

    val scaleX = width / (maxX - minX + 1.0)
    val scaleY = height / (maxY - minY + 1.0)

    fun toCanvasX(x: Double) = margin + (x - minX) * scaleX.toFloat()
    fun toCanvasY(y: Double) = margin + height - (y - minY) * scaleY.toFloat()
    fun getZColor(z: Double) = when {
        z > designLevel -> Color.Red.copy(alpha = 0.7f) // Cut
        z < designLevel -> Color.Blue.copy(alpha = 0.7f) // Fill
        else -> Color.Green
    }

    // Draw design level as a grid
    val gridColor = Color.Green.copy(alpha = 0.3f)
    for (i in 0..10) {
        val x = margin + width * i / 10
        drawLine(
            color = gridColor,
            start = Offset(x, margin),
            end = Offset(x, margin + height),
            strokeWidth = 1.dp.toPx()
        )
    }

    for (i in 0..10) {
        val y = margin + height * i / 10
        drawLine(
            color = gridColor,
            start = Offset(margin, y),
            end = Offset(margin + width, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Draw points with elevation-based colors
    points.forEach { point ->
        val x = toCanvasX(point.x).toFloat()
        val y = toCanvasY(point.y).toFloat()
        val color = getZColor(point.z)
        // Point size based on elevation
        val size = (8.0f + (point.z - minZ).toFloat() / (maxZ - minZ).toFloat() * 12.0f).dp.toPx()

        drawCircle(
            color = color,
            radius = size,
            center = Offset(x, y)
        )

        // Draw elevation difference
        if (point.z != designLevel) {
            val diff = (point.z - designLevel).toFloat()
            val lineColor = if (diff > 0) Color.Red else Color.Blue
            drawLine(
                color = lineColor,
                start = Offset(x, y),
                end = Offset(x, y - diff * 20f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun calculateVolume(
    points: List<VolumePoint>,
    designLevel: Double,
    method: String
): VolumeResult {
    if (points.isEmpty()) return VolumeResult()

    // Simplified volume calculation using triangular prism method
    var cutVolume = 0.0
    var fillVolume = 0.0
    var totalArea = 0.0

    // Create a simple grid-based calculation
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }

    val gridSize = 5.0 // 5m grid
    val cellArea = gridSize * gridSize

    var x = minX
    while (x < maxX) {
        var y = minY
        while (y < maxY) {
            // Find nearest point
            val nearestPoint = points.minByOrNull {
                sqrt((it.x - x).pow(2) + (it.y - y).pow(2))
            }

            nearestPoint?.let { point ->
                val heightDiff = point.z - designLevel
                val volume = abs(heightDiff) * cellArea

                if (heightDiff > 0) {
                    cutVolume += volume
                } else {
                    fillVolume += volume
                }

                totalArea += cellArea
            }

            y += gridSize
        }
        x += gridSize
    }

    return VolumeResult(
        area = totalArea,
        cutVolume = cutVolume,
        fillVolume = fillVolume,
        netVolume = cutVolume - fillVolume
    )
}

private fun calculateTinVolume(points: List<VolumePoint>, designLevel: Double): VolumeResult {
    if (points.size < 3) return VolumeResult()
    data class P(val x: Double, val y: Double, val z: Double)
    val pts = points.map { P(it.x, it.y, it.z) }

    data class Triangle(val a: Int, val b: Int, val c: Int)

    // Süper üçgen oluştur (geniş bounding)
    val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }
    val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
    val dx = maxX - minX; val dy = maxY - minY; val delta = max(dx, dy) * 10
    val sx1 = minX - delta; val sy1 = minY - 1.0 * delta
    val sx2 = minX + 0.5 * dx; val sy2 = maxY + delta * 2
    val sx3 = maxX + delta; val sy3 = minY - 1.0 * delta

    val work = pts.toMutableList()
    val superStart = work.size
    work += listOf(P(sx1, sy1, designLevel), P(sx2, sy2, designLevel), P(sx3, sy3, designLevel))

    val triangles = mutableListOf(Triangle(superStart, superStart + 1, superStart + 2))

    data class Circle(val x: Double, val y: Double, val r: Double)

    fun circumcircle(i1: Int, i2: Int, i3: Int): Circle? {
        val p1 = work[i1]; val p2 = work[i2]; val p3 = work[i3]
        val ax = p1.x; val ay = p1.y
        val bx = p2.x; val by = p2.y
        val cx = p3.x; val cy = p3.y
        val d = 2 * (ax*(by-cy) + bx*(cy-ay) + cx*(ay-by))
        if (abs(d) < 1e-12) return null
        val ux = ((ax*ax+ay*ay)*(by-cy) + (bx*bx+by*by)*(cy-ay) + (cx*cx+cy*cy)*(ay-by)) / d
        val uy = ((ax*ax+ay*ay)*(cx-bx) + (bx*bx+by*by)*(ax-cx) + (cx*cx+cy*cy)*(bx-ax)) / d
        val r = sqrt((ux-ax).pow(2) + (uy-ay).pow(2))
        return Circle(ux, uy, r)
    }

    data class Edge(val u: Int, val v: Int) { override fun hashCode(): Int = (u*73856093) xor (v*19349663); override fun equals(other: Any?): Boolean = other is Edge && u==other.u && v==other.v }

    for (pi in pts.indices) {
        val p = work[pi]
        val bad = mutableListOf<Triangle>()
        val circles = mutableMapOf<Triangle, Circle>()
        triangles.forEach { t ->
            val c = circumcircle(t.a, t.b, t.c) ?: return@forEach
            val dist = sqrt((c.x - p.x).pow(2) + (c.y - p.y).pow(2))
            if (dist <= c.r + 1e-9) {
                bad += t; circles[t] = c
            }
        }
        val edgeCount = mutableMapOf<Edge, Int>()
        fun addEdge(a: Int, b: Int) {
            val e = if (a < b) Edge(a,b) else Edge(b,a)
            edgeCount[e] = (edgeCount[e] ?: 0) + 1
        }
        bad.forEach { t ->
            addEdge(t.a, t.b); addEdge(t.b, t.c); addEdge(t.c, t.a)
        }
        triangles.removeAll(bad.toSet())
        edgeCount.filter { it.value == 1 }.forEach { (e, _) ->
            triangles += Triangle(e.u, e.v, pi)
        }
    }
    // Süper üçgen vertex'leri içerenleri sil
    triangles.removeAll { it.a >= superStart || it.b >= superStart || it.c >= superStart }

    if (triangles.isEmpty()) return VolumeResult()

    var cut = 0.0; var fill = 0.0; var totalArea = 0.0
    triangles.forEach { t ->
        val p1 = work[t.a]; val p2 = work[t.b]; val p3 = work[t.c]
        val area = 0.5 * abs((p2.x - p1.x)*(p3.y - p1.y) - (p3.x - p1.x)*(p2.y - p1.y))
        if (area <= 0) return@forEach
        val diffAvg = ((p1.z - designLevel) + (p2.z - designLevel) + (p3.z - designLevel))/3.0
        val vol = diffAvg * area
        if (diffAvg > 0) cut += vol else fill += -vol
        totalArea += area
    }
    return VolumeResult(area = totalArea, cutVolume = cut, fillVolume = fill, netVolume = cut - fill)
}

private fun shareVolumeCsv(
    context: android.content.Context,
    result: VolumeResult,
    points: List<VolumePoint>,
    designLevel: Double,
    method: String
) {
    runCatching {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val name = "volume_${sdf.format(Date())}.csv"
        val file = File(context.filesDir, name)
        FileOutputStream(file).use { out ->
            out.writer(Charsets.UTF_8).use { w ->
                w.appendLine("# Volume Calculation Export")
                w.appendLine("# Method,$method")
                w.appendLine("# DesignLevel,$designLevel")
                w.appendLine("# Area,${"%.3f".format(Locale.US, result.area)}")
                w.appendLine("# Cut,${"%.3f".format(Locale.US, result.cutVolume)}")
                w.appendLine("# Fill,${"%.3f".format(Locale.US, result.fillVolume)}")
                w.appendLine("# Net,${"%.3f".format(Locale.US, result.netVolume)}")
                w.appendLine()
                w.appendLine("Point,East,North,Elevation,Diff")
                points.forEach { p ->
                    val diff = p.z - designLevel
                    w.appendLine("${p.name},${"%.3f".format(Locale.US,p.x)},${"%.3f".format(Locale.US,p.y)},${"%.3f".format(Locale.US,p.z)},${"%.3f".format(Locale.US,diff)}")
                }
            }
        }
        val uri = FileProvider.getUriForFile(context, "com.example.tugis3.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Hacim Hesaplama CSV")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "CSV Paylaş"))
    }.onFailure { }
}

data class VolumePoint(
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val description: String
)

data class VolumeResult(
    val area: Double = 0.0,
    val cutVolume: Double = 0.0,
    val fillVolume: Double = 0.0,
    val netVolume: Double = 0.0
)
