package com.example.tugis3

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.tugis3.util.crash.CrashLogger

@HiltAndroidApp
class Tugis3Application : Application() {

    companion object {
        @Volatile lateinit var appContext: Application
            private set
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
    }
}