package com.example.tugis3.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.tugis3.data.model.CorsStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.Socket
import java.util.*

class NtripService : Service() {
    
    private val binder = NtripServiceBinder()
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    private var currentStation: CorsStation? = null
    
    inner class NtripServiceBinder : Binder() {
        fun getService(): NtripService = this@NtripService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    fun connectToCorsStation(station: CorsStation): Boolean {
        return try {
            currentStation = station
            socket = Socket(station.ntripUrl, station.port)
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()
            
            // Send NTRIP authentication
            sendNtripRequest(station)
            
            isConnected = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
            false
        }
    }
    
    private fun sendNtripRequest(station: CorsStation) {
        val request = buildString {
            append("GET /${station.mountPoint} HTTP/1.1\r\n")
            append("User-Agent: CORS-GNSS-Android/1.0\r\n")
            append("Accept: */*\r\n")
            append("Connection: close\r\n")
            if (station.username.isNotEmpty() && station.password.isNotEmpty()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${station.username}:${station.password}".toByteArray()
                )
                append("Authorization: Basic $credentials\r\n")
            }
            append("\r\n")
        }
        
        outputStream?.write(request.toByteArray())
        outputStream?.flush()
    }
    
    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isConnected = false
            currentStation = null
        }
    }
    
    fun isConnected(): Boolean = isConnected
    
    fun getCurrentStation(): CorsStation? = currentStation
    
    fun startReceivingCorrections(callback: (ByteArray) -> Unit) {
        if (!isConnected) return
        
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            try {
                while (isConnected && inputStream != null) {
                    val bytesRead = inputStream?.read(buffer) ?: 0
                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        callback(data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isConnected = false
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}
