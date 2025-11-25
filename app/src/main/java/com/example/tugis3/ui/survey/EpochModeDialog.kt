package com.example.tugis3.ui.survey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.ui.device.GnssDeviceViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EpochModeDialog(
    onDismiss: () -> Unit,
    viewModel: GnssDeviceViewModel = hiltViewModel()
) {
    val deviceSettings by viewModel.deviceSettings.collectAsState()
    val currentEpoch by viewModel.currentEpoch.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val hrmsValue by viewModel.hrmsValue.collectAsState()
    val vrmsValue by viewModel.vrmsValue.collectAsState()

    // Simulated epoch data points
    val epochPoints = remember { mutableStateListOf<EpochPoint>() }

    LaunchedEffect(currentEpoch) {
        if (currentEpoch > 0 && epochPoints.size < currentEpoch) {
            epochPoints.add(
                EpochPoint(
                    x = (Math.random() * 0.02 - 0.01).toFloat(),
                    y = (Math.random() * 0.02 - 0.01).toFloat(),
                    epoch = currentEpoch
                )
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Başlık
                Text(
                    text = "Epoklu Alım Modu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Koordinat bilgileri
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("X: 4415708.373", fontSize = 12.sp)
                            Text("Z: 1131.004", fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Y: 485069.253", fontSize = 12.sp)
                            Text("Anten Y: 1.4m±0.07m", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dairesel grafik
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EpochCircularChart(
                        epochPoints = epochPoints,
                        centerAccuracy = 0.02f,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Merkez noktası
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alt bilgi paneli
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Epok Sayısı",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "$currentEpoch/${deviceSettings.epochCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "HRMS",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%.3f", hrmsValue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "VRMS",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%.3f", vrmsValue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kontrol butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Kapat")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isRecording) "Durdur" else "Başlat")
                    }
                }
            }
        }
    }
}

@Composable
fun EpochCircularChart(
    epochPoints: List<EpochPoint>,
    centerAccuracy: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) / 2 - 20.dp.toPx()

        // Dış daire (doğruluk limiti)
        drawCircle(
            color = Color.Red,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // İç daireler (doğruluk zonları)
        for (i in 1..3) {
            drawCircle(
                color = Color.Gray,
                radius = radius * i / 4,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }

        // Çapraz çizgiler
        drawLine(
            color = Color.Gray,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.Gray,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.dp.toPx()
        )

        // Epok noktaları
        epochPoints.forEachIndexed { index, point ->
            val pointX = center.x + (point.x / centerAccuracy) * radius
            val pointY = center.y + (point.y / centerAccuracy) * radius

            // Nokta
            drawCircle(
                color = Color.Cyan,
                radius = 3.dp.toPx(),
                center = Offset(pointX, pointY)
            )

            // Epok numarası
            drawContext.canvas.nativeCanvas.drawText(
                point.epoch.toString(),
                pointX + 8.dp.toPx(),
                pointY - 8.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10.sp.toPx()
                }
            )
        }

        // Doğruluk değeri etiketi
        drawContext.canvas.nativeCanvas.drawText(
            "${centerAccuracy}m",
            center.x - radius + 10.dp.toPx(),
            center.y + radius - 10.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 12.sp.toPx()
            }
        )
    }
}

data class EpochPoint(
    val x: Float,
    val y: Float,
    val epoch: Int
)
