package com.example.tugis3.ui.project

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PointDatabaseActivity : ComponentActivity() {
    private val vm: PointDatabaseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                PointDatabaseScreen(vm = vm, onBack = { finish() }, context = this)
            }
        }
    }
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PointDatabaseViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val pointRepo: SurveyPointRepository
) : ViewModel() {

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val pointsFlow: Flow<List<SurveyPointEntity>> = activeProject
        .filterNotNull()
        .flatMapLatest { p -> pointRepo.observePoints(p.id) }

    val points = pointsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterType = MutableStateFlow("Tümü")
    val filterType: StateFlow<String> = _filterType

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _message

    val filteredPoints: StateFlow<List<SurveyPointEntity>> = combine(
        points,
        _searchQuery,
        _filterType
    ) { pts, query, filter ->
        pts.filter { point ->
            val matchesSearch = query.isEmpty() ||
                    point.name.contains(query, ignoreCase = true) ||
                    (point.code?.contains(query, ignoreCase = true) == true)
            val matchesFilter = filter == "Tümü" || point.code == filter
            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterType(type: String) {
        _filterType.value = type
    }

    fun addPoint(name: String, northing: Double, easting: Double, height: Double, code: String) {
        viewModelScope.launch {
            val project = activeProject.value ?: return@launch
            runCatching {
                pointRepo.insert(
                    SurveyPointEntity(
                        projectId = project.id,
                        name = name,
                        code = code.ifBlank { null },
                        latitude = null,
                        longitude = null,
                        elevation = height,
                        northing = northing,
                        easting = easting,
                        zone = null,
                        hrms = null,
                        vrms = null,
                        pdop = null,
                        satellites = null,
                        fixType = null,
                        antennaHeight = null,
                        timestamp = System.currentTimeMillis()
                    )
                )
                _message.tryEmit("✓ Nokta eklendi: $name")
            }.onFailure {
                _message.tryEmit("✗ Hata: ${it.message}")
            }
        }
    }

    fun deletePoint(point: SurveyPointEntity) {
        viewModelScope.launch {
            runCatching {
                pointRepo.delete(point)
                _message.tryEmit("✓ Nokta silindi: ${point.name}")
            }.onFailure {
                _message.tryEmit("✗ Silme hatası: ${it.message}")
            }
        }
    }

    fun exportPointsAsCsv(context: Context) {
        viewModelScope.launch {
            runCatching {
                val csv = buildString {
                    appendLine("Adı,Kod,Kuzey(m),Doğu(m),Yükseklik(m),HRMS,VRMS,PDOP,Uydu,Anten Yüksekliği")
                    points.value.forEach { point ->
                        appendLine(
                            "${point.name},${point.code ?: ""},${point.northing ?: 0.0},${point.easting ?: 0.0},${point.elevation ?: 0.0}," +
                            "${point.hrms ?: ""},${point.vrms ?: ""},${point.pdop ?: ""},${point.satellites ?: ""},${point.antennaHeight ?: ""}"
                        )
                    }
                }
                // CSV'yi kaydet
                _message.tryEmit("✓ ${points.value.size} nokta CSV olarak dışa aktarıldı")
            }.onFailure {
                _message.tryEmit("✗ Dışa aktarma hatası: ${it.message}")
            }
        }
    }

    fun exportPointsAsNcn(context: Context) {
        viewModelScope.launch {
            runCatching {
                val ncn = buildString {
                    points.value.forEach { point ->
                        val name = point.name.replace(" ", "_")
                        val easting = String.format(Locale.US, "%.3f", point.easting ?: 0.0)
                        val northing = String.format(Locale.US, "%.3f", point.northing ?: 0.0)
                        val height = String.format(Locale.US, "%.3f", point.elevation ?: 0.0)
                        appendLine("$name $easting $northing $height")
                    }
                }
                // NCN'yi kaydet
                _message.tryEmit("✓ ${points.value.size} nokta NCN formatında dışa aktarıldı")
            }.onFailure {
                _message.tryEmit("✗ NCN dışa aktarma hatası: ${it.message}")
            }
        }
    }

    fun importPointsFromNcn(ncnContent: String) {
        viewModelScope.launch {
            val project = activeProject.value ?: return@launch
            var importedCount = 0
            runCatching {
                ncnContent.trim().lines().forEach { line ->
                    if (line.isBlank()) return@forEach
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        val pointName = parts[0]
                        val easting = parts[1].toDoubleOrNull() ?: return@forEach
                        val northing = parts[2].toDoubleOrNull() ?: return@forEach
                        val height = parts[3].toDoubleOrNull() ?: 0.0

                        pointRepo.insert(
                            SurveyPointEntity(
                                projectId = project.id,
                                name = pointName,
                                code = null,
                                latitude = null,
                                longitude = null,
                                elevation = height,
                                northing = northing,
                                easting = easting,
                                zone = null,
                                hrms = null,
                                vrms = null,
                                pdop = null,
                                satellites = null,
                                fixType = null,
                                antennaHeight = null,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        importedCount++
                    }
                }
                _message.tryEmit("✓ $importedCount nokta NCN'den içe aktarıldı")
            }.onFailure {
                _message.tryEmit("✗ NCN içe aktarma hatası: ${it.message}")
            }
        }
    }

    fun exportPoints(context: Context) {
        viewModelScope.launch {
            runCatching {
                val csv = buildString {
                    appendLine("Adı,Kod,Kuzey(m),Doğu(m),Yükseklik(m),HRMS,VRMS,PDOP,Uydu,Anten Yüksekliği")
                    points.value.forEach { point ->
                        appendLine(
                            "${point.name},${point.code ?: ""},${point.northing ?: 0.0},${point.easting ?: 0.0},${point.elevation ?: 0.0}," +
                            "${point.hrms ?: ""},${point.vrms ?: ""},${point.pdop ?: ""},${point.satellites ?: ""},${point.antennaHeight ?: ""}"
                        )
                    }
                }

                val fileName = "Points_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(csv)

                _message.tryEmit("✓ ${points.value.size} nokta dışa aktarıldı: $fileName")
            }.onFailure {
                _message.tryEmit("✗ Dışa aktarma hatası: ${it.message}")
            }
        }
    }

    fun importPoints(uri: Uri?, context: Context) {
        if (uri == null) {
            _message.tryEmit("⚠ Dosya seçilmedi")
            return
        }

        viewModelScope.launch {
            runCatching {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Dosya açılamadı")
                val lines = inputStream.bufferedReader().readLines()

                if (lines.isEmpty()) throw Exception("Dosya boş")

                var importedCount = 0
                val project = activeProject.value ?: throw Exception("Proje seçilmedi")

                // İlk satır başlık olduğu için atla
                lines.drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 5) {
                        try {
                            val point = SurveyPointEntity(
                                projectId = project.id,
                                name = parts[0].trim(),
                                code = parts[1].trim().ifBlank { null },
                                latitude = null,
                                longitude = null,
                                elevation = parts[4].trim().toDoubleOrNull(),
                                northing = parts[2].trim().toDoubleOrNull(),
                                easting = parts[3].trim().toDoubleOrNull(),
                                zone = null,
                                hrms = parts.getOrNull(5)?.trim()?.toDoubleOrNull(),
                                vrms = parts.getOrNull(6)?.trim()?.toDoubleOrNull(),
                                pdop = parts.getOrNull(7)?.trim()?.toDoubleOrNull(),
                                satellites = parts.getOrNull(8)?.trim()?.toIntOrNull(),
                                fixType = null,
                                antennaHeight = parts.getOrNull(9)?.trim()?.toDoubleOrNull(),
                                timestamp = System.currentTimeMillis()
                            )
                            pointRepo.insert(point)
                            importedCount++
                        } catch (e: Exception) {
                            // Hatalı satırı atla
                        }
                    }
                }

                _message.tryEmit("✓ $importedCount nokta içe aktarıldı")
            }.onFailure {
                _message.tryEmit("✗ İçe aktarma hatası: ${it.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointDatabaseScreen(
    vm: PointDatabaseViewModel,
    onBack: () -> Unit,
    context: Context
) {
    val activeProject by vm.activeProject.collectAsState()
    val filteredPoints by vm.filteredPoints.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val filterType by vm.filterType.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var selectedPoint by remember { mutableStateOf<SurveyPointEntity?>(null) }

    // Dosya seçici launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        vm.importPoints(uri, context)
    }

    // NCN dosya seçici
    val ncnFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (content != null) {
                    vm.importPointsFromNcn(content)
                }
            } catch (e: Exception) {
                // Hata mesajı
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nokta Veri Tabanı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Nokta Ekle")
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filtrele")
                    }

                    // İçe Aktar Menüsü
                    Box {
                        IconButton(onClick = { showImportMenu = !showImportMenu }) {
                            Icon(Icons.Default.Download, "İçe Aktar")
                        }
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("CSV Dosyası") },
                                onClick = {
                                    showImportMenu = false
                                    filePickerLauncher.launch("text/comma-separated-values")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("NCN Dosyası") },
                                onClick = {
                                    showImportMenu = false
                                    ncnFilePickerLauncher.launch("text/plain")
                                }
                            )
                        }
                    }

                    // Dışa Aktar Menüsü
                    Box {
                        IconButton(onClick = { showExportMenu = !showExportMenu }) {
                            Icon(Icons.Default.Upload, "Dışa Aktar")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("CSV Formatı") },
                                onClick = {
                                    showExportMenu = false
                                    vm.exportPointsAsCsv(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("NCN Formatı") },
                                onClick = {
                                    showExportMenu = false
                                    vm.exportPointsAsNcn(context)
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Proje Bilgisi
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Aktif Proje", style = MaterialTheme.typography.labelMedium)
                        Text(activeProject?.name ?: "Yok", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Toplam Nokta", style = MaterialTheme.typography.labelMedium)
                        Text("${filteredPoints.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Arama
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.updateSearchQuery(it) },
                label = { Text("Nokta Ara (Ad / Kod)") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Temizle")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Filtre Bilgisi
            if (filterType != "Tümü") {
                AssistChip(
                    onClick = { vm.updateFilterType("Tümü") },
                    label = { Text("Filtre: $filterType") },
                    trailingIcon = { Icon(Icons.Default.Close, null) }
                )
            }

            // Nokta Listesi
            if (filteredPoints.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocationOff, null, Modifier.size(48.dp))
                        Text("Nokta bulunamadı")
                        Text(
                            if (searchQuery.isNotEmpty() || filterType != "Tümü")
                                "Arama/filtre kriterlerini değiştirin"
                            else
                                "Yeni nokta eklemek için + butonunu kullanın",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPoints, key = { it.id }) { point ->
                        PointCard(
                            point = point,
                            onClick = { selectedPoint = point },
                            onDelete = { vm.deletePoint(point) }
                        )
                    }
                }
            }
        }
    }

    // Nokta Ekle Dialog
    if (showAddDialog) {
        AddPointDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, n, e, h, code ->
                vm.addPoint(name, n, e, h, code)
                showAddDialog = false
            }
        )
    }

    // Filtre Dialog
    if (showFilterDialog) {
        val filterOptions = listOf("Tümü", "Kontrol", "Detay", "Yapı", "Traversy")
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Nokta Filtresi") },
            text = {
                Column {
                    filterOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.updateFilterType(option)
                                    showFilterDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = filterType == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Nokta Detay Dialog
    selectedPoint?.let { point ->
        AlertDialog(
            onDismissRequest = { selectedPoint = null },
            title = { Text(point.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    point.code?.let {
                        DetailRow("Kod", it)
                    }
                    point.northing?.let {
                        DetailRow("Kuzey (m)", String.format("%.3f", it))
                    }
                    point.easting?.let {
                        DetailRow("Doğu (m)", String.format("%.3f", it))
                    }
                    point.elevation?.let {
                        DetailRow("Yükseklik (m)", String.format("%.3f", it))
                    }
                    point.hrms?.let {
                        DetailRow("HRMS", String.format("%.4f", it))
                    }
                    point.vrms?.let {
                        DetailRow("VRMS", String.format("%.4f", it))
                    }
                    point.pdop?.let {
                        DetailRow("PDOP", String.format("%.1f", it))
                    }
                    point.satellites?.let {
                        DetailRow("Uydu Sayısı", it.toString())
                    }
                    point.antennaHeight?.let {
                        DetailRow("Anten Yüksekliği (m)", String.format("%.2f", it))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPoint = null }) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
private fun PointCard(
    point: SurveyPointEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(point.name, fontWeight = FontWeight.Bold)
                val n = point.northing ?: 0.0
                val e = point.easting ?: 0.0
                val h = point.elevation ?: 0.0
                Text(
                    "K: ${String.format("%.2f", n)} D: ${String.format("%.2f", e)} Y: ${String.format("%.2f", h)}",
                    style = MaterialTheme.typography.bodySmall
                )
                point.code?.let {
                    Text(
                        "Kod: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPointDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("P${System.currentTimeMillis() % 1000}") }
    var northing by remember { mutableStateOf("") }
    var easting by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("Detay") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Nokta Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nokta Adı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = northing,
                    onValueChange = { northing = it },
                    label = { Text("Kuzey (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = easting,
                    onValueChange = { easting = it },
                    label = { Text("Doğu (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Yükseklik (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                var codeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = codeExpanded,
                    onExpandedChange = { codeExpanded = !codeExpanded }
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Kod Türü") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = codeExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = codeExpanded,
                        onDismissRequest = { codeExpanded = false }
                    ) {
                        listOf("Kontrol", "Detay", "Yapı", "Traversy").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    code = option
                                    codeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        name,
                        northing.toDoubleOrNull() ?: 0.0,
                        easting.toDoubleOrNull() ?: 0.0,
                        height.toDoubleOrNull() ?: 0.0,
                        code
                    )
                },
                enabled = name.isNotBlank() && northing.isNotBlank() && easting.isNotBlank()
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
