package com.example.tugis3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tugis3.service.GnssService

class AutoStartUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("AutoStartUpReceiver", "Boot completed, starting GNSS service")
            
            // Start GNSS service on boot
            val serviceIntent = Intent(context, GnssService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
