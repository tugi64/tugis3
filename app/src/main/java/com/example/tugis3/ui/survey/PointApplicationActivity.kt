@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.tugis3.ui.survey

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.navigation.MenuItem
import com.example.tugis3.ui.common.PlaceholderActivity
import com.example.tugis3.ui.project.PointListActivity
import com.example.tugis3.ui.project.FileManagerActivity
import com.example.tugis3.ui.project.DataUploadActivity
import dagger.hilt.android.AndroidEntryPoint
import com.example.tugis3.ui.theme.Tugis3Theme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.example.tugis3.ui.survey.DetailSurveyActivity
import com.example.tugis3.ui.survey.LineStakeoutActivity
import com.example.tugis3.ui.cad.CadApplicationActivity
import com.example.tugis3.ui.survey.PointStakeoutActivity
import com.example.tugis3.ui.survey.QuickPointSurveyActivity
import com.example.tugis3.ui.survey.GraphicalSurveyActivity
import androidx.compose.foundation.layout.FlowRow
import com.example.tugis3.ui.navigation.MenuScreenScaffold

@AndroidEntryPoint
class PointApplicationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { PointApplicationRoot(onBack = { finish() }) } }
    }
    companion object {
        const val EXTRA_MEAS_MODE = "measurement_mode"
    }
}

@Composable
private fun PointApplicationRoot(onBack: () -> Unit) {
    val context = LocalContext.current
    val menuItems = listOf(
        MenuItem("Detay Alımı", Icons.Filled.Map),
        MenuItem("Hızlı Alım", Icons.Filled.Speed),
        MenuItem("Grafik Alım", Icons.Filled.LocationOn),
        MenuItem("Aplikasyon", Icons.Filled.GpsFixed),
        MenuItem("Hat Aplikasyonu", Icons.Filled.Timeline),
        MenuItem("CAD Aplikasyonu", Icons.Filled.Layers),
        MenuItem("Nokta Listesi", Icons.AutoMirrored.Filled.List)
    )
    val measurementModes = listOf("Standart", "Epoklu", "Hızlı", "Otomatik")
    var selectedMode by remember { mutableStateOf(measurementModes.first()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nokta Aplikasyonu") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            // Ölçüm modu seçici
            Text(
                "Ölçüm Modu",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                measurementModes.forEach { mm ->
                    FilterChip(
                        selected = selectedMode == mm,
                        onClick = { selectedMode = mm },
                        label = { Text(mm) },
                        leadingIcon = if (selectedMode == mm) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null
                    )
                }
            }
            AssistChip(
                onClick = {},
                label = { Text("Seçili Mod: $selectedMode") },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Divider(Modifier.padding(top = 4.dp))
            // Favori & Son kullanılan destekli grid
            MenuScreenScaffold(rawItems = menuItems) { mi ->
                try {
                    when (mi.title) {
                        "Detay Alımı" -> {
                            val intent = Intent(context, DetailSurveyActivity::class.java).apply {
                                putExtra(PointApplicationActivity.EXTRA_MEAS_MODE, selectedMode)
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                        "Hızlı Alım" -> {
                            val intent = Intent(context, QuickPointSurveyActivity::class.java).apply {
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                        "Grafik Alım" -> {
                            val intent = Intent(context, GraphicalSurveyActivity::class.java).apply {
                                putExtra(PointApplicationActivity.EXTRA_MEAS_MODE, selectedMode)
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                        "Aplikasyon" -> {
                            val intent = Intent(context, PointStakeoutActivity::class.java).apply {
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                        "Hat Aplikasyonu" -> {
                            val intent = Intent(context, LineStakeoutActivity::class.java).apply {
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                        "CAD Aplikasyonu" -> {
                            val intent = Intent(context, CadApplicationActivity::class.java).apply {
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                        "Nokta Listesi" -> {
                            val intent = Intent(context, PointListActivity::class.java).apply {
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                        else -> {
                            val intent = Intent(context, PlaceholderActivity::class.java).apply {
                                putExtra(PlaceholderActivity.EXTRA_TITLE, mi.title)
                            }
                            context.startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    // Herhangi bir activity başlatılamadıysa PlaceholderActivity aç
                    val intent = Intent(context, PlaceholderActivity::class.java).apply {
                        putExtra(PlaceholderActivity.EXTRA_TITLE, "${mi.title} (Geliştirme aşamasında)")
                    }
                    context.startActivity(intent)
                }
            }
            HelpBottomSheetEntry(Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun HelpBottomSheetEntry(modifier: Modifier = Modifier) {
    Surface(tonalElevation = 2.dp, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Kılavuz Özeti", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Menü: Detay, Hızlı, Grafik, Aplikasyon (Stakeout), Hat, CAD ve Nokta Listesi. Veri içe/dışa aktarma Proje menüsündedir. Seçili ölçüm modu Detay & Grafik ekranlarına aktarılır.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
