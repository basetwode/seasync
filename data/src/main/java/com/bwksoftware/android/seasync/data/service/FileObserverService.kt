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

    lateinit var filesObservers: LinkedList<RecursiveFileObserver>

    lateinit var cacheObservers: LinkedList<RecursiveFileObserver>

    var isRunning: Boolean = false


    override fun onBind(intent: Intent?): IBinder {
        Log.d("FileObserverService", "started")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FileObserverService", "on start command")
        if (intent?.action == OnBootReceiver.ACTION_RESTART_CACHE_OBSERVER) {
            if (isRunning)
                for (observer in cacheObservers) {
                    observer.stopWatching()
                    observer.startWatching()
                }
        } else if (intent?.action == OnBootReceiver.ACTION_RESTART_FILE_OBSERVER) {
            if (isRunning) {
                for (observer in filesObservers) {
                    observer.stopWatching()
                    observer.startWatching()
                }
            }
//        } else if (isRunning) {
//            for (observer in filesObservers) {
//                observer.stopWatching()
//                observer.startWatching()
//            }
//            for (observer in cacheObservers) {
//                observer.stopWatching()
//                observer.startWatching()
//            }
        } else {
            val accountMgr = AccountManager.get(baseContext)
            val restApi = RestApiImpl(baseContext)
            filesObservers = LinkedList()
            cacheObservers = LinkedList()
            for (account in accountMgr.getAccountsByType(getString(R.string.authtype))) {
                val accountFileObserver = RecursiveFileObserver(restApi, account, baseContext,
                        File(baseContext.filesDir, account.name).absolutePath, false)
                val accountFileObserver2 = RecursiveFileObserver(restApi, account, baseContext,
                        File(baseContext.getExternalFilesDir(null), account.name).absolutePath,
                        false)
                val cacheObserver = RecursiveFileObserver(restApi, account, baseContext,
                        File(baseContext.cacheDir, account.name).absolutePath, true)
                cacheObservers.add(cacheObserver)
                filesObservers.add(accountFileObserver)
                filesObservers.add(accountFileObserver2)

                accountFileObserver.startWatching()
                accountFileObserver2.startWatching()
                cacheObserver.startWatching()
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