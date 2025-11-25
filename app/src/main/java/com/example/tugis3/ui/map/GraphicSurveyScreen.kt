package com.example.tugis3.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.ui.device.GnssDeviceViewModel

// CAD Seçenekleri
enum class CadTool {
    KARE, HAT, COKLU_CIZGI, DIKTORTGEN, DIKTORTGEN_MERKEZI,
    COKGON, DAIRE_2P, DAIRE_3P, YAY, NOKTA, COKLU_EGRI, DEFAULT
}

data class CadOption(
    val tool: CadTool,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicSurveyScreen(
    viewModel: GnssDeviceViewModel = hiltViewModel()
) {
    var selectedCadTool by remember { mutableStateOf(CadTool.DEFAULT) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPointList by remember { mutableStateOf(false) }
    var showCADDialog by remember { mutableStateOf(false) }

    // CAD Seçenekleri
    val cadOptions = listOf(
        CadOption(CadTool.KARE, "Kare", Icons.Default.CropSquare),
        CadOption(CadTool.HAT, "Hat", Icons.Default.Timeline),
        CadOption(CadTool.COKLU_CIZGI, "Çoklu Çizgi", Icons.Default.Polyline),
        CadOption(CadTool.DIKTORTGEN, "Dikdörtgen", Icons.Default.Rectangle),
        CadOption(CadTool.DIKTORTGEN_MERKEZI, "Dikdörtgen Merkezi", Icons.Default.CenterFocusStrong),
        CadOption(CadTool.COKGON, "Çokgen", Icons.Default.Pentagon),
        CadOption(CadTool.DAIRE_2P, "Daire 2P", Icons.Default.Circle),
        CadOption(CadTool.DAIRE_3P, "Daire 3P", Icons.Default.RadioButtonUnchecked),
        CadOption(CadTool.YAY, "Yay", Icons.Default.Architecture),
        CadOption(CadTool.NOKTA, "Nokta", Icons.Default.FiberManualRecord),
        CadOption(CadTool.COKLU_EGRI, "Çoklu Eğri", Icons.Default.Timeline),
        CadOption(CadTool.DEFAULT, "Varsayılan", Icons.Default.TouchApp)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst durum çubuğu
        TopStatusBar()

        // Alım Modu Seçici
        SurveyModeSelector()

        // Ana içerik alanı
        Row(modifier = Modifier.weight(1f)) {
            // Sol araç çubuğu
            GraphicToolsPanel(
                modifier = Modifier.width(80.dp),
                onSettingsClick = { showSettingsDialog = true },
                onPointListClick = { showPointList = true },
                onCADClick = { showCADDialog = true }
            )

            // Harita alanı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .background(
                        Color.LightGray,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                // Harita içeriği burada olacak
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "Map",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text(
                        "Harita Alanı",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray
                    )
                    Text(
                        "Seçili CAD Aracı: ${selectedCadTool.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }

                // Harita koordinat bilgileri (sağ alt)
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("X: 4415708.373", fontSize = 10.sp)
                        Text("Y: 485069.253", fontSize = 10.sp)
                        Text("Anten Y: 2m±0.07m", fontSize = 10.sp)
                        Text("Baz Msf: 0.329", fontSize = 10.sp)
                    }
                }
            }

            // Sağ bilgi paneli
            RightInfoPanel(
                modifier = Modifier.width(200.dp),
                selectedTool = selectedCadTool
            )
        }

        // Alt durum çubuğu
        BottomGraphicStatusBar()
    }

    // CAD Seçenekleri Dialog'u
    if (showCADDialog) {
        CADToolsDialog(
            cadOptions = cadOptions,
            selectedTool = selectedCadTool,
            onToolSelected = { selectedCadTool = it },
            onDismiss = { showCADDialog = false }
        )
    }

    // Ayarlar Dialog'u
    if (showSettingsDialog) {
        GraphicSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    // Nokta Listesi Dialog'u
    if (showPointList) {
        GraphicPointListDialog(onDismiss = { showPointList = false })
    }
}

@Composable
fun TopStatusBar() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Çözüm Durumu", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Hatalar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Uydu Durumu", fontWeight = FontWeight.Bold, fontSize = 12.sp)

            // GNSS Alıcısı Batarya Durumu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Battery3Bar, contentDescription = "Battery", modifier = Modifier.size(16.dp))
                Text("100%", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SurveyModeSelector() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf("Standart", "Epoklu", "Hızlı", "Otomatik")
            var selectedMode by remember { mutableStateOf("Standart") }

            modes.forEach { mode ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = mode },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == mode)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            fontSize = 10.sp,
                            color = if (selectedMode == mode) Color.White else Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GraphicToolsPanel(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onPointListClick: () -> Unit,
    onCADClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CAD Seçenekleri butonu
            ToolButton(Icons.Default.Draw, "CAD Seçenekleri", onCADClick)
            ToolButton(Icons.Default.Settings, "Ayarlar", onSettingsClick)
            ToolButton(Icons.Default.List, "Nokta Listesi", onPointListClick)
            ToolButton(Icons.Default.Save, "Kaydet") { }
            ToolButton(Icons.Default.FileOpen, "Aç") { }
            ToolButton(Icons.Default.ZoomIn, "Yakınlaştır") { }
            ToolButton(Icons.Default.ZoomOut, "Uzaklaştır") { }

            Spacer(modifier = Modifier.weight(1f))

            // Ana alım butonu
            Card(
                modifier = Modifier.size(60.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Alım Butonu",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(50.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun RightInfoPanel(
    modifier: Modifier = Modifier,
    selectedTool: CadTool
) {
    Card(
        modifier = modifier.fillMaxHeight().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Bilgi Paneli",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Text("Seçili Araç:")
            Text(
                selectedTool.name,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Koordinat Bilgileri:")
            Text("X: 4415708.373", fontSize = 12.sp)
            Text("Y: 485069.253", fontSize = 12.sp)
            Text("Z: 1131.004", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text("Doğruluk:")
            Text("HRMS: 0.05", fontSize = 12.sp)
            Text("VRMS: 0.1", fontSize = 12.sp)
            Text("PDOP: 3.0", fontSize = 12.sp)
        }
    }
}

@Composable
fun BottomGraphicStatusBar() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grafik Nesneleri: 0", fontSize = 12.sp)
            Text("Yakınlaştırma: 1:1000", fontSize = 12.sp)
            Text("Harita Modu", fontSize = 12.sp)

            // Durum göstergesi
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Green, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hazır", fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CADToolsDialog(
    cadOptions: List<CadOption>,
    selectedTool: CadTool,
    onToolSelected: (CadTool) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CAD Seçenekleri") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cadOptions) { option ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                onToolSelected(option.tool)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTool == option.tool)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                option.icon,
                                contentDescription = option.name,
                                modifier = Modifier.size(24.dp),
                                tint = if (selectedTool == option.tool) Color.White else Color.Black
                            )
                            Text(
                                option.name,
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                color = if (selectedTool == option.tool) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicSettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grafik Alım Ayarları") },
        text = {
            Column {
                Text("Çizim Kalınlığı: 2px")
                Text("Renk: Kırmızı")
                Text("Otomatik Kaydet: Açık")
                Text("Grid Göster: Kapalı")
                Text("Koordinat Göster: Açık")
                Text("Ölçek: 1:1000")
                Text("Nokta Boyutu: Orta")
                Text("Çizgi Stili: Sürekli")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("TAMAM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("VARSAYILAN")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicPointListDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grafik Nokta Listesi") },
        text = {
            Column {
                Text("Hiç grafik nesnesi yok")
                Text("CAD araçlarını kullanarak çizim yapın")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
