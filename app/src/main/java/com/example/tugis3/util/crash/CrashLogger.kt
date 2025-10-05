package com.example.tugis3.util.crash

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun logException(context: Context, throwable: Throwable) {
        try {
            val dir = File(context.filesDir, LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, LOG_FILE)
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
}
