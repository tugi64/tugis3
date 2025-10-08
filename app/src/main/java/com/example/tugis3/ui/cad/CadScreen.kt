package com.example.tugis3.ui.cad

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.os.Environment
import android.content.Intent
import android.content.Context
import android.view.View
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.unit.sp
import com.example.tugis3.core.cad.model.CadEntity
import com.example.tugis3.core.cad.model.CadLine
import com.example.tugis3.core.cad.model.CadPolyline
import com.example.tugis3.core.cad.model.CadCircle
import com.example.tugis3.core.cad.model.CadArc
import com.example.tugis3.core.cad.model.CadText
import com.example.tugis3.core.cad.model.Point
import com.example.tugis3.core.cad.model.CadPolygon
import com.example.tugis3.core.cad.model.CadPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

@Composable
fun CadScreen(vm: CadViewModel, snackbarHost: SnackbarHostState) {
    val entities by vm.simplifiedFilteredEntities.collectAsState()
    val picked by vm.pickedPoints.collectAsState()
    val status by vm.status.collectAsState()
    val mode by vm.mode.collectAsState()
    val layers by vm.layers.collectAsState()
    val activeLayers by vm.activeLayers.collectAsState()
    val snapEnabled by vm.snapEnabled.collectAsState()
    val selectionMode by vm.selectionMode.collectAsState()
    val selectedEntity by vm.selectedEntity.collectAsState()
    val gridVisible by vm.gridVisible.collectAsState()
    val stake by vm.stakeout.collectAsState()
    val dynamicSnap by vm.dynamicSnap.collectAsState()
    val effectiveSnap by vm.effectiveSnapTolerancePx.collectAsState()
    val clusterEnabled by vm.clusterEnabled.collectAsState()
    val clusters by vm.clusters.collectAsState()
    val snapWorldMode by vm.snapWorldMode.collectAsState()
    val snapWorldTol by vm.snapWorldToleranceM.collectAsState()
    val undoDepth by vm.undoDepth.collectAsState()
    val redoDepth by vm.redoDepth.collectAsState()
    val snapTolPx by vm.snapTolerancePx.collectAsState()
    val scope = rememberCoroutineScope()

    val bounds = remember(entities) { computeBounds(entities) }
    val gnssLatLon = vm.currentLatLon()
    val nearestLatLon = vm.selectedEntityNearestPointLatLon()
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current

    var showMap by rememberSaveable { mutableStateOf(true) }
    var mapType by rememberSaveable { mutableStateOf(MapType.NORMAL) }
    var followGnss by rememberSaveable { mutableStateOf(true) }
    var showGnss by rememberSaveable { mutableStateOf(true) }
    var showGuidance by rememberSaveable { mutableStateOf(true) }
    var hideCanvas by rememberSaveable { mutableStateOf(false) }
    var mapTheme by rememberSaveable { mutableStateOf(MapTheme.AUTO) }
    var syncViewport by rememberSaveable { mutableStateOf(false) }

    val styleOptions = remember(mapTheme, systemDark, context) {
        when (mapTheme) {
            MapTheme.AUTO -> if (systemDark) loadDarkStyle(context) else null
            MapTheme.DARK -> loadDarkStyle(context)
            MapTheme.LIGHT -> null
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { showMap = !showMap }, label = { Text(if (showMap) "Harita Gizle" else "Harita Göster") })
            AssistChip(onClick = {
                mapType = when(mapType){ MapType.NORMAL -> MapType.SATELLITE; MapType.SATELLITE -> MapType.TERRAIN; MapType.TERRAIN -> MapType.HYBRID; MapType.HYBRID -> MapType.NORMAL; else -> MapType.NORMAL }
            }, label = { Text("Tip") })
            AssistChip(onClick = { followGnss = !followGnss }, label = { Text(if (followGnss) "Takip✔" else "Takip") })
            AssistChip(onClick = { showGnss = !showGnss }, label = { Text(if (showGnss) "GNSS✔" else "GNSS") })
            AssistChip(onClick = { showGuidance = !showGuidance }, label = { Text(if (showGuidance) "Rehber✔" else "Rehber") })
            AssistChip(onClick = { hideCanvas = !hideCanvas }, label = { Text(if (hideCanvas) "CAD Kapalı" else "CAD Açık") })
            AssistChip(onClick = { syncViewport = !syncViewport }, label = { Text(if (syncViewport) "Senk✔" else "Senk") })
            AssistChip(onClick = { mapTheme = when(mapTheme){ MapTheme.AUTO -> MapTheme.LIGHT; MapTheme.LIGHT -> MapTheme.DARK; MapTheme.DARK -> MapTheme.AUTO } }, label = { Text("Tema: ${mapTheme.label}") })
            AssistChip(onClick = { vm.toggleDynamicSnap() }, label = { Text(if (dynamicSnap) "Snap Dyn ${effectiveSnap}px" else "Snap Fix ${effectiveSnap}px") })
        }
        // İkinci satır ek kontroller
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { vm.toggleSnap() }, label = { Text(if (snapEnabled) "Snap✔" else "Snap") })
            AssistChip(onClick = { vm.cycleSnapTolerance() }, label = { Text("TolPx:${snapTolPx}") })
            AssistChip(onClick = { vm.toggleSnapWorldMode() }, label = { Text(if (snapWorldMode) "W-Snap✔" else "W-Snap") })
            AssistChip(onClick = { vm.cycleSnapWorldTolerance() }, label = { Text("W:${String.format(Locale.US, "%.1f", snapWorldTol)}m") })
            AssistChip(onClick = { vm.toggleCluster() }, label = { Text(if (clusterEnabled) "Clus:${clusters.size}" else "Clus Kapalı") })
            AssistChip(onClick = { vm.toggleSelectionMode() }, label = { Text(if (selectionMode) "Seçim✔" else "Seçim") })
            AssistChip(onClick = { vm.undoPicked() }, enabled = undoDepth>0, label = { Text("Undo(${undoDepth})") })
            AssistChip(onClick = { vm.redoPicked() }, enabled = redoDepth>0, label = { Text("Redo(${redoDepth})") })
            InfoHelpChip()
        }
        // Durum kartı
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(8.dp)) {
                Text(status, style = MaterialTheme.typography.bodySmall)
                if (picked.size >= 2) {
                    Text(
                        "Seçim: ${picked.size} nokta | Toplam mesafe: ${"%.3f".format(vm.totalDistance())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (picked.size >= 3) {
                    Text(
                        "Alan: ${"%.3f".format(vm.polygonArea())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (clusterEnabled) {
                    Text("Cluster R≈${"%.1f".format(vm.currentClusterRadiusMeters())}m", style = MaterialTheme.typography.labelSmall)
                }
                // Layer filtreleri
                if (layers.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = activeLayers.size == layers.size,
                                onClick = {
                                    val allActive = activeLayers.size == layers.size
                                    vm.setAllLayers(!allActive)
                                },
                                label = { Text(if (activeLayers.size == layers.size) "Tümü" else "Tümü Yok") }
                            )
                        }
                        items(layers) { layer ->
                            FilterChip(
                                selected = layer in activeLayers,
                                onClick = { vm.toggleLayer(layer) },
                                label = { Text(layer) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
                // Seçilen noktalar hızlı düzenleme
                if (picked.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Noktalar:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(picked.indices.toList()) { idx ->
                            AssistChip(
                                onClick = { vm.removePoint(idx) },
                                label = { Text("${idx+1}") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
        // Harita + CAD
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val cameraState = rememberCameraPositionState()
            LaunchedEffect(cameraState.position.zoom) { val z = cameraState.position.zoom; if (z>0f) vm.updateZoom(z) }
            // Harita
            if (showMap) {
                LaunchedEffect(followGnss, gnssLatLon) {
                    if (followGnss && gnssLatLon != null) {
                        val ll = LatLng(gnssLatLon.first, gnssLatLon.second)
                        cameraState.position = CameraPosition.fromLatLngZoom(ll, cameraState.position.zoom.takeIf { it>0 } ?: 18f)
                    }
                }
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    properties = MapProperties(mapType = mapType, mapStyleOptions = styleOptions),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    cameraPositionState = cameraState
                ) {
                    if (showGnss && gnssLatLon != null) {
                        Marker(state = com.google.maps.android.compose.rememberMarkerState(position = LatLng(gnssLatLon.first, gnssLatLon.second)), title = "GNSS")
                    }
                    if (showGuidance && gnssLatLon != null && nearestLatLon != null && selectedEntity != null) {
                        val from = LatLng(gnssLatLon.first, gnssLatLon.second)
                        val to = LatLng(nearestLatLon.first, nearestLatLon.second)
                        Polyline(points = listOf(from, to), color = Color.Cyan, width = 6f)
                        val mid = LatLng((from.latitude + to.latitude)/2.0, (from.longitude + to.longitude)/2.0)
                        val dist = stake.horizontalDistance?.let { d -> "Δ=${"%.3f".format(d)} m" }
                        if (dist != null) Marker(state = com.google.maps.android.compose.rememberMarkerState(position = mid), title = dist)
                        val selName = selectedEntity?.let { it::class.simpleName ?: "Entity" }
                        Marker(state = com.google.maps.android.compose.rememberMarkerState(position = to), title = selName)
                    }
                    // Cluster markerları
                    if (clusterEnabled) {
                        clusters.forEach { cl ->
                            val ll = vm.localToLatLon(cl.center.x, cl.center.y)
                            if (ll!=null) {
                                Marker(
                                    state = com.google.maps.android.compose.rememberMarkerState(position = LatLng(ll.first, ll.second)),
                                    title = if (cl.members.size>1) "${cl.members.size} nokta" else "1",
                                    onClick = {
                                        if (cl.members.size>1 && cameraState.position.zoom < 21f) {
                                            scope.launch { cameraState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(ll.first,ll.second), cameraState.position.zoom + 2f)) }
                                        }
                                        true
                                    }
                                )
                            }
                        }
                    }
                    // Geometri çizimleri
                    entities.forEach { e ->
                        val isSelected = (e === selectedEntity)
                        when(e){
                            is CadLine -> {
                                val a = vm.localToLatLon(e.start.x, e.start.y)
                                val b = vm.localToLatLon(e.end.x, e.end.y)
                                if (a!=null && b!=null) {
                                    Polyline(points = listOf(LatLng(a.first,a.second), LatLng(b.first,b.second)), color = if (isSelected) Color.Yellow else Color.Cyan, width = if (isSelected) 8f else 4f)
                                }
                            }
                            is CadPolyline -> {
                                val pts = e.points.mapNotNull { p -> vm.localToLatLon(p.x,p.y)?.let { LatLng(it.first,it.second) } }
                                if (pts.size >= 2) Polyline(points = pts, color = if (isSelected) Color.Yellow else Color.Green, width = if (isSelected) 8f else 4f)
                            }
                            is CadPolygon -> {
                                val outer = e.rings.first().mapNotNull { p -> vm.localToLatLon(p.x,p.y)?.let { LatLng(it.first,it.second) } }
                                if (outer.size >= 3) {
                                    Polygon(points = outer, fillColor = if (isSelected) Color(0x44FFFF00) else Color(0x3300FF00), strokeColor = if (isSelected) Color.Yellow else Color.Green, strokeWidth = if (isSelected) 6f else 2f)
                                    // Hole ringlerini Polyline olarak çiz
                                    if (e.rings.size > 1) {
                                        e.rings.drop(1).forEach { holeRing ->
                                            val holePts = holeRing.mapNotNull { p -> vm.localToLatLon(p.x,p.y)?.let { LatLng(it.first,it.second) } }
                                            if (holePts.size >= 2) {
                                                Polyline(points = holePts + holePts.first(), color = if (isSelected) Color.Yellow.copy(alpha = 0.7f) else Color.Red.copy(alpha = 0.6f), width = if (isSelected) 7f else 4f)
                                            }
                                        }
                                    }
                                }
                            }
                            is CadCircle -> {
                                // Basit örnek: 36 nokta ile çevre
                                val center = vm.localToLatLon(e.center.x, e.center.y)
                                if (center!=null) {
                                    val list = (0 until 36).map { i ->
                                        val ang = i * 10.0 * Math.PI/180
                                        val ex = e.center.x + e.radius * cos(ang)
                                        val ny = e.center.y + e.radius * sin(ang)
                                        vm.localToLatLon(ex, ny)
                                    }.mapNotNull { it?.let { LatLng(it.first,it.second) } }
                                    if (list.size>2) Polyline(points = list + list.first(), color = if (isSelected) Color.Yellow else Color.Magenta, width = if (isSelected) 7f else 3f)
                                }
                            }
                            is CadArc -> {
                                val center = vm.localToLatLon(e.center.x, e.center.y)
                                if (center!=null){
                                    val sweep = ((e.endAngleDeg - e.startAngleDeg).let { if (it<0) it+360 else it })
                                    val steps = (sweep/8).toInt().coerceAtLeast(4)
                                    val list = (0..steps).map { idx ->
                                        val ang = Math.toRadians((e.startAngleDeg + idx * (sweep/steps)) )
                                        val ex = e.center.x + e.radius * sin(ang)
                                        val ny = e.center.y + e.radius * cos(ang)
                                        vm.localToLatLon(ex, ny)
                                    }.mapNotNull { it?.let { LatLng(it.first,it.second) } }
                                    if (list.size>=2) Polyline(points = list, color = if (isSelected) Color.Yellow else Color.Red, width = if (isSelected) 8f else 4f)
                                }
                            }
                            is CadText -> {
                                val ll = vm.localToLatLon(e.position.x, e.position.y)
                                if (ll!=null) Marker(state = com.google.maps.android.compose.rememberMarkerState(position = LatLng(ll.first,ll.second)), title = e.text, alpha = if (isSelected) 1f else 0.8f)
                            }
                            is CadPoint -> if (!clusterEnabled) {
                                val ll = vm.localToLatLon(e.position.x, e.position.y)
                                if (ll!=null) Marker(state = com.google.maps.android.compose.rememberMarkerState(position = LatLng(ll.first,ll.second)), title = "P", alpha = if (isSelected) 1f else 0.8f)
                            }
                        }
                    }
                }
            }
            if (!hideCanvas) {
                Box(Modifier.matchParentSize()) {
                    if (entities.isNotEmpty()) {
                        CadCanvas(
                            entities = entities,
                            picked = picked,
                            bounds = bounds,
                            mode = mode,
                            snapEnabled = snapEnabled,
                            snapTolerancePx = effectiveSnap.toFloat(),
                            selectionMode = selectionMode,
                            selectedEntity = selectedEntity,
                            gridVisible = gridVisible,
                            syncViewport = syncViewport,
                            worldSnapMode = snapWorldMode,
                            worldSnapTolM = snapWorldTol.toFloat(),
                            onSelectEntity = { vm.selectEntity(it) },
                            onPick = { vm.addPicked(it) }
                        )
                    } else {
                        Text("DXF yok / yüklenemedi", color = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                    }
                    if (selectedEntity != null) {
                        SelectedEntityInfo(entity = selectedEntity!!, vm = vm, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                    }
                    // Stakeout Panel
                    if (stake.hasFix) {
                        Card(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xAA222222))
                        ) {
                            Column(Modifier.padding(10.dp).widthIn(min = 220.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("CAD Stakeout", style = MaterialTheme.typography.labelLarge, color = Color.White)
                                Text("Fix: ${stake.fixLabel}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                if (stake.entityType != null) Text("Entity: ${stake.entityType}", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                val dist = stake.horizontalDistance?.let { String.format(Locale.US, "%.3f m", it) } ?: "-"
                                val bearing = stake.bearingDeg?.let { String.format(Locale.US, "%.1f°", it) } ?: "-"
                                Text("ΔE: ${stake.deltaE?.let { String.format(Locale.US, "%.3f", it) } ?: "-"}  ΔN: ${stake.deltaN?.let { String.format(Locale.US, "%.3f", it) } ?: "-"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("Uzaklık: $dist  Yön: $bearing", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text(stake.message, color = if (stake.canSave) MaterialTheme.colorScheme.primary else Color.Yellow, style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { vm.selectEntity(null) }, enabled = selectedEntity!=null, modifier = Modifier.weight(1f)) { Text("Seçimi Sil") }
                                    Button(onClick = {
                                        val ok = vm.saveStakePoint()
                                        if (ok) scope.launch { snackbarHost.showSnackbar("Stake kaydedildi") }
                                    }, enabled = stake.canSave, modifier = Modifier.weight(1f)) { Text("Kaydet") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class MapTheme(val label: String) { AUTO("O"), LIGHT("A"), DARK("K") }

// Rewritten as non-composable helper to allow calling inside remember block
private fun loadDarkStyle(context: Context): MapStyleOptions? {
    return try {
        val id = context.resources.getIdentifier("map_dark_style", "raw", context.packageName)
        if (id != 0) {
            val raw = context.resources.openRawResource(id).bufferedReader().use { it.readText() }
            MapStyleOptions(raw)
        } else null
    } catch (_: Exception) { null }
}

@Composable
private fun InfoHelpChip() {
    var show by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { show = !show }, label = { Text("Info") }, leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) })
        if (show) {
            Surface(shadowElevation = 4.dp, tonalElevation = 2.dp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp)) {
                Column(Modifier.padding(8.dp).widthIn(max = 260.dp)) {
                    Text("Snap Dyn: zoom'a göre piksel toleransı ölçekler.", fontSize = 11.sp)
                    Text("W-Snap: Model (metre) uzayında tolerans.", fontSize = 11.sp)
                    Text("Cluster: Yakın CAD noktalarını grupla.", fontSize = 11.sp)
                    Text("Undo/Redo: Ölçüm noktası yığınları.", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SelectedEntityInfo(entity: CadEntity, vm: CadViewModel, modifier: Modifier = Modifier) {
    val info = remember(entity) {
        val base = when(entity) {
            is CadLine -> {
                val len = CadViewModel.polylineLength(listOf(entity.start, entity.end))
                "Line | L=${String.format(Locale.US, "%.3f", len)}"
            }
            is CadPolyline -> {
                val len = CadViewModel.polylineLength(entity.points)
                val area = if (entity.isClosed) CadViewModel.polygonAreaOf(entity.points) else null
                "Polyline | pts=${entity.points.size} L=${String.format(Locale.US, "%.3f", len)}" + (if (area!=null) " A=${String.format(Locale.US, "%.3f", area)}" else "") + if (entity.isClosed) " (closed)" else ""
            }
            is CadPolygon -> {
                val outer = entity.rings.first()
                val perim = CadViewModel.polylineLength(outer + outer.first())
                val gross = CadViewModel.polygonAreaOf(outer)
                val net = vm.polygonNetArea(entity.rings)
                "Polygon | rings=${entity.rings.size} gross=${"%.3f".format(gross)} net=${"%.3f".format(net)} P=${"%.3f".format(perim)}"
            }
            is CadCircle -> {
                val c = CadViewModel.circleCircumference(entity.radius)
                val a = CadViewModel.circleArea(entity.radius)
                "Circle | R=${String.format(Locale.US, "%.3f", entity.radius)} C=${String.format(Locale.US, "%.3f", c)} A=${"%.3f".format(a)}"
            }
            is CadArc -> {
                val arcLen = CadViewModel.arcLength(entity.radius, entity.startAngleDeg, entity.endAngleDeg)
                val sweep = ((entity.endAngleDeg - entity.startAngleDeg).let { if (it<0) it+360 else it })
                "Arc | R=${String.format(Locale.US, "%.3f", entity.radius)} Δ=${String.format(Locale.US, "%.1f", sweep)}° L=${String.format(Locale.US, "%.3f", arcLen)}"
            }
            is CadText -> "Text \"${entity.text}\" h=${entity.height}"
            is CadPoint -> "Point (${String.format(Locale.US, "%.3f", entity.position.x)}, ${String.format(Locale.US, "%.3f", entity.position.y)})"
        }
        base + " | Layer=${entity.layer}" + (entity.colorIndex?.let { " | ACI=$it" } ?: "")
    }
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xAA222222))) {
        Text(info, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

// CadCanvas güncellemesi: worldSnapMode parametresi
@Composable
private fun CadCanvas(
    entities: List<CadEntity>,
    picked: List<Point>,
    bounds: Bounds,
    @Suppress("UNUSED_PARAMETER") mode: CadViewModel.MeasurementMode,
    snapEnabled: Boolean,
    snapTolerancePx: Float,
    selectionMode: Boolean,
    selectedEntity: CadEntity?,
    gridVisible: Boolean,
    syncViewport: Boolean,
    worldSnapMode: Boolean,
    worldSnapTolM: Float,
    onSelectEntity: (CadEntity?) -> Unit,
    onPick: (Point) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var snapPointScreen by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun allVertices(): List<Point> = buildList {
        entities.forEach { e ->
            when(e) {
                is CadLine -> { add(e.start); add(e.end) }
                is CadPolyline -> addAll(e.points)
                is CadPolygon -> addAll(e.rings.first())
                is CadCircle -> add(e.center)
                is CadArc -> add(e.center)
                is CadText -> add(e.position)
                is CadPoint -> add(e.position)
            }
        }
    }

    fun lineDistancePx(a: Offset, b: Offset, p: Offset): Float {
        val ab = b - a
        val ap = p - a
        val ab2 = ab.x * ab.x + ab.y * ab.y
        val t = if (ab2 == 0f) 0f else (ap.x * ab.x + ap.y * ab.y) / ab2
        val clamped = t.coerceIn(0f,1f)
        val proj = a + ab * clamped
        return (proj - p).getDistance()
    }

    fun entityHitTest(off: Offset, scale: Float, pan: Offset, w: Float, h: Float): Pair<CadEntity, Float>? {
        var best: CadEntity? = null
        var bestD = Float.MAX_VALUE
        entities.forEach { e ->
            when(e) {
                is CadLine -> {
                    val s = (worldToScreen(e.start, w.toDouble(), h.toDouble(), bounds) * scale) + pan
                    val e2 = (worldToScreen(e.end, w.toDouble(), h.toDouble(), bounds) * scale) + pan
                    val d = lineDistancePx(s,e2,off)
                    if (d < bestD) { bestD = d; best = e }
                }
                is CadPolyline -> {
                    val pts = e.points.map { (worldToScreen(it, w.toDouble(), h.toDouble(), bounds) * scale) + pan }
                    for (i in 1 until pts.size) {
                        val d = lineDistancePx(pts[i-1], pts[i], off)
                        if (d < bestD) { bestD = d; best = e }
                    }
                    if (e.isClosed && pts.size>2) {
                        val d = lineDistancePx(pts.last(), pts.first(), off)
                        if (d < bestD) { bestD = d; best = e }
                    }
                }
                is CadPolygon -> {
                    val pts = e.rings.first().map { (worldToScreen(it, w.toDouble(), h.toDouble(), bounds) * scale) + pan }
                    for (i in 1 until pts.size) {
                        val d = lineDistancePx(pts[i-1], pts[i], off)
                        if (d < bestD) { bestD = d; best = e }
                    }
                    if (pts.size > 2) {
                        val d = lineDistancePx(pts.last(), pts.first(), off)
                        if (d < bestD) { bestD = d; best = e }
                    }
                }
                is CadCircle -> {
                    val c = (worldToScreen(e.center, w.toDouble(), h.toDouble(), bounds) * scale) + pan
                    val r = worldRadiusToPx(e.radius, bounds, w.toInt(), h.toInt(), scale)
                    val d = abs((c - off).getDistance() - r)
                    if (d < bestD) { bestD = d; best = e }
                }
                is CadArc -> {
                    val c = (worldToScreen(e.center, w.toDouble(), h.toDouble(), bounds) * scale) + pan
                    val r = worldRadiusToPx(e.radius, bounds, w.toInt(), h.toInt(), scale)
                    val v = off - c
                    val ang = Math.toDegrees(atan2((-v.y).toDouble(), v.x.toDouble())).let { if (it < 0) it + 360 else it }
                    val start = e.startAngleDeg
                    val end = e.endAngleDeg
                    val inRange = if (start <= end) (ang >= start && ang <= end) else (ang >= start || ang <= end)
                    if (inRange) {
                        val d = abs(v.getDistance() - r)
                        if (d < bestD) { bestD = d; best = e }
                    }
                }
                is CadText -> {
                    val pos = (worldToScreen(e.position, w.toDouble(), h.toDouble(), bounds) * scale) + pan
                    val d = (pos - off).getDistance()
                    if (d < bestD) { bestD = d; best = e }
                }
                is CadPoint -> {
                    val pos = (worldToScreen(e.position, w.toDouble(), h.toDouble(), bounds) * scale) + pan
                    val d = (pos - off).getDistance()
                    if (d < bestD) { bestD = d; best = e }
                }
            }
        }
        return best?.let { it to bestD }
    }

    fun entityBounding(e: CadEntity): Bounds = when(e) {
        is CadLine -> Bounds(min(e.start.x,e.end.x), min(e.start.y,e.end.y), max(e.start.x,e.end.x), max(e.start.y,e.end.y))
        is CadPolyline -> {
            val xs = e.points.map{it.x}
            val ys = e.points.map{it.y}
            Bounds(xs.minOrNull()?:0.0, ys.minOrNull()?:0.0, xs.maxOrNull()?:0.0, ys.maxOrNull()?:0.0)
        }
        is CadPolygon -> {
            val ring = e.rings.first()
            val xs = ring.map{it.x}; val ys = ring.map{it.y}
            Bounds(xs.minOrNull()?:0.0, ys.minOrNull()?:0.0, xs.maxOrNull()?:0.0, ys.maxOrNull()?:0.0)
        }
        is CadCircle -> Bounds(e.center.x - e.radius, e.center.y - e.radius, e.center.x + e.radius, e.center.y + e.radius)
        is CadArc -> Bounds(e.center.x - e.radius, e.center.y - e.radius, e.center.x + e.radius, e.center.y + e.radius)
        is CadText -> Bounds(e.position.x, e.position.y, e.position.x, e.position.y)
        is CadPoint -> Bounds(e.position.x, e.position.y, e.position.x, e.position.y)
    }

    fun overlaps(a: Bounds, b: Bounds) = a.maxX >= b.minX && a.minX <= b.maxX && a.maxY >= b.minY && a.minY <= b.maxY

    Canvas(modifier = Modifier
        .onSizeChanged { canvasSize = it }
        .pointerInput(entities, bounds, picked, snapEnabled, selectionMode, snapTolerancePx, canvasSize, worldSnapMode, worldSnapTolM) {
            detectTapGestures { off ->
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                if (selectionMode) {
                    val hit = entityHitTest(off, scale, pan, w, h)
                    if (hit != null && hit.second <= 40f) onSelectEntity(hit.first) else onSelectEntity(null)
                } else {
                    val modelRaw = screenToWorldInverse(off, w.toDouble(), h.toDouble(), bounds, scale, pan)
                    var finalPoint = modelRaw
                    if (snapEnabled) {
                        if (worldSnapMode) {
                            // Model uzayında tolerans (metre)
                            val verts = allVertices()
                            var best: Point? = null
                            var bestD = Double.MAX_VALUE
                            verts.forEach { v ->
                                val d = hypot(v.x - modelRaw.x, v.y - modelRaw.y)
                                if (d < bestD) { bestD = d; best = v }
                            }
                            if (best != null && bestD <= worldSnapTolM) {
                                finalPoint = best
                                val base = worldToScreen(best, w.toDouble(), h.toDouble(), bounds)
                                snapPointScreen = (base * scale) + pan
                            } else {
                                snapPointScreen = null
                            }
                        } else {
                            val verts = allVertices()
                            var best: Point? = null
                            var bestDist = Float.MAX_VALUE
                            verts.forEach { v ->
                                val base = worldToScreen(v, w.toDouble(), h.toDouble(), bounds)
                                val scr = (base * scale) + pan
                                val d = (scr - off).getDistance()
                                if (d < bestDist) { bestDist = d; best = v }
                            }
                            if ((best == null || bestDist > snapTolerancePx)) {
                                generateGridIntersections(bounds).forEach { v ->
                                    val base = worldToScreen(v, w.toDouble(), h.toDouble(), bounds)
                                    val scr = (base * scale) + pan
                                    val d = (scr - off).getDistance()
                                    if (d < bestDist) { bestDist = d; best = v }
                                }
                            }
                            if (best != null && bestDist <= snapTolerancePx) {
                                finalPoint = best
                                val base = worldToScreen(best, w.toDouble(), h.toDouble(), bounds)
                                snapPointScreen = (base * scale) + pan
                            } else { snapPointScreen = null }
                        }
                    } else { snapPointScreen = null }
                    onPick(finalPoint)
                }
            }
        }
        .pointerInput(entities, bounds, picked, syncViewport) {
            // Senk açıkken canvas gesture devre dışı
            if (!syncViewport) {
                detectTransformGestures { _, panChange, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.25f, 20f)
                    pan += panChange
                }
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val wF = size.width
        val hF = size.height
        val w0 = screenToWorldInverse(Offset(0f,0f), w.toDouble(), h.toDouble(), bounds, scale, pan)
        val w1 = screenToWorldInverse(Offset(wF,hF), w.toDouble(), h.toDouble(), bounds, scale, pan)
        val vis = Bounds(min(w0.x,w1.x), min(w0.y,w1.y), max(w0.x,w1.x), max(w0.y,w1.y))

        drawLine(Color.DarkGray, start = Offset(0f, hF / 2f), end = Offset(wF, hF / 2f), strokeWidth = 1f)
        drawLine(Color.DarkGray, start = Offset(wF / 2f, 0f), end = Offset(wF / 2f, hF), strokeWidth = 1f)

        fun applyTransform(o: Offset): Offset = Offset(o.x * scale + pan.x, o.y * scale + pan.y)

        val drawEntities = entities.filter { overlaps(entityBounding(it), vis) }
        drawEntities.forEach { e ->
            val highlighted = (e === selectedEntity)
            when (e) {
                is CadLine -> {
                    val baseColor = if (highlighted) Color.Yellow else aciToColor(e.colorIndex)
                    val s = applyTransform(worldToScreen(e.start, w.toDouble(), h.toDouble(), bounds))
                    val e2 = applyTransform(worldToScreen(e.end, w.toDouble(), h.toDouble(), bounds))
                    drawLine(baseColor, s, e2, strokeWidth = (if (highlighted) 4f else 2f) * scale)
                }
                is CadPolyline -> {
                    val baseColor = if (highlighted) Color.Yellow else aciToColor(e.colorIndex)
                    val pts = e.points.map { applyTransform(worldToScreen(it, w.toDouble(), h.toDouble(), bounds)) }
                    for (i in 1 until pts.size) drawLine(baseColor, pts[i-1], pts[i], (if (highlighted) 4f else 2f) * scale)
                    if (e.isClosed && pts.size > 2) drawLine(baseColor, pts.last(), pts.first(), (if (highlighted) 4f else 2f) * scale)
                }
                is CadPolygon -> {
                    val baseColor = if (highlighted) Color.Yellow else aciToColor(e.colorIndex)
                    val outer = e.rings.first()
                    val pts = outer.map { applyTransform(worldToScreen(it, w.toDouble(), h.toDouble(), bounds)) }
                    for (i in 1 until pts.size) drawLine(baseColor, pts[i-1], pts[i], (if (highlighted) 4f else 2f) * scale)
                    if (pts.size > 2) drawLine(baseColor, pts.last(), pts.first(), (if (highlighted) 4f else 2f) * scale)
                    if (e.rings.size > 1) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(12f * scale, 8f * scale), 0f)
                        e.rings.drop(1).forEach { hole ->
                            if (hole.size < 2) return@forEach
                            val hpts = hole.map { applyTransform(worldToScreen(it, w.toDouble(), h.toDouble(), bounds)) }
                            for (i in 1 until hpts.size) drawLine(
                                color = baseColor.copy(alpha = 0.6f),
                                start = hpts[i-1],
                                end = hpts[i],
                                strokeWidth = (if (highlighted) 3f else 1.5f) * scale,
                                pathEffect = dash
                            )
                            if (hpts.size > 2) drawLine(
                                color = baseColor.copy(alpha = 0.6f),
                                start = hpts.last(),
                                end = hpts.first(),
                                strokeWidth = (if (highlighted) 3f else 1.5f) * scale,
                                pathEffect = dash
                            )
                        }
                    }
                }
                is CadCircle -> {
                    val baseColor = if (highlighted) Color.Yellow else aciToColor(e.colorIndex)
                    val c = applyTransform(worldToScreen(e.center, w.toDouble(), h.toDouble(), bounds))
                    val radiusPx = worldRadiusToPx(e.radius, bounds, w.toInt(), h.toInt(), scale)
                    drawCircle(
                        color = baseColor.copy(alpha = 0.9f),
                        center = c,
                        radius = radiusPx,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = (if (highlighted) 4f else 2f) * scale)
                    )
                }
                is CadArc -> {
                    val baseColor = if (highlighted) Color.Yellow else aciToColor(e.colorIndex)
                    val c = applyTransform(worldToScreen(e.center, w.toDouble(), h.toDouble(), bounds))
                    val radiusPx = worldRadiusToPx(e.radius, bounds, w.toInt(), h.toInt(), scale)
                    var sweep = e.endAngleDeg - e.startAngleDeg
                    if (sweep == 0.0) sweep = 360.0
                    val startAndroid = -e.startAngleDeg.toFloat()
                    val sweepAndroid = -sweep.toFloat()
                    val leftTop = Offset(c.x - radiusPx, c.y - radiusPx)
                    val sizeArc = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f)
                    drawArc(
                        color = baseColor,
                        startAngle = startAndroid,
                        sweepAngle = sweepAndroid,
                        useCenter = false,
                        topLeft = leftTop,
                        size = sizeArc,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = (if (highlighted) 4f else 2f) * scale)
                    )
                }
                is CadText -> {
                    val baseColor = if (highlighted) Color.Yellow else aciToColor(e.colorIndex)
                    val pos = applyTransform(worldToScreen(e.position, w.toDouble(), h.toDouble(), bounds))
                    // nativeCanvas kaldırıldı: metin yerine basit gösterge
                    drawCircle(color = baseColor, center = pos, radius = (3f * scale).coerceAtLeast(2f))
                }
                is CadPoint -> {
                    val baseColor = if (highlighted) Color.Yellow else aciToColor(e.colorIndex)
                    val c = applyTransform(worldToScreen(e.position, w.toDouble(), h.toDouble(), bounds))
                    drawCircle(color = baseColor, center = c, radius = (4f * scale).coerceAtLeast(2f))
                }
            }
        }
        // Bu kodu Compose ortamında doğrudan kullanmak yerine, Text composable ile gösterin.
        if (gridVisible) drawGrid(bounds, scale, pan)
        snapPointScreen?.let { sp ->
            drawCircle(Color.Yellow, radius = 10f, center = sp, alpha = 0.85f)
            drawCircle(Color.Black, radius = 4f, center = sp)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(bounds: Bounds, scale: Float, pan: Offset) {
    val w = size.width
    val h = size.height
    val wF = size.width
    val hF = size.height
    val majorTarget = 8
    val stepWorldX = niceStep(bounds.width / majorTarget)
    val stepWorldY = niceStep(bounds.height / majorTarget)
    val startX = floor(bounds.minX / stepWorldX) * stepWorldX
    val endX = bounds.maxX
    val startY = floor(bounds.minY / stepWorldY) * stepWorldY
    val endY = bounds.maxY
    val gridColor = Color(0x22FFFFFF)
    var x = startX
    while (x <= endX) {
        val base = worldToScreen(Point(x, bounds.minY), w.toDouble(), h.toDouble(), bounds)
        val scr = (base * scale) + pan
        drawLine(gridColor, start = Offset(scr.x, 0f), end = Offset(scr.x, hF), strokeWidth = 1f)
        x += stepWorldX
    }
    var y = startY
    while (y <= endY) {
        val base = worldToScreen(Point(bounds.minX, y), w.toDouble(), h.toDouble(), bounds)
        val scr = (base * scale) + pan
        drawLine(gridColor, start = Offset(0f, scr.y), end = Offset(wF, scr.y), strokeWidth = 1f)
        y += stepWorldY
    }
}

private fun niceStep(raw: Double): Double {
    if (raw <= 0) return 1.0
    val exp = floor(log10(raw)).toInt()
    val scale = 10.0.pow(exp.toDouble())
    val base = raw / scale
    val nice = when {
        base < 1.5 -> 1.0
        base < 3.5 -> 2.0
        base < 7.5 -> 5.0
        else -> 10.0
    }
    return nice * scale
}

private fun worldToScreen(pt: Point, w: Double, h: Double, b: Bounds): Offset {
    val sx = (pt.x - b.minX) / b.width
    val sy = 1 - (pt.y - b.minY) / b.height // y ekseni ters
    return Offset((sx * w).toFloat(), (sy * h).toFloat())
}

private fun screenToWorldInverse(off: Offset, w: Double, h: Double, b: Bounds, scale: Float, pan: Offset): Point {
    // Ters dönüşüm: (off - pan)/scale -> base screen -> world
    val base = (off - pan) / scale
    val nx = base.x / w
    val ny = base.y / h
    val wx = b.minX + nx * b.width
    val wy = b.minY + (1 - ny) * b.height
    return Point(wx, wy)
}

private fun aciToColor(aci: Int?): Color = when (aci) {
    1 -> Color(0xFFFF0000) // Red
    2 -> Color(0xFFFFFF00) // Yellow
    3 -> Color(0xFF00FF00) // Green
    4 -> Color(0xFF00FFFF) // Cyan
    5 -> Color(0xFF0000FF) // Blue
    6 -> Color(0xFFFF00FF) // Magenta
    7 -> Color(0xFFFFFFFF) // White
    else -> Color(0xFF4FC3F7)
}

private fun generateGridIntersections(bounds: Bounds): List<Point> {
    val majorTarget = 8
    val stepX = niceStep(bounds.width / majorTarget)
    val stepY = niceStep(bounds.height / majorTarget)
    val startX = floor(bounds.minX / stepX) * stepX
    val endX = bounds.maxX
    val startY = floor(bounds.minY / stepY) * stepY
    val endY = bounds.maxY
    val out = mutableListOf<Point>()
    var x = startX
    while (x <= endX) {
        var y = startY
        while (y <= endY) { out += Point(x,y); y += stepY }
        x += stepX
    }
    return out
}

private fun worldRadiusToPx(r: Double, b: Bounds, w: Int, h: Int, scale: Float): Float {
    val worldMax = max(b.width, b.height).takeIf { it > 0 } ?: 1.0
    val viewMin = min(w, h).toFloat()
    return ((r / worldMax) * viewMin).toFloat() * scale
}

fun exportCadAndMap(context: Context, view: View) {
    val bitmap = getBitmapFromView(view)
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "cad_export_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Paylaş"))
}

fun getBitmapFromView(view: View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    view.draw(canvas)
    return bitmap
}

// Bounds ve computeBounds geri eklendi
private data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double) {
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
}

private fun computeBounds(entities: List<CadEntity>): Bounds {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    fun acc(p: Point) { minX = min(minX, p.x); maxX = max(maxX, p.x); minY = min(minY, p.y); maxY = max(maxY, p.y) }
    entities.forEach { e ->
        when (e) {
            is CadLine -> { acc(e.start); acc(e.end) }
            is CadPolyline -> e.points.forEach(::acc)
            is CadPolygon -> e.rings.first().forEach(::acc)
            is CadCircle -> { acc(Point(e.center.x - e.radius, e.center.y - e.radius)); acc(Point(e.center.x + e.radius, e.center.y + e.radius)) }
            is CadArc -> { acc(Point(e.center.x - e.radius, e.center.y - e.radius)); acc(Point(e.center.x + e.radius, e.center.y + e.radius)) }
            is CadText -> acc(e.position)
            is CadPoint -> acc(e.position)
        }
    }
    if (minX == Double.POSITIVE_INFINITY) return Bounds(0.0,0.0,1.0,1.0)
    val pad = 0.05 * max(maxX - minX, maxY - minY)
    return Bounds(minX - pad, minY - pad, maxX + pad, maxY + pad)
}
