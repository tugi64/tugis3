package com.example.tugis3.ui.project.coord

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.prefs.PrefsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ProjectionParametersActivity : ComponentActivity() {
    private val vm: ProjectionParametersViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProjectionParametersScreen(vm = vm, onBack = { finish() }) }
    }
}

@HiltViewModel
class ProjectionParametersViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val prefs: PrefsRepository
) : ViewModel() {
    private val activeProjectFlow = projectRepo.observeActiveProject()
    val uiState = combine(activeProjectFlow, prefs.autoProjectionEnabled) { proj, auto ->
        ProjectionUiState(
            projectName = proj?.name ?: "(Aktif proje yok)",
            utmZone = proj?.utmZone?.toString().orEmpty(),
            northHemisphere = proj?.utmNorthHemisphere ?: true,
            epsg = proj?.epsgCode?.toString().orEmpty(),
            ellipsoid = proj?.ellipsoidName ?: "?",
            autoProjection = auto,
            hasEllipsoid = proj?.semiMajorA != null && proj.invFlattening != null && proj.ellipsoidName != null,
            projType = proj?.projectionType,
            cm = proj?.projCentralMeridianDeg,
            fe = proj?.projFalseEasting,
            fn = proj?.projFalseNorthing,
            k0 = proj?.projScaleFactor,
            lat0 = proj?.projLatOrigin,
            sp1 = proj?.projStdParallel1,
            sp2 = proj?.projStdParallel2
        ) to proj?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), (ProjectionUiState() to null))

    fun toggleAuto(v:Boolean) = viewModelScope.launch { prefs.setAutoProjectionEnabled(v) }

    // Uyumlu güncelleme: doğrudan updateUtm çağrısı (reflection kaldırıldı)
    private suspend fun ProjectRepository.updateUtmCompat(projectId: Long, zone: Int?, north: Boolean, epsg: Int?) {
        runCatching { updateUtm(projectId, zone, north, epsg) }
            .onFailure { Log.w("ProjectionParams", "updateUtm failed: ${it.message}") }
    }

    fun save(zoneStr:String, north:Boolean, epsgStr:String) = viewModelScope.launch {
        val (_, projId) = uiState.value
        val id = projId ?: return@launch
        val zone = zoneStr.toIntOrNull()?.takeIf { it in 1..60 }
        val epsg = when {
            epsgStr.isNotBlank() -> epsgStr.toIntOrNull()
            zone != null -> (if (north) 32600 else 32700) + zone
            else -> null
        }
        projectRepo.updateUtmCompat(id, zone, north, epsg)
    }

    suspend fun applyDefinition(def: ProjectionDefinition) {
        val (_, projId) = uiState.value
        val id = projId ?: return
        // Elipsoid
        def.ellipsoidName?.let { name ->
            if (def.semiMajor != null && def.invF != null) {
                projectRepo.updateEllipsoid(id, name, def.semiMajor, def.invF)
            }
        }
        // Projeksiyon türü ve parametreleri
        val type = def.projection
        val cm = def.centralMeridianDeg
        val fn = def.falseNorthing
        val fe = def.falseEasting
        val k0 = def.scale
        val lat0 = def.latitudeOrigin
        val sp1 = def.stdParallel1
        val sp2 = def.stdParallel2
        projectRepo.updateProjectionAdvanced(id, type, cm, fn, fe, k0, lat0, sp1, sp2)

        // UTM türet (sadece TM ve cm uygun ise)
        if (type == "Transverse_Mercator") {
            val zone = cm?.let { (((it + 183.0) / 6.0).roundToInt()).takeIf { z -> z in 1..60 && abs(z*6 - 183 - cm) < 1e-6 } }
            if (zone != null) {
                val north = (lat0 ?: 0.0) >= 0
                val epsg = (if (north) 32600 else 32700) + zone
                projectRepo.updateUtm(id, zone, north, epsg)
            }
        }
    }

    fun clearAdvanced() = viewModelScope.launch {
        val (_, projId) = uiState.value
        val id = projId ?: return@launch
        projectRepo.updateProjectionAdvanced(id, null, null, null, null, null, null, null, null)
    }

    fun saveAdvanced(
        type: String?,
        cm: String?,
        fn: String?,
        fe: String?,
        k0: String?,
        lat0: String?,
        sp1: String?,
        sp2: String?
    ) = viewModelScope.launch {
        val (_, projId) = uiState.value
        val id = projId ?: return@launch
        val t = type?.takeIf { it.isNotBlank() }
        val cmV = cm?.toDoubleOrNull()
        val fnV = fn?.toDoubleOrNull()
        val feV = fe?.toDoubleOrNull()
        var k0V = k0?.toDoubleOrNull()
        val lat0V = lat0?.toDoubleOrNull()
        val sp1V = sp1?.toDoubleOrNull()
        val sp2V = sp2?.toDoubleOrNull()
        when (t) {
            "Transverse_Mercator" -> {
                if (cmV == null) return@launch
                if (k0V == null) k0V = 0.9996
            }
            "Lambert_Conformal_Conic_2SP" -> if (cmV == null || lat0V == null || sp1V == null || sp2V == null) return@launch
        }
        projectRepo.updateProjectionAdvanced(id, t, cmV, fnV, feV, k0V, lat0V, sp1V, sp2V)
    }

    val favoriteKeys = prefs.favoriteProjectionKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    fun toggleFavorite(key:String) = viewModelScope.launch { prefs.toggleFavorite(key) }
}

