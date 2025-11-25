@file:Suppress("unused")
package com.example.tugis3.ui.cad

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tugis3.core.cad.export.CadExportUtil
import com.example.tugis3.core.cad.model.*
import com.example.tugis3.core.cad.repository.CadRepository
import com.example.tugis3.coord.transform.NoOpTransformer
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.data.repository.CadPersistenceRepository
import com.example.tugis3.data.repository.CadPersistenceRepository.CadItem
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.gnss.GnssEngine
import com.example.tugis3.gnss.model.GnssObservation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class CadViewModel @Inject constructor(
    private val repo: CadRepository,
    private val app: Application,
    private val projectRepo: ProjectRepository,
    private val surveyPointRepo: SurveyPointRepository,
    private val gnssEngine: GnssEngine,
    private val cadStore: CadPersistenceRepository
) : ViewModel() {

    // -------- Pick / Measure --------
    private val _pickedPoints = MutableStateFlow<List<Point>>(emptyList())
    val pickedPoints: StateFlow<List<Point>> = _pickedPoints
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status
    enum class MeasurementMode { DISTANCE, AREA }
    private val _mode = MutableStateFlow(MeasurementMode.DISTANCE)
    val mode: StateFlow<MeasurementMode> = _mode

    // -------- Layers --------
    private val _layers = MutableStateFlow<List<String>>(emptyList())
    val layers: StateFlow<List<String>> = _layers
    private val _activeLayers = MutableStateFlow<Set<String>>(emptySet())
    val activeLayers: StateFlow<Set<String>> = _activeLayers

    // -------- Snap (Pixel) & Grid --------
    private val _snapEnabled = MutableStateFlow(false)
    val snapEnabled: StateFlow<Boolean> = _snapEnabled
    private val _snapTolerancePx = MutableStateFlow(24)
    val snapTolerancePx: StateFlow<Int> = _snapTolerancePx
    private val snapPreset = intArrayOf(5,10,24,40,64)
    private val _gridVisible = MutableStateFlow(true)
    val gridVisible: StateFlow<Boolean> = _gridVisible

    // -------- Background Color --------
    private val _backgroundColor = MutableStateFlow(0xFF000000) // Default black
    val backgroundColor: StateFlow<Long> = _backgroundColor

    // -------- Dynamic Snap --------
    private val _dynamicSnap = MutableStateFlow(false)
    val dynamicSnap: StateFlow<Boolean> = _dynamicSnap
    private val _lastZoom = MutableStateFlow(18f)
    val lastZoom: StateFlow<Float> = _lastZoom
    private val _effectiveSnapTolerancePx = MutableStateFlow(24)
    val effectiveSnapTolerancePx: StateFlow<Int> = _effectiveSnapTolerancePx

    // -------- World Snap (Model Space) --------
    private val _snapWorldMode = MutableStateFlow(false)
    val snapWorldMode: StateFlow<Boolean> = _snapWorldMode
    private val worldSnapPreset = doubleArrayOf(0.5,1.0,2.0,5.0,10.0)
    private val _snapWorldToleranceM = MutableStateFlow(1.0)
    val snapWorldToleranceM: StateFlow<Double> = _snapWorldToleranceM

    // -------- Cluster --------
    data class Cluster(val center: Point, val members: List<CadPoint>)
    private val _clusterEnabled = MutableStateFlow(false)
    val clusterEnabled: StateFlow<Boolean> = _clusterEnabled
    private val _clusters = MutableStateFlow<List<Cluster>>(emptyList())
    val clusters: StateFlow<List<Cluster>> = _clusters
    private val clusterBaseRadiusM = 20.0

    // -------- Selection --------
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode
    private val _selectedId = MutableStateFlow<Long?>(null)

    // -------- Aplikasyon (Stakeout) Mode --------
    enum class ApplicationMode {
        NONE,
        POINT_TO_POINT,           // İki nokta arası
        SINGLE_POINT_DISTANCE,    // Tek noktadan mesafe/açı
        OFFSET_DISTANCE,          // Çizgiye ofset
        OFFSET_ANGLE              // Açılı ofset
    }
    private val _applicationMode = MutableStateFlow(ApplicationMode.NONE)
    val applicationMode: StateFlow<ApplicationMode> = _applicationMode

    data class ApplicationPoint(
        val point: Point,
        val entityId: Long?,
        val description: String
    )

    private val _applicationPoints = MutableStateFlow<List<ApplicationPoint>>(emptyList())
    val applicationPoints: StateFlow<List<ApplicationPoint>> = _applicationPoints

    private val _applicationDistance = MutableStateFlow(0.0)
    val applicationDistance: StateFlow<Double> = _applicationDistance

    private val _applicationAngle = MutableStateFlow(0.0)
    val applicationAngle: StateFlow<Double> = _applicationAngle

    private val _applicationOffset = MutableStateFlow(0.0)
    val applicationOffset: StateFlow<Double> = _applicationOffset

    data class ApplicationResult(
        val targetPoint: Point,
        val distance: Double,
        val bearing: Double,
        val description: String
    )

    private val _applicationResult = MutableStateFlow<ApplicationResult?>(null)
    val applicationResult: StateFlow<ApplicationResult?> = _applicationResult

    // -------- Project / Entities --------
    private val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val cadItems: StateFlow<List<CadItem>> = activeProject
        .flatMapLatest { p -> if (p==null) flowOf(emptyList()) else cadStore.observe(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val entities: StateFlow<List<CadEntity>> = cadItems.map { it.map { c -> c.entity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredEntities: StateFlow<List<CadEntity>> = combine(entities, _activeLayers) { ents, act ->
        if (act.isEmpty()) emptyList() else ents.filter { it.layer in act }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val simplifiedFilteredEntities: StateFlow<List<CadEntity>> = filteredEntities.map { ents ->
        val threshold = 500
        if (ents.size < threshold) ents else ents.map { e ->
            when(e){
                is CadPolyline -> e.copy(points = douglasPeucker(e.points, 0.5))
                is CadPolygon -> e.copy(rings = listOf(douglasPeucker(e.rings.first(), 0.5)))
                else -> e
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedEntity: StateFlow<CadEntity?> = combine(_selectedId, cadItems) { id, items ->
        if (id==null) null else items.firstOrNull { it.id == id }?.entity
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val observation: StateFlow<GnssObservation?> = gnssEngine.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // -------- Stakeout --------
    data class CadStakeoutState(
        val hasFix: Boolean = false,
        val fixLabel: String? = null,
        val currentE: Double? = null,
        val currentN: Double? = null,
        val targetE: Double? = null,
        val targetN: Double? = null,
        val deltaE: Double? = null,
        val deltaN: Double? = null,
        val horizontalDistance: Double? = null,
        val bearingDeg: Double? = null,
        val entityType: String? = null,
        val canSave: Boolean = false,
        val message: String = ""
    )
    private val _stakeout = MutableStateFlow(CadStakeoutState())
    val stakeout: StateFlow<CadStakeoutState> = _stakeout

    // -------- Undo / Redo (picked points) --------
    private val undoStack = ArrayDeque<List<Point>>()
    private val redoStack = ArrayDeque<List<Point>>()
    private val _undoDepth = MutableStateFlow(0)
    val undoDepth: StateFlow<Int> = _undoDepth
    private val _redoDepth = MutableStateFlow(0)
    val redoDepth: StateFlow<Int> = _redoDepth

    // -------- Init --------
    init {
        viewModelScope.launch { entities.collect { refreshLayersFromEntities(it) } }
        loadSampleIfEmpty()
        startGnss()
        observeStakeout()
        // Cluster güncellemeleri
        viewModelScope.launch {
            combine(entities, _lastZoom, _clusterEnabled) { ents, _, enabled -> ents to enabled }
                .collect { (ents, enabled) -> if (enabled) recomputeClusters(ents) }
        }
    }

    // -------- Layer Ops --------
    private fun refreshLayersFromEntities(list: List<CadEntity>) {
        val ls = list.map { it.layer }.distinct().sorted()
        _layers.value = ls
        if (_activeLayers.value.isEmpty()) _activeLayers.value = ls.toSet()
    }
    fun setAllLayers(enableAll: Boolean) { _activeLayers.value = if (enableAll) _layers.value.toSet() else emptySet() }
    fun toggleLayer(layer: String) { _activeLayers.value = _activeLayers.value.let { if (layer in it) it - layer else it + layer } }

    // -------- Sample Data --------
    private fun loadSampleIfEmpty() { viewModelScope.launch(Dispatchers.IO) {
        try {
            val proj = activeProject.value ?: return@launch
            if (cadItems.first().isNotEmpty()) return@launch
            addEntityPersist(CadLine(Point(0.0,0.0), Point(50.0,0.0), layer = "BASE"))
            addEntityPersist(CadLine(Point(50.0,0.0), Point(50.0,40.0), layer = "BASE"))
            addEntityPersist(CadLine(Point(50.0,40.0), Point(0.0,40.0), layer = "BASE"))
            addEntityPersist(CadLine(Point(0.0,40.0), Point(0.0,0.0), layer = "BASE"))
            addEntityPersist(CadCircle(Point(25.0,20.0), 10.0, layer = "CIRC"))
            addEntityPersist(CadText(Point(5.0,42.0), 2.5, "SAMPLE", layer = "TXT"))
        } catch (_: Exception) {}
    } }
    fun loadSample() = loadSampleIfEmpty()
    private suspend fun addEntityPersist(entity: CadEntity) { try { val proj = activeProject.value ?: return; cadStore.addEntity(proj.id, entity) } catch (_: Exception) {} }

    // -------- Selection --------
    fun toggleSelectionMode() { _selectionMode.value = !_selectionMode.value; if (!_selectionMode.value) _selectedId.value = null }
    fun selectEntity(e: CadEntity?) { if (e==null) { _selectedId.value = null; return }; _selectedId.value = cadItems.value.firstOrNull { it.entity == e }?.id }
    fun deleteSelectedEntity() { val id = _selectedId.value ?: return; viewModelScope.launch { cadStore.deleteEntity(id); _selectedId.value = null } }

    // Yeni eklenen yardımcılar
    fun toggleMode() { _mode.value = if (_mode.value == MeasurementMode.DISTANCE) MeasurementMode.AREA else MeasurementMode.DISTANCE }
    fun toggleGrid() { _gridVisible.value = !_gridVisible.value }
    fun setBackgroundColor(color: Long) { _backgroundColor.value = color }

    // -------- Zoom Extents (Bounds) --------
    data class Bounds(val minE: Double, val maxE: Double, val minN: Double, val maxN: Double, val centerLat: Double, val centerLon: Double)

    fun computeProjectBounds(): Bounds? {
        val ents = entities.value
        if (ents.isEmpty()) return null

        var minE = Double.MAX_VALUE
        var maxE = -Double.MAX_VALUE
        var minN = Double.MAX_VALUE
        var maxN = -Double.MAX_VALUE

        ents.forEach { entity ->
            when (entity) {
                is CadPoint -> {
                    minE = minOf(minE, entity.position.x)
                    maxE = maxOf(maxE, entity.position.x)
                    minN = minOf(minN, entity.position.y)
                    maxN = maxOf(maxN, entity.position.y)
                }
                is CadLine -> {
                    minE = minOf(minE, entity.start.x, entity.end.x)
                    maxE = maxOf(maxE, entity.start.x, entity.end.x)
                    minN = minOf(minN, entity.start.y, entity.end.y)
                    maxN = maxOf(maxN, entity.start.y, entity.end.y)
                }
                is CadPolyline -> {
                    entity.points.forEach { pt ->
                        minE = minOf(minE, pt.x)
                        maxE = maxOf(maxE, pt.x)
                        minN = minOf(minN, pt.y)
                        maxN = maxOf(maxN, pt.y)
                    }
                }
                is CadCircle -> {
                    minE = minOf(minE, entity.center.x - entity.radius)
                    maxE = maxOf(maxE, entity.center.x + entity.radius)
                    minN = minOf(minN, entity.center.y - entity.radius)
                    maxN = maxOf(maxN, entity.center.y + entity.radius)
                }
                is CadPolygon -> {
                    entity.rings.flatten().forEach { pt ->
                        minE = minOf(minE, pt.x)
                        maxE = maxOf(maxE, pt.x)
                        minN = minOf(minN, pt.y)
                        maxN = maxOf(maxN, pt.y)
                    }
                }
                else -> {}
            }
        }

        if (minE == Double.MAX_VALUE) return null

        val centerE = (minE + maxE) / 2.0
        val centerN = (minN + maxN) / 2.0
        val (centerLat, centerLon) = localToLatLon(centerE, centerN) ?: return null

        return Bounds(minE, maxE, minN, maxN, centerLat, centerLon)
    }

    // -------- GNSS / Transform --------
    private fun startGnss() { gnssEngine.start() }
    fun stopGnss() { gnssEngine.stop() }
    fun localToLatLon(e: Double, n: Double) = runCatching { activeProject.value?.let { ProjectionEngine.forProject(it).inverse(e,n) } }.getOrNull()
    fun latLonToLocal(lat: Double, lon: Double) = runCatching { activeProject.value?.let { ProjectionEngine.forProject(it).forward(lat,lon) } }.getOrNull()
    fun currentLatLon(): Pair<Double,Double>? { val o = observation.value ?: return null; val la = o.latDeg ?: return null; val lo = o.lonDeg ?: return null; return la to lo }
    fun selectedEntityNearestPointLatLon(): Pair<Double,Double>? {
        val ent = selectedEntity.value ?: return null
        val obs = observation.value ?: return null
        val proj = activeProject.value ?: return null
        val tf = ProjectionEngine.forProject(proj)
        val (curE, curN) = if (obs.latDeg!=null && obs.lonDeg!=null) runCatching { tf.forward(obs.latDeg, obs.lonDeg) }.getOrElse { return null } else return null
        val (tE,tN) = nearestPointOnEntity(ent, curE, curN)
        return runCatching { tf.inverse(tE,tN) }.getOrNull()
    }

    private fun observeStakeout() { viewModelScope.launch { combine(observation, selectedEntity, activeProject) { _,_,_ -> } .collect { recomputeStakeout() } } }
    private fun recomputeStakeout() {
        val obs = observation.value
        val ent = selectedEntity.value
        if (obs==null || obs.latDeg==null || obs.lonDeg==null) { _stakeout.value = CadStakeoutState(hasFix=false, message = "GNSS yok"); return }
        val transformer = activeProject.value?.let { ProjectionEngine.forProject(it) } ?: NoOpTransformer
        val (curE, curN) = runCatching { if (transformer!==NoOpTransformer) transformer.forward(obs.latDeg, obs.lonDeg) else (obs.lonDeg*111000) to (obs.latDeg*111000) }.getOrElse { (obs.lonDeg?:0.0)*111000 to (obs.latDeg?:0.0)*111000 }
        if (ent==null) { _stakeout.value = CadStakeoutState(true, obs.fixType.name, curE, curN, message = "Entity seçiniz"); return }
        val (tE,tN) = nearestPointOnEntity(ent, curE, curN)
        val dE = tE - curE; val dN = tN - curN
        val dist = hypot(dE,dN)
        val bearing = ((Math.toDegrees(atan2(dE,dN)) + 360) % 360)
        val save = dist < 0.05
        _stakeout.value = CadStakeoutState(true, obs.fixType.name, curE, curN, tE, tN, dE, dN, dist, bearing, ent.javaClass.simpleName, save, if (save) "✅ Hedef" else "Δ=${"%.3f".format(dist)} m")
    }
    private fun nearestPointOnEntity(entity: CadEntity, e: Double, n: Double): Pair<Double,Double> = when(entity){
        is CadLine -> closestPointOnSegment(entity.start.x, entity.start.y, entity.end.x, entity.end.y, e,n)
        is CadPolyline -> {
            var best: Pair<Double,Double>? = null; var bestD = Double.MAX_VALUE
            val pts = entity.points
            for (i in 1 until pts.size){
                val cp = closestPointOnSegment(pts[i-1].x, pts[i-1].y, pts[i].x, pts[i].y, e, n)
                val d = hypot(cp.first - e, cp.second - n); if (d < bestD){ bestD = d; best = cp }
            }
            if (entity.isClosed && pts.size>2){
                val cp = closestPointOnSegment(pts.last().x, pts.last().y, pts.first().x, pts.first().y, e,n)
                val d = hypot(cp.first - e, cp.second - n); if (d < bestD){ bestD = d; best = cp }
            }
            best ?: (e to n)
        }
        is CadCircle -> {
            val dx = e - entity.center.x; val dy = n - entity.center.y; val len = hypot(dx,dy)
            if (len < 1e-6) (entity.center.x + entity.radius) to entity.center.y else (entity.center.x + dx/len*entity.radius) to (entity.center.y + dy/len*entity.radius)
        }
        is CadArc -> {
            val dx = e - entity.center.x; val dy = n - entity.center.y
            val baseAng = Math.toDegrees(atan2(dx, dy)).let { if (it < 0) it + 360 else it }
            val start = entity.startAngleDeg; val end = entity.endAngleDeg
            val inRange = if (start <= end) baseAng in start..end else (baseAng >= start || baseAng <= end)
            val targetAng = if (inRange) baseAng else nearestAngleOnArc(baseAng, start, end)
            val rad = Math.toRadians(targetAng)
            (entity.center.x + sin(rad)*entity.radius) to (entity.center.y + cos(rad)*entity.radius)
        }
        is CadText -> entity.position.x to entity.position.y
        is CadPoint -> entity.position.x to entity.position.y
        else -> e to n
    }
    private fun nearestAngleOnArc(test: Double, start: Double, end: Double): Double {
        fun norm(a: Double) = (a + 360) % 360
        val t = norm(test); val s = norm(start); val e = norm(end)
        fun diff(a: Double,b: Double) = min(abs(a-b), 360-abs(a-b))
        return if (diff(t,s) < diff(t,e)) s else e
    }
    private fun closestPointOnSegment(x1: Double, y1: Double, x2: Double, y2: Double, px: Double, py: Double): Pair<Double,Double>{
        val dx = x2 - x1; val dy = y2 - y1; val len2 = dx*dx + dy*dy
        if (len2 < 1e-12) return x1 to y1
        var t = ((px - x1)*dx + (py - y1)*dy) / len2; t = t.coerceIn(0.0,1.0)
        return (x1 + t*dx) to (y1 + t*dy)
    }

    // -------- Pick Ops --------
    fun addPicked(p: Point) { pushUndo(); redoStack.clear(); _redoDepth.value = 0; _pickedPoints.value = _pickedPoints.value + p }
    fun removePoint(index: Int) { val cur = _pickedPoints.value.toMutableList(); if (index in cur.indices) { pushUndo(); redoStack.clear(); _redoDepth.value = 0; cur.removeAt(index); _pickedPoints.value = cur } }
    fun clearPicked() { if (_pickedPoints.value.isNotEmpty()) { pushUndo(); redoStack.clear(); _redoDepth.value = 0; _pickedPoints.value = emptyList() } }
    fun totalDistance(): Double { val pts = _pickedPoints.value; var d=0.0; for(i in 1 until pts.size) d += hypot(pts[i].x-pts[i-1].x, pts[i].y-pts[i-1].y); return d }
    fun polygonArea(): Double = com.example.tugis3.core.cad.geom.polygonArea(_pickedPoints.value)
    fun undoLast() = undoPicked(); fun redoLast() = redoPicked()

    // -------- Undo / Redo internal --------
    private fun pushUndo() { undoStack.addLast(_pickedPoints.value.toList()); _undoDepth.value = undoStack.size }
    private fun pushRedo(snapshot: List<Point>) { redoStack.addLast(snapshot); _redoDepth.value = redoStack.size }
    fun undoPicked() {
        if (undoStack.isEmpty()) return
        val cur = _pickedPoints.value
        val prev = undoStack.removeLast()
        pushRedo(cur)
        _pickedPoints.value = prev
        _undoDepth.value = undoStack.size
    }
    fun redoPicked() {
        if (redoStack.isEmpty()) return
        val cur = _pickedPoints.value
        val next = redoStack.removeLast()
        undoStack.addLast(cur)
        _pickedPoints.value = next
        _undoDepth.value = undoStack.size
        _redoDepth.value = redoStack.size
    }

    // -------- Snap Helpers --------
    fun toggleSnap() { _snapEnabled.value = !_snapEnabled.value }
    fun cycleSnapTolerance() { val cur = _snapTolerancePx.value; val idx = snapPreset.indexOf(cur).takeIf { it>=0 } ?: -1; _snapTolerancePx.value = if (idx==-1) snapPreset.first() else snapPreset[(idx+1)%snapPreset.size]; recomputeEffectiveSnap() }
    fun toggleDynamicSnap() { _dynamicSnap.value = !_dynamicSnap.value; recomputeEffectiveSnap() }
    fun updateZoom(z: Float) { _lastZoom.value = z; recomputeEffectiveSnap(); if (_clusterEnabled.value) recomputeClusters(entities.value) }
    private fun recomputeEffectiveSnap() {
        if (_snapWorldMode.value) { _effectiveSnapTolerancePx.value = _snapTolerancePx.value; return }
        _effectiveSnapTolerancePx.value = if (_dynamicSnap.value) {
            val z = _lastZoom.value.coerceIn(3f, 23f)
            val factor = (18f / z).coerceIn(0.4f, 3f)
            (_snapTolerancePx.value * factor).roundToInt().coerceIn(4, 96)
        } else _snapTolerancePx.value
    }

    // -------- World Snap Helpers --------
    fun toggleSnapWorldMode() { _snapWorldMode.value = !_snapWorldMode.value; recomputeEffectiveSnap() }
    fun cycleSnapWorldTolerance() { val cur = _snapWorldToleranceM.value; val idx = worldSnapPreset.indexOfFirst { it==cur }.takeIf { it>=0 } ?: 0; _snapWorldToleranceM.value = worldSnapPreset[(idx+1)%worldSnapPreset.size] }

    // -------- Cluster Ops --------
    fun toggleCluster() { _clusterEnabled.value = !_clusterEnabled.value; if (!_clusterEnabled.value) _clusters.value = emptyList() else recomputeClusters(entities.value) }
    private fun clusterRadiusMeters(): Double { val z = _lastZoom.value; val exp = (14f - z).coerceIn(-5f,5f); return clusterBaseRadiusM * 2.0.pow(exp.toDouble()) }
    fun currentClusterRadiusMeters(): Double = clusterRadiusMeters()
    private fun recomputeClusters(ents: List<CadEntity>) {
        val points = ents.filterIsInstance<CadPoint>()
        if (points.isEmpty()) { _clusters.value = emptyList(); return }
        val radius = clusterRadiusMeters()
        val remaining = points.toMutableList()
        val out = mutableListOf<Cluster>()
        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val members = mutableListOf(seed)
            val it = remaining.iterator()
            while (it.hasNext()) {
                val p = it.next()
                val d = hypot(p.position.x - seed.position.x, p.position.y - seed.position.y)
                if (d <= radius) { members += p; it.remove() }
            }
            val cx = members.map { it.position.x }.average()
            val cy = members.map { it.position.y }.average()
            out += Cluster(Point(cx,cy), members)
        }
        _clusters.value = out
    }

    // -------- Export Picked --------
    fun exportPicked(context: android.content.Context): Result<File> = runCatching {
        val pts = _pickedPoints.value; require(pts.isNotEmpty()) { "Nokta yok" }
        val dir = File(context.filesDir, "cad_exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val f = File(dir, "picked_$ts.csv")
        f.printWriter().use { pw ->
            pw.println("index,x,y")
            pts.forEachIndexed { i,p -> pw.println("$i,${p.x},${p.y}") }
            if (pts.size>=2) pw.println("#distance,${totalDistance()}")
            if (pts.size>=3) pw.println("#area,${polygonArea()}")
        }; f }

    fun exportPickedGeoJson(context: android.content.Context): Result<File> = runCatching {
        val pts = _pickedPoints.value; require(pts.isNotEmpty()) { "Nokta yok" }
        val dir = File(context.filesDir, "cad_exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val f = File(dir, "picked_$ts.geojson")
        val json = buildString {
            append("{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n    {\n      \"type\": \"Feature\",\n      \"properties\": {\n        \"distance\": ")
            append(totalDistance())
            if (pts.size>=3) { append(",\n        \"area\": "); append(polygonArea()) }
            append("\n      },\n      \"geometry\": {\n        \"type\": \"")
            append(if (pts.size>=3) "Polygon" else "LineString")
            append("\",\n        \"coordinates\": ")
            if (pts.size>=3) {
                val coords = (pts + pts.first()).joinToString(prefix = "[[", postfix = "]]", separator = ",") { "[${it.x},${it.y}]" }
                append("["); append(coords); append("]")
            } else {
                val coords = pts.joinToString(prefix = "[", postfix = "]", separator = ",") { "[${it.x},${it.y}]" }
                append(coords)
            }
            append("\n      }\n    }\n  ]\n}")
        }
        f.writeText(json); f }

    fun exportPickedDxf(context: android.content.Context): Result<File> = runCatching {
        val pts = _pickedPoints.value; val closed = pts.size>=3 && _mode.value==MeasurementMode.AREA
        val dxf = buildMeasurementDxf(pts, closed)
        val dir = File(context.filesDir, "cad_exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val f = File(dir, "picked_$ts.dxf"); f.writeText(dxf); f }

    private fun buildMeasurementDxf(points: List<Point>, closed: Boolean): String = buildString {
        append("0\nSECTION\n2\nENTITIES\n")
        if (points.size>=2) {
            append("0\nLWPOLYLINE\n8\nMEASURE\n70\n"); append(if (closed) 1 else 0).append('\n')
            append("90\n").append(points.size).append('\n')
            points.forEach { p -> append("10\n").append(p.x).append('\n'); append("20\n").append(p.y).append('\n') }
        }
        append("0\nENDSEC\n0\nEOF")
    }

    // -------- Entity Add / Import / Export All --------
    fun addLine(p1: Point, p2: Point, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadLine(p1,p2,layer=layer)) } }
    fun addCircle(c: Point, r: Double, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadCircle(c,r,layer=layer)) } }
    fun addText(p: Point, text: String, h: Double = 2.5, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadText(p,h,text,layer=layer)) } }
    fun addPolyline(points: List<Point>, closed: Boolean, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadPolyline(points,isClosed=closed,layer=layer)) } }
    fun loadFromUri(resolver: android.content.ContentResolver, uri: android.net.Uri) { viewModelScope.launch(Dispatchers.IO) {
        val proj = activeProject.value ?: run {
            android.util.Log.e("CAD", "loadFromUri: Aktif proje yok!")
            _status.value = "❌ Önce bir proje seçin"
            return@launch
        }

        // Dosya uzantısını kontrol et
        val fileName = uri.lastPathSegment ?: ""
        val ext = fileName.lowercase().substringAfterLast('.', "")
        android.util.Log.d("CAD", "loadFromUri: Dosya=$fileName, ext=$ext")

        // DWG dosyası mı kontrol et
        if (ext == "dwg") {
            android.util.Log.w("CAD", "DWG dosyası seçildi - desteklenmiyor")
            _status.value = """⚠️ DWG formatı desteklenmiyor!
                |
                |Lütfen DXF formatına çevirin:
                |• AutoCAD: File → Save As → DXF
                |• Online: dwgconvert.com
                |• QGIS: Export → DXF""".trimMargin()
            return@launch
        }

        // Yükleme başladı
        _status.value = "🔄 $fileName yükleniyor..."
        android.util.Log.d("CAD", "DXF parse başladı...")

        // DXF ve diğer formatlar için yükle
        val ents = runCatching {
            repo.loadDxf(resolver, uri)
        }.getOrElse { ex ->
            android.util.Log.e("CAD", "DXF parse HATA: ${ex.message}", ex)
            _status.value = "❌ Dosya okunamadı: ${ex.message}\n\nDosya DXF R12+ formatında mı?"
            return@launch
        }

        android.util.Log.d("CAD", "DXF parse tamamlandı: ${ents.size} entity")

        if (ents.isEmpty()) {
            android.util.Log.w("CAD", "Dosyada entity yok!")
            _status.value = """⚠️ Dosya boş veya desteklenmeyen entity'ler var
                |
                |Desteklenen: LINE, POLYLINE, CIRCLE, ARC, TEXT
                |Dosya formatı: DXF R12 veya üstü""".trimMargin()
            return@launch
        }

        // Entity'leri veritabanına kaydet
        android.util.Log.d("CAD", "Entity'ler kaydediliyor...")
        ents.forEach { cadStore.addEntity(proj.id, it) }

        // Katmanları çıkar ve güncelle
        val uniqueLayers = ents.map { it.layer }.distinct()
        android.util.Log.d("CAD", "Katmanlar: ${uniqueLayers.joinToString()}")

        // Katman listesini güncelle (mevcut katmanlarla birleştir)
        val currentLayers = _layers.value.toSet()
        val newLayers = (currentLayers + uniqueLayers).sorted()
        _layers.value = newLayers

        // Yeni katmanları otomatik aktif et
        val currentActive = _activeLayers.value
        _activeLayers.value = (currentActive + uniqueLayers).toSet()

        android.util.Log.d("CAD", "Katmanlar güncellendi: ${_layers.value.size} katman, ${_activeLayers.value.size} aktif")

        // Auto-zoom: Tüm entity'lerin bounding box'ını hesapla
        val allPoints = ents.flatMap { entity ->
            when (entity) {
                is CadLine -> listOf(entity.start, entity.end)
                is CadPolyline -> entity.points
                is CadCircle -> {
                    val c = entity.center; val r = entity.radius
                    listOf(Point(c.x - r, c.y - r), Point(c.x + r, c.y + r))
                }
                is CadArc -> {
                    val c = entity.center; val r = entity.radius
                    listOf(Point(c.x - r, c.y - r), Point(c.x + r, c.y + r))
                }
                is CadText -> listOf(entity.position)
                is CadPoint -> listOf(entity.position)
                else -> emptyList()
            }
        }

        if (allPoints.isNotEmpty()) {
            val minX = allPoints.minOf { it.x }
            val maxX = allPoints.maxOf { it.x }
            val minY = allPoints.minOf { it.y }
            val maxY = allPoints.maxOf { it.y }

            val centerX = (minX + maxX) / 2.0
            val centerY = (minY + maxY) / 2.0
            val width = maxX - minX
            val height = maxY - minY
            val maxDim = max(width, height)

            android.util.Log.d("CAD", "Auto-zoom: center=($centerX, $centerY), extent=${maxDim}m")

            // Zoom seviyesini ayarla (tüm çizim görünsün diye biraz margin ekle)
            val targetZoom = when {
                maxDim < 10 -> 20f
                maxDim < 50 -> 18f
                maxDim < 200 -> 16f
                maxDim < 1000 -> 14f
                maxDim < 5000 -> 12f
                else -> 10f
            }

            // Main thread'e geç ve zoom'u ayarla
            viewModelScope.launch(Dispatchers.Main) {
                _lastZoom.value = targetZoom
                android.util.Log.d("CAD", "Zoom ayarlandı: $targetZoom (extent: ${maxDim}m)")
            }
        }

        _status.value = "✅ ${ents.size} varlık yüklendi (${uniqueLayers.size} katman)\n📐 Zoom yapıldı"
        android.util.Log.i("CAD", "Yükleme tamamlandı: ${ents.size} entity, ${uniqueLayers.size} katman")
    } }
    fun undoEntityLast() { val last = cadItems.value.maxByOrNull { it.id }?.id ?: return; viewModelScope.launch { cadStore.deleteEntity(last) } }
    fun exportAllGeoJson(context: android.content.Context, circleSegments: Int = 64, arcSegAngle: Double = 10.0): Result<File> = runCatching {
        val ents = entities.value; require(ents.isNotEmpty()) { "Entity yok" }
        val dir = File(context.filesDir, "cad_exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val f = File(dir, "cad_all_$ts.geojson")
        val json = CadExportUtil.toGeoJson(ents, CadExportUtil.Options(circleSegments, arcSegAngle))
        f.writeText(json); f }

    // -------- Stake Save --------
    fun saveStakePoint(): Boolean {
        val st = _stakeout.value
        if (!st.hasFix || st.targetE==null || st.targetN==null || !st.canSave) return false
        val proj = activeProject.value ?: return false
        viewModelScope.launch {
            val name = "CAD_${System.currentTimeMillis().toString().takeLast(5)}"
            surveyPointRepo.insert(
                SurveyPointEntity(
                    projectId = proj.id,
                    name = name,
                    code = "CAD_STK",
                    latitude = null,
                    longitude = null,
                    elevation = null,
                    northing = st.targetN,
                    easting = st.targetE,
                    zone = proj.utmZone?.let { it.toString() + if (proj.utmNorthHemisphere) "N" else "S" },
                    hrms = observation.value?.hrms,
                    vrms = observation.value?.vrms,
                    pdop = observation.value?.pdop,
                    satellites = observation.value?.satellitesInUse,
                    fixType = observation.value?.fixType?.name,
                    antennaHeight = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            _status.value = "Stake kaydedildi: $name"
        }
        return true
    }

    // -------- Polygon Net Area --------
    fun polygonNetArea(rings: List<List<Point>>): Double {
        if (rings.isEmpty()) return 0.0
        val outer = com.example.tugis3.core.cad.geom.polygonArea(rings.first())
        val holes = rings.drop(1).sumOf { abs(com.example.tugis3.core.cad.geom.polygonArea(it)) }
        return outer - holes
    }

    // -------- Aplikasyon (Stakeout) İşlemleri --------
    /**
     * Aplikasyon modunu başlat/değiştir
     */
    fun startApplicationMode(mode: ApplicationMode) {
        _applicationMode.value = mode
        _applicationPoints.value = emptyList()
        _applicationResult.value = null
        _status.value = when(mode) {
            ApplicationMode.NONE -> "Aplikasyon modu kapatıldı"
            ApplicationMode.POINT_TO_POINT -> "İki nokta seçin (çizgi uçları veya kırık noktalar)"
            ApplicationMode.SINGLE_POINT_DISTANCE -> "Bir nokta seçin ve mesafe/açı girin"
            ApplicationMode.OFFSET_DISTANCE -> "Bir çizgi seçin ve mesafe girin"
            ApplicationMode.OFFSET_ANGLE -> "İki nokta seçin ve açı/mesafe girin"
        }
    }

    /**
     * Aplikasyon modunu kapat
     */
    fun stopApplicationMode() {
        _applicationMode.value = ApplicationMode.NONE
        _applicationPoints.value = emptyList()
        _applicationResult.value = null
        _status.value = "Aplikasyon iptal edildi"
    }

    /**
     * Çizimden nokta seç (çizgi ucu, kırık nokta, kesişim noktası vb.)
     */
    fun selectApplicationPoint(point: Point, entity: CadEntity?) {
        val mode = _applicationMode.value
        if (mode == ApplicationMode.NONE) return

        val currentPoints = _applicationPoints.value
        val entityId = entity?.let { cadItems.value.firstOrNull { it.entity == entity }?.id }

        when (mode) {
            ApplicationMode.POINT_TO_POINT -> {
                if (currentPoints.size < 2) {
                    val desc = "Nokta ${currentPoints.size + 1}"
                    _applicationPoints.value = currentPoints + ApplicationPoint(point, entityId, desc)

                    if (_applicationPoints.value.size == 2) {
                        calculatePointToPointApplication()
                    } else {
                        _status.value = "İkinci noktayı seçin"
                    }
                }
            }
            ApplicationMode.SINGLE_POINT_DISTANCE -> {
                if (currentPoints.isEmpty()) {
                    val desc = "Referans Nokta"
                    _applicationPoints.value = currentPoints + ApplicationPoint(point, entityId, desc)
                    _status.value = "Mesafe veya açı girin"
                } else {
                    // Mesafe veya açı hesapla
                    calculateSinglePointApplication(currentPoints[0].point, point)
                }
            }
            ApplicationMode.OFFSET_DISTANCE -> {
                if (currentPoints.size < 2) {
                    val desc = if (currentPoints.isEmpty()) "Başlangıç" else "Bitiş"
                    _applicationPoints.value = currentPoints + ApplicationPoint(point, entityId, desc)

                    if (_applicationPoints.value.size == 2) {
                        _status.value = "Ofset mesafesini girin"
                    } else {
                        _status.value = "Çizgi bitiş noktasını seçin"
                    }
                }
            }
            ApplicationMode.OFFSET_ANGLE -> {
                if (currentPoints.size < 2) {
                    val desc = if (currentPoints.isEmpty()) "Başlangıç" else "Yön noktası"
                    _applicationPoints.value = currentPoints + ApplicationPoint(point, entityId, desc)

                    if (_applicationPoints.value.size == 2) {
                        _status.value = "Açı ve mesafeyi girin"
                    } else {
                        _status.value = "Yön noktasını seçin"
                    }
                }
            }
            ApplicationMode.NONE -> {}
        }
    }

    /**
     * İki nokta arası aplikasyon hesapla
     */
    private fun calculatePointToPointApplication() {
        val points = _applicationPoints.value
        if (points.size != 2) return

        val p1 = points[0].point
        val p2 = points[1].point

        val distance = hypot(p2.x - p1.x, p2.y - p1.y)
        val bearing = Math.toDegrees(atan2(p2.x - p1.x, p2.y - p1.y)).let {
            if (it < 0) it + 360 else it
        }

        _applicationResult.value = ApplicationResult(
            targetPoint = p2,
            distance = distance,
            bearing = bearing,
            description = "P1 → P2: %.3f m, %.1f°".format(distance, bearing)
        )

        _status.value = "✓ Aplikasyon hesaplandı: %.3f m, Açı: %.1f°".format(distance, bearing)
    }

    /**
     * Tek noktadan mesafe/açı aplikasyonu hesapla
     */
    private fun calculateSinglePointApplication(reference: Point, target: Point) {
        val dx = target.x - reference.x
        val dy = target.y - reference.y
        val distance = hypot(dx, dy)
        val bearing = Math.toDegrees(atan2(dx, dy)).let {
            if (it < 0) it + 360 else it
        }

        _applicationResult.value = ApplicationResult(
            targetPoint = target,
            distance = distance,
            bearing = bearing,
            description = "Mesafe: %.3f m, Açı: %.1f°".format(distance, bearing)
        )

        _applicationDistance.value = distance
        _applicationAngle.value = bearing
        _status.value = "✓ Mesafe/açı hesaplandı: %.3f m, %.1f°".format(distance, bearing)
    }

    /**
     * Mesafe ile ofset aplikasyon
     */
    fun calculateOffsetByDistance(offsetDistance: Double) {
        val points = _applicationPoints.value
        if (points.size != 2) {
            _status.value = "İki nokta seçmelisiniz"
            return
        }

        val p1 = points[0].point
        val p2 = points[1].point

        // Çizgi vektörü
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val lineLength = hypot(dx, dy)

        if (lineLength < 1e-6) {
            _status.value = "Geçersiz çizgi"
            return
        }

        // Dik vektör (90° sola)
        val perpX = -dy / lineLength
        val perpY = dx / lineLength

        // Ofset noktası
        val targetX = p1.x + perpX * offsetDistance
        val targetY = p1.y + perpY * offsetDistance
        val targetPoint = Point(targetX, targetY)

        val bearing = Math.toDegrees(atan2(targetX - p1.x, targetY - p1.y)).let {
            if (it < 0) it + 360 else it
        }

        _applicationResult.value = ApplicationResult(
            targetPoint = targetPoint,
            distance = abs(offsetDistance),
            bearing = bearing,
            description = "Ofset: %.3f m dik mesafe".format(abs(offsetDistance))
        )

        _applicationOffset.value = offsetDistance
        _status.value = "✓ Ofset aplikasyon: %.3f m".format(abs(offsetDistance))
    }

    /**
     * Açı ve mesafe ile aplikasyon
     */
    fun calculateOffsetByAngleDistance(angle: Double, distance: Double) {
        val points = _applicationPoints.value
        if (points.size != 2) {
            _status.value = "İki nokta seçmelisiniz"
            return
        }

        val p1 = points[0].point
        val p2 = points[1].point

        // Referans yön
        val baseAngle = Math.toDegrees(atan2(p2.x - p1.x, p2.y - p1.y))

        // Hedef açı (referansa göre)
        val targetAngle = baseAngle + angle
        val rad = Math.toRadians(targetAngle)

        // Hedef nokta
        val targetX = p1.x + sin(rad) * distance
        val targetY = p1.y + cos(rad) * distance
        val targetPoint = Point(targetX, targetY)

        _applicationResult.value = ApplicationResult(
            targetPoint = targetPoint,
            distance = distance,
            bearing = (targetAngle + 360) % 360,
            description = "Açı: %.1f°, Mesafe: %.3f m".format(angle, distance)
        )

        _applicationDistance.value = distance
        _status.value = "✓ Açılı aplikasyon: %.1f° × %.3f m".format(angle, distance)
    }

    /**
     * Çizimden nokta seçimi için yardımcı fonksiyon
     */
    fun findDrawingPoints(entity: CadEntity): List<Point> {
        return when (entity) {
            is CadLine -> listOf(entity.start, entity.end)
            is CadPolyline -> entity.points
            is CadPolygon -> entity.rings.firstOrNull() ?: emptyList()
            is CadCircle -> listOf(
                Point(entity.center.x + entity.radius, entity.center.y),
                Point(entity.center.x - entity.radius, entity.center.y),
                Point(entity.center.x, entity.center.y + entity.radius),
                Point(entity.center.x, entity.center.y - entity.radius)
            )
            is CadArc -> {
                val startRad = Math.toRadians(entity.startAngleDeg)
                val endRad = Math.toRadians(entity.endAngleDeg)
                listOf(
                    Point(
                        entity.center.x + sin(startRad) * entity.radius,
                        entity.center.y + cos(startRad) * entity.radius
                    ),
                    Point(
                        entity.center.x + sin(endRad) * entity.radius,
                        entity.center.y + cos(endRad) * entity.radius
                    )
                )
            }
            is CadPoint -> listOf(entity.position)
            is CadText -> listOf(entity.position)
            else -> emptyList()
        }
    }

    /**
     * En yakın çizim noktasını bul
     */
    fun findNearestDrawingPoint(screenPoint: Point, tolerance: Double = 10.0): Pair<Point, CadEntity?>? {
        var nearest: Pair<Point, CadEntity?>? = null
        var minDist = tolerance

        entities.value.forEach { entity ->
            val points = findDrawingPoints(entity)
            points.forEach { point ->
                val dist = hypot(point.x - screenPoint.x, point.y - screenPoint.y)
                if (dist < minDist) {
                    minDist = dist
                    nearest = point to entity
                }
            }
        }

        return nearest
    }

    /**
     * Aplikasyon hedefine git (GPS ile)
     */
    fun navigateToApplicationTarget(): Boolean {
        val result = _applicationResult.value ?: return false
        val obs = observation.value ?: return false

        if (obs.latDeg == null || obs.lonDeg == null) {
            _status.value = "GPS sinyali yok"
            return false
        }

        val transformer = activeProject.value?.let { ProjectionEngine.forProject(it) } ?: NoOpTransformer
        val (curE, curN) = runCatching {
            transformer.forward(obs.latDeg, obs.lonDeg)
        }.getOrElse {
            _status.value = "Koordinat dönüşüm hatası"
            return false
        }

        val targetE = result.targetPoint.x
        val targetN = result.targetPoint.y

        val dE = targetE - curE
        val dN = targetN - curN
        val dist = hypot(dE, dN)
        val bearing = Math.toDegrees(atan2(dE, dN)).let { if (it < 0) it + 360 else it }

        _stakeout.value = CadStakeoutState(
            hasFix = true,
            fixLabel = obs.fixType.name,
            currentE = curE,
            currentN = curN,
            targetE = targetE,
            targetN = targetN,
            deltaE = dE,
            deltaN = dN,
            horizontalDistance = dist,
            bearingDeg = bearing,
            entityType = "Aplikasyon",
            canSave = dist < 0.05,
            message = if (dist < 0.05) "✅ Hedefteyiz" else "→ %.3f m, %.1f°".format(dist, bearing)
        )

        return true
    }

    /**
     * Aplikasyon noktasını kaydet
     */
    fun saveApplicationPoint(): Boolean {
        val result = _applicationResult.value ?: return false
        val proj = activeProject.value ?: return false

        viewModelScope.launch {
            val name = "APL_${System.currentTimeMillis().toString().takeLast(5)}"
            surveyPointRepo.insert(
                SurveyPointEntity(
                    projectId = proj.id,
                    name = name,
                    code = "APLIKASYON",
                    latitude = null,
                    longitude = null,
                    elevation = null,
                    northing = result.targetPoint.y,
                    easting = result.targetPoint.x,
                    zone = proj.utmZone?.let { it.toString() + if (proj.utmNorthHemisphere) "N" else "S" },
                    hrms = observation.value?.hrms,
                    vrms = observation.value?.vrms,
                    pdop = observation.value?.pdop,
                    satellites = observation.value?.satellitesInUse,
                    fixType = observation.value?.fixType?.name,
                    antennaHeight = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            _status.value = "✓ Aplikasyon noktası kaydedildi: $name"
        }

        return true
    }

    // -------- Camera Control for CAD View --------
    private val _cameraPosition = MutableStateFlow<Pair<Double, Double>?>(null)
    val cameraPosition: StateFlow<Pair<Double, Double>?> = _cameraPosition.asStateFlow()

    private val _cameraZoom = MutableStateFlow(1.0)
    val cameraZoom: StateFlow<Double> = _cameraZoom.asStateFlow()

    fun setCameraPosition(easting: Double, northing: Double) {
        _cameraPosition.value = easting to northing
    }

    fun fitBounds(minE: Double, minN: Double, maxE: Double, maxN: Double) {
        // Merkezi ayarla
        val centerE = (minE + maxE) / 2.0
        val centerN = (minN + maxN) / 2.0
        _cameraPosition.value = centerE to centerN

        // Zoom seviyesini hesapla (tüm projeyi gösterecek şekilde)
        val width = maxE - minE
        val height = maxN - minN
        val maxDimension = maxOf(width, height)

        // Zoom'u ayarla (daha büyük değer = daha uzaktan bakış)
        // Ekran boyutuna göre otomatik ayarlama yapılabilir
        _cameraZoom.value = maxDimension / 100.0 // Basit bir zoom hesabı
    }

    // ...existing code...

    // -------- Companion Utilities --------
    companion object {
        fun polylineLength(points: List<Point>) = com.example.tugis3.core.cad.geom.polylineLength(points)
        fun polygonAreaOf(points: List<Point>) = com.example.tugis3.core.cad.geom.polygonArea(points)
        fun circleCircumference(r: Double) = com.example.tugis3.core.cad.geom.circleCircumference(r)
        fun circleArea(r: Double) = com.example.tugis3.core.cad.geom.circleArea(r)
        fun arcLength(r: Double, startDeg: Double, endDeg: Double) = com.example.tugis3.core.cad.geom.arcLength(r, startDeg, endDeg)
        fun buildMeasurementDxf(points: List<Point>, closed: Boolean): String = buildString {
            require(points.size>=2) { "En az 2 nokta gerekli" }
            appendLine("0\nSECTION\n2\nENTITIES")
            appendLine("0\nLWPOLYLINE")
            appendLine("8\n0")
            appendLine("90\n${points.size}")
            appendLine("70\n${if (closed) 1 else 0}")
            points.forEach { p -> appendLine("10\n${p.x}\n20\n${p.y}") }
            appendLine("0\nENDSEC\n0\nEOF")
        }
        fun douglasPeucker(points: List<Point>, epsilon: Double): List<Point> {
            if (points.size < 3) return points
            val keep = BooleanArray(points.size) { false }
            keep[0] = true; keep[points.lastIndex] = true
            fun dp(s: Int, e: Int) {
                var max = 0.0; var idx = -1
                val a = points[s]; val b = points[e]
                for (i in s+1 until e) {
                    val d = perpendicularDistance(a,b,points[i]); if (d>max) { max = d; idx = i }
                }
                if (max > epsilon && idx != -1) { keep[idx] = true; dp(s, idx); dp(idx, e) }
            }
            dp(0, points.lastIndex)
            return points.filterIndexed { i, _ -> keep[i] }
        }
        fun perpendicularDistance(a: Point, b: Point, p: Point): Double {
            val dx = b.x - a.x; val dy = b.y - a.y
            if (dx==0.0 && dy==0.0) return sqrt((p.x-a.x).pow(2) + (p.y-a.y).pow(2))
            val t = ((p.x - a.x)*dx + (p.y - a.y)*dy) / (dx*dx + dy*dy)
            val projX = a.x + t*dx; val projY = a.y + t*dy
            return sqrt((p.x-projX).pow(2) + (p.y-projY).pow(2))
        }
    }

    /**
     * Zoom In - Yakınlaştır
     */
    fun zoomIn() {
        val currentZoom = _lastZoom.value
        val newZoom = (currentZoom * 1.2f).coerceAtMost(30f) // Max 30x zoom
        _lastZoom.value = newZoom

        // Dynamic snap enabled ise snap tolerance'ı güncelle
        if (_dynamicSnap.value) {
            updateDynamicSnapTolerance(newZoom)
        }
    }

    /**
     * Zoom Out - Uzaklaştır
     */
    fun zoomOut() {
        val currentZoom = _lastZoom.value
        val newZoom = (currentZoom / 1.2f).coerceAtLeast(1f) // Min 1x zoom
        _lastZoom.value = newZoom

        // Dynamic snap enabled ise snap tolerance'ı güncelle
        if (_dynamicSnap.value) {
            updateDynamicSnapTolerance(newZoom)
        }
    }

    /**
     * Zoom seviyesini ayarla
     */
    fun setZoom(zoom: Float) {
        _lastZoom.value = zoom.coerceIn(1f, 30f)

        if (_dynamicSnap.value) {
            updateDynamicSnapTolerance(zoom)
        }
    }

    /**
     * Dynamic snap tolerance'ı zoom seviyesine göre güncelle
     */
    private fun updateDynamicSnapTolerance(zoom: Float) {
        // Zoom arttıkça snap tolerance azalır (daha hassas seçim)
        val baseSnap = _snapTolerancePx.value
        val adjusted = (baseSnap / (zoom / 18f)).toInt().coerceIn(5, 64)
        _effectiveSnapTolerancePx.value = adjusted
    }
}
