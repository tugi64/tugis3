package com.example.tugis3

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.tugis3.util.crash.CrashLogger
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

@HiltAndroidApp
class Tugis3Application : Application() {

    companion object {
        @Volatile lateinit var appContext: Application
            private set
        const val GNSS_CHANNEL_ID = "gnss_tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        // Global crash yakalama
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                CrashLogger.logException(applicationContext, e)
            } catch (_: Exception) {}
            // Önce bizim log, sonra eski handler (eğer null değilse)
            previous?.uncaughtException(t, e)
        }
        // Uygulama başlangıç ayarları
        // Repository'ler Hilt tarafından otomatik olarak yönetilecek
        createGnssChannel()
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