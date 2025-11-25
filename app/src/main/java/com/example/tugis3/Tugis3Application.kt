package com.example.tugis3

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.tugis3.util.crash.CrashLogger
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Intent
import android.util.Log
import android.content.pm.ApplicationInfo

@HiltAndroidApp
class Tugis3Application : Application() {

    companion object {
        @Volatile lateinit var appContext: Application
            private set
        const val GNSS_CHANNEL_ID = "gnss_tracking_channel"
        private const val TAG = "Tugis3App"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // DEBUG: Ana thread bloklarını yakalamak için StrictMode
        if (isDebug) {
            try {
                android.os.StrictMode.setThreadPolicy(
                    android.os.StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
                android.os.StrictMode.setVmPolicy(
                    android.os.StrictMode.VmPolicy.Builder()
                        .detectLeakedClosableObjects()
                        .detectLeakedSqlLiteObjects()
                        .penaltyLog()
                        .build()
                )
            } catch (_: Exception) { }
        }

        // Global crash yakalama - debug modunda process'in anında ölmesini engelleyen geliştirilmiş sürüm
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { CrashLogger.logException(applicationContext, e) } catch (_: Exception) {}
            if (isDebug) {
                Log.e(TAG, "Yakalanmamış hata (DEBUG sürümü, process öldürülmüyor):", e)
                runCatching {
                    startActivity(Intent(applicationContext, com.example.tugis3.util.crash.CrashLogActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } else {
                previous?.uncaughtException(t, e)
            }
        }
        createGnssChannelSafely(isDebug)
    }

    private fun createGnssChannelSafely(isDebug: Boolean) {
        runCatching { createGnssChannel() }.onFailure { err ->
            if (isDebug) Log.w(TAG, "Notification channel oluşturulamadı: ${err.message}")
        }
    }

    private fun createGnssChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(GNSS_CHANNEL_ID, "GNSS Takip", NotificationManager.IMPORTANCE_LOW).apply {
                description = "GNSS konum izleme durumu"
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }
}