package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

@AndroidEntryPoint
class GisDataCollectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { GisDataCollectionScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GisDataCollectionScreen(onBack: () -> Unit) {
    data class GisEntity(val id:Int, val type:String, val attr:String)
    val entities = remember { mutableStateListOf<GisEntity>() }
    var counter by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf("Nokta") }
    var attr by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GIS Veri Toplama") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                entities.add(GisEntity(counter++, selectedType, attr.ifBlank { "-" }))
                attr = ""
            }) { Icon(Icons.Default.Add, null) }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Yeni Öge", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedType=="Nokta", onClick = { selectedType = "Nokta" }, label = { Text("Nokta") })
                    FilterChip(selected = selectedType=="Çizgi", onClick = { selectedType = "Çizgi" }, label = { Text("Çizgi") })
                    FilterChip(selected = selectedType=="Alan", onClick = { selectedType = "Alan" }, label = { Text("Alan") })
                }
                OutlinedTextField(value = attr, onValueChange = { attr = it }, label = { Text("Öznitelik") }, modifier = Modifier.fillMaxWidth())
            } }
            Spacer(Modifier.height(12.dp))
            Text("Toplanan (${entities.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.weight(1f)) {
                items(entities) { e ->
                    ListItem(headlineContent = { Text("#${e.id} ${e.type}") }, supportingContent = { Text(e.attr) })
                    Divider()
                }
            }
        }
    }
}
