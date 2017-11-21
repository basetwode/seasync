package com.bwksoftware.android.seasync.presentation.presenter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.bwksoftware.android.seasync.data.authentication.Authenticator
import com.bwksoftware.android.seasync.data.sync.SyncManager.Companion.DOWNLOAD_PROGRESS
import com.bwksoftware.android.seasync.data.sync.SyncManager.Companion.DOWNLOAD_RATE
import com.bwksoftware.android.seasync.data.sync.SyncManager.Companion.PROGRESS_VALUE
import com.bwksoftware.android.seasync.presentation.mapper.ModelMapper
import com.bwksoftware.android.seasync.presentation.view.views.DownloadView
import javax.inject.Inject

class DownloadPresenter @Inject constructor(val modelMapper: ModelMapper, val context: Context) {

    internal var downloadView: DownloadView? = null
    @Inject lateinit var authenticator: Authenticator
    lateinit var progressReceiver: DownloadProgressReceiver

    init {
        progressReceiver = DownloadProgressReceiver()
        context.registerReceiver(progressReceiver, IntentFilter(DOWNLOAD_PROGRESS))
    }

    fun setView(view: DownloadView) {
        downloadView = view
    }

    fun destroy() {
        context.unregisterReceiver(progressReceiver)
    }

    inner class DownloadProgressReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent!!.getIntExtra(PROGRESS_VALUE, 0)
            val rate = intent.getFloatExtra(DOWNLOAD_RATE, 0F)
            if (downloadView != null)
                downloadView!!.updateProgress(progress,rate)
        }

    }

}