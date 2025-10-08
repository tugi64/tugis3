package com.example.tugis3.gnss

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** NMEA satırlarını hafızada (dairesel buffer) tutar ve dosyaya export eder. */
object NmeaLogRepository {
    private val mutex = Mutex()
    private val maxLines = 5000
    private val lines: ArrayDeque<String> = ArrayDeque()

    suspend fun add(line: String) {
        if (line.isBlank()) return
        mutex.withLock {
            if (lines.size >= maxLines) lines.removeFirst()
            lines.addLast(line)
        }
    }

    suspend fun snapshot(): List<String> = mutex.withLock { lines.toList() }

    fun buildFilename(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "nmea_${sdf.format(Date())}.log"
    }

    suspend fun exportToAppExternal(context: android.content.Context): Result<java.io.File> = kotlin.runCatching {
        val data = snapshot()
        val dir = context.getExternalFilesDir("nmea") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val file = java.io.File(dir, buildFilename())
        file.bufferedWriter().use { out ->
            data.forEach { out.appendLine(it) }
        }
        file
    }
}

