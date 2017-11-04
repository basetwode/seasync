package com.bwksoftware.android.seasync.data.service

import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.bwksoftware.android.seasync.data.R
import com.bwksoftware.android.seasync.data.net.RestApiImpl
import com.bwksoftware.android.seasync.data.sync.RecursiveFileObserver
import java.io.File
import java.util.*


class FileObserverService : Service() {

    val binder = MonitorBinder()

    lateinit var filesObserver: LinkedList<RecursiveFileObserver>

    lateinit var cacheObserver: RecursiveFileObserver

    var isRunning :Boolean =false


    override fun onBind(intent: Intent?): IBinder {
        Log.d("FileObserverService", "started")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FileObserverService", "on start command")

        if (isRunning)
            for(observer in filesObserver) {
                observer.stopWatching()
                observer.startWatching()
            }
        else {
            val accountMgr = AccountManager.get(baseContext)
            val restApi = RestApiImpl(baseContext)
            filesObserver = LinkedList()
            for (account in accountMgr.getAccountsByType(getString(R.string.authtype))) {
                val accountFileObserver = RecursiveFileObserver(restApi, account, baseContext,
                        File(baseContext.filesDir, account.name).absolutePath)
                filesObserver.add(accountFileObserver)
                accountFileObserver.startWatching()
            }
//        cacheObserver = RecursiveFileObserver(baseContext.cacheDir.absolutePath)
//        cacheObserver.startWatching()
            isRunning = true
        }
        return START_STICKY
    }

    inner class MonitorBinder : Binder() {
        val service: FileObserverService
            get() = this@FileObserverService
    }

}