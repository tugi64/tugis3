package com.example.tugis3.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class GnssService : Service() {

    companion object {
        private const val TAG = "GnssService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "GNSS Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "GNSS Service started")
        // GNSS tracking logic will be implemented here
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "GNSS Service destroyed")
    }
}