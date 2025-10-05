package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ARApplicationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { ARPlaceholderScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ARPlaceholderScreen(onBack: () -> Unit) {
    val arEnabled = remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AR Aplikasyon (Placeholder)") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AR modülü henüz stabilize edilmedi.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Bu sürümde kamera & 3B overlay devre dışı. Uygulama derlenebilir hale getirildi.")
            Button(onClick = { arEnabled.value = !arEnabled.value }) {
                Text(if (arEnabled.value) "Devre Dışı Bırak" else "Sözde AR Başlat")
            }
            AssistChip(onClick = {}, label = { Text(if (arEnabled.value) "Çalışıyor" else "Beklemede") }, leadingIcon = { Icon(Icons.Filled.Info, null) })
        }
    }
}
