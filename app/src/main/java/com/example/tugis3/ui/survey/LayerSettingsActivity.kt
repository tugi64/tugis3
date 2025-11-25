package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.data.db.entity.CadLayerEntity
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * KATMAN AYARLARI APLİKASYONU
 *
 * SurPad 4.2 Uyumlu Layer Settings
 * Referans: kullanimklavuz3.txt (Sayfa 100-102)
 *
 * Özellikler:
 * ✅ Yeni katman ekleme
 * ✅ Katman renk seçimi
 * ✅ Aktif katman (work layer) belirleme
 * ✅ Katman görünürlüğü (visible/invisible)
 * ✅ Katman kilitleme (lock/unlock)
 * ✅ Katman yeniden adlandırma
 * ✅ Katman silme
 * ✅ Tüm katmanları gizle
 * ✅ Long press menü
 */
@AndroidEntryPoint
class LayerSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { LayerSettingsScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LayerSettingsScreen(
    onBack: () -> Unit,
    vm: LayerSettingsViewModel = hiltViewModel()
) {
    val layers by vm.layers.collectAsState()
    val busy by vm.busy.collectAsState()

    var showNewLayerDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<CadLayerEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<CadLayerEntity?>(null) }
    var showColorPicker by remember { mutableStateOf<CadLayerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Katman Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    // Tüm katmanları gizle/göster
                    IconButton(
                        onClick = { vm.toggleAllLayersVisibility() }
                    ) {
                        Icon(
                            if (layers.any { it.visible == 1 }) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            "Tümünü Gizle/Göster"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewLayerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Yeni Katman")
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
        ) {
            // İstatistik Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoColumn("Toplam", "${layers.size}")
                    InfoColumn("Görünür", "${layers.count { it.visible == 1 }}")
                    InfoColumn("Kilitli", "${layers.count { it.locked == 1 }}")
                    InfoColumn("Aktif", layers.find { it.isWorkLayer == 1 }?.name ?: "-")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Katman Listesi Header
            Text(
                text = "Katmanlar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Uzun basarak menüyü açın",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (layers.isEmpty() && !busy) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Henüz katman yok",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Yeni katman eklemek için + butonuna tıklayın",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(layers, key = { it.id }) { layer ->
                    LayerCard(
                        layer = layer,
                        onSetWorkLayer = { vm.setWorkLayer(layer.id) },
                        onToggleVisible = { vm.toggleVisibility(layer.id, layer.visible) },
                        onToggleLock = { vm.toggleLock(layer.id, layer.locked) },
                        onRename = { showRenameDialog = layer },
                        onDelete = { showDeleteDialog = layer },
                        onColorPick = { showColorPicker = layer }
                    )
                }
            }
        }
    }

    // Yeni Katman Dialog
    if (showNewLayerDialog) {
        NewLayerDialog(
            onDismiss = { showNewLayerDialog = false },
            onConfirm = { name, colorIndex, isWorkLayer, isVisible, isLocked ->
                vm.addLayer(name, colorIndex, isWorkLayer, isVisible, isLocked)
                showNewLayerDialog = false
            }
        )
    }

    // Yeniden Adlandır Dialog
    showRenameDialog?.let { layer ->
        RenameLayerDialog(
            currentName = layer.name,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newName ->
                vm.renameLayer(layer.id, newName)
                showRenameDialog = null
            }
        )
    }

    // Silme Onay Dialog
    showDeleteDialog?.let { layer ->
        DeleteLayerDialog(
            layerName = layer.name,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                vm.deleteLayer(layer.id)
                showDeleteDialog = null
            }
        )
    }

    // Renk Seçici Dialog
    showColorPicker?.let { layer ->
        ColorPickerDialog(
            currentColorIndex = layer.colorIndex ?: 0,
            onDismiss = { showColorPicker = null },
            onConfirm = { newColorIndex ->
                vm.changeLayerColor(layer.id, newColorIndex)
                showColorPicker = null
            }
        )
    }
}

// Composable Bileşenler

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LayerCard(
    layer: CadLayerEntity,
    onSetWorkLayer: () -> Unit,
    onToggleVisible: () -> Unit,
    onToggleLock: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onColorPick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSetWorkLayer,
                onLongClick = { showMenu = true }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Katman Bilgisi
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Renk Göstergesi
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                getColorFromIndex(layer.colorIndex ?: 0),
                                CircleShape
                            )
                            .clickable { onColorPick() }
                    )

                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = layer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Aktif Katman İşareti
                            if (layer.isWorkLayer == 1) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "Aktif Katman",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = formatLayerDate(layer.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // İkon Butonları
                Row {
                    // Görünürlük
                    IconButton(onClick = onToggleVisible) {
                        Icon(
                            if (layer.visible == 1) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            "Görünürlük",
                            tint = if (layer.visible == 1) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Kilit
                    IconButton(onClick = onToggleLock) {
                        Icon(
                            if (layer.locked == 1) Icons.Default.Lock
                            else Icons.Default.LockOpen,
                            "Kilit",
                            tint = if (layer.locked == 1) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Long Press Menü
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Aktif Katman Yap") },
                onClick = {
                    onSetWorkLayer()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
            )
            DropdownMenuItem(
                text = { Text("Yeniden Adlandır") },
                onClick = {
                    onRename()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text("Renk Değiştir") },
                onClick = {
                    onColorPick()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Palette, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Sil") },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

// Dialog Bileşenleri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewLayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Boolean, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var colorIndex by remember { mutableStateOf(0) }
    var isWorkLayer by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Katman") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Katman Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Renk Seçici (Basit)
                Text("Renk:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 1, 2, 3, 4, 5).forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(getColorFromIndex(index), CircleShape)
                                .clickable { colorIndex = index }
                        ) {
                            if (colorIndex == index) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    modifier = Modifier.align(Alignment.Center),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Ayarlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktif Katman")
                    Switch(checked = isWorkLayer, onCheckedChange = { isWorkLayer = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Görünür")
                    Switch(checked = isVisible, onCheckedChange = { isVisible = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kilitli")
                    Switch(checked = isLocked, onCheckedChange = { isLocked = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, colorIndex, isWorkLayer, isVisible, isLocked)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameLayerDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Katmanı Yeniden Adlandır") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Yeni Ad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (newName.isNotBlank()) onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteLayerDialog(
    layerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Katmanı Sil") },
        text = { Text("'$layerName' katmanını silmek istediğinizden emin misiniz?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerDialog(
    currentColorIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(currentColorIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renk Seç") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Renk Paleti
                (0..15).chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        row.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(getColorFromIndex(index), CircleShape)
                                    .clickable { selectedIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedIndex == index) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIndex) }) {
                Text("Seç")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

// Yardımcı Fonksiyonlar

private fun formatLayerDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getColorFromIndex(index: Int): Color {
    // CAD Standard Renkler (AutoCAD ACI)
    return when (index % 16) {
        0 -> Color(0xFFFF0000) // Kırmızı
        1 -> Color(0xFFFFFF00) // Sarı
        2 -> Color(0xFF00FF00) // Yeşil
        3 -> Color(0xFF00FFFF) // Cyan
        4 -> Color(0xFF0000FF) // Mavi
        5 -> Color(0xFFFF00FF) // Magenta
        6 -> Color(0xFFFFFFFF) // Beyaz
        7 -> Color(0xFF808080) // Gri
        8 -> Color(0xFFC0C0C0) // Açık Gri
        9 -> Color(0xFFFF8080) // Açık Kırmızı
        10 -> Color(0xFF80FF80) // Açık Yeşil
        11 -> Color(0xFF8080FF) // Açık Mavi
        12 -> Color(0xFFFFFF80) // Açık Sarı
        13 -> Color(0xFFFF80FF) // Açık Magenta
        14 -> Color(0xFF80FFFF) // Açık Cyan
        15 -> Color(0xFF000000) // Siyah
        else -> Color.Gray
    }
}