data class ProjectionUiState(
    val projectName: String = "",
    val utmZone: String = "",
    val northHemisphere: Boolean = true,
    val epsg: String = "",
    val ellipsoid: String = "",
    val autoProjection: Boolean = true,
    val hasEllipsoid: Boolean = false,
    val projType: String? = null,
    val cm: Double? = null,
    val fe: Double? = null,
    val fn: Double? = null,
    val k0: Double? = null,
    val lat0: Double? = null,
    val sp1: Double? = null,
    val sp2: Double? = null
)

// Katalog satırı veri sınıfı
data class ProjectionDefinition(
    val country: String?,
    val name: String?,
    val ellipsoidName: String?,
    val semiMajor: Double?,
    val invF: Double?,
    val projection: String?,
    val centralMeridianDeg: Double?,
    val falseNorthing: Double?,
    val falseEasting: Double?,
    val scale: Double?,
    val latitudeOrigin: Double?,
    val stdParallel1: Double?,
    val stdParallel2: Double?
)

// Gelişmiş: CSV parse sonucu uyarıları ile birlikte döndür
private data class ProjectionCatalog(val list: List<ProjectionDefinition>, val warnings: List<String>)

private fun parseProjectionCsvValidated(raw:String): ProjectionCatalog {
    val warnings = mutableListOf<String>()
    val lines = raw.lineSequence().toList()
    if (lines.isEmpty()) return ProjectionCatalog(emptyList(), listOf("Dosya boş"))
    val dataLines = lines.asSequence()
        .map { it.trim('\ufeff') }
        .filter { it.isNotBlank() && !it.startsWith("//") && !it.startsWith("#") }
        .toList()
    if (dataLines.isEmpty()) return ProjectionCatalog(emptyList(), listOf("Geçerli veri satırı bulunamadı"))
    val defs = mutableListOf<ProjectionDefinition>()
    dataLines.forEachIndexed { idx, line ->
        val parts = line.split(',')
        if (parts.size < 13) {
            warnings += "Satır ${idx+1}: sütun sayısı yetersiz (${parts.size})"
        } else {
            try {
                defs += ProjectionDefinition(
                    country = parts.getOrNull(0),
                    name = parts.getOrNull(1),
                    ellipsoidName = parts.getOrNull(2),
                    semiMajor = parts.getOrNull(3)?.toDoubleOrNull(),
                    invF = parts.getOrNull(4)?.toDoubleOrNull(),
                    projection = parts.getOrNull(5),
                    centralMeridianDeg = parts.getOrNull(6)?.toDoubleOrNull(),
                    falseNorthing = parts.getOrNull(7)?.toDoubleOrNull(),
                    falseEasting = parts.getOrNull(8)?.toDoubleOrNull(),
                    scale = parts.getOrNull(9)?.toDoubleOrNull(),
                    latitudeOrigin = parts.getOrNull(10)?.toDoubleOrNull(),
                    stdParallel1 = parts.getOrNull(11)?.toDoubleOrNull(),
                    stdParallel2 = parts.getOrNull(12)?.toDoubleOrNull()
                )
            } catch (e:Exception) {
                warnings += "Satır ${idx+1}: parse hatası (${e.message})"
            }
        }
    }
    if (defs.isEmpty() && warnings.isEmpty()) warnings += "Hiç tanım üretilemedi"
    return ProjectionCatalog(defs, warnings)
}

