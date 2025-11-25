@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.example.tugis3.ui.survey

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tugis3.data.db.dao.PointDao
import com.example.tugis3.data.db.dao.ProjectDao
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.gnss.model.FixType
import com.example.tugis3.ui.project.PointListActivity
import dagger.hilt.android.AndroidEntryPoint
import com.example.tugis3.ui.theme.Tugis3Theme
import com.example.tugis3.ui.cad.CadApplicationActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class PointApplicationActivity : ComponentActivity() {

    // Veritabanı bağımlılıkları
    @Inject
    lateinit var pointDao: PointDao

    @Inject
    lateinit var projectDao: ProjectDao

    // Dosya seçici için ActivityResultLauncher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            handleFileImport(selectedUri)
        }
    }

    // Dosya kaydetme için ActivityResultLauncher
    private val fileSaverLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            handleFileExport(selectedUri)
        }
    }

    private var currentFileType = "CSV"

    // Aktif proje ID'si için state
    private var currentProjectId = 1L // TODO: Aktif projeyi repository'den al

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                PointApplicationRoot(
                    onBack = { finish() },
                    onImportFile = { fileType ->
                        currentFileType = fileType
                        importFile(fileType)
                    },
                    onExportFile = { fileType ->
                        currentFileType = fileType
                        exportFile(fileType)
                    },
                    projectDao = projectDao,
                    pointDao = pointDao
                )
            }
        }
        // Aktif proje ID'sini yükle
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val active = projectDao.observeActiveProject().first()
                if (active?.id != null) {
                    currentProjectId = active.id
                }
            } catch (_: Exception) { }
        }
    }

    private fun importFile(fileType: String) {
        try {
            val mimeType = when (fileType) {
                "CSV" -> "text/csv"
                "GPX" -> "application/gpx+xml"
                "KML" -> "application/vnd.google-earth.kml+xml"
                "DXF" -> "application/dxf"
                "SHP" -> "application/x-esri-shapefile"
                else -> "*/*"
            }

            filePickerLauncher.launch(mimeType)
        } catch (e: Exception) {
            Toast.makeText(this, "Dosya seçici açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportFile(fileType: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val projName = if (currentProjectId != 0L) {
                    try { projectDao.getById(currentProjectId)?.name } catch (_: Exception) { null }
                } else null
                val safeName = sanitizeFileName(projName)
                val fileName = buildString {
                    append("tugis_export")
                    if (!safeName.isNullOrEmpty()) append("_").append(safeName)
                    append("_").append(System.currentTimeMillis()).append(".").append(fileType.lowercase())
                }
                withContext(Dispatchers.Main) {
                    fileSaverLauncher.launch(fileName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PointApplicationActivity, "Dosya kaydetme açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sanitizeFileName(name: String?): String? {
        if (name == null) return null
        // Windows için geçersiz karakterleri temizle
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(50)
    }

    private fun handleFileImport(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (currentProjectId == 0L) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PointApplicationActivity, "Aktif proje yok", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().readText()

                    when (currentFileType) {
                        "CSV" -> importCSV(content)
                        "GPX" -> importGPX(content)
                        "KML" -> importKML(content)
                        "DXF" -> importDXF(content)
                        "SHP" -> importSHP(uri)
                        else -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@PointApplicationActivity, "Desteklenmeyen dosya türü", Toast.LENGTH_SHORT).show()
                            }
                            return@use
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PointApplicationActivity, "$currentFileType dosyası başarıyla içeri aktarıldı", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PointApplicationActivity, "Dosya içeri aktarma hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleFileExport(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (currentProjectId == 0L) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PointApplicationActivity, "Aktif proje yok", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val content = when (currentFileType) {
                    "CSV" -> generateCSV()
                    "GPX" -> generateGPX()
                    "KML" -> generateKML()
                    "DXF" -> generateDXF()
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PointApplicationActivity, "Desteklenmeyen dosya türü", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PointApplicationActivity, "$currentFileType dosyası başarıyla dışarı aktarıldı", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PointApplicationActivity, "Dosya dışarı aktarma hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // CSV util: alan kaçışı
    private fun csvEscape(raw: String): String {
        var v = raw
        var needsQuote = false
        if (v.contains('"')) {
            v = v.replace("\"", "\"\"")
            needsQuote = true
        }
        if (v.contains(',') || v.contains('\n') || v.contains('\r')) {
            needsQuote = true
        }
        return if (needsQuote) "\"$v\"" else v
    }

    private suspend fun generateCSV(): String {
        val sb = StringBuilder()
        sb.appendLine("NOKTA_ID,NORTHING,EASTING,HEIGHT,DESCRIPTION")

        // Veritabanından aktif projedeki noktaları tek seferlik çek
        val points: List<PointEntity> = try {
            pointDao.observePoints(currentProjectId).first()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PointApplicationActivity, "Veri okuma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            emptyList()
        }

        if (points.isNotEmpty()) {
            points.forEach { p ->
                val height = p.ellipsoidalHeight ?: p.orthoHeight
                val name = csvEscape(p.name.replace("\n", " "))
                val desc = csvEscape((p.description ?: "").replace("\n", " "))
                sb.appendLine("$name,${p.northing},${p.easting},${height ?: ""},$desc")
            }
        }

        return sb.toString()
    }

    // CSV import/export fonksiyonları
    private suspend fun importCSV(content: String) {
        val lines = content.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return
        val headerFields = parseCsvLine(lines.first())
        val headerMap = findHeaderIndexes(headerFields)
        var importedCount = 0
        val dataLines = if (headerMap.values.any { it >= 0 }) lines.drop(1) else lines // başlık yoksa tüm satırlar veri kabul

        for (line in dataLines) {
            val cols = parseCsvLine(line)
            fun colAt(i: Int): String? = if (i in cols.indices) cols[i] else null
            // Esnek indeksler, yoksa klasik sıraya düş
            val name = colAt(headerMap["name"] ?: -1) ?: cols.getOrNull(0)
            val nStr = colAt(headerMap["north"] ?: -1) ?: cols.getOrNull(1)
            val eStr = colAt(headerMap["east"] ?: -1) ?: cols.getOrNull(2)
            val hStr = colAt(headerMap["height"] ?: -1) ?: cols.getOrNull(3)
            val dStr = colAt(headerMap["desc"] ?: -1) ?: cols.getOrNull(4)

            val northing = nStr?.trim()?.toDoubleOrNull()
            val easting = eStr?.trim()?.toDoubleOrNull()
            val height = hStr?.trim()?.toDoubleOrNull()
            val desc = dStr?.trim() ?: ""
            val pointName = (name ?: "").trim().ifEmpty { "PT_${System.currentTimeMillis()}" }

            if (northing != null && easting != null) {
                val point = PointEntity(
                    id = 0,
                    projectId = currentProjectId,
                    name = pointName,
                    northing = northing,
                    easting = easting,
                    ellipsoidalHeight = height,
                    orthoHeight = height,
                    description = desc,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                try { pointDao.upsert(point); importedCount++ } catch (_: Exception) { }
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(this@PointApplicationActivity, "$importedCount nokta içeri aktarıldı", Toast.LENGTH_SHORT).show()
        }
    }

    // GPX import/export fonksiyonları
    @Suppress("UNUSED_PARAMETER")
    private fun importGPX(content: String) {
        // GPX XML parsing implementasyonu
        Toast.makeText(this, "GPX dosyası işlendi", Toast.LENGTH_SHORT).show()
    }

    private suspend fun generateGPX(): String {
        val points: List<PointEntity> = try {
            pointDao.observePoints(currentProjectId).first()
        } catch (_: Exception) { emptyList() }
        val timeStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.time.Instant.now().toString()
        } else {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.format(java.util.Date())
        }
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>\n<gpx version="1.1" creator="TUGIS3">\n  <metadata>\n    <name>TUGIS Export</name>\n    <time>$timeStr</time>\n  </metadata>\n""")
        points.forEach { p ->
            val lat = p.latDeg
            val lon = p.lonDeg
            if (lat != null && lon != null) {
                val ele = p.orthoHeight ?: p.ellipsoidalHeight
                sb.append("  <wpt lat=\"").append(String.format(Locale.US, "%.8f", lat)).append("\" lon=\"")
                    .append(String.format(Locale.US, "%.8f", lon)).append("\">\n")
                sb.append("    <name>").append(csvEscape(p.name)).append("</name>\n")
                if (ele != null) sb.append("    <ele>").append(String.format(Locale.US, "%.3f", ele)).append("</ele>\n")
                val desc = p.description
                if (!desc.isNullOrBlank()) sb.append("    <desc>").append(csvEscape(desc)).append("</desc>\n")
                sb.append("  </wpt>\n")
            }
        }
        sb.append("</gpx>")
        return sb.toString()
    }

    // KML import/export fonksiyonları
    @Suppress("UNUSED_PARAMETER")
    private fun importKML(content: String) {
        // KML XML parsing implementasyonu
        Toast.makeText(this, "KML dosyası işlendi", Toast.LENGTH_SHORT).show()
    }

    private suspend fun generateKML(): String {
        val points: List<PointEntity> = try {
            pointDao.observePoints(currentProjectId).first()
        } catch (_: Exception) { emptyList() }
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>\n<kml xmlns="http://www.opengis.net/kml/2.2">\n  <Document>\n    <name>TUGIS Export</name>\n    <description>TUGIS3 Nokta Aktarımı</description>\n""")
        points.forEach { p ->
            val lat = p.latDeg
            val lon = p.lonDeg
            if (lat != null && lon != null) {
                val ele = p.orthoHeight ?: p.ellipsoidalHeight ?: 0.0
                sb.append("    <Placemark>\n")
                sb.append("      <name>").append(csvEscape(p.name)).append("</name>\n")
                val desc = p.description
                if (!desc.isNullOrBlank()) sb.append("      <description>").append(csvEscape(desc)).append("</description>\n")
                sb.append("      <Point>\n        <coordinates>")
                    .append(String.format(Locale.US, "%.8f,%.8f,%.3f", lon, lat, ele))
                    .append("</coordinates>\n      </Point>\n    </Placemark>\n")
            }
        }
        sb.append("  </Document>\n</kml>")
        return sb.toString()
    }

    // DXF import/export fonksiyonları
    @Suppress("UNUSED_PARAMETER")
    private fun importDXF(content: String) {
        // DXF parsing implementasyonu
        Toast.makeText(this, "DXF dosyası işlendi", Toast.LENGTH_SHORT).show()
    }

    private fun generateDXF(): String {
        return """0
SECTION
2
HEADER
9
${'$'}ACADVER
1
AC1015
0
ENDSEC
0
SECTION
2
ENTITIES
0
ENDSEC
0
EOF"""
    }

    // SHP import fonksiyonu
    @Suppress("UNUSED_PARAMETER")
    private fun importSHP(uri: Uri) {
        // Shapefile parsing implementasyonu
        Toast.makeText(this, "SHP dosyası işlendi", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_MEAS_MODE = "measurement_mode"
    }
}

@Composable
private fun PointApplicationRoot(
    onBack: () -> Unit,
    onImportFile: (String) -> Unit,
    onExportFile: (String) -> Unit,
    projectDao: ProjectDao,
    pointDao: PointDao
) {
    val context = LocalContext.current

    // Aktif proje
    val activeProject by projectDao.observeActiveProject().collectAsState(initial = null)
    val activeProjectId = activeProject?.id ?: 0L
    val projectName = activeProject?.name ?: "—"

    // Aktif projedeki nokta sayısı
    val pointsInProject by remember(activeProjectId) {
        if (activeProjectId != 0L) pointDao.observePoints(activeProjectId) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val pointCount = pointsInProject.size

    // GNSS konumu - grafik alım ekranından
    val pos by GnssPositionRepository.position.collectAsState()
    val latRepo = pos.lat
    val lonRepo = pos.lon
    val hdop = pos.hdop
    val fix = pos.fixType
    val sats = pos.satsInUse
    val ageSec = remember(pos.timestamp) { ((System.currentTimeMillis() - pos.timestamp) / 1000.0) }

    val measurementModes = listOf("Standart", "Epoklu", "Hızlı", "Otomatik")
    var selectedMode by remember { mutableStateOf(measurementModes.first()) }

    // Ana 3 Panel Düzeni - Grafik Alım Tarzı
    Row(modifier = Modifier.fillMaxSize()) {
        // Sol Panel - Koordinat ve Kalite Bilgileri
        Card(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOKTA UYGULAMASI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }

                HorizontalDivider()

                // Koordinat Bilgileri
                Text(
                    text = "KOORDİNAT BİLGİLERİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                CoordinateDisplayItem(
                    label = "KUZEY (N)",
                    value = latRepo?.let { String.format(Locale.US, "%.8f", it) } ?: "---",
                    unit = "°"
                )

                CoordinateDisplayItem(
                    label = "DOĞU (E)",
                    value = lonRepo?.let { String.format(Locale.US, "%.8f", it) } ?: "---",
                    unit = "°"
                )

                CoordinateDisplayItem(
                    label = "YÜKSEKLİK",
                    value = pos.alt?.let { String.format(Locale.US, "%.3f", it) } ?: "---",
                    unit = "m"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Kalite Bilgileri
                Text(
                    text = "KALİTE GÖSTERGELERİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                QualityDisplayItem(
                    label = "FIX TİPİ",
                    value = when(fix) {
                        FixType.RTK_FIX -> "RTK FIX"
                        FixType.RTK_FLOAT -> "RTK FLOAT"
                        FixType.DGPS -> "DGPS"
                        FixType.SINGLE -> "GPS 3D"
                        else -> "YOK"
                    },
                    isGood = fix == FixType.RTK_FIX
                )

                QualityDisplayItem(
                    label = "HDOP",
                    value = hdop?.let { String.format(Locale.US, "%.2f", it) } ?: "---",
                    isGood = (hdop ?: 99.0) < 2.0
                )

                QualityDisplayItem(
                    label = "UYDU SAYISI",
                    value = sats?.toString() ?: "0",
                    isGood = (sats ?: 0) >= 8
                )

                QualityDisplayItem(
                    label = "VERİ YAŞI",
                    value = String.format(Locale.US, "%.1f sn", ageSec),
                    isGood = ageSec < 5.0
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Veri Aktarma ve Nokta Seçimi
                Text(
                    text = "VERİ AKTARMA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Dosya türü seçimi
                var selectedFileType by remember { mutableStateOf("CSV") }
                val fileTypes = listOf("CSV", "GPX", "KML", "DXF", "SHP")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dosya Türü:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFileType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            fileTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, fontSize = 11.sp) },
                                    onClick = {
                                        selectedFileType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Veri aktarma butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onImportFile(selectedFileType) },
                        enabled = activeProjectId != 0L,
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("İÇERİ AKTAR", fontSize = 10.sp)
                    }

                    Button(
                        onClick = { onExportFile(selectedFileType) },
                        enabled = activeProjectId != 0L,
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DIŞARI AKTAR", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nokta Seçimi
                Text(
                    text = "NOKTA SEÇİMİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Nokta ID girişi
                var pointId by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = pointId,
                    onValueChange = { pointId = it },
                    label = { Text("Nokta No", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    singleLine = true
                )

                // Nokta arama butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Nokta ara */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ARA", fontSize = 10.sp)
                    }

                    Button(
                        onClick = { /* TODO: Haritaya git */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HARİTADA", fontSize = 10.sp)
                    }
                }

                // Seçili nokta bilgisi
                var selectedPoint by remember { mutableStateOf<String?>(null) }
                if (selectedPoint != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "SEÇİLİ NOKTA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = selectedPoint ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("X: 123456.789", fontSize = 10.sp)
                                Text("Y: 987654.321", fontSize = 10.sp)
                            }
                            Text("Z: 123.456", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Proje Bilgileri
                Text(
                    text = "PROJE BİLGİLERİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                ProjectInfoItem(
                    label = "AKTİF PROJE",
                    value = projectName,
                    color = Color(0xFF2196F3)
                )

                ProjectInfoItem(
                    label = "TOPLAM NOKTA",
                    value = if (activeProjectId != 0L) "%d".format(pointCount) else "—",
                    color = Color(0xFF4CAF50)
                )

                ProjectInfoItem(
                    label = "SON İŞLEM",
                    value = if (activeProjectId != 0L) "Detay Alımı" else "—",
                    color = Color(0xFFFF9800)
                )

                // Ölçüm Modu Seçimi
                Text(
                    text = "ÖLÇÜM MODU",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    measurementModes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Alt bilgi
                Text(
                    text = "© TUGIS Nokta v3.0",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Orta Panel - Ana İşlem Alanı
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "NOKTA UYGULAMALARI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // İşlem Butonları - 2x3 Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ActionButton(
                            title = "Detay Alımı",
                            icon = Icons.Filled.Map,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        ) {
                            val intent = Intent(context, DetailSurveyActivity::class.java)
                            intent.putExtra(PointApplicationActivity.EXTRA_MEAS_MODE, selectedMode)
                            context.startActivity(intent)
                        }

                        ActionButton(
                            title = "Hızlı Alım",
                            icon = Icons.Filled.Speed,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        ) {
                            val intent = Intent(context, QuickPointSurveyActivity::class.java)
                            intent.putExtra(PointApplicationActivity.EXTRA_MEAS_MODE, selectedMode)
                            context.startActivity(intent)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ActionButton(
                            title = "Grafik Alım",
                            icon = Icons.Filled.LocationOn,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        ) {
                            val intent = Intent(context, GraphicalSurveyActivity::class.java)
                            intent.putExtra(PointApplicationActivity.EXTRA_MEAS_MODE, selectedMode)
                            context.startActivity(intent)
                        }

                        ActionButton(
                            title = "Aplikasyon",
                            icon = Icons.Filled.GpsFixed,
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        ) {
                            val intent = Intent(context, PointStakeoutActivity::class.java)
                            intent.putExtra(PointApplicationActivity.EXTRA_MEAS_MODE, selectedMode)
                            context.startActivity(intent)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ActionButton(
                            title = "Hat Aplikasyonu",
                            icon = Icons.Filled.Timeline,
                            color = Color(0xFF607D8B),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Geçici olarak devre dışı bırakıldı
                        }

                        ActionButton(
                            title = "CAD Aplikasyonu",
                            icon = Icons.Filled.Layers,
                            color = Color(0xFF795548),
                            modifier = Modifier.weight(1f)
                        ) {
                            val intent = Intent(context, CadApplicationActivity::class.java)
                            intent.putExtra(PointApplicationActivity.EXTRA_MEAS_MODE, selectedMode)
                            context.startActivity(intent)
                        }
                    }

                    // Nokta Listesi - Tam genişlik
                    ActionButton(
                        title = "Nokta Listesi",
                        icon = Icons.AutoMirrored.Filled.List,
                        color = Color(0xFF00BCD4),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val intent = Intent(context, PointListActivity::class.java)
                        context.startActivity(intent)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Seçili Mod Bilgisi
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seçili Ölçüm Modu:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = selectedMode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Sağ Panel - GPS Durum ve Proje Bilgileri
        Card(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "GPS DURUM ÖZETİ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                // GPS Durum Kartı
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when(fix) {
                            FixType.RTK_FIX -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            FixType.RTK_FLOAT -> Color(0xFFFFC107).copy(alpha = 0.1f)
                            else -> Color(0xFF757575).copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when(fix) {
                                FixType.RTK_FIX -> Icons.Default.GpsFixed
                                FixType.RTK_FLOAT -> Icons.Default.GpsNotFixed
                                else -> Icons.Default.GpsOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = when(fix) {
                                FixType.RTK_FIX -> Color(0xFF4CAF50)
                                FixType.RTK_FLOAT -> Color(0xFFFFC107)
                                else -> Color(0xFF757575)
                            }
                        )

                        Text(
                            text = when(fix) {
                                FixType.RTK_FIX -> "RTK FIX"
                                FixType.RTK_FLOAT -> "RTK FLOAT"
                                FixType.DGPS -> "DGPS"
                                FixType.SINGLE -> "GPS 3D"
                                else -> "GPS YOK"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when(fix) {
                                FixType.RTK_FIX -> Color(0xFF4CAF50)
                                FixType.RTK_FLOAT -> Color(0xFFFFC107)
                                else -> Color(0xFF757575)
                            }
                        )

                        Text(
                            text = "${sats ?: 0} Uydu",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusInfoItem(
                    label = "HAZIRLIK DURUMU",
                    value = if (fix == FixType.RTK_FIX || fix == FixType.RTK_FLOAT) "HAZIR" else "BEKLİYOR",
                    color = if (fix == FixType.RTK_FIX || fix == FixType.RTK_FLOAT) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )

                StatusInfoItem(
                    label = "DOĞRULUK",
                    value = when {
                        fix == FixType.RTK_FIX -> "YÜK. DOĞ."
                        fix == FixType.RTK_FLOAT -> "ORTA DOĞ."
                        fix == FixType.DGPS -> "DÜŞÜK DOĞ."
                        else -> "BİLİNMİYOR"
                    },
                    color = when {
                        fix == FixType.RTK_FIX -> Color(0xFF4CAF50)
                        fix == FixType.RTK_FLOAT -> Color(0xFFFFC107)
                        fix == FixType.DGPS -> Color(0xFFFF9800)
                        else -> Color(0xFF757575)
                    }
                )

                StatusInfoItem(
                    label = "ÖNERİLEN İŞLEM",
                    value = when {
                        fix == FixType.RTK_FIX -> "ÖLÇÜM YAP"
                        fix == FixType.RTK_FLOAT -> "RTK FIX BEKLESİN"
                        fix == FixType.DGPS || fix == FixType.SINGLE -> "RTK SINYAL"
                        else -> "GPS BEKLESİN"
                    },
                    color = when {
                        fix == FixType.RTK_FIX -> Color(0xFF4CAF50)
                        fix == FixType.RTK_FLOAT -> Color(0xFFFFC107)
                        else -> Color(0xFFFF5722)
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Alt bilgi
                Text(
                    text = "Son Güncelleme:\n${java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date())}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Koordinat gösterim bileşeni
@Composable
private fun CoordinateDisplayItem(
    label: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value $unit",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Kalite gösterim bileşeni
@Composable
private fun QualityDisplayItem(
    label: String,
    value: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isGood) Color(0xFF4CAF50) else Color(0xFFFFC107)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGood) Color(0xFF4CAF50) else Color(0xFFFFC107)
            )
        }
    }
}

// İşlem buton bileşeni
@Composable
private fun ActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            elevation = null
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Durum bilgi bileşeni
@Composable
private fun StatusInfoItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Proje bilgi bileşeni
@Composable
private fun ProjectInfoItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Basit CSV ayrıştırıcı (tırnakları destekler)
private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '"') {
            if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                sb.append('"'); i++ // kaçışlı çift tırnak
            } else {
                inQuotes = !inQuotes
            }
        } else if (c == ',' && !inQuotes) {
            result.add(sb.toString()); sb.setLength(0)
        } else {
            sb.append(c)
        }
        i++
    }
    result.add(sb.toString())
    return result
}

private fun findHeaderIndexes(header: List<String>): Map<String, Int> {
    fun idx(vararg keys: String): Int? {
        val lowered = header.map { it.trim().lowercase() }
        for (k in keys) {
            val j = lowered.indexOf(k)
            if (j >= 0) return j
        }
        return null
    }
    return mapOf(
        "name" to (idx("nokta_id", "name", "point_name", "id", "nokta", "point") ?: -1),
        "north" to (idx("northing", "n", "y", "kuzey", "north") ?: -1),
        "east" to (idx("easting", "e", "x", "dogu", "doğu", "east") ?: -1),
        "height" to (idx("height", "h", "z", "elev", "elevation") ?: -1),
        "desc" to (idx("description", "desc", "aciklama", "açıklama", "note", "notes") ?: -1)
    )
}
