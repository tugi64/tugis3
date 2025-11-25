@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickModuleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                QuickModuleScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickModuleScreen(onBack: () -> Unit) {
    val quickModules = remember {
        listOf(
            QuickModule("Hızlı Nokta", "RTK nokta ölçümü", Icons.Default.LocationOn, true),
            QuickModule("Hızlı Alan", "Alan hesaplama", Icons.Default.CropFree, false),
            QuickModule("Hızlı Yol", "Yol ölçümü", Icons.Default.Timeline, true),
            QuickModule("Hızlı Mesafe", "İki nokta arası mesafe", Icons.Default.Straighten, true),
            QuickModule("Hızlı Yükseklik", "Yükseklik ölçümü", Icons.Default.Height, false),
            QuickModule("Hızlı Grid", "Grid noktaları", Icons.Default.GridOn, false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hızlı Modül") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hızlı Erişim Bilgisi
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null)
                        Text(
                            "Hızlı Modüller",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Sık kullanılan ölçüm işlemlerini hızlıca gerçekleştirin",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Modül Durumu
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("6", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Toplam Modül", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("3", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Green)
                        Text("Aktif", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("12", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Bugün Kullanım", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Modül Listesi
            Text(
                "Mevcut Modüller",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickModules) { module ->
                    QuickModuleCard(
                        module = module,
                        onModuleClick = { /* Modülü başlat */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickModuleCard(
    module: QuickModule,
    onModuleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onModuleClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                module.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (module.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    module.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (module.isActive) {
                AssistChip(
                    onClick = onModuleClick,
                    label = { Text("Başlat") },
                    leadingIcon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            } else {
                Text(
                    "Kapalı",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

data class QuickModule(
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isActive: Boolean
)
