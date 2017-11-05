package com.bwksoftware.android.seasync.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class OnBootReceiver : BroadcastReceiver() {

    companion object {
        val ACTION_START_FILE_OBSERVER = "com.bwksoftware.android.seasync.data.service.fileobserver.ACTION_START_FILE_OBSERVER"
        val ACTION_RESTART_CACHE_OBSERVER = "com.bwksoftware.android.seasync.data.service.fileobserver.ACTION_RESTART_CACHE_OBSERVER"
        val ACTION_RESTART_FILE_OBSERVER = "com.bwksoftware.android.seasync.data.service.fileobserver.ACTION_RESTART_FILE_OBSERVER"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("OnBootReceiver", "System booted")
        val serviceIntent = Intent(context, FileObserverService::class.java)
        val b = context!!.startService(serviceIntent)
        Log.d("OnBootReceiver", b.toString())
    }
}