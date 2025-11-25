package com.example.tugis3.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tugis3.R
import com.example.tugis3.Tugis3Application
import com.example.tugis3.gnss.GnssPositionRepository
import com.example.tugis3.ui.map.MapActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class GnssService : Service() {

    companion object {
        private const val TAG = "GnssService"
        private const val NOTIF_ID = 4101
        const val ACTION_START = "com.example.tugis3.action.GNSS_START"
        const val ACTION_STOP = "com.example.tugis3.action.GNSS_STOP"
        private const val INACTIVITY_SECONDS = 180L

        /** Kolay başlatma helper'ları */
        fun start(context: Context) = runCatching {
            val i = Intent(context, GnssService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        fun stop(context: Context) = runCatching {
            context.startService(Intent(context, GnssService::class.java).setAction(ACTION_STOP))
        }
    }

    private val started = AtomicBoolean(false)
    private val serviceJob: Job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "GNSS Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stop action alındı")
                safeStop()
                return START_NOT_STICKY
            }
            ACTION_START, null -> startForegroundIfNeeded()
        }
        return START_STICKY
    }

    private fun startForegroundIfNeeded() {
        if (!started.compareAndSet(false, true)) {
            Log.d(TAG, "Zaten başlatılmış; tekrar başlatma yoksayıldı")
            return
        }
        ensureChannel()
        val initial = buildNotification("GNSS izleme başlıyor")
        try {
            startForeground(NOTIF_ID, initial)
        } catch (se: SecurityException) {
            Log.e(TAG, "Foreground başlatılamadı (permission?)", se)
            // Yine de normal servis olarak devam; kullanıcıya görsel yok.
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground hata", t)
        }
        launchCollector()
    }

    private fun launchCollector() {
        scope.launch {
            Log.d(TAG, "Konum akışı collect başlıyor")
            GnssPositionRepository.position
                .catch { e ->
                    Log.e(TAG, "Position flow hata", e)
                }
                .collectLatest { pos ->
                    try {
                        val fixName = pos.fixType?.displayName ?: "Fix Yok"
                        val latStr = pos.lat?.let { String.format("%.6f", it) } ?: "--"
                        val lonStr = pos.lon?.let { String.format("%.6f", it) } ?: "--"
                        val age = (System.currentTimeMillis() - pos.timestamp) / 1000
                        val content = "$fixName | $latStr,$lonStr | ${age}s"
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIF_ID, buildNotification(content))
                        if (age > INACTIVITY_SECONDS) {
                            Log.d(TAG, "İnaktif > $INACTIVITY_SECONDS sn, servis kapanıyor")
                            safeStop()
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Notification güncelleme hata", t)
                    }
                }
        }
    }

    private fun safeStop() {
        runCatching { stopForeground(STOP_FOREGROUND_DETACH) }
        runCatching { stopSelf() }
        started.set(false)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            val id = Tugis3Application.GNSS_CHANNEL_ID
            val existing = nm.getNotificationChannel(id)
            if (existing == null) {
                val ch = NotificationChannel(id, "GNSS Takip", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "GNSS konum izleme durumu"
                    setShowBadge(false)
                }
                runCatching { nm.createNotificationChannel(ch) }
            }
        }
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, MapActivity::class.java)
        val piFlags = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, 0, openIntent, piFlags)
        return NotificationCompat.Builder(this, Tugis3Application.GNSS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("GNSS Takip")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "GNSS Service destroyed")
        scope.cancel()
        started.set(false)
        super.onDestroy()
    }
}