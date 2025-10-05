package com.example.tugis3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SDCardReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SDCardReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MEDIA_MOUNTED -> {
                Log.d(TAG, "SD Card mounted")
                // SD card mounted logic
            }
            Intent.ACTION_MEDIA_EJECT -> {
                Log.d(TAG, "SD Card ejected")
                // SD card ejected logic
            }
        }
    }
}