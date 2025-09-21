package com.example.tugis3.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.tugis3.MainActivity
import com.example.tugis3.R
import com.example.tugis3.data.model.GnssData
import com.example.tugis3.repository.GnssRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class GnssService : Service(), LocationListener {
    
    private val binder = GnssServiceBinder()
    private lateinit var locationManager: LocationManager
    private lateinit var gnssRepository: GnssRepository
    private var isLocationUpdatesActive = false
    
    private val CHANNEL_ID = "GNSS_SERVICE_CHANNEL"
    private val NOTIFICATION_ID = 1
    
    inner class GnssServiceBinder : Binder() {
        fun getService(): GnssService = this@GnssService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GNSS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "CORS GNSS Location Service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CORS GNSS Service")
            .setContentText("GNSS location tracking is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    fun startLocationUpdates() {
        if (!isLocationUpdatesActive) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // 1 second
                    1.0f, // 1 meter
                    this,
                    Looper.getMainLooper()
                )
                isLocationUpdatesActive = true
            } catch (e: SecurityException) {
                // Handle permission denied
            }
        }
    }
    
    fun stopLocationUpdates() {
        if (isLocationUpdatesActive) {
            locationManager.removeUpdates(this)
            isLocationUpdatesActive = false
        }
    }
    
    override fun onLocationChanged(location: Location) {
        // Process location data
        val gnssData = GnssData(
            id = UUID.randomUUID().toString(),
            projectId = "default", // Get from active project
            pointName = "AUTO_${System.currentTimeMillis()}",
            latitude = location.latitude,
            longitude = location.longitude,
            elevation = location.altitude,
            accuracy = location.accuracy,
            fixType = "GPS",
            satelliteCount = 0, // Get from GNSS status
            hdop = 0f,
            vdop = 0f,
            pdop = 0f,
            timestamp = System.currentTimeMillis(),
            isCorsCorrected = false
        )
        
        // Save to database
        CoroutineScope(Dispatchers.IO).launch {
            gnssRepository.insertData(gnssData)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
