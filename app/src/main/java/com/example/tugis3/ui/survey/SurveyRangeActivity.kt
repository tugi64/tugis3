package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.combinedClickable
import com.example.tugis3.data.db.entity.SurveyRangeEntity

@AndroidEntryPoint
class SurveyRangeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { SurveyRangeScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveyRangeScreen(onBack: () -> Unit, vm: SurveyRangeViewModel = hiltViewModel()) {
    val ranges by vm.ranges.collectAsState()
    val inProgress by vm.inProgress.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ölçüm Alanları") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!inProgress) vm.addRandom() },
                modifier = if (inProgress) Modifier.alpha(0.55f) else Modifier
            ) {
                if (inProgress) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.AddLocation, null)
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            if (ranges.isEmpty() && !inProgress) {
                Text("Henüz alan yok. FAB ile ekleyin.", color = MaterialTheme.colorScheme.primary)
            }
            if (inProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(Modifier.weight(1f)) {
                items(ranges, key = { it.id }) { r -> RangeRow(r, onDelete = { vm.delete(r.id) }) }
            }
        }
    }
}

@Composable
private fun RangeRow(range: SurveyRangeEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = { /* detay gelecekte */ }, onLongClick = onDelete)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(range.name, fontWeight = FontWeight.Bold); Text("Nokta: ${range.pointCount}") }
            Column(horizontalAlignment = Alignment.End) {
                range.area?.let { Text("${"%.0f".format(it)} m²", style = MaterialTheme.typography.bodyMedium) }
                range.perimeter?.let { Text("P=${"%.1f".format(it)} m", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
