package com.example.tugis3.gnss

import android.content.Context
import com.example.tugis3.gnss.model.FixType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Basit hafif track (iz) kaydı yöneticisi. */
object TrackRepository {
    data class TrackPoint(val lat: Double, val lon: Double, val time: Long, val fixType: FixType?)
    private const val BASE_MIN_DISTANCE_SMALL = 0.05
    private const val LARGE_TRACK_MIN_DISTANCE = 0.2

    private val mutex = Mutex()
    private val _points = MutableStateFlow<List<TrackPoint>>(emptyList())
    val points: StateFlow<List<TrackPoint>> = _points.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance.asStateFlow()

    private val _rtkFixDistance = MutableStateFlow(0.0)
    val rtkFixDistance: StateFlow<Double> = _rtkFixDistance.asStateFlow()

    private val _rtkFloatDistance = MutableStateFlow(0.0)
    val rtkFloatDistance: StateFlow<Double> = _rtkFloatDistance.asStateFlow()

    private val _exportSuggestion = MutableStateFlow(false)
    val exportSuggestion: StateFlow<Boolean> = _exportSuggestion.asStateFlow()

    private val _startTime = MutableStateFlow<Long?>(null)
    val startTime: StateFlow<Long?> = _startTime.asStateFlow()
    private val _endTime = MutableStateFlow<Long?>(null)
    val endTime: StateFlow<Long?> = _endTime.asStateFlow()

    private val _durationMillis = MutableStateFlow<Long?>(null)
    val durationMillis: StateFlow<Long?> = _durationMillis.asStateFlow()

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    private fun iso(ms: Long?): String = ms?.let { isoFmt.format(Date(it)) } ?: ""

    private fun updateDuration() {
        val st = _startTime.value
        val et = _endTime.value ?: _points.value.lastOrNull()?.time
        val dur = if (st != null && et != null && et >= st) et - st else null
        _durationMillis.value = dur
    }

    fun consumeExportSuggestion() { _exportSuggestion.value = false }

    fun setRecording(enabled: Boolean) {
        val prev = _recording.value
        _recording.value = enabled
        if (!prev && enabled) {
            // yeni kayıt
            _startTime.value = null
            _endTime.value = null
            _durationMillis.value = null
        }
        if (prev && !enabled && _points.value.isNotEmpty()) {
            _endTime.value = _points.value.last().time
            updateDuration()
            _exportSuggestion.value = true
        }
    }

    suspend fun addPoint(lat: Double, lon: Double, fix: FixType?, time: Long = System.currentTimeMillis()) {
        if (!_recording.value) return
        mutex.withLock {
            val current = _points.value
            val last = current.lastOrNull()
            val minDist = if (current.size >= 100_000) LARGE_TRACK_MIN_DISTANCE else BASE_MIN_DISTANCE_SMALL
            var distInc = 0.0
            if (last != null) {
                val d = haversineMeters(last.lat, last.lon, lat, lon)
                if (d < minDist) return
                distInc = d
            }
            _points.value = current + TrackPoint(lat, lon, time, fix)
            if (_startTime.value == null) _startTime.value = time
            _totalDistance.value = _totalDistance.value + distInc
            if (fix == FixType.RTK_FIX) _rtkFixDistance.value = _rtkFixDistance.value + distInc
            if (fix == FixType.RTK_FLOAT) _rtkFloatDistance.value = _rtkFloatDistance.value + distInc
            updateDuration()
        }
    }

    suspend fun clear() { mutex.withLock { _points.value = emptyList(); _totalDistance.value = 0.0; _rtkFixDistance.value = 0.0; _rtkFloatDistance.value = 0.0; _exportSuggestion.value = false; _startTime.value = null; _endTime.value = null; _durationMillis.value = null } }

    private fun ts(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun dir(context: Context): File = (context.getExternalFilesDir("track") ?: context.filesDir).apply { if (!exists()) mkdirs() }

    fun minDistanceForSize(size: Int): Double = if (size >= 100_000) 0.2 else 0.05

    fun exportGpx(context: Context, simplifyScale: Double? = null): Result<File> = runCatching {
        val snapshot = _points.value
        require(snapshot.isNotEmpty()) { "Kayıt yok" }
        val file = File(dir(context), "track_${ts()}.gpx")
        val total = _totalDistance.value
        val rtk = _rtkFixDistance.value
        val floatDist = _rtkFloatDistance.value
        val dur = (_durationMillis.value ?: 0L)
        val st = _startTime.value
        val et = _endTime.value ?: snapshot.lastOrNull()?.time
        val durSec = if (dur > 0) dur / 1000.0 else 0.0
        val avgMps = if (durSec > 0) total / durSec else 0.0
        val avgKmh = avgMps * 3.6
        val stIso = iso(st)
        val etIso = iso(et)
        val minDistUsed = minDistanceForSize(snapshot.size)
        val scaleVal = simplifyScale ?: 1.0
        file.bufferedWriter().use { out ->
            out.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            out.appendLine("<gpx version=\"1.1\" creator=\"tugis3\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
            out.appendLine("  <metadata>")
            out.appendLine("    <extensions>")
            out.appendLine("      <totalDistanceMeters>${total}</totalDistanceMeters>")
            out.appendLine("      <rtkFixDistanceMeters>${rtk}</rtkFixDistanceMeters>")
            out.appendLine("      <rtkFloatDistanceMeters>${floatDist}</rtkFloatDistanceMeters>")
            out.appendLine("      <pointCount>${snapshot.size}</pointCount>")
            out.appendLine("      <durationMillis>${dur}</durationMillis>")
            out.appendLine("      <startTime>${stIso}</startTime>")
            out.appendLine("      <endTime>${etIso}</endTime>")
            out.appendLine("      <averageSpeedMps>${avgMps}</averageSpeedMps>")
            out.appendLine("      <averageSpeedKmh>${avgKmh}</averageSpeedKmh>")
            out.appendLine("      <minDistanceMeters>${minDistUsed}</minDistanceMeters>")
            out.appendLine("      <simplifyScale>${scaleVal}</simplifyScale>")
            out.appendLine("    </extensions>")
            out.appendLine("  </metadata>")
            out.appendLine("  <trk><name>Track ${ts()}</name><trkseg>")
            snapshot.forEach { p ->
                val isoTime = iso(p.time)
                out.appendLine("    <trkpt lat=\"${p.lat}\" lon=\"${p.lon}\"><time>${isoTime}</time><extensions><fixType>${p.fixType?.name ?: ""}</fixType></extensions></trkpt>")
            }
            out.appendLine("  </trkseg></trk>")
            out.appendLine("</gpx>")
        }
        file
    }

    fun exportCsv(context: Context): Result<File> = runCatching {
        val snapshot = _points.value
        require(snapshot.isNotEmpty()) { "Kayıt yok" }
        val file = File(dir(context), "track_${ts()}.csv")
        file.bufferedWriter().use { out ->
            out.appendLine("time,lat,lon,fixType")
            snapshot.forEach { p ->
                out.appendLine("${p.time},${p.lat},${p.lon},${p.fixType?.name ?: ""}")
            }
        }
        file
    }

    fun exportBothZip(context: Context, simplifyScale: Double? = null): Result<File> = runCatching {
        val gpx = exportGpx(context, simplifyScale).getOrThrow()
        val csv = exportCsv(context).getOrThrow()
        val zipFile = File(dir(context), "track_${ts()}.zip")
        val total = _totalDistance.value
        val rtk = _rtkFixDistance.value
        val floatDist = _rtkFloatDistance.value
        val dur = (_durationMillis.value ?: 0L)
        val st = _startTime.value
        val et = _endTime.value ?: _points.value.lastOrNull()?.time
        val durSec = if (dur > 0) dur / 1000.0 else 0.0
        val avgMps = if (durSec > 0) total / durSec else 0.0
        val avgKmh = avgMps * 3.6
        val stIso = iso(st)
        val etIso = iso(et)
        val minDistUsed = minDistanceForSize(_points.value.size)
        val scaleVal = simplifyScale ?: 1.0
        fun fmtDur(ms: Long): String {
            val s = ms / 1000
            val h = s / 3600
            val m = (s % 3600) / 60
            val sec = s % 60
            return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec) else String.format(Locale.US, "%02d:%02d", m, sec)
        }
        val readmeContent = buildString {
            appendLine("Track Özeti")
            appendLine("============")
            appendLine("Toplam Mesafe (m): ${"%.3f".format(Locale.US, total)}")
            appendLine("RTK FIX Mesafe (m): ${"%.3f".format(Locale.US, rtk)}")
            appendLine("RTK FLOAT Mesafe (m): ${"%.3f".format(Locale.US, floatDist)}")
            appendLine("Nokta Sayısı: ${_points.value.size}")
            appendLine("Süre (ms): ${dur}")
            appendLine("Süre (formatlı): ${fmtDur(dur)}")
            appendLine("Başlangıç: ${stIso}")
            appendLine("Bitiş: ${etIso}")
            appendLine("Ortalama Hız (m/s): ${"%.3f".format(Locale.US, avgMps)}")
            appendLine("Ortalama Hız (km/h): ${"%.3f".format(Locale.US, avgKmh)}")
            appendLine("Min Nokta Aralığı (m): ${minDistUsed}")
            appendLine("Simplify Ölçeği: ${scaleVal}")
            val rtkPerc = if (total>0) (rtk/total*100.0) else 0.0
            val floatPerc = if (total>0) (floatDist/total*100.0) else 0.0
            appendLine("RTK FIX Oran (%): ${"%.2f".format(Locale.US, rtkPerc)}")
            appendLine("RTK FLOAT Oran (%): ${"%.2f".format(Locale.US, floatPerc)}")
            appendLine()
            appendLine("Dosyalar:")
            appendLine("- ${gpx.name}")
            appendLine("- ${csv.name}")
            appendLine()
            appendLine("Oluşturulma: ${iso(System.currentTimeMillis())}")
        }
        java.util.zip.ZipOutputStream(zipFile.outputStream()).use { zos ->
            listOf(gpx to gpx.name, csv to csv.name).forEach { (f, name) ->
                val entry = java.util.zip.ZipEntry(name)
                zos.putNextEntry(entry)
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
            val readmeEntry = java.util.zip.ZipEntry("README.txt")
            zos.putNextEntry(readmeEntry)
            zos.write(readmeContent.toByteArray())
            zos.closeEntry()
        }
        zipFile
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}
