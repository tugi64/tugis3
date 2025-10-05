package com.example.tugis3.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PointListViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository
) : ViewModel() {

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val points: StateFlow<List<PointEntity>> = activeProject
        .flatMapLatest { p ->
            if (p == null) flowOf(emptyList()) else pointRepo.observePoints(p.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val deletedPoints: StateFlow<List<PointEntity>> = activeProject
        .flatMapLatest { p ->
            if (p == null) flowOf<List<PointEntity>>(emptyList()) else pointRepo.observeDeletedPoints(p.id, 50)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // UI Durumları
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _sort = MutableStateFlow(SortOption.CREATED_DESC)
    val sort: StateFlow<SortOption> = _sort.asStateFlow()

    private val _autoResolveDuplicates = MutableStateFlow(true)
    val autoResolveDuplicates: StateFlow<Boolean> = _autoResolveDuplicates.asStateFlow()
    fun setAutoResolveDuplicates(v: Boolean) { _autoResolveDuplicates.value = v }

    private val undoStack = ArrayDeque<List<Long>>()

    enum class SortOption { NAME_ASC, NAME_DESC, CREATED_DESC, NORTH_ASC, EAST_ASC }

    // Bellek içi filtreleme yerine: arama boşsa observePoints, değilse searchPoints
    private val basePoints: Flow<List<PointEntity>> = activeProject.flatMapLatest { proj ->
        if (proj == null) flowOf(emptyList()) else search.flatMapLatest { q ->
            if (q.isBlank()) pointRepo.observePoints(proj.id) else pointRepo.searchPoints(proj.id, "%${q.replace('%','_')}%")
        }
    }

    val filtered: StateFlow<List<PointEntity>> = combine(basePoints, sort) { list, s ->
        when (s) {
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortOption.CREATED_DESC -> list // DAO zaten createdAt DESC
            SortOption.NORTH_ASC -> list.sortedBy { it.northing }
            SortOption.EAST_ASC -> list.sortedBy { it.easting }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(q: String) { _search.value = q }
    fun setSort(s: SortOption) { _sort.value = s }

    fun addManual(
        name: String,
        n: Double?,
        e: Double?,
        h: Double?,
        featureCode: String? = null,
        description: String? = null,
        resolveDuplicate: Boolean = _autoResolveDuplicates.value
    ): Result<Unit> {
        val proj = activeProject.value ?: return Result.failure(IllegalStateException("No active project"))
        if (n == null || e == null) return Result.failure(IllegalArgumentException("Coordinates null"))
        return runCatching {
            viewModelScope.launch {
                var finalName = name.ifBlank { "P${System.currentTimeMillis()%100000}" }
                if (resolveDuplicate) {
                    var idx = 1
                    while (pointRepo.isNameTaken(proj.id, finalName)) {
                        finalName = "${name}_$idx"; idx++
                    }
                } else if (pointRepo.isNameTaken(proj.id, finalName)) {
                    throw IllegalArgumentException("İsim zaten kullanılıyor")
                }
                pointRepo.upsert(
                    PointEntity(
                        projectId = proj.id,
                        name = finalName,
                        northing = n,
                        easting = e,
                        ellipsoidalHeight = h,
                        featureCode = featureCode?.ifBlank { null },
                        description = description?.ifBlank { null }
                    )
                )
            }
        }
    }

    fun updatePoint(
        id: Long,
        name: String,
        n: Double,
        e: Double,
        h: Double?,
        code: String?,
        desc: String?,
        resolveDuplicate: Boolean = _autoResolveDuplicates.value
    ) = viewModelScope.launch {
        val proj = activeProject.value ?: return@launch
        var finalName = name.ifBlank { "P$id" }
        if (resolveDuplicate) {
            var idx = 1
            while (pointRepo.isNameTaken(proj.id, finalName, excludeId = id)) {
                finalName = "${name}_$idx"; idx++
            }
        } else if (pointRepo.isNameTaken(proj.id, finalName, excludeId = id)) {
            return@launch
        }
        pointRepo.updateFull(id, finalName, n, e, h, code?.ifBlank { null }, desc?.ifBlank { null })
    }

    fun attemptAddManual(
        name: String,
        n: Double?,
        e: Double?,
        h: Double?,
        featureCode: String? = null,
        description: String? = null,
        resolveDuplicate: Boolean = _autoResolveDuplicates.value
    ): Result<Unit> {
        return addManual(name, n, e, h, featureCode, description, resolveDuplicate)
    }

    suspend fun attemptUpdatePoint(
        id: Long,
        name: String,
        n: Double,
        e: Double,
        h: Double?,
        code: String?,
        desc: String?,
        resolveDuplicate: Boolean = _autoResolveDuplicates.value
    ): Result<Unit> {
        val proj = activeProject.value ?: return Result.failure(IllegalStateException("No active project"))
        var finalName = name.ifBlank { "P$id" }
        if (!resolveDuplicate && pointRepo.isNameTaken(proj.id, finalName, excludeId = id)) {
            return Result.failure(IllegalArgumentException("İsim zaten kullanılıyor"))
        }
        if (resolveDuplicate) {
            var idx = 1
            while (pointRepo.isNameTaken(proj.id, finalName, excludeId = id)) {
                finalName = "${name}_$idx"; idx++
            }
        }
        return runCatching {
            pointRepo.updateFull(id, finalName, n, e, h, code?.ifBlank { null }, desc?.ifBlank { null })
        }
    }

    fun deletePoint(id: Long) = viewModelScope.launch {
        undoStack.addLast(listOf(id))
        pointRepo.softDelete(listOf(id))
    }

    fun deletePoints(ids: Set<Long>) = viewModelScope.launch {
        if (ids.isNotEmpty()) {
            undoStack.addLast(ids.toList())
            pointRepo.softDelete(ids.toList())
        }
    }

    fun undoLastDelete() = viewModelScope.launch {
        val last = undoStack.removeLastOrNull() ?: return@launch
        pointRepo.restore(last)
    }

    fun restorePoints(ids: List<Long>) = viewModelScope.launch {
        if (ids.isNotEmpty()) pointRepo.restore(ids)
    }

    fun restoreAllDeleted() = viewModelScope.launch {
        val ids = deletedPoints.value.map { it.id }
        if (ids.isNotEmpty()) pointRepo.restore(ids)
    }

    data class ImportResult(val added: Int, val failed: Int)

    fun importFromCsv(raw: String, onResult: (ImportResult) -> Unit) = viewModelScope.launch {
        val proj = activeProject.value ?: return@launch onResult(ImportResult(0,0))
        var added = 0
        var failed = 0
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                // Beklenen format: name,northing,easting[,height][,code][,desc]
                val parts = line.split(',', ';', '\t').map { it.trim() }
                if (parts.size < 3) { failed++; return@forEach }
                val name = parts[0].ifBlank { "P${System.currentTimeMillis()}" }
                val n = parts[1].replace(',', '.').toDoubleOrNull()
                val e = parts[2].replace(',', '.').toDoubleOrNull()
                val h = parts.getOrNull(3)?.replace(',', '.')?.toDoubleOrNull()
                val code = parts.getOrNull(4)?.ifBlank { null }
                val desc = parts.getOrNull(5)?.ifBlank { null }
                if (n == null || e == null) { failed++; return@forEach }
                pointRepo.upsert(
                    PointEntity(
                        projectId = proj.id,
                        name = name,
                        northing = n,
                        easting = e,
                        ellipsoidalHeight = h,
                        featureCode = code,
                        description = desc
                    )
                )
                added++
            }
        onResult(ImportResult(added, failed))
    }

    // Export yardımcıları (EPSG kodu eklenmiş)
    private fun epsgOf(): Int? = activeProject.value?.epsgCode

    fun buildCsv(points: List<PointEntity>): String = buildString {
        epsgOf()?.let { append("# EPSG:").append(it).append('\n') }
        append("id,name,code,desc,northing,easting,ellipsoidalHeight,lat,lon,fix,hrms\n")
        points.forEach { p ->
            append(listOf(
                p.id, p.name, p.featureCode ?: "", p.description?.replace(',', ';') ?: "",
                p.northing, p.easting, p.ellipsoidalHeight ?: "", p.latDeg ?: "", p.lonDeg ?: "", p.fixType ?: "", p.hrms ?: ""
            ).joinToString(","))
            append('\n')
        }
    }

    fun buildKml(points: List<PointEntity>, docName: String = "Points"): String = buildString {
        val epsg = epsgOf()
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>")
        append(docName)
        epsg?.let { append(" (EPSG:").append(it).append(')') }
        append("</name>")
        epsg?.let { append("<description>CRS: EPSG:").append(it).append("</description>") }
        points.forEach { p ->
            val lon = p.lonDeg ?: p.easting
            val lat = p.latDeg ?: p.northing
            append("<Placemark><name>${p.name}</name>")
            val descStr = buildString {
                p.description?.let { append(it).append('\n') }
                p.featureCode?.let { append("Kod: ").append(it).append('\n') }
                p.hrms?.let { append("HRMS: ").append(it).append('\n') }
                p.fixType?.let { append("Fix: ").append(it).append('\n') }
                append("N:").append(p.northing).append(" E:").append(p.easting)
            }
            append("<description>${descStr}</description>")
            append("<Point><coordinates>${lon},${lat},${p.ellipsoidalHeight ?: 0.0}</coordinates></Point></Placemark>\n")
        }
        append("</Document></kml>")
    }

    fun buildGeoJson(points: List<PointEntity>): String = buildString {
        val epsg = epsgOf()
        append("{\"type\":\"FeatureCollection\",")
        epsg?.let { append("\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:$it\"}},") }
        append("\"features\":[")
        points.forEachIndexed { i, p ->
            if (i>0) append(',')
            val lon = p.lonDeg ?: p.easting
            val lat = p.latDeg ?: p.northing
            val props = buildString {
                append("\"name\":\"").append(p.name).append("\"")
                p.featureCode?.let { append(",\"code\":\"").append(it).append("\"") }
                p.hrms?.let { append(",\"hrms\":").append(it) }
                p.fixType?.let { append(",\"fixType\":\"").append(it).append("\"") }
                p.description?.let { append(",\"desc\":\"").append(it.replace("\"","'")) .append("\"") }
            }
            append("{\"type\":\"Feature\",\"properties\":{${props}},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${lon},${lat},${p.ellipsoidalHeight ?: 0.0}]}}")
        }
        append("]}")
    }
}
