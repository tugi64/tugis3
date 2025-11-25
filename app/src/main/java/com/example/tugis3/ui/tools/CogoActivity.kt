package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.*

@AndroidEntryPoint
class CogoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CogoScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CogoScreen(onBack: () -> Unit) {
    var selectedFunction by remember { mutableStateOf("Azimut-Mesafe") }

    // Azimut-Mesafe parametreleri
    var startPoint by remember { mutableStateOf("") }
    var azimuth by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }

    // İki Nokta Arası parametreleri
    var point1 by remember { mutableStateOf("") }
    var point2 by remember { mutableStateOf("") }

    // Kesişim parametreleri
    var basePoint1 by remember { mutableStateOf("") }
    var azimuth1 by remember { mutableStateOf("") }
    var basePoint2 by remember { mutableStateOf("") }
    var azimuth2 by remember { mutableStateOf("") }

    // Offset parametreleri
    var lineStartPoint by remember { mutableStateOf("") }
    var lineEndPoint by remember { mutableStateOf("") }
    var offsetDistance by remember { mutableStateOf("") }
    var offsetSide by remember { mutableStateOf("Sol") }

    // Sonuç bilgileri
    var resultInfo by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("COGO Hesaplamaları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Yardım */ }) {
                        Icon(Icons.Default.Help, "Yardım")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Fonksiyon Seçimi", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val functions = listOf(
                            "Azimut-Mesafe" to "Başlangıç noktası, azimut ve mesafe ile yeni nokta hesaplar",
                            "İki Nokta Arası" to "İki nokta arasındaki azimut ve mesafeyi hesaplar",
                            "Kesişim" to "İki doğrunun kesişim noktasını bulur",
                            "Offset" to "Çizgiye paralel ofset noktası hesaplar",
                            "Bölme" to "Çizgiyi belirtilen sayıda eşit parçaya böler"
                        )

                        functions.forEach { (func, desc) ->
                            FilterChip(
                                selected = selectedFunction == func,
                                onClick = {
                                    selectedFunction = func
                                    resultInfo = ""
                                },
                                label = {
                                    Column {
                                        Text(func, fontWeight = FontWeight.Bold)
                                        Text(desc, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Parametreler", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        when (selectedFunction) {
                            "Azimut-Mesafe" -> {
                                OutlinedTextField(
                                    value = startPoint,
                                    onValueChange = { startPoint = it },
                                    label = { Text("Başlangıç Noktası (örn: P1)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Place, null) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = azimuth,
                                    onValueChange = { azimuth = it },
                                    label = { Text("Azimut (DD.MMSS veya Ondalık Derece)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Explore, null) },
                                    placeholder = { Text("örn: 45.3015 veya 45°30'15\"") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = distance,
                                    onValueChange = { distance = it },
                                    label = { Text("Mesafe (m)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Straighten, null) }
                                )
                            }

                            "İki Nokta Arası" -> {
                                OutlinedTextField(
                                    value = point1,
                                    onValueChange = { point1 = it },
                                    label = { Text("Başlangıç Noktası") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Place, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { /* Nokta seç */ }) {
                                            Icon(Icons.Default.List, "Listeden Seç")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = point2,
                                    onValueChange = { point2 = it },
                                    label = { Text("Bitiş Noktası") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Place, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { /* Nokta seç */ }) {
                                            Icon(Icons.Default.List, "Listeden Seç")
                                        }
                                    }
                                )
                            }

                            "Kesişim" -> {
                                Text("Birinci Doğru", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = basePoint1,
                                    onValueChange = { basePoint1 = it },
                                    label = { Text("Nokta 1") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = azimuth1,
                                    onValueChange = { azimuth1 = it },
                                    label = { Text("Azimut 1 (°)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text("İkinci Doğru", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = basePoint2,
                                    onValueChange = { basePoint2 = it },
                                    label = { Text("Nokta 2") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = azimuth2,
                                    onValueChange = { azimuth2 = it },
                                    label = { Text("Azimut 2 (°)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            "Offset" -> {
                                OutlinedTextField(
                                    value = lineStartPoint,
                                    onValueChange = { lineStartPoint = it },
                                    label = { Text("Hat Başlangıç Noktası") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = lineEndPoint,
                                    onValueChange = { lineEndPoint = it },
                                    label = { Text("Hat Bitiş Noktası") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = offsetDistance,
                                    onValueChange = { offsetDistance = it },
                                    label = { Text("Offset Mesafesi (m)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Offset Yönü", style = MaterialTheme.typography.labelMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = offsetSide == "Sol",
                                        onClick = { offsetSide = "Sol" },
                                        label = { Text("Sol") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = offsetSide == "Sağ",
                                        onClick = { offsetSide = "Sağ" },
                                        label = { Text("Sağ") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            "Bölme" -> {
                                Text("Bu fonksiyon yakında eklenecek...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (resultInfo.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, "Sonuç", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hesaplama Sonuçları", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(resultInfo, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Temizle
                            startPoint = ""
                            azimuth = ""
                            distance = ""
                            point1 = ""
                            point2 = ""
                            resultInfo = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Clear, "Temizle")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Temizle")
                    }

                    Button(
                        onClick = {
                            // Hesapla
                            resultInfo = when (selectedFunction) {
                                "Azimut-Mesafe" -> "Yeni Nokta: X=... Y=... Z=..."
                                "İki Nokta Arası" -> "Azimut: ... °\nMesafe: ... m\nYatay Mesafe: ... m"
                                "Kesişim" -> "Kesişim Noktası: X=... Y=..."
                                "Offset" -> "Offset Noktası: X=... Y=..."
                                else -> "Hesaplama tamamlandı"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Calculate, "Hesapla")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hesapla")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Icon(Icons.Default.Info, "Bilgi", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sonuçlar nokta veritabanına kaydedilebilir veya doğrudan aplikasyon için kullanılabilir.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
