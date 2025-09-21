package com.example.tugis3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SDCardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MEDIA_MOUNTED -> {
                Log.d("SDCardReceiver", "SD Card mounted")
                // Handle SD card mounted
            }
            Intent.ACTION_MEDIA_EJECT -> {
                Log.d("SDCardReceiver", "SD Card ejected")
                // Handle SD card ejected
            }
        }
    }
}
