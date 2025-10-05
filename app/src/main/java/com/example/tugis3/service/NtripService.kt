package com.example.tugis3.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class NtripService : Service() {

    companion object {
        private const val TAG = "NtripService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NTRIP Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NTRIP Service started")
        // NTRIP client logic will be implemented here
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NTRIP Service destroyed")
    }
}