package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.data.db.entity.CadLayerEntity
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LayerSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { LayerSettingsScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerSettingsScreen(onBack: () -> Unit, vm: LayerSettingsViewModel = hiltViewModel()) {
    val layers by vm.layers.collectAsState()
    val busy by vm.busy.collectAsState()
    var newLayerName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Katman Ayarları") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            // Ekleme satırı
            OutlinedTextField(
                value = newLayerName,
                onValueChange = { newLayerName = it },
                label = { Text("Yeni Katman Adı") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        vm.addLayer(newLayerName)
                        if (newLayerName.isNotBlank()) newLayerName = ""
                    },
                    enabled = !busy && newLayerName.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Ekle") }
                AssistChip(onClick = { newLayerName = "" }, label = { Text("Temizle") }, enabled = newLayerName.isNotBlank())
            }
            Spacer(Modifier.height(12.dp))

            Text("Katmanlar (${layers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (layers.isEmpty() && !busy) {
                Text("Katman yok. Yeni bir katman ekleyin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f)) {
                items(layers, key = { it.id }) { l -> LayerRow(l, onToggleVisible = { vm.toggleVisibility(l.id, l.visible) }) }
            }
        }
    }
}

@Composable
private fun LayerRow(layer: CadLayerEntity, onToggleVisible: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(layer.name, fontWeight = FontWeight.Bold)
                val colorLabel = layer.colorIndex?.let { "RenkIndex=${it}" } ?: "Renk Yok"
                Text(colorLabel, style = MaterialTheme.typography.labelSmall)
                Text("Oluşturuldu: ${formatShortTs(layer.createdAt)}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onToggleVisible) {
                Icon(if (layer.visible == 1) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
            }
        }
    }
}

@Composable
private fun formatShortTs(ts: Long): String {
    // Basit relative format (saniyeye hassas gerek yok)
    val diff = System.currentTimeMillis() - ts
    val hours = diff / 3_600_000
    return if (hours < 1) "<1s" else if (hours < 24) "${hours}sa" else "${hours/24}g"
}
