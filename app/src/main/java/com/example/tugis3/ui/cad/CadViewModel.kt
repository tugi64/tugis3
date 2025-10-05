package com.example.tugis3.ui.cad

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class CadViewModel @Inject constructor(
    private val repo: CadRepository,
    private val app: Application, // ileride gerekirse
    private val projectRepo: ProjectRepository,
    private val surveyPointRepo: SurveyPointRepository,
    private val gnssEngine: GnssEngine,
    private val cadStore: CadPersistenceRepository
) : ViewModel() {

    // ---------- Ölçüm / Pick ----------
    private val _pickedPoints = MutableStateFlow<List<Point>>(emptyList())
    val pickedPoints: StateFlow<List<Point>> = _pickedPoints
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status
    enum class MeasurementMode { DISTANCE, AREA }
    private val _mode = MutableStateFlow(MeasurementMode.DISTANCE)
    val mode: StateFlow<MeasurementMode> = _mode

    // ---------- Layer filtresi ----------
    private val _layers = MutableStateFlow<List<String>>(emptyList())
    val layers: StateFlow<List<String>> = _layers
    private val _activeLayers = MutableStateFlow<Set<String>>(emptySet())
    val activeLayers: StateFlow<Set<String>> = _activeLayers

    // ---------- Snap & Grid ----------
    private val _snapEnabled = MutableStateFlow(false)
    val snapEnabled: StateFlow<Boolean> = _snapEnabled
    private val _snapTolerancePx = MutableStateFlow(24)
    val snapTolerancePx: StateFlow<Int> = _snapTolerancePx
    private val snapPreset = intArrayOf(5,10,24,40,64)
    private val _gridVisible = MutableStateFlow(true)
    val gridVisible: StateFlow<Boolean> = _gridVisible

    // ---------- Selection ----------
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode
    private val _selectedId = MutableStateFlow<Long?>(null)

    // ---------- Proje / Entity akışları ----------
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

    // ---------- Stakeout State ----------
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

    // ---------- Init ----------
    init {
        viewModelScope.launch { entities.collect { refreshLayersFromEntities(it) } }
        loadSampleIfEmpty()
        startGnss()
        observeStakeout()
    }

    // ---------- Layers ----------
    private fun refreshLayersFromEntities(list: List<CadEntity>) {
        val ls = list.map { it.layer }.distinct().sorted()
        _layers.value = ls
        if (_activeLayers.value.isEmpty()) _activeLayers.value = ls.toSet()
    }
    fun setAllLayers(enableAll: Boolean) { _activeLayers.value = if (enableAll) _layers.value.toSet() else emptySet() }
    fun toggleLayer(layer: String) { _activeLayers.value = _activeLayers.value.let { if (layer in it) it - layer else it + layer } }

    // ---------- Sample Data ----------
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

    // ---------- Selection ----------
    fun toggleSelectionMode() { _selectionMode.value = !_selectionMode.value; if (!_selectionMode.value) _selectedId.value = null }
    fun selectEntity(e: CadEntity?) { if (e==null) { _selectedId.value = null; return }; _selectedId.value = cadItems.value.firstOrNull { it.entity == e }?.id }
    fun deleteSelectedEntity() { val id = _selectedId.value ?: return; viewModelScope.launch { cadStore.deleteEntity(id); _selectedId.value = null } }

    // ---------- GNSS & Transform ----------
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

    // ---------- Pick / Measure ----------
    fun addPicked(p: Point) { _pickedPoints.value = _pickedPoints.value + p }
    fun removePoint(index: Int) { val cur = _pickedPoints.value.toMutableList(); if (index in cur.indices) { cur.removeAt(index); _pickedPoints.value = cur } }
    fun clearPicked() { _pickedPoints.value = emptyList() }
    private fun Point.distanceTo(o: Point) = hypot(o.x - x, o.y - y)
    fun totalDistance(): Double { val pts = _pickedPoints.value; var d = 0.0; for (i in 1 until pts.size) d += pts[i-1].distanceTo(pts[i]); return d }
    fun polygonArea(): Double = com.example.tugis3.core.cad.geom.polygonArea(_pickedPoints.value)
    fun toggleMode() { _mode.value = if (_mode.value==MeasurementMode.DISTANCE) MeasurementMode.AREA else MeasurementMode.DISTANCE }
    fun undoLast() { val cur = _pickedPoints.value; if (cur.isNotEmpty()) _pickedPoints.value = cur.dropLast(1) }

    // ---------- Snap / Grid ----------
    fun toggleSnap() { _snapEnabled.value = !_snapEnabled.value }
    fun cycleSnapTolerance() { val cur = _snapTolerancePx.value; val idx = snapPreset.indexOf(cur).takeIf { it>=0 } ?: 0; _snapTolerancePx.value = snapPreset[(idx+1)%snapPreset.size] }
    fun toggleGrid() { _gridVisible.value = !_gridVisible.value }

    // ---------- Export Picked ----------
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

    // ---------- Entity Add Helpers ----------
    fun addLine(p1: Point, p2: Point, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadLine(p1,p2,layer=layer)) } }
    fun addCircle(c: Point, r: Double, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadCircle(c,r,layer=layer)) } }
    fun addText(p: Point, text: String, h: Double = 2.5, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadText(p,h,text,layer=layer)) } }
    fun addPolyline(points: List<Point>, closed: Boolean, layer: String = "0") { viewModelScope.launch { addEntityPersist(CadPolyline(points,isClosed=closed,layer=layer)) } }

    // DXF import
    fun loadFromUri(resolver: android.content.ContentResolver, uri: android.net.Uri) { viewModelScope.launch(Dispatchers.IO) {
        val proj = activeProject.value ?: return@launch
        val ents = runCatching { repo.loadDxf(resolver, uri) }.getOrElse { emptyList() }
        ents.forEach { cadStore.addEntity(proj.id, it) }
        _status.value = "Imported ${ents.size} entities"
    } }

    fun undoEntityLast() { val last = cadItems.value.maxByOrNull { it.id }?.id ?: return; viewModelScope.launch { cadStore.deleteEntity(last) } }

    fun exportAllGeoJson(context: android.content.Context, circleSegments: Int = 64, arcSegAngle: Double = 10.0): Result<File> = runCatching {
        val ents = entities.value; require(ents.isNotEmpty()) { "Entity yok" }
        val dir = File(context.filesDir, "cad_exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val f = File(dir, "cad_all_$ts.geojson")
        val json = CadExportUtil.toGeoJson(ents, CadExportUtil.Options(circleSegments, arcSegAngle))
        f.writeText(json); f }

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
}
