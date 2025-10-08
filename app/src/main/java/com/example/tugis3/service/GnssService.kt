package com.example.tugis3.service

import android.app.Notification
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GnssService : Service() {

    companion object {
        private const val TAG = "GnssService"
        private const val NOTIF_ID = 4101
        const val ACTION_START = "com.example.tugis3.action.GNSS_START"
        const val ACTION_STOP = "com.example.tugis3.action.GNSS_STOP"
        private const val INACTIVITY_SECONDS = 180L
    }

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf(); return START_NOT_STICKY
            }
            else -> startForegroundIfNeeded()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "GNSS Service created")
    }

    private fun startForegroundIfNeeded() {
        if (started) return
        started = true
        startForeground(NOTIF_ID, buildNotification("GNSS izleme başlıyor"))
        scope.launch {
            GnssPositionRepository.position.collectLatest { pos ->
                val fixName = pos.fixType?.displayName ?: "Fix Yok"
                val latStr = pos.lat?.let { String.format("%.6f", it) } ?: "--"
                val lonStr = pos.lon?.let { String.format("%.6f", it) } ?: "--"
                val age = (System.currentTimeMillis() - pos.timestamp) / 1000
                val content = "$fixName | $latStr,$lonStr | ${age}s"
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildNotification(content))
                if (age > INACTIVITY_SECONDS) {
                    Log.d(TAG, "İnaktif > $INACTIVITY_SECONDS sn, servis kapanıyor")
                    stopSelf()
                }
            }
        }
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, MapActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
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
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "GNSS Service destroyed")
    }
}