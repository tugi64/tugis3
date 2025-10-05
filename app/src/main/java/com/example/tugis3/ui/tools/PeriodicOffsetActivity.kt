package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme

/**
 * 6.9 Add Offsets to Points at Specified Period özelliği için placeholder ekran.
 * Gelecek adımlar:
 *  - Periyot (saniye) seçimi
 *  - Offset vektörü (dN, dE, dH) veya hız (mm/yıl) parametreleri
 *  - Hedef nokta seçimi / çoklu seçim
 *  - Otomatik zamanlayıcı ile epoch üretimi ve nokta koordinat güncellemesi
 *  - CSV / GeoJSON dışa aktarım
 */
class PeriodicOffsetActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { PeriodicOffsetScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodicOffsetScreen(onBack: () -> Unit) {
    val periodSec = remember { mutableStateOf("60") }
    val offsetN = remember { mutableStateOf("0.000") }
    val offsetE = remember { mutableStateOf("0.000") }
    val offsetH = remember { mutableStateOf("0.000") }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Periyodik Offset") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Bu özellik seçilen noktalara belirlenen periyotla otomatik offset uygulamak için hazırlanacaktır.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
            item { OutlinedTextField(value = periodSec.value, onValueChange = { periodSec.value = it }, label = { Text("Periyot (s)") }) }
            item { Text("Offset Vektörü (metre)", style = MaterialTheme.typography.titleSmall) }
            item { OutlinedTextField(value = offsetN.value, onValueChange = { offsetN.value = it }, label = { Text("dN") }) }
            item { OutlinedTextField(value = offsetE.value, onValueChange = { offsetE.value = it }, label = { Text("dE") }) }
            item { OutlinedTextField(value = offsetH.value, onValueChange = { offsetH.value = it }, label = { Text("dH") }) }
            item { ElevatedButton(onClick = { /* TODO: Başlat */ }, enabled = false) { Text("Başlat (TODO)") } }
            item { OutlinedButton(onClick = { /* TODO: Dışa aktar */ }, enabled = false) { Text("Dışa Aktar (TODO)") } }
        }
    }
}