private fun defKey(def: ProjectionDefinition): String = listOf(
    def.name.orEmpty(),
    def.projection.orEmpty(),
    def.centralMeridianDeg?.toString().orEmpty(),
    def.stdParallel1?.toString().orEmpty(),
    def.stdParallel2?.toString().orEmpty(),
    def.ellipsoidName.orEmpty()
).joinToString("|")

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' }.take(18)) },
        singleLine = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProjectionRow(
    def: ProjectionDefinition,
    isFavorite: Boolean,
    onApply: (ProjectionDefinition) -> Unit,
    onToggleFavorite: (ProjectionDefinition) -> Unit
) {
    // projectionKey kaldırıldı (kullanılmıyordu)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onApply(def) }
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(def.name ?: "(adsız)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    def.ellipsoidName?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    def.projection?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    def.centralMeridianDeg?.let { Text("CM=${it}", style = MaterialTheme.typography.labelSmall) }
                }
            }
            IconButton(onClick = { onToggleFavorite(def) }) {
                if (isFavorite) Icon(Icons.Filled.Star, contentDescription = "Favoriden çıkar") else Icon(
                    Icons.Outlined.StarBorder,
                    contentDescription = "Favoriye ekle"
                )
            }
        }
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectionParametersScreen(vm: ProjectionParametersViewModel, onBack: () -> Unit) {
    val (state, _) = vm.uiState.collectAsState().value
    val favoriteKeys by vm.favoriteKeys.collectAsState()
    var zone by remember(state.utmZone) { mutableStateOf(state.utmZone) }
    var epsg by remember(state.epsg) { mutableStateOf(state.epsg) }
    var north by remember(state.northHemisphere) { mutableStateOf(state.northHemisphere) }
    var auto by remember(state.autoProjection) { mutableStateOf(state.autoProjection) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCatalog by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var advType by remember(state.projType) { mutableStateOf(state.projType ?: "") }
    var advCm by remember(state.cm) { mutableStateOf(state.cm?.toString().orEmpty()) }
    var advFn by remember(state.fn) { mutableStateOf(state.fn?.toString().orEmpty()) }
    var advFe by remember(state.fe) { mutableStateOf(state.fe?.toString().orEmpty()) }
    var advK0 by remember(state.k0) { mutableStateOf(state.k0?.toString().orEmpty()) }
    var advLat0 by remember(state.lat0) { mutableStateOf(state.lat0?.toString().orEmpty()) }
    var advSp1 by remember(state.sp1) { mutableStateOf(state.sp1?.toString().orEmpty()) }
    var advSp2 by remember(state.sp2) { mutableStateOf(state.sp2?.toString().orEmpty()) }
    val projectionTypes = listOf("", "Transverse_Mercator", "Lambert_Conformal_Conic_2SP")
    val zoneInt = zone.toIntOrNull()
    val centralMeridianDeg = remember(zoneInt) { zoneInt?.let { it * 6 - 183 } }
    val k0 = 0.9996

    // CSV yükleme sade
    val catalogResult = remember {
        runCatching {
            val text = try {
                context.assets.open("Projeksions.csv").use { it.reader(Charsets.UTF_8).readText() }
            } catch (_: Exception) {
                try {
                    val fileStream = File(context.filesDir.parentFile?.parentFile, "Projeksions.csv").takeIf { it.exists() }?.readText()
                    fileStream ?: ""
                } catch (_: Exception) { "" }
            }
            parseProjectionCsvValidated(text)
        }.getOrElse { ProjectionCatalog(emptyList(), listOf("Okuma hatası: ${it.message}")) }
    }
    val definitions = catalogResult.list

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val customDefsFile = remember { File(context.filesDir, "custom_projections.json") }
    val customDefs = remember {
        mutableStateListOf<ProjectionDefinition>().apply {
            val list = runCatching { readCustomDefinitions(customDefsFile) }.getOrDefault(emptyList())
            addAll(list)
        }
    }
    fun persistCustom() { runCatching { writeCustomDefinitions(customDefsFile, customDefs) } }
    var loadedLimit by remember { mutableStateOf(300) }
    val baseDefinitions = remember(definitions, customDefs) { (definitions + customDefs).take(loadedLimit) }
    var filter by remember { mutableStateOf("") }

    fun exportProjections(): File? {
        val gson = Gson()
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(context.filesDir, "projection_export_${time}.json")
        val current = vm.uiState.value.first
        val favoritesMap = baseDefinitions.associateBy { defKey(it) }
        val favorites = vm.favoriteKeys.value.mapNotNull { favoritesMap[it] }
        val exportObj = mapOf(
            "currentProjectProjection" to mapOf(
                "projectionType" to current.projType,
                "centralMeridianDeg" to current.cm,
                "falseNorthing" to current.fn,
                "falseEasting" to current.fe,
                "scale" to current.k0,
                "latitudeOrigin" to current.lat0,
                "stdParallel1" to current.sp1,
                "stdParallel2" to current.sp2,
                "utmZone" to current.utmZone,
                "epsg" to current.epsg,
                "ellipsoid" to current.ellipsoid
            ),
            "favorites" to favorites,
            "customDefinitions" to customDefs
        )
        return runCatching { outFile.writeText(gson.toJson(exportObj)); outFile }.getOrNull()
    }

    fun importJson(text: String): Int {
        val element = JsonParser.parseString(text)
        val added = mutableListOf<ProjectionDefinition>()
        fun toDef(obj: JsonElement): ProjectionDefinition? = runCatching {
            val o = obj.asJsonObject
            ProjectionDefinition(
                country = o.get("country")?.asString,
                name = o.get("name")?.asString,
                ellipsoidName = o.get("ellipsoidName")?.asString ?: o.get("ellipsoid")?.asString,
                semiMajor = o.get("semiMajor")?.asDouble,
                invF = o.get("invF")?.asDouble,
                projection = o.get("projection")?.asString ?: o.get("projectionType")?.asString,
                centralMeridianDeg = o.get("centralMeridianDeg")?.asDouble,
                falseNorthing = o.get("falseNorthing")?.asDouble,
                falseEasting = o.get("falseEasting")?.asDouble,
                scale = o.get("scale")?.asDouble,
                latitudeOrigin = o.get("latitudeOrigin")?.asDouble,
                stdParallel1 = o.get("stdParallel1")?.asDouble,
                stdParallel2 = o.get("stdParallel2")?.asDouble
            )
        }.onFailure { Log.w("ProjectionParams", "JSON tanım parse hatası: ${it.message}") }.getOrNull()
        if (element.isJsonArray) element.asJsonArray.forEach { el -> toDef(el)?.let { added += it } } else if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("favorites")) obj.getAsJsonArray("favorites").forEach { el -> toDef(el)?.let { added += it } }
            if (obj.has("customDefinitions")) obj.getAsJsonArray("customDefinitions").forEach { el -> toDef(el)?.let { added += it } }
            toDef(element)?.let { added += it }
        }
        val existingKeys = (definitions + customDefs).map { defKey(it) }.toMutableSet()
        var count = 0
        added.forEach { d -> val k = defKey(d); if (k !in existingKeys) { customDefs += d; existingKeys += k; count++ } }
        if (count > 0) {
            persistCustom()
            Log.i("ProjectionParams", "$count yeni tanım eklendi")
        }
        return count
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projeksiyon Parametreleri") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        vm.save(zone, north, epsg)
                        scope.launch { snack.showSnackbar("Kaydedildi") }
                    }) { Icon(Icons.Default.Save, contentDescription = null) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menü") }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Dışa Aktar JSON") }, onClick = {
                                menuExpanded = false
                                val file = exportProjections()
                                scope.launch { snack.showSnackbar(file?.let { "Kaydedildi: ${it.name}" } ?: "Hata: yazılamadı") }
                            })
                            DropdownMenuItem(text = { Text("İçe Aktar JSON") }, onClick = {
                                menuExpanded = false
                                showImportDialog = true
                            })
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCatalog = true }) { Text("Katalogdan Seç") }
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aktif Proje: ${state.projectName}", style = MaterialTheme.typography.titleMedium)
                    Text("Elipsoid: ${state.ellipsoid}${if (!state.hasEllipsoid) " (eksik - Ellipsoid Parametreleri ekranından seçin)" else ""}")
                    if (state.projType != null) {
                        HorizontalDivider()
                        Text("Projeksiyon: ${state.projType}", style = MaterialTheme.typography.labelLarge)
                        Text("CM: ${state.cm ?: "-"}  k0: ${state.k0 ?: "-"}")
                        Text("FalseE: ${state.fe ?: "-"}  FalseN: ${state.fn ?: "-"}")
                        if (state.projType.contains("Lambert")) {
                            Text("Lat0: ${state.lat0 ?: "-"}  SP1: ${state.sp1 ?: "-"}  SP2: ${state.sp2 ?: "-"}")
                        } else if (state.projType == "Transverse_Mercator") {
                            Text("Lat0: ${state.lat0 ?: 0.0}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.clearAdvanced() }) { Text("Temizle") }
                        }
                    }
                }
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Otomatik Doldurma", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = auto, onCheckedChange = {
                            auto = it; vm.toggleAuto(it)
                        })
                        Spacer(Modifier.width(12.dp))
                        Text("İlk GNSS fix ile WGS84 + UTM otomatik ayarla")
                    }
                    if (!auto) {
                        Text("Manuel mod: Aşağıdaki alanları düzenleyip kaydedin.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("UTM / EPSG", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = zone,
                        onValueChange = { zone = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("UTM Zone (1-60)") },
                        singleLine = true,
                        supportingText = { Text("Boş bırakılırsa EPSG otomatik hesaplanmaz") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !auto
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = north, onCheckedChange = { north = it }, enabled = !auto)
                        Spacer(Modifier.width(8.dp))
                        Text(if (north) "Kuzey Yarımküre" else "Güney Yarımküre")
                    }
                    OutlinedTextField(
                        value = epsg,
                        onValueChange = { epsg = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text("EPSG Kodu") },
                        singleLine = true,
                        supportingText = { Text("Boş ise zone + yarımküreye göre otomatik") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !auto
                    )
                    if (zoneInt != null && zoneInt in 1..60) {
                        // Bilgi kartı
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Türetilmiş Bilgiler", style = MaterialTheme.typography.titleSmall)
                                Text("Merkezi Meridyen (°): ${centralMeridianDeg}")
                                Text("Ölçek Faktörü k0: $k0")
                                Text("False Easting: 500000 m")
                                Text("False Northing: ${if (north) 0 else 10000000} m")
                            }
                        }
                    }
                    Button(onClick = {
                        zone = ""; epsg = ""; north = true
                        scope.launch { snack.showSnackbar("Alanlar sıfırlandı") }
                    }, enabled = !auto) { Text("Sıfırla") }
                }
            }
            // --- Yeni: Gelişmiş manuel projeksiyon parametre kartı ---
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Gelişmiş Projeksiyon", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(if (showAdvanced) "Gizle" else "Göster") }
                    }
                    if (showAdvanced) {
                        var typeMenu by remember { mutableStateOf(false) }
                        // ExposedDropdownMenu yerine basit Box + DropdownMenu
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = advType,
                                onValueChange = {},
                                label = { Text("Projeksiyon Tipi") },
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { typeMenu = !typeMenu }
                            )
                            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                                projectionTypes.forEach { t ->
                                    DropdownMenuItem(text = { Text(if (t.isBlank()) "(Seçilmemiş)" else t) }, onClick = {
                                        advType = t
                                        if (t == "Transverse_Mercator" && advK0.isBlank()) advK0 = "0.9996"
                                        typeMenu = false
                                    })
                                }
                            }
                        }
                        when (advType) {
                            "Transverse_Mercator" -> {
                                NumField(advCm, { advCm = it }, "Central Meridian (deg)")
                                NumField(advK0, { advK0 = it }, "Scale Factor k0")
                                NumField(advFe, { advFe = it }, "False Easting (m)")
                                NumField(advFn, { advFn = it }, "False Northing (m)")
                                NumField(advLat0, { advLat0 = it }, "Latitude Origin (deg)")
                            }
                            "Lambert_Conformal_Conic_2SP" -> {
                                NumField(advCm, { advCm = it }, "Central Meridian (deg)")
                                NumField(advLat0, { advLat0 = it }, "Latitude Origin (deg)")
                                NumField(advSp1, { advSp1 = it }, "Std Parallel 1 (deg)")
                                NumField(advSp2, { advSp2 = it }, "Std Parallel 2 (deg)")
                                NumField(advFe, { advFe = it }, "False Easting (m)")
                                NumField(advFn, { advFn = it }, "False Northing (m)")
                            }
                            else -> Text("Tip seçilmemiş. Katalogdan seçim yapabilir veya tip seçerek değer girebilirsiniz.", style = MaterialTheme.typography.labelSmall)
                        }
                        val validationError = remember(advType, advCm, advLat0, advSp1, advSp2) {
                            when (advType) {
                                "Transverse_Mercator" -> if (advCm.isBlank()) "TM için merkezi meridyen gerekli" else null
                                "Lambert_Conformal_Conic_2SP" -> if (advCm.isBlank() || advLat0.isBlank() || advSp1.isBlank() || advSp2.isBlank()) "LCC için CM, Lat0, SP1, SP2 gerekli" else null
                                else -> null
                            }
                        }
                        validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                vm.saveAdvanced(
                                    advType.ifBlank { null },
                                    advCm.ifBlank { null },
                                    advFn.ifBlank { null },
                                    advFe.ifBlank { null },
                                    advK0.ifBlank { null },
                                    advLat0.ifBlank { null },
                                    advSp1.ifBlank { null },
                                    advSp2.ifBlank { null }
                                )
                                scope.launch { snack.showSnackbar("Gelişmiş projeksiyon kaydedildi") }
                            }, enabled = validationError == null) { Text("Kaydet") }
                            OutlinedButton(onClick = {
                                advType = ""; advCm = ""; advFn = ""; advFe = ""; advK0 = ""; advLat0 = ""; advSp1 = ""; advSp2 = ""
                                vm.clearAdvanced()
                                scope.launch { snack.showSnackbar("Temizlendi") }
                            }) { Text("Temizle") }
                        }
                    }
                }
            }
            Text(
                "Not: Gelişmiş projeksiyon parametreleri (CM, k0, False E/N, Lat0, Standard Parallels) veri tabanına eklendi. Katalogdan seçim yapınca uygun alanlar otomatik kaydedilir. Manuel düzenleme için ileride ayrı bir form eklenebilir.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    // KATALOG DİYALOĞU
    if (showCatalog) {
        AlertDialog(
            onDismissRequest = { showCatalog = false },
            confirmButton = { TextButton(onClick = { showCatalog = false }) { Text("Kapat") } },
            title = { Text("Projeksiyon Kataloğu (${definitions.size})") },
            text = {
                var catalogTab by remember { mutableStateOf(0) }
                var expandedCountry by remember { mutableStateOf<String?>(null) }
                var expandedEllipsoid by remember { mutableStateOf<String?>(null) }
                val favoritesDefs = remember(definitions, favoriteKeys) {
                    val keyMap = definitions.associateBy { defKey(it) }
                    favoriteKeys.mapNotNull { keyMap[it] }
                }
                val countries = remember(definitions) { definitions.mapNotNull { it.country?.takeIf { c -> c.isNotBlank() } }.distinct().sorted() }
                val ellipsoids = remember(definitions) { definitions.mapNotNull { it.ellipsoidName?.takeIf { e -> e.isNotBlank() } }.distinct().sorted() }
                val defsByCountry = remember(definitions) { definitions.groupBy { it.country.orEmpty() } }
                val defsByEllipsoid = remember(definitions) { definitions.groupBy { it.ellipsoidName.orEmpty() } }
                val hasFavorites = favoritesDefs.isNotEmpty()
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (catalogResult.warnings.isNotEmpty()) {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Uyarılar", style = MaterialTheme.typography.labelLarge)
                                catalogResult.warnings.take(4).forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
                                if (catalogResult.warnings.size > 4) Text("… ${catalogResult.warnings.size - 4} daha", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    OutlinedTextField(value = filter, onValueChange = { filter = it }, singleLine = true, label = { Text("Ara") }, modifier = Modifier.fillMaxWidth())
                    val tabs = buildList { if (hasFavorites) add("Favoriler"); add("Ülke"); add("Elipsoid") }
                    val effectiveIndex = when {
                        !hasFavorites && catalogTab > 1 -> 0
                        else -> catalogTab
                    }
                    TabRow(selectedTabIndex = effectiveIndex) {
                        tabs.forEachIndexed { idx, title ->
                            Tab(
                                selected = effectiveIndex == idx,
                                onClick = { catalogTab = idx },
                                text = { Text(title) }
                            )
                        }
                    }
                    HorizontalDivider()
                    val listModifier = Modifier.height(320.dp)
                    val applyDef: (ProjectionDefinition) -> Unit = { def ->
                        scope.launch { vm.applyDefinition(def) }
                        def.centralMeridianDeg?.let { cmVal ->
                            val z = ((cmVal + 183.0) / 6.0)
                            if (abs(z - z.roundToInt()) < 1e-6) {
                                val zi = z.roundToInt(); if (zi in 1..60) zone = zi.toString()
                            }
                        }
                        showAdvanced = true
                        scope.launch { snack.showSnackbar("${def.name} uygulandı") }
                        showCatalog = false
                    }
                    val toggleFav: (ProjectionDefinition) -> Unit = { d -> vm.toggleFavorite(defKey(d)) }
                    when {
                        definitions.isEmpty() -> {
                            Text("Katalog boş veya yüklenemedi", style = MaterialTheme.typography.bodyMedium)
                        }
                        hasFavorites && effectiveIndex == 0 -> {
                            if (favoritesDefs.isEmpty()) {
                                Text("Henüz favori yok", style = MaterialTheme.typography.labelSmall)
                            } else {
                                val favFiltered = favoritesDefs.filter { def ->
                                    filter.isBlank() || (def.name ?: "").contains(filter, true) || (def.country ?: "").contains(filter, true)
                                }
                                LazyColumn(listModifier) {
                                    items(favFiltered, key = { defKey(it) }) { def -> ProjectionRow(def, defKey(def) in favoriteKeys, applyDef, toggleFav) }
                                }
                            }
                        }
                        (hasFavorites && effectiveIndex == 1) || (!hasFavorites && effectiveIndex == 0) -> {
                            val countryFiltered = if (filter.isBlank()) countries else countries.filter { it.contains(filter, true) }
                            LazyColumn(listModifier) {
                                items(countryFiltered, key = { it }) { c ->
                                    val defs = defsByCountry[c].orEmpty().let { list -> if (filter.isBlank()) list else list.filter { d -> (d.name ?: "").contains(filter, true) || (d.ellipsoidName ?: "").contains(filter, true) } }
                                    val expanded = expandedCountry == c
                                    Column(Modifier.fillMaxWidth().clickable { expandedCountry = if (expanded) null else c }.padding(vertical = 6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("$c (${defs.size})", style = MaterialTheme.typography.bodyMedium)
                                            Text(if (expanded) "-" else "+", style = MaterialTheme.typography.labelLarge)
                                        }
                                        if (expanded) {
                                            defs.take(200).forEach { d -> key(defKey(d)) { ProjectionRow(d, defKey(d) in favoriteKeys, applyDef, toggleFav) } }
                                            if (defs.size > 200) Text("… ${defs.size - 200} daha", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                        else -> {
                            val ellipsoidFiltered = if (filter.isBlank()) ellipsoids else ellipsoids.filter { it.contains(filter, true) }
                            LazyColumn(listModifier) {
                                items(ellipsoidFiltered, key = { it }) { eName ->
                                    val defs = defsByEllipsoid[eName].orEmpty().let { list -> if (filter.isBlank()) list else list.filter { d -> (d.name ?: "").contains(filter, true) || (d.country ?: "").contains(filter, true) } }
                                    val expanded = expandedEllipsoid == eName
                                    Column(Modifier.fillMaxWidth().clickable { expandedEllipsoid = if (expanded) null else eName }.padding(vertical = 6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("$eName (${defs.size})", style = MaterialTheme.typography.bodyMedium)
                                            Text(if (expanded) "-" else "+", style = MaterialTheme.typography.labelLarge)
                                        }
                                        if (expanded) {
                                            defs.take(200).forEach { d -> key(defKey(d)) { ProjectionRow(d, defKey(d) in favoriteKeys, applyDef, toggleFav) } }
                                            if (defs.size > 200) Text("… ${defs.size - 200} daha", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            confirmButton = { TextButton(onClick = {
                importError = null
                val added = runCatching { importJson(importText) }.getOrElse { e -> importError = e.message; 0 }
                if (importError == null) {
                    scope.launch { snack.showSnackbar("$added tanım eklendi") }
                    showImportDialog = false
                    importText = ""
                }
            }) { Text("İçe Aktar") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("İptal") } },
            title = { Text("Projeksiyon JSON İçe Aktar") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("JSON") },
                        minLines = 6,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth()
                    )
                    importError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                    Text("Format: Tekil obje veya { favorites:[...], customDefinitions:[...] }", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }
}
// Yardımcı dosya okuma/yazma fonksiyonları
private fun readCustomDefinitions(file: File): List<ProjectionDefinition> = runCatching {
    if (!file.exists()) return emptyList()
    val json = file.readText()
    val arr = JsonParser.parseString(json).asJsonArray
    arr.mapNotNull { el ->
        runCatching {
            val o = el.asJsonObject
            ProjectionDefinition(
                country = o.get("country")?.asString,
                name = o.get("name")?.asString,
                ellipsoidName = o.get("ellipsoidName")?.asString ?: o.get("ellipsoid")?.asString,
                semiMajor = o.get("semiMajor")?.asDouble,
                invF = o.get("invF")?.asDouble,
                projection = o.get("projection")?.asString ?: o.get("projectionType")?.asString,
                centralMeridianDeg = o.get("centralMeridianDeg")?.asDouble,
                falseNorthing = o.get("falseNorthing")?.asDouble,
                falseEasting = o.get("falseEasting")?.asDouble,
                scale = o.get("scale")?.asDouble,
                latitudeOrigin = o.get("latitudeOrigin")?.asDouble,
                stdParallel1 = o.get("stdParallel1")?.asDouble,
                stdParallel2 = o.get("stdParallel2")?.asDouble
            )
        }.getOrNull()
    }
}.getOrDefault(emptyList())

private fun writeCustomDefinitions(file: File, defs: List<ProjectionDefinition>) {
    val gson = Gson()
    file.writeText(gson.toJson(defs))
}
