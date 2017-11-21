package com.bwksoftware.android.seasync.presentation.view.fragment

import android.accounts.Account
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.bwksoftware.android.seasync.data.authentication.SeafAccountManager
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import com.bwksoftware.android.seasync.presentation.R
import com.bwksoftware.android.seasync.presentation.presenter.DownloadPresenter
import com.bwksoftware.android.seasync.presentation.view.views.DownloadView
import javax.inject.Inject

class DownloadFragment : BaseFragment(), DownloadView {

    companion object {
        private const val PARAM_ACCOUNT = "param_account"
        private const val PARAM_DIRECTORY = "param_directory"
        private const val PARAM_REPOID = "param_repoid"
        private const val PARAM_REPONAME = "param_reponame"

        fun forAccountRepoAndDir(account: Account, repoId: String,
                                 repoName: String,
                                 directory: String): DownloadFragment {
            val downloadFragment = DownloadFragment()
            val arguments = Bundle()
            arguments.putString(PARAM_ACCOUNT, account.name)
            arguments.putString(PARAM_DIRECTORY, directory)
            arguments.putString(PARAM_REPOID, repoId)
            arguments.putString(PARAM_REPONAME, repoName)
            downloadFragment.arguments = arguments
            return downloadFragment
        }
    }


    @Inject lateinit var downloadPresenter: DownloadPresenter
    @Inject lateinit var seafAccountManager: SeafAccountManager
    @Inject lateinit var seafPreferences: SharedPrefsController

    lateinit var repoId: String
    lateinit var repoName: String
    lateinit var directory: String
    lateinit var address: String

    lateinit var fileName: TextView
    lateinit var downloadRate: TextView
    lateinit var progressBar: ProgressBar

    override fun layoutId() = R.layout.fragment_download

    override fun name() = "Downloading File"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        repoId = arguments.getString(DownloadFragment.PARAM_REPOID)
        repoName = arguments.getString(DownloadFragment.PARAM_REPONAME)
        directory = arguments.getString(DownloadFragment.PARAM_DIRECTORY)
        address = seafAccountManager.getServerAddress(seafAccountManager.getCurrentAccount())!!
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        downloadPresenter.setView(this)
        fileName = view!!.findViewById(R.id.file_name)
        progressBar = view.findViewById(R.id.download_progress)
        downloadRate = view.findViewById(R.id.download_rate)
        progressBar.max = 100
        fileName.text = directory
    }

    override fun updateProgress(progress: Int, rate: Float) {
        //progressBar.text = progressBar.toString()
        progressBar.progress = progress
        Log.d("DownloadFragment",rate.toString())
        downloadRate.text = "%.2f MB/s".format(rate)
        if (progress >= 99) {
            activity.onBackPressed()
            downloadPresenter.destroy()
        }
    }

}
