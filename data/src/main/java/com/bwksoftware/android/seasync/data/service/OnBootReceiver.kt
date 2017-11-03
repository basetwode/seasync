package com.bwksoftware.android.seasync.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("OnBootReceiver", "System booted")
        val serviceIntent = Intent(context, FileObserverService::class.java)
        val b = context!!.startService(serviceIntent)
        Log.d("OnBootReceiver", b.toString())
    }
}