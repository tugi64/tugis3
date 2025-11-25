package com.example.tugis3.util.crash

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ek veri sınıfları */
data class CrashSection(val timestamp: String, val lines: List<String>)
data class CrashFileInfo(val exists: Boolean, val sizeBytes: Long, val lastModified: Long?)

/**
 * Global uncaught exception logger. Writes stack traces to a file under internal storage.
 */
object CrashLogger {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "crash.log"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private const val PREFS = "crash_prefs"
    private const val KEY_PENDING = "has_pending"
    private const val KEY_LAST_TIME = "last_time"

    private fun logFile(context: Context): File = File(File(context.filesDir, LOG_DIR), LOG_FILE)

    fun logException(context: Context, throwable: Throwable) {
        try {
            val dir = File(context.filesDir, LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            val file = logFile(context)
            val timestamp = dateFormat.format(Date())
            FileWriter(file, true).use { fw ->
                PrintWriter(fw).use { pw ->
                    pw.println("===== Crash at $timestamp =====")
                    throwable.printStackTrace(pw)
                    pw.println()
                }
            }
            markPendingCrash(context, timestamp)
        } catch (_: Exception) {
            // ignore logging failures
        }
    }

    private fun markPendingCrash(context: Context, time: String) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_PENDING, true).putString(KEY_LAST_TIME, time).apply()
    }

    fun consumePendingCrash(context: Context): String? {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val has = sp.getBoolean(KEY_PENDING, false)
        val ts = sp.getString(KEY_LAST_TIME, null)
        if (has) {
            sp.edit().putBoolean(KEY_PENDING, false).apply()
            return ts
        }
        return null
    }

    // --- Yeni yardımcı fonksiyonlar ---

    /** Ham satırları (opsiyonel limit ile) döner. */
    fun readLines(context: Context, tailLimit: Int = 1000): List<String> = runCatching {
        val f = logFile(context)
        if (!f.exists()) return emptyList()
        // Büyük dosyalarda performans için tümünü okuyup tail alıyoruz (bu boyut için yeterli)
        val all = f.readLines()
        if (all.size <= tailLimit) all else all.takeLast(tailLimit)
    }.getOrElse { emptyList() }

    /** Dosyayı temizler (yoksa no-op). */
    fun clear(context: Context) = runCatching { logFile(context).takeIf { it.exists() }?.writeText("") }

    /** Dosya bilgisini döner. */
    fun fileInfo(context: Context): CrashFileInfo {
        val f = logFile(context)
        return if (f.exists()) CrashFileInfo(true, f.length(), f.lastModified()) else CrashFileInfo(false, 0, null)
    }

    /**
     * Log dosyasını "===== Crash at ... =====" başlıklarına göre bölümlere ayırır.
     * En yeni bölümler sonda olduğundan tailLimit satır okuması sonrası ters çevirebiliriz.
     */
    fun readSections(context: Context, maxSections: Int = 20, tailLimit: Int = 4000): List<CrashSection> {
        val lines = readLines(context, tailLimit)
        if (lines.isEmpty()) return emptyList()
        val sections = mutableListOf<CrashSection>()
        var currentTs: String? = null
        val currentLines = mutableListOf<String>()
        fun flush() {
            val ts = currentTs ?: return
            if (currentLines.isNotEmpty()) {
                sections.add(CrashSection(ts, currentLines.toList()))
            }
            currentLines.clear()
        }
        val headerPrefix = "===== Crash at "
        for (l in lines) {
            if (l.startsWith(headerPrefix)) {
                flush()
                currentTs = l.removePrefix(headerPrefix).trim().removeSuffix(" =====")
            } else {
                currentLines.add(l)
            }
        }
        flush()
        // Son eklenen en yeni olabilir; kronolojik sırayı korumak için (varsayım) sıralamaya dokunmuyoruz.
        return if (sections.size <= maxSections) sections else sections.takeLast(maxSections)
    }
}
