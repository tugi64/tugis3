package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import android.content.Context
import com.example.tugis3.ui.theme.Tugis3Theme

@AndroidEntryPoint
class CoordinateSystemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { CoordinateSystemScreen(onNavigateEllipsoid = {
            startActivity(Intent(this, EllipsoidParametersActivity::class.java))
        }, onBack = { finish() }) } }
    }
}

data class CoordParamItem(val title: String, val enabled: Boolean, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoordinateSystemScreen(onNavigateEllipsoid: () -> Unit, onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    fun onNavigate(simple: String) { context.safeStartByName("com.example.tugis3.ui.project.coord.$simple", simple) }
    val items = remember {
        listOf(
            CoordParamItem("Elipsoid Parametreleri", true, onNavigateEllipsoid),
            CoordParamItem("Projeksiyon Parametreleri", true) { onNavigate("ProjectionParametersActivity") },
            CoordParamItem("ITRF Parametreleri", true) { onNavigate("ItrfParametersActivity") },
            CoordParamItem("Yedi Parametre", true) { onNavigate("SevenParamsActivity") },
            CoordParamItem("Dört Parametre / Yatay Ayar", true) { onNavigate("FourParamsActivity") },
            CoordParamItem("Dikey Kontrol Parametreleri", true) { onNavigate("VerticalControlParametersActivity") },
            CoordParamItem("Dikey Ayar Parametreleri", true) { onNavigate("VerticalAdjustmentParametersActivity") },
            CoordParamItem("Grid Dosyası", true) { onNavigate("GridFileActivity") },
            CoordParamItem("Geoid Dosyası", true) { onNavigate("GeoidFileActivity") },
            CoordParamItem("Yerel Ofsetler", true) { onNavigate("LocalOffsetsActivity") }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koordinat Sistemi") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (item.enabled) Modifier.clickable { item.action() } else Modifier.clickable {
                            // Disabled item tapped -> future: show snackbar
                        }.alpha(0.55f)
                    )
                ElevatedCard(
                    modifier = cardModifier
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            if (!item.enabled) Text("Hazırlanıyor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun Context.launch(cls: Class<*>) = runCatching { startActivity(Intent(this, cls)) }

// Extension function for safe activity launching by name
private fun Context.safeStartByName(className: String, title: String? = null) {
    try {
        val cls = Class.forName(className)
        val intent = Intent(this, cls)
        if (title != null) intent.putExtra("title", title)
        startActivity(intent)
    } catch (_: Exception) {
        // Fallback to placeholder or show error
        runCatching {
            val intent = Intent(this, com.example.tugis3.ui.common.PlaceholderActivity::class.java)
            intent.putExtra(com.example.tugis3.ui.common.PlaceholderActivity.EXTRA_TITLE, title ?: className)
            startActivity(intent)
        }
    }
}
