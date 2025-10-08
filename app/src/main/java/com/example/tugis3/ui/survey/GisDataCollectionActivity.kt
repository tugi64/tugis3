package com.example.tugis3.ui.survey

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.maps.android.SphericalUtil

@AndroidEntryPoint
class GisDataCollectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId = intent?.getLongExtra("projectId", 1L) ?: 1L
        setContent { Tugis3Theme { GisDataCollectionRoot(projectId = projectId, onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GisDataCollectionRoot(projectId: Long, onBack: () -> Unit, vm: GisDataCollectionViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(projectId) { vm.setProjectId(projectId) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap: Map<String, Boolean> ->
        val granted = (grantMap[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (grantMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        vm.setPermissionGranted(granted)
    }

    LaunchedEffect(ui.autoCenter, ui.currentLocation) {
        val loc = ui.currentLocation
        if (ui.autoCenter && loc != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(loc, 18f),
                durationMs = 600
            )
        }
    }

    val onMapClick: (LatLng) -> Unit = { latLng ->
        if (ui.editingExistingId == null && !ui.selectionMode) vm.addVertex(latLng)
    }

    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    val txt = ins.bufferedReader().readText()
                    vm.importGeoJson(txt)
                }
            } catch (e: Exception) {
                vm.notify("Import hata: ${e.message}")
            }
        }
    }
    var shareMenu by remember { mutableStateOf(false) }

    // Filtered features (shared for map & list & header)
    val filteredFeatures by remember(ui.features, ui.activeLayerFilters, ui.showPoints, ui.showLines, ui.showPolygons) {
        mutableStateOf(
            ui.features.filter { f ->
                (ui.activeLayerFilters.isEmpty() || f.layer in ui.activeLayerFilters) &&
                    when (f.geometry) {
                        is GisGeometry.Point -> ui.showPoints
                        is GisGeometry.LineString -> ui.showLines
                        is GisGeometry.Polygon -> ui.showPolygons
                        is GisGeometry.PolygonWithHoles -> ui.showPolygons
                    }
            }
        )
    }

    Scaffold(
        topBar = {
            val selectionMode = ui.selectionMode
            TopAppBar(
                title = { if (selectionMode) Text("Seçilen: ${ui.selectedIds.size}") else Text("GIS Veri Toplama (P$projectId)") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    val selectionMode = ui.selectionMode
                    if (selectionMode) {
                        IconButton(onClick = { vm.selectAllFeatures() }) { Icon(Icons.Filled.SelectAll, contentDescription = "Tümünü Seç") }
                        IconButton(onClick = { vm.invertSelection() }) { Icon(Icons.Filled.Flip, contentDescription = "Terse Çevir") }
                        IconButton(onClick = { if (ui.selectedIds.isNotEmpty()) vm.deleteSelected() }) { Icon(Icons.Default.Delete, contentDescription = "Seçileni Sil") }
                        IconButton(onClick = { vm.toggleSelectionMode() }) { Icon(Icons.Default.Close, contentDescription = "Seçimi Kapat") }
                    } else {
                        IconButton(onClick = { vm.toggleAutoCenter() }) {
                            Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Oto Merkez")
                        }
                        IconButton(onClick = { vm.toggleSnap() }) {
                            Icon(Icons.Filled.GridOn, tint = if (ui.snapEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current, contentDescription = "Snap")
                        }
                        IconButton(onClick = { vm.toggleClusters() }) {
                            Icon(Icons.Filled.Group, contentDescription = "Cluster", tint = if (ui.showClusters) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        Box {
                            IconButton(onClick = {
                                if (ui.features.isEmpty()) {
                                    vm.notify("Paylaşılacak feature yok")
                                } else {
                                    shareMenu = true
                                }
                            }) { Icon(Icons.Default.Share, contentDescription = "GeoJSON Paylaş") }
                            DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                                DropdownMenuItem(text = { Text("Tümü Export") }, onClick = {
                                    shareGeoJson(context = context, projectId = projectId, json = vm.buildGeoJsonFeatureCollection(selectedOnly = false))
                                    shareMenu = false
                                })
                                DropdownMenuItem(text = { Text("Seçilenler Export", maxLines = 1, overflow = TextOverflow.Ellipsis) }, enabled = ui.selectedIds.isNotEmpty(), onClick = {
                                    shareGeoJson(context = context, projectId = projectId, json = vm.buildGeoJsonFeatureCollection(selectedOnly = true))
                                    shareMenu = false
                                })
                            }
                        }
                        IconButton(onClick = { importLauncher.launch("application/*") }) { Icon(Icons.Filled.FileUpload, contentDescription = "Import GeoJSON") }
                        IconButton(onClick = { vm.toggleSelectionMode() }) { Icon(Icons.Filled.List, contentDescription = "Seçim Modu") }
                        var mapMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { mapMenu = true }) { Icon(Icons.Outlined.Map, contentDescription = "Harita Tipi") }
                            DropdownMenu(expanded = mapMenu, onDismissRequest = { mapMenu = false }) {
                                MapType.entries.forEach { mt ->
                                    DropdownMenuItem(
                                        text = { Text(mt.name) },
                                        onClick = {
                                            vm.setMapType(mt)
                                            mapMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val editingExisting = ui.editingExistingId != null
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editingExisting) {
                    ExtendedFloatingActionButton(
                        text = { Text("Kaydet") },
                        icon = { Icon(Icons.Default.Save, contentDescription = null) },
                        onClick = { vm.saveEditingExisting() },
                        expanded = false
                    )
                    FloatingActionButton(onClick = { vm.cancelEditingExisting() }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                } else {
                    if (ui.editingType != "Nokta") {
                        ExtendedFloatingActionButton(
                            text = { Text("Tamamla") },
                            icon = { Icon(Icons.Default.Done, contentDescription = null) },
                            onClick = { vm.finalizeComplexGeometry() },
                            expanded = false
                        )
                        FloatingActionButton(onClick = { vm.undoLastVertex() }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        }
                    }
                    FloatingActionButton(onClick = { vm.clearAll() }, containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Üst kontrol paneli
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = ui.editingType == "Nokta", onClick = { vm.setEditingType("Nokta") }, label = { Text("Nokta") })
                FilterChip(selected = ui.editingType == "Çizgi", onClick = { vm.setEditingType("Çizgi") }, label = { Text("Çizgi") })
                FilterChip(selected = ui.editingType == "Alan", onClick = { vm.setEditingType("Alan") }, label = { Text("Alan") })
            }
            OutlinedTextField(
                value = ui.attrText,
                onValueChange = { vm.updateAttr(it) },
                label = { Text("Öznitelik") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
            // Ölçüm bilgi satırı
            if (ui.editingVertices.isNotEmpty() && (ui.editingLengthMeters != null || ui.editingAreaSquareMeters != null)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ui.editingLengthMeters?.let { len ->
                        AssistChip(onClick = {}, label = { Text("Uzunluk: ${formatLength(len)}") })
                    }
                    if (ui.editingType == "Alan") {
                        ui.editingAreaSquareMeters?.let { a ->
                            AssistChip(onClick = {}, label = { Text("Alan: ${formatArea(a)}") })
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(onClick = { vm.toggleShowPoints() }, label = { Text("P") }, leadingIcon = {
                    Icon(if (ui.showPoints) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = if (ui.showPoints) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                })
                AssistChip(onClick = { vm.toggleShowLines() }, label = { Text("L") }, leadingIcon = {
                    Icon(if (ui.showLines) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = if (ui.showLines) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                })
                AssistChip(onClick = { vm.toggleShowPolygons() }, label = { Text("A") }, leadingIcon = {
                    Icon(if (ui.showPolygons) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = if (ui.showPolygons) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                })
            }
            OutlinedTextField(
                value = ui.layerInput,
                onValueChange = { vm.setLayerInput(it) },
                label = { Text("Katman") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
            if (ui.availableLayers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ui.availableLayers.forEach { layer ->
                        FilterChip(
                            selected = layer in ui.activeLayerFilters,
                            onClick = { vm.toggleLayerFilter(layer) },
                            label = { Text(layer) },
                            leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) }
                        )
                    }
                }
            }
            // Snap mesafe ayarları ve Undo/Redo butonları
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Snap ${ui.snapDistanceMeters.toInt()} m", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(90.dp))
                Slider(
                    value = ui.snapDistanceMeters.toFloat(),
                    onValueChange = { vm.setSnapDistance(it) },
                    valueRange = 1f..50f,
                    steps = 49,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = { vm.geometryUndo() }, enabled = ui.undoDepth > 0, label = { Text("Undo ${ui.undoDepth}") }, leadingIcon = { Icon(Icons.Outlined.Undo, contentDescription = null) })
                AssistChip(onClick = { vm.geometryRedo() }, enabled = ui.redoDepth > 0, label = { Text("Redo ${ui.redoDepth}") }, leadingIcon = { Icon(Icons.Outlined.Redo, contentDescription = null) })
            }
            // Harita
            Box(Modifier.weight(1f)) {
                if (!ui.locationPermissionGranted) {
                    // İzin yoksa üstte placeholder + buton
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Konum izni gerekli", fontWeight = FontWeight.Bold)
                            Button(onClick = {
                                permissionLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }) { Text("İzin İste") }
                        }
                    }
                } else {
                    // Harita
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(mapType = ui.mapType, isMyLocationEnabled = ui.locationPermissionGranted),
                        uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
                        onMapClick = onMapClick
                    ) {
                        // Mevcut konum marker (opsiyonel, MyLocationEnabled zaten daire gösterir)
                        ui.currentLocation?.let {
                            Marker(
                                state = MarkerState(it),
                                title = "Konum",
                                snippet = "${it.latitude.format(6)}, ${it.longitude.format(6)}"
                            )
                        }
                        // Aktif çizim (çeşitli türler)
                        if (ui.editingType == "Çizgi" && ui.editingVertices.size >= 2) {
                            Polyline(points = ui.editingVertices, color = MaterialTheme.colorScheme.primary, width = 8f)
                        }
                        if (ui.editingType == "Alan" && ui.editingVertices.size >= 2) {
                            val closed = if (ui.editingVertices.first() != ui.editingVertices.last()) ui.editingVertices + ui.editingVertices.first() else ui.editingVertices
                            Polygon(
                                points = closed,
                                strokeColor = MaterialTheme.colorScheme.primary,
                                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                strokeWidth = 5f
                            )
                        }
                        // Aktif vertex noktaları
                        ui.editingVertices.forEachIndexed { idx, v ->
                            Marker(
                                state = MarkerState(v),
                                title = "${ui.editingType} V${idx + 1}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                        // Kayıtlı feature'lar
                        val pointFeatures = filteredFeatures.filter { it.geometry is GisGeometry.Point }
                        val useClusters = ui.showClusters && pointFeatures.size > 1 && ui.showPoints
                        val clusterMarkers: List<Triple<LatLng, Int, List<GisFeature>>> = if (useClusters) {
                            val thresholdMeters = 60.0 // temel cluster yarıçapı
                            val clusters = mutableListOf<MutableList<GisFeature>>()
                            pointFeatures.forEach { f ->
                                val pt = (f.geometry as GisGeometry.Point).position
                                var added = false
                                for (c in clusters) {
                                    val center = (c.first().geometry as GisGeometry.Point).position
                                    val dist = SphericalUtil.computeDistanceBetween(center, pt)
                                    if (dist < thresholdMeters) {
                                        c.add(f)
                                        added = true
                                        break
                                    }
                                }
                                if (!added) clusters.add(mutableListOf(f))
                            }
                            clusters.map { group ->
                                val avgLat = group.map { (it.geometry as GisGeometry.Point).position.latitude }.average()
                                val avgLon = group.map { (it.geometry as GisGeometry.Point).position.longitude }.average()
                                Triple(LatLng(avgLat, avgLon), group.size, group.toList())
                            }
                        } else emptyList()
                        filteredFeatures.forEach { f ->
                            if (useClusters && f.geometry is GisGeometry.Point) return@forEach // pointler cluster modunda tekil çizilmeyecek
                            val selected = f.id in ui.selectedIds
                            val lineColor = if (selected) Color(0xFFFFA000) else Color(0xFF1976D2)
                            val polyStroke = if (selected) Color(0xFFFFA000) else Color(0xFF388E3C)
                            val polyFill = if (selected) Color(0x55FFA000) else Color(0x55388E3C)
                            when (val g = f.geometry) {
                                is GisGeometry.Point -> Marker(
                                    state = MarkerState(g.position),
                                    title = "#${f.id} ${f.type}",
                                    snippet = listOfNotNull(f.attr.takeIf { it.isNotBlank() }, f.layer).joinToString(" | ").takeIf { it.isNotBlank() },
                                    onClick = {
                                        if (ui.selectionMode) vm.toggleSelectFeature(f.id) else vm.startEditingFeature(f.id)
                                        true
                                    },
                                    icon = if (selected) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW) else BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED
                                    )
                                )
                                is GisGeometry.LineString -> Polyline(
                                    points = g.vertices,
                                    color = lineColor,
                                    width = 6f,
                                    onClick = {
                                        if (ui.selectionMode) vm.toggleSelectFeature(f.id) else vm.startEditingFeature(f.id)
                                        true
                                    }
                                )
                                is GisGeometry.Polygon -> Polygon(
                                    points = g.vertices,
                                    strokeColor = polyStroke,
                                    fillColor = polyFill,
                                    strokeWidth = 4f,
                                    onClick = {
                                        if (ui.selectionMode) vm.toggleSelectFeature(f.id) else vm.startEditingFeature(f.id)
                                        true
                                    }
                                )
                                is GisGeometry.PolygonWithHoles -> Polygon(
                                    points = g.outer,
                                    holes = g.holes,
                                    strokeColor = polyStroke,
                                    fillColor = polyFill,
                                    strokeWidth = 4f,
                                    onClick = {
                                        if (ui.selectionMode) vm.toggleSelectFeature(f.id) else vm.startEditingFeature(f.id)
                                        true
                                    }
                                )
                            }
                        }
                        // Cluster marker çizimi
                        clusterMarkers.forEach { (center, count, group) ->
                            val title = if (count == 1) "#${group.first().id}" else "$count nokta"
                            Marker(
                                state = MarkerState(center),
                                title = title,
                                snippet = if (count == 1) group.first().attr else group.take(3).joinToString { it.attr }.takeIf { it.isNotBlank() },
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                    }
                    // Sağ üst mini butonlar
                    Column(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        FilledIconButton(onClick = {
                            if (ui.currentLocation == null) return@FilledIconButton
                            vm.toggleAutoCenter()
                        }) { Icon(Icons.Default.GpsFixed, contentDescription = "Merkez") }
                        FilledIconButton(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) }) {
                            Icon(Icons.Default.MyLocation, contentDescription = "İzin Kontrol")
                        }
                    }
                }

                // Alt list paneli (expandable)
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kaydedilen (${filteredFeatures.size}/${ui.features.size})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Gizle" else "Göster") }
                        }
                        if (expanded) {
                            HorizontalDivider()
                            LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                                items(filteredFeatures) { f ->
                                    val selected = f.id in ui.selectedIds
                                    ListItem(
                                        headlineContent = { Text("#${f.id} ${f.type}") },
                                        supportingContent = { Text(listOfNotNull(f.attr.takeIf { it.isNotBlank() }, f.layer).joinToString(" | ")) },
                                        leadingContent = {
                                            if (ui.selectionMode) {
                                                Checkbox(checked = selected, onCheckedChange = { vm.toggleSelectFeature(f.id) })
                                            }
                                        },
                                        trailingContent = {
                                            if (!ui.selectionMode) {
                                                TextButton(onClick = { vm.startEditingFeature(f.id) }) { Text("Düzenle") }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Mesaj snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }
    Box(Modifier.fillMaxSize()) { SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)) }
}

private fun Double.format(decimals: Int) = String.format("%1$.${decimals}f", this)

private fun formatLength(meters: Double): String = if (meters >= 1000) {
    "${(meters / 1000).format(2)} km"
} else "${meters.format(2)} m"

private fun formatArea(areaM2: Double): String = when {
    areaM2 >= 1_000_000 -> "${(areaM2 / 1_000_000).format(2)} km²"
    areaM2 >= 10_000 -> "${(areaM2 / 10_000).format(2)} ha" // hektar
    else -> "${areaM2.format(2)} m²"
}

// Paylaşım fonksiyonu (menu kullanımı için)
private fun shareGeoJson(context: android.content.Context, projectId: Long, json: String) {
    val dir = File(context.filesDir, "exports").apply { mkdirs() }
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(dir, "features_p${projectId}_$ts.geojson")
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "com.example.tugis3.fileprovider", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/geo+json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, "GeoJSON Paylaş"))
}
