package com.example.tugis3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AutoStartUpReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoStartUpReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting services")
            // Auto start services logic
        }
    }
}