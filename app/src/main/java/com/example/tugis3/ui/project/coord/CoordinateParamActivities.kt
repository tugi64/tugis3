@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tugis3.ui.project.coord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Basit parametre depolama (in-memory); TODO: DataStore ile kalıcı hale getir */
object CoordParamsStore {
    private val map = mutableStateMapOf<String, String>()
    fun get(key: String, def: String = ""): String = map[key] ?: def
    fun put(key: String, v: String) { map[key] = v }
}

private fun genericTitle(id: String): String = when(id) {
    "ProjectionParametersActivity" -> "Projeksiyon Parametreleri"
    "ItrfParametersActivity" -> "ITRF Parametreleri"
    "SevenParamsActivity" -> "Yedi Parametre"
    "FourParamsActivity" -> "Dört Parametre / Yatay Ayar"
    "VerticalControlParametersActivity" -> "Dikey Kontrol Parametreleri"
    "VerticalAdjustmentParametersActivity" -> "Dikey Ayar Parametreleri"
    "GridFileActivity" -> "Grid Dosyası"
    "GeoidFileActivity" -> "Geoid Dosyası"
    "LocalOffsetsActivity" -> "Yerel Ofsetler"
    else -> id
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenericParamScreen(activityId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var dirty by remember { mutableStateOf(false) }

    // Basit parametre kümeleri
    val fields = remember(activityId) {
        when(activityId) {
            "ProjectionParametersActivity" -> listOf("Central Meridian","Scale Factor","False Easting","False Northing")
            "ItrfParametersActivity" -> listOf("Epoch Year","dX (m)","dY (m)","dZ (m)","RotX (arcsec)","RotY","RotZ","Scale (ppm)")
            "SevenParamsActivity" -> listOf("dX","dY","dZ","RotX","RotY","RotZ","Scale")
            "FourParamsActivity" -> listOf("dX","dY","Rotation (deg)","Scale (ppm)")
            "VerticalControlParametersActivity" -> listOf("Geoid Sep (m)","Ortho Offset (m)","Dynamic Corr (Y/N)")
            "VerticalAdjustmentParametersActivity" -> listOf("Ref Level (m)","Shift (m)","Scale","Trend")
            "GridFileActivity" -> listOf("Grid Path","Checksum","Version")
            "GeoidFileActivity" -> listOf("Geoid Path","Model Name","Version")
            "LocalOffsetsActivity" -> listOf("dNorth (m)","dEast (m)","dUp (m)")
            else -> listOf("Param1","Param2")
        }
    }

    val stateValues = remember(activityId) { fields.associateWith { mutableStateOf(CoordParamsStore.get(activityId+":"+it)) } }

    fun saveAll() {
        stateValues.forEach { (k, v) -> CoordParamsStore.put(activityId+":"+k, v.value.trim()) }
        dirty = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(genericTitle(activityId)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    if (dirty) {
                        IconButton(onClick = { saveAll() }) { Icon(Icons.Default.Save, contentDescription = "Kaydet") }
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Parametreleri girin / düzenleyin", style = MaterialTheme.typography.bodyMedium)
            stateValues.forEach { (label, state) ->
                OutlinedTextField(
                    value = state.value,
                    onValueChange = {
                        state.value = it; dirty = true
                    },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { saveAll() }, enabled = dirty, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save,null); Spacer(Modifier.width(6.dp)); Text("Kaydet") }
                OutlinedButton(onClick = {
                    stateValues.forEach { it.value.value = "" }; dirty = true
                }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh,null); Spacer(Modifier.width(6.dp)); Text("Temizle") }
            }
            AssistChip(onClick = {}, label = { Text(if (dirty) "Değişiklikler kaydedilmedi" else "Güncel") }, leadingIcon = { Icon(if (dirty) Icons.Default.Warning else Icons.Default.Check, null) })
            Spacer(Modifier.height(32.dp))
            Text("Not: Bu ekran prototip amaçlıdır. Kalıcı saklama için DataStore/DB entegrasyonu yapılmalıdır.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

abstract class BaseParamActivity: ComponentActivity() {
    abstract val id: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { GenericParamScreen(id) { finish() } } }
    }
}

@AndroidEntryPoint class ItrfParametersActivity: BaseParamActivity() { override val id = "ItrfParametersActivity" }
@AndroidEntryPoint class SevenParamsActivity: BaseParamActivity() { override val id = "SevenParamsActivity" }
@AndroidEntryPoint class FourParamsActivity: BaseParamActivity() { override val id = "FourParamsActivity" }
@AndroidEntryPoint class VerticalControlParametersActivity: BaseParamActivity() { override val id = "VerticalControlParametersActivity" }
@AndroidEntryPoint class VerticalAdjustmentParametersActivity: BaseParamActivity() { override val id = "VerticalAdjustmentParametersActivity" }
@AndroidEntryPoint class GridFileActivity: BaseParamActivity() { override val id = "GridFileActivity" }
@AndroidEntryPoint class GeoidFileActivity: BaseParamActivity() { override val id = "GeoidFileActivity" }
@AndroidEntryPoint class LocalOffsetsActivity: BaseParamActivity() { override val id = "LocalOffsetsActivity" }
