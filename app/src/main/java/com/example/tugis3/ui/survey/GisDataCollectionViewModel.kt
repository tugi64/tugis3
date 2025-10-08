package com.example.tugis3.ui.survey

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.maps.android.compose.MapType
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.tugis3.data.repository.GisFeatureRepository
import org.json.JSONObject
import com.google.maps.android.SphericalUtil
import kotlin.math.pow

sealed class GisGeometry {
    data class Point(val position: LatLng): GisGeometry()
    data class LineString(val vertices: List<LatLng>): GisGeometry()
    data class Polygon(val vertices: List<LatLng>): GisGeometry() // simple ring (closed or open)
    data class PolygonWithHoles(val outer: List<LatLng>, val holes: List<List<LatLng>>): GisGeometry()
}

data class GisFeature(
    val id: Long,
    val type: String,
    val attr: String,
    val geometry: GisGeometry,
    val layer: String? = null
)

data class GisUiState(
    val features: List<GisFeature> = emptyList(),
    val editingType: String = "Nokta",
    val editingVertices: List<LatLng> = emptyList(),
    val attrText: String = "",
    val mapType: MapType = MapType.NORMAL,
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val message: String? = null,
    val autoCenter: Boolean = true,
    val editingLengthMeters: Double? = null,
    val editingAreaSquareMeters: Double? = null,
    val editingExistingId: Long? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    // Yeni opsiyonel state
    val snapEnabled: Boolean = false,
    val showPoints: Boolean = true,
    val showLines: Boolean = true,
    val showPolygons: Boolean = true,
    val availableLayers: Set<String> = emptySet(),
    val activeLayerFilters: Set<String> = emptySet(),
    val layerInput: String = "Default",
    val totalPointCount: Int = 0,
    val totalLineCount: Int = 0,
    val totalPolygonCount: Int = 0,
    val totalLineLength: Double = 0.0,
    val totalPolygonArea: Double = 0.0,
    val clusterMode: Boolean = false,
    val showClusters: Boolean = false,
    val snapDistanceMeters: Double = 5.0,
    val undoDepth: Int = 0,
    val redoDepth: Int = 0,
    val dynamicSnap: Boolean = false,
    val snapEffectiveMeters: Double = 5.0,
    val clusterRadiusMeters: Double = 60.0,
    // Düzenlenen poligon deliklerini tutmak için
    val editingHoles: List<List<LatLng>> = emptyList()
)

