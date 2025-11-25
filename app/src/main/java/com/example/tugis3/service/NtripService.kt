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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.Socket
import java.util.Base64

/**
 * NTRIP (Networked Transport of RTCM via Internet Protocol) Client Service
 * CORS/RTK düzeltme verilerini alır ve GNSS motoruna iletir
 */
class NtripService : Service() {

    companion object {
        private const val TAG = "NtripService"
        private const val NOTIF_ID = 4102
        const val ACTION_START = "com.example.tugis3.action.NTRIP_START"
        const val ACTION_STOP = "com.example.tugis3.action.NTRIP_STOP"

        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_MOUNTPOINT = "mountpoint"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"

        fun start(context: Context, host: String, port: Int, mountPoint: String,
                  username: String = "", password: String = "") {
            val intent = Intent(context, NtripService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_MOUNTPOINT, mountPoint)
                putExtra(EXTRA_USERNAME, username)
                putExtra(EXTRA_PASSWORD, password)
            }
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, NtripService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    private val started = java.util.concurrent.atomic.AtomicBoolean(false)
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var ntripSocket: Socket? = null
    private var connectionJob: Job? = null
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    private val _bytesReceived = MutableStateFlow(0L)

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val mountPoint: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NTRIP Service created")
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stop action alındı")
                disconnect()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val host = intent.getStringExtra(EXTRA_HOST) ?: return START_NOT_STICKY
                val port = intent.getIntExtra(EXTRA_PORT, 2101)
                val mountPoint = intent.getStringExtra(EXTRA_MOUNTPOINT) ?: return START_NOT_STICKY
                val username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
                val password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""

                startForegroundService(mountPoint)
                connectToNtripCaster(host, port, mountPoint, username, password)
            }
        }
        return START_STICKY
    }

    private fun startForegroundService(mountPoint: String) {
        if (!started.compareAndSet(false, true)) return

        val notification = buildNotification("NTRIP bağlanıyor: $mountPoint")
        try {
            startForeground(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Foreground başlatılamadı", e)
        }
    }

    private fun connectToNtripCaster(host: String, port: Int, mountPoint: String,
                                     username: String, password: String) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            _connectionState.value = ConnectionState.Connecting

            try {
                disconnect()

                Log.d(TAG, "NTRIP bağlantısı kuruluyor: $host:$port/$mountPoint")
                ntripSocket = Socket(host, port)

                val socket = ntripSocket ?: throw IOException("Socket oluşturulamadı")
                val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII)
                val reader = BufferedInputStream(socket.getInputStream())

                // NTRIP 1.0 Request
                val auth = if (username.isNotEmpty()) {
                    val credentials = "$username:$password"
                    val encoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Base64.getEncoder().encodeToString(credentials.toByteArray())
                    } else {
                        android.util.Base64.encodeToString(credentials.toByteArray(),
                                                          android.util.Base64.NO_WRAP)
                    }
                    "Authorization: Basic $encoded\r\n"
                } else ""

                val request = "GET /$mountPoint HTTP/1.0\r\n" +
                        "User-Agent: NTRIP Tugis3Client/1.0\r\n" +
                        auth +
                        "Accept: */*\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"

                writer.write(request)
                writer.flush()

                // Response header'ı oku
                val response = readHttpResponse(reader)
                if (!response.startsWith("ICY 200 OK") && !response.contains("200 OK")) {
                    throw IOException("NTRIP sunucu hatası: $response")
                }

                Log.i(TAG, "✓ NTRIP bağlantısı başarılı: $mountPoint")
                _connectionState.value = ConnectionState.Connected(mountPoint)
                updateNotification("NTRIP aktif: $mountPoint")

                // RTCM verilerini sürekli oku
                readRtcmData(reader)

            } catch (e: Exception) {
                Log.e(TAG, "NTRIP bağlantı hatası", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Bilinmeyen hata")
                updateNotification("NTRIP Hata: ${e.message}")

                // Yeniden bağlanmayı dene
                scheduleReconnect(host, port, mountPoint, username, password)
            }
        }
    }

    private fun readHttpResponse(input: BufferedInputStream): String {
        val headerBytes = ByteArrayOutputStream()
        var prev = 0
        var current: Int

        while (true) {
            current = input.read()
            if (current == -1) break
            headerBytes.write(current)

            // \r\n\r\n (HTTP header sonu)
            if (prev == '\r'.code && current == '\n'.code) {
                val next1 = input.read()
                val next2 = input.read()
                if (next1 == '\r'.code && next2 == '\n'.code) {
                    break
                }
                headerBytes.write(next1)
                headerBytes.write(next2)
                prev = next2
            } else {
                prev = current
            }
        }

        return headerBytes.toString("US-ASCII")
    }

    private suspend fun readRtcmData(input: BufferedInputStream) = coroutineScope {
        val buffer = ByteArray(4096)
        var totalBytes = 0L

        while (isActive && ntripSocket?.isConnected == true) {
            try {
                val available = input.available()
                if (available > 0) {
                    val count = input.read(buffer, 0, minOf(available, buffer.size))
                    if (count > 0) {
                        totalBytes += count
                        _bytesReceived.value = totalBytes

                        // RTCM verisini GNSS motoruna ilet (gelecekte implement edilecek)
                        // GnssEngine.injectRtcmCorrection(buffer, count)

                        if (totalBytes % 10000 < count) {
                            Log.d(TAG, "RTCM alındı: ${totalBytes / 1024} KB")
                        }
                    }
                } else {
                    delay(100)
                }
            } catch (e: IOException) {
                Log.e(TAG, "RTCM okuma hatası", e)
                break
            }
        }
    }

    private fun scheduleReconnect(host: String, port: Int, mountPoint: String,
                                  username: String, password: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000) // 5 saniye bekle
            if (started.get()) {
                Log.d(TAG, "Yeniden bağlanma denemesi...")
                connectToNtripCaster(host, port, mountPoint, username, password)
            }
        }
    }

    private fun disconnect() {
        connectionJob?.cancel()
        reconnectJob?.cancel()

        ntripSocket?.let { socket ->
            try {
                if (!socket.isClosed) {
                    socket.close()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket kapatma hatası", e)
            }
        }
        ntripSocket = null
        _connectionState.value = ConnectionState.Disconnected
        _bytesReceived.value = 0
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            val existing = nm.getNotificationChannel("ntrip_channel")
            if (existing == null) {
                val channel = NotificationChannel(
                    "ntrip_channel",
                    "NTRIP Bağlantı",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "CORS/RTK düzeltme verisi durumu"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "ntrip_channel")
            .setContentTitle("NTRIP Client")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NTRIP Service destroyed")
        started.set(false)
        disconnect()
        scope.cancel()
    }
}
