package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme

/**
 * Dış Radyo Yapılandırması (Manual 6.7 External Radio Configuration) için iskelet.
 * İleride: Seri/Bluetooth port seçimi, baud rate, frekans listesi, güç seviyesi, modulation vb.
 */
class ExternalRadioConfigActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { ExternalRadioConfigScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalRadioConfigScreen(onBack: () -> Unit) {
    val placeholders = remember {
        listOf(
            "Mevcut Bağlantı: (Henüz bağlı değil)",
            "Port Tipi Seçimi (Bluetooth / Seri / TCP)",
            "Baud Rate: 115200 (TODO: dinamik)",
            "Radyo Frekansı: --- MHz (TODO)",
            "Protokol / Modülasyon: (TODO)",
            "Güç Seviyesi: (Low/Med/High) (TODO)",
            "Çalışma Rolü: Base / Rover (TODO)",
            "Frekans Ön Ayarlarını Kaydet (TODO)",
            "Radyo Tanılama / RSSI Görüntüleme (TODO)"
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dış Radyo") },
                navigationIcon = { IconButton(onClick = onBack){ Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { Icon(Icons.Outlined.Settings, contentDescription = null) }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("ÖZET", style = MaterialTheme.typography.titleMedium) }
            items(placeholders){ line -> ElevatedCard { Text(line, modifier = Modifier.padding(12.dp)) } }
            item { Text("NOT: Bu ekran henüz fonksiyonel değil; manueldeki gereksinimler doğrultusunda genişletilecek.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