@HiltViewModel
class GisDataCollectionViewModel @Inject constructor(
    private val fused: FusedLocationProviderClient,
    private val repository: GisFeatureRepository
): ViewModel() {

    private val _ui = MutableStateFlow(GisUiState())
    val ui: StateFlow<GisUiState> = _ui.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var observing = false
    private var projectId: Long? = null
    private var lastZoom: Float = 18f

    fun setProjectId(id: Long) {
        if (projectId == id) return
        projectId = id
        if (!observing) {
            observing = true
            viewModelScope.launch {
                repository.observe(id).collect { list ->
                    val feats = list.mapNotNull { e ->
                        val geom = e.geometryJson?.let { parseGeometry(it) } ?: return@mapNotNull null
                        GisFeature(
                            id = e.id,
                            type = e.type,
                            attr = e.attr ?: "-",
                            geometry = geom,
                            layer = e.layer
                        )
                    }
                    _ui.value = _ui.value.copy(features = feats)
                    recomputeSummary()
                }
            }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _ui.value = _ui.value.copy(locationPermissionGranted = granted)
        if (granted) startLocationUpdates() else stopLocationUpdates()
    }

    fun setMapType(type: MapType) { _ui.value = _ui.value.copy(mapType = type) }
    fun setEditingType(type: String) { _ui.value = _ui.value.copy(editingType = type, editingVertices = emptyList(), editingLengthMeters = null, editingAreaSquareMeters = null) }
    fun updateAttr(text: String) { _ui.value = _ui.value.copy(attrText = text) }
    fun toggleAutoCenter() { _ui.value = _ui.value.copy(autoCenter = !_ui.value.autoCenter) }

    private val undoStack = ArrayDeque<List<LatLng>>()
    private val redoStack = ArrayDeque<List<LatLng>>()

    private fun pushUndoState() {
        undoStack.addLast(_ui.value.editingVertices.toList())
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        refreshUndoRedoDepth()
    }

    private fun refreshUndoRedoDepth() {
        _ui.value = _ui.value.copy(undoDepth = undoStack.size, redoDepth = redoStack.size)
    }

    fun geometryUndo() {
        if (undoStack.isEmpty()) return
        val current = _ui.value.editingVertices
        redoStack.addLast(current)
        val prev = undoStack.removeLast()
        _ui.value = _ui.value.copy(editingVertices = prev)
        computeEditingMetrics()
        refreshUndoRedoDepth()
    }

    fun geometryRedo() {
        if (redoStack.isEmpty()) return
        val current = _ui.value.editingVertices
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        _ui.value = _ui.value.copy(editingVertices = next)
        computeEditingMetrics()
        refreshUndoRedoDepth()
    }

    fun toggleSnap() { _ui.value = _ui.value.copy(snapEnabled = !_ui.value.snapEnabled) }
    fun toggleShowPoints() { _ui.value = _ui.value.copy(showPoints = !_ui.value.showPoints) }
    fun toggleShowLines() { _ui.value = _ui.value.copy(showLines = !_ui.value.showLines) }
    fun toggleShowPolygons() { _ui.value = _ui.value.copy(showPolygons = !_ui.value.showPolygons) }
    fun setSnapDistance(m: Float) { _ui.value = _ui.value.copy(snapDistanceMeters = m.coerceIn(1f, 50f).toDouble()) }
    fun toggleClusters() { _ui.value = _ui.value.copy(showClusters = !_ui.value.showClusters) }
    fun toggleDynamicSnap() { _ui.value = _ui.value.copy(dynamicSnap = !_ui.value.dynamicSnap); adaptiveRecalc() }

    fun setLayerInput(layer: String) { _ui.value = _ui.value.copy(layerInput = layer) }
    fun toggleLayerFilter(layer: String) {
        val st = _ui.value
        val new = st.activeLayerFilters.toMutableSet().apply { if (!add(layer)) remove(layer) }
        _ui.value = st.copy(activeLayerFilters = new)
    }

    private fun recomputeSummary() {
        val feats = _ui.value.features
        var lineLen = 0.0
        var polyArea = 0.0
        var pc = 0; var lc = 0; var ac = 0
        val layerSet = mutableSetOf<String>()
        feats.forEach { f ->
            f.layer?.let { if (it.isNotBlank()) layerSet.add(it) }
            when (val g = f.geometry) {
                is GisGeometry.Point -> pc++
                is GisGeometry.LineString -> { lc++; lineLen += (0 until g.vertices.size - 1).sumOf { i -> SphericalUtil.computeDistanceBetween(g.vertices[i], g.vertices[i+1]) } }
                is GisGeometry.Polygon -> { ac++; polyArea += SphericalUtil.computeArea(g.vertices) }
                is GisGeometry.PolygonWithHoles -> { ac++; val outerArea = SphericalUtil.computeArea(g.outer); val holesArea = g.holes.sumOf { SphericalUtil.computeArea(it) }; polyArea += (outerArea - holesArea).coerceAtLeast(0.0) }
            }
        }
        _ui.value = _ui.value.copy(
            totalPointCount = pc,
            totalLineCount = lc,
            totalPolygonCount = ac,
            totalLineLength = lineLen,
            totalPolygonArea = polyArea,
            availableLayers = layerSet,
            clusterMode = pc > 40 && _ui.value.mapType == MapType.NORMAL
        )
    }

    fun addVertex(latLng: LatLng) {
        val snapped = maybeSnap(latLng)
        val st = _ui.value
        when (st.editingType) {
            "Nokta" -> {
                val pid = projectId ?: run { emitMessage("Proje yok (projectId) - kaydedilemedi"); return }
                val geom = GisGeometry.Point(snapped)
                val geoJson = toGeoJson(geom)
                viewModelScope.launch {
                    repository.add(
                        projectId = pid,
                        type = st.editingType,
                        attr = st.attrText.ifBlank { null },
                        geometryJson = geoJson,
                        layer = st.layerInput.ifBlank { "Default" }
                    )
                }
                _ui.value = st.copy(attrText = "")
            }
            "Çizgi", "Alan" -> {
                pushUndoState()
                _ui.value = st.copy(editingVertices = st.editingVertices + snapped)
                computeEditingMetrics()
            }
        }
    }

    fun undoLastVertex() {
        val st = _ui.value
        if (st.editingVertices.isNotEmpty()) {
            _ui.value = st.copy(editingVertices = st.editingVertices.dropLast(1))
            computeEditingMetrics()
        }
    }

    fun finalizeComplexGeometry() {
        val st = _ui.value
        val pid = projectId ?: run { emitMessage("Proje yok (projectId) - kaydedilemedi"); return }
        when (st.editingType) {
            "Çizgi" -> if (st.editingVertices.size >= 2) {
                val geom = GisGeometry.LineString(st.editingVertices)
                val geoJson = toGeoJson(geom)
                viewModelScope.launch {
                    repository.add(pid, st.editingType, st.attrText.ifBlank { null }, geoJson, st.layerInput.ifBlank { "Default" })
                }
                _ui.value = st.copy(editingVertices = emptyList(), attrText = "", editingLengthMeters = null, editingAreaSquareMeters = null)
                undoStack.clear(); redoStack.clear(); refreshUndoRedoDepth()
            } else emitMessage("Yetersiz nokta sayısı")
            "Alan" -> if (st.editingVertices.size >= 3) {
                val verts = st.editingVertices.let { if (it.first() != it.last()) it + it.first() else it }
                val geom = GisGeometry.Polygon(verts)
                val geoJson = toGeoJson(geom)
                viewModelScope.launch {
                    repository.add(pid, st.editingType, st.attrText.ifBlank { null }, geoJson, st.layerInput.ifBlank { "Default" })
                }
                _ui.value = st.copy(editingVertices = emptyList(), attrText = "", editingLengthMeters = null, editingAreaSquareMeters = null)
                undoStack.clear(); redoStack.clear(); refreshUndoRedoDepth()
            } else emitMessage("Yetersiz nokta sayısı")
            else -> emitMessage("Geçersiz finalize türü")
        }
    }

    fun clearAll() {
        val pid = projectId
        if (pid == null) {
            _ui.value = _ui.value.copy(features = emptyList(), editingVertices = emptyList())
            return
        }
        viewModelScope.launch { repository.clear(pid) }
        _ui.value = _ui.value.copy(editingVertices = emptyList(), editingLengthMeters = null, editingAreaSquareMeters = null)
    }

    fun clearMessage() { _ui.value = _ui.value.copy(message = null) }

    private fun emitMessage(msg: String) { _ui.value = _ui.value.copy(message = msg) }

    private fun computeEditingMetrics() {
        val st = _ui.value
        val verts = st.editingVertices
        if (verts.size < 2) {
            _ui.value = st.copy(editingLengthMeters = null, editingAreaSquareMeters = null)
            return
        }
        val length = (0 until verts.size - 1).sumOf { i ->
            SphericalUtil.computeDistanceBetween(verts[i], verts[i + 1])
        }
        if (st.editingType == "Çizgi") {
            _ui.value = st.copy(editingLengthMeters = length, editingAreaSquareMeters = null)
        } else if (st.editingType == "Alan") {
            // Alan için en az 3 vertex; kapatarak alan hesapla
            if (verts.size >= 3) {
                val closed = if (verts.first() != verts.last()) verts + verts.first() else verts
                val area = SphericalUtil.computeArea(closed)
                _ui.value = st.copy(editingLengthMeters = length, editingAreaSquareMeters = area)
            } else {
                _ui.value = st.copy(editingLengthMeters = length, editingAreaSquareMeters = null)
            }
        }
    }

    // --- GeoJSON Helpers ---
    private fun toGeoJson(geometry: GisGeometry): String = when (geometry) {
        is GisGeometry.Point -> "{" + "\"type\":\"Point\",\"coordinates\":[${geometry.position.longitude},${geometry.position.latitude}]}"
        is GisGeometry.LineString -> {
            val coords = geometry.vertices.joinToString(prefix = "[", postfix = "]") { "[${it.longitude},${it.latitude}]" }
            "{" + "\"type\":\"LineString\",\"coordinates\":$coords}"
        }
        is GisGeometry.Polygon -> {
            val ring = geometry.vertices.joinToString(prefix = "[", postfix = "]") { "[${it.longitude},${it.latitude}]" }
            "{" + "\"type\":\"Polygon\",\"coordinates\":[$ring]}"
        }
        is GisGeometry.PolygonWithHoles -> {
            val outer = geometry.outer.joinToString(prefix = "[", postfix = "]") { "[${it.longitude},${it.latitude}]" }
            val holes = geometry.holes.joinToString(separator = ",") { ring ->
                ring.joinToString(prefix = "[", postfix = "]") { "[${it.longitude},${it.latitude}]" }
            }
            val rings = buildString {
                append("[")
                append(outer)
                if (holes.isNotBlank()) { append(","); append(holes) }
                append("]")
            }
            "{" + "\"type\":\"Polygon\",\"coordinates\":$rings}"
        }
    }

    // parseGeometry yeniden eklendi (PolygonWithHoles desteği ile)
    private fun parseGeometry(json: String): GisGeometry? {
        return try {
            val obj = JSONObject(json)
            when (obj.getString("type")) {
                "Point" -> {
                    val arr = obj.getJSONArray("coordinates")
                    GisGeometry.Point(LatLng(arr.getDouble(1), arr.getDouble(0)))
                }
                "LineString" -> {
                    val arr = obj.getJSONArray("coordinates")
                    val list = mutableListOf<LatLng>()
                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONArray(i)
                        list.add(LatLng(c.getDouble(1), c.getDouble(0)))
                    }
                    GisGeometry.LineString(list)
                }
                "Polygon" -> {
                    val arr = obj.getJSONArray("coordinates")
                    if (arr.length() == 0) return null
                    val outerArr = arr.getJSONArray(0)
                    val outer = mutableListOf<LatLng>()
                    for (i in 0 until outerArr.length()) {
                        val c = outerArr.getJSONArray(i)
                        outer.add(LatLng(c.getDouble(1), c.getDouble(0)))
                    }
                    val holes = mutableListOf<List<LatLng>>()
                    for (r in 1 until arr.length()) {
                        val ringArr = arr.getJSONArray(r)
                        val ring = mutableListOf<LatLng>()
                        for (i in 0 until ringArr.length()) {
                            val c = ringArr.getJSONArray(i)
                            ring.add(LatLng(c.getDouble(1), c.getDouble(0)))
                        }
                        if (ring.size >= 3) holes.add(ring)
                    }
                    if (holes.isEmpty()) GisGeometry.Polygon(outer) else GisGeometry.PolygonWithHoles(outer, holes)
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }

    fun startLocationUpdates() {
        if (locationCallback != null) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc: Location = result.lastLocation ?: return
                val ll = LatLng(loc.latitude, loc.longitude)
                val st = _ui.value
                _ui.value = st.copy(currentLocation = ll)
            }
        }
        try {
            fused.requestLocationUpdates(req, locationCallback as LocationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) { }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }

    fun startEditingFeature(id: Long) {
        val feature = _ui.value.features.firstOrNull { it.id == id } ?: return
        val (type, verts, holes) = when (val g = feature.geometry) {
            is GisGeometry.Point -> Triple("Nokta", listOf(g.position), emptyList())
            is GisGeometry.LineString -> Triple("Çizgi", g.vertices, emptyList())
            is GisGeometry.Polygon -> Triple(
                "Alan",
                g.vertices.let { if (it.size >= 2 && it.first() == it.last()) it.dropLast(1) else it },
                emptyList()
            )
            is GisGeometry.PolygonWithHoles -> {
                val outer = g.outer.let { if (it.size >= 2 && it.first() == it.last()) it.dropLast(1) else it }
                Triple("Alan", outer, g.holes)
            }
        }
        _ui.value = _ui.value.copy(
            editingExistingId = feature.id,
            editingType = type,
            editingVertices = verts,
            editingLengthMeters = null,
            editingAreaSquareMeters = null,
            editingHoles = holes
        )
        computeEditingMetrics()
    }

    fun cancelEditingExisting() {
        _ui.value = _ui.value.copy(
            editingExistingId = null,
            editingVertices = emptyList(),
            editingLengthMeters = null,
            editingAreaSquareMeters = null,
            editingHoles = emptyList()
        )
        undoStack.clear(); redoStack.clear(); refreshUndoRedoDepth()
    }

    fun updateEditingVertex(index: Int, newPos: LatLng) {
        val snapped = maybeSnap(newPos)
        val st = _ui.value
        if (index < 0 || index >= st.editingVertices.size) return
        pushUndoState()
        val mut = st.editingVertices.toMutableList()
        mut[index] = snapped
        _ui.value = st.copy(editingVertices = mut)
        computeEditingMetrics()
    }

    fun saveEditingExisting() {
        val st = _ui.value
        val id = st.editingExistingId ?: return
        when (st.editingType) {
            "Nokta" -> if (st.editingVertices.size == 1) {
                val geom = GisGeometry.Point(st.editingVertices.first())
                val geoJson = toGeoJson(geom)
                viewModelScope.launch { repository.updateGeometry(id, geoJson) }
            } else emitMessage("Nokta için 1 vertex gerekli")
            "Çizgi" -> if (st.editingVertices.size >= 2) {
                val geom = GisGeometry.LineString(st.editingVertices)
                val geoJson = toGeoJson(geom)
                viewModelScope.launch { repository.updateGeometry(id, geoJson) }
            } else emitMessage("Çizgi için 2 vertex gerekli")
            "Alan" -> if (st.editingVertices.size >= 3) {
                val verts = st.editingVertices.let { if (it.first() != it.last()) it + it.first() else it }
                val geom = if (st.editingHoles.isNotEmpty()) GisGeometry.PolygonWithHoles(verts, st.editingHoles) else GisGeometry.Polygon(verts)
                val geoJson = toGeoJson(geom)
                viewModelScope.launch { repository.updateGeometry(id, geoJson) }
            } else emitMessage("Alan için 3 vertex gerekli")
        }
        cancelEditingExisting()
    }

    fun buildGeoJsonFeatureCollection(selectedOnly: Boolean = false): String {
        val featsSource = if (selectedOnly && _ui.value.selectedIds.isNotEmpty()) _ui.value.features.filter { it.id in _ui.value.selectedIds } else _ui.value.features
        val featuresJson = featsSource.joinToString(separator = ",") { f ->
            val geomJson = when (val g = f.geometry) {
                is GisGeometry.Point -> toGeoJson(g)
                is GisGeometry.LineString -> toGeoJson(g)
                is GisGeometry.Polygon -> toGeoJson(g)
                is GisGeometry.PolygonWithHoles -> toGeoJson(g)
            }
            val safeAttr = JSONObject.quote(f.attr)
            val layerProp = f.layer?.let { "\"layer\":\"${it}\"," } ?: ""
            "{" +
                "\"type\":\"Feature\"," +
                "\"id\":${f.id}," +
                "\"properties\":{${layerProp}\"type\":\"${f.type}\",\"attr\":$safeAttr}," +
                "\"geometry\":$geomJson" +
            "}"
        }
        return "{" + "\"type\":\"FeatureCollection\",\"features\":[" + featuresJson + "]}"
    }

    fun notify(msg: String) = emitMessage(msg)

    fun toggleSelectionMode() {
        val st = _ui.value
        _ui.value = st.copy(selectionMode = !st.selectionMode, selectedIds = emptySet())
    }

    fun toggleSelectFeature(id: Long) {
        val st = _ui.value
        if (!st.selectionMode) return
        val newSet = st.selectedIds.toMutableSet().apply { if (!add(id)) remove(id) }
        _ui.value = st.copy(selectedIds = newSet)
    }

    fun clearSelection() {
        val st = _ui.value
        _ui.value = st.copy(selectedIds = emptySet())
    }

    fun deleteSelected() {
        val st = _ui.value
        val ids = st.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch { repository.delete(ids.toList()) }
        _ui.value = st.copy(selectedIds = emptySet(), selectionMode = false)
    }

    fun selectAllFeatures() {
        val st = _ui.value
        if (!st.selectionMode) return
        _ui.value = st.copy(selectedIds = st.features.map { it.id }.toSet())
    }

    fun invertSelection() {
        val st = _ui.value
        if (!st.selectionMode) return
        val all = st.features.map { it.id }.toSet()
        val inverted = all - st.selectedIds + (st.selectedIds - all)
        _ui.value = st.copy(selectedIds = inverted)
    }

    override fun onCleared() {
        stopLocationUpdates()
        super.onCleared()
    }

    // Snapping yardımcı: en yakın vertex'i bul
    private fun maybeSnap(latLng: LatLng): LatLng {
        if (!_ui.value.snapEnabled) return latLng
        val thresholdMeters = _ui.value.snapEffectiveMeters
        var best: LatLng? = null
        var bestDist = Double.MAX_VALUE
        fun consider(v: LatLng) {
            val d = SphericalUtil.computeDistanceBetween(latLng, v)
            if (d < thresholdMeters && d < bestDist) { best = v; bestDist = d }
        }
        _ui.value.editingVertices.forEach { consider(it) }
        _ui.value.features.forEach { f ->
            when (val g = f.geometry) {
                is GisGeometry.Point -> consider(g.position)
                is GisGeometry.LineString -> g.vertices.forEach { consider(it) }
                is GisGeometry.Polygon -> g.vertices.forEach { consider(it) }
                is GisGeometry.PolygonWithHoles -> {
                    g.outer.forEach { consider(it) }
                    g.holes.forEach { hole -> hole.forEach { consider(it) } }
                }
            }
        }
        return best ?: latLng
    }

    fun importGeoJson(json: String) {
        try {
            val obj = JSONObject(json)
            if (obj.optString("type") != "FeatureCollection") {
                emitMessage("Geçersiz GeoJSON (FeatureCollection değil)")
                return
            }
            val arr = obj.getJSONArray("features")
            val pid = projectId ?: run { emitMessage("Proje yok - import iptal"); return }
            viewModelScope.launch {
                var added = 0
                for (i in 0 until arr.length()) {
                    val feat = arr.getJSONObject(i)
                    val props = feat.optJSONObject("properties")
                    val geom = feat.optJSONObject("geometry") ?: continue
                    val gType = geom.optString("type")
                    val propsType = props?.optString("type")
                    val baseType = propsType ?: gType
                    val attr = props?.optString("attr") ?: props?.optString("name")
                    val layer = props?.optString("layer")?.takeIf { it.isNotBlank() } ?: "Imported"
                    fun addSimple(typeStr: String, geometryJson: String) {
                        viewModelScope.launch {
                            repository.add(
                                projectId = pid,
                                type = typeStr.ifBlank { "?" },
                                attr = attr,
                                geometryJson = geometryJson,
                                layer = layer
                            )
                        }
                        added++
                    }
                    when (gType) {
                        "Point", "LineString", "Polygon" -> addSimple(baseType ?: gType, geom.toString())
                        "MultiPoint" -> {
                            val coords = geom.optJSONArray("coordinates") ?: continue
                            for (pi in 0 until coords.length()) {
                                val c = coords.getJSONArray(pi)
                                val single = JSONObject().apply {
                                    put("type", "Point")
                                    put("coordinates", c)
                                }
                                addSimple("Nokta", single.toString())
                            }
                        }
                        "MultiLineString" -> {
                            val lines = geom.optJSONArray("coordinates") ?: continue
                            for (li in 0 until lines.length()) {
                                val line = lines.getJSONArray(li)
                                val single = JSONObject().apply {
                                    put("type", "LineString")
                                    put("coordinates", line)
                                }
                                addSimple("Çizgi", single.toString())
                            }
                        }
                        "MultiPolygon" -> {
                            val polys = geom.optJSONArray("coordinates") ?: continue
                            for (pi in 0 until polys.length()) {
                                val poly = polys.getJSONArray(pi)
                                if (poly.length() == 0) continue
                                val outerArr = poly.getJSONArray(0)
                                val outer = org.json.JSONArray()
                                for (oi in 0 until outerArr.length()) outer.put(outerArr.getJSONArray(oi))
                                val holeList = mutableListOf<org.json.JSONArray>()
                                for (hi in 1 until poly.length()) {
                                    val hArr = poly.getJSONArray(hi)
                                    if (hArr.length() >= 3) holeList.add(hArr)
                                }
                                val ringsJson = org.json.JSONArray().apply {
                                    put(outer)
                                    holeList.forEach { put(it) }
                                }
                                val single = JSONObject().apply {
                                    put("type", "Polygon")
                                    put("coordinates", ringsJson)
                                }
                                addSimple("Alan", single.toString())
                            }
                        }
                        else -> { /* desteklenmeyen tür atlandı */ }
                    }
                }
                emitMessage("Import tamamlandı: $added feature")
            }
        } catch (e: Exception) {
            emitMessage("Import hata: ${e.message}")
        }
    }

    private fun adaptiveRecalc() {
        val st = _ui.value
        val effSnap = if (st.dynamicSnap) {
            val factor = 2.0.pow((15.0 - lastZoom).toDouble().coerceIn(-5.0, 5.0))
            (st.snapDistanceMeters * factor).coerceIn(0.5, 200.0)
        } else st.snapDistanceMeters
        val effCluster = (60.0 * 2.0.pow((14.0 - lastZoom).toDouble().coerceIn(-5.0, 5.0))).coerceIn(10.0, 500.0)
        _ui.value = st.copy(snapEffectiveMeters = effSnap, clusterRadiusMeters = effCluster)
    }

    fun updateZoom(z: Float) { lastZoom = z; adaptiveRecalc() }
}
