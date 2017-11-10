package com.bwksoftware.android.seasync.presentation.view.fragment

import android.accounts.Account
import android.os.Bundle
import com.bwksoftware.android.seasync.data.authentication.SeafAccountManager
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import com.bwksoftware.android.seasync.presentation.R
import com.bwksoftware.android.seasync.presentation.presenter.DirectoryPresenter
import javax.inject.Inject

class DownloadFragment : BaseFragment() {
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


    @Inject lateinit var directoryPresenter: DirectoryPresenter
    @Inject lateinit var seafAccountManager: SeafAccountManager
    @Inject lateinit var seafPreferences: SharedPrefsController

    lateinit var repoId : String
    lateinit var repoName : String
    lateinit var directory : String
    lateinit var address: String

    override fun layoutId() = R.layout.fragment_directory

    override fun name() = arguments.getString(DownloadFragment.PARAM_REPONAME) + arguments.getString(
            DownloadFragment.PARAM_DIRECTORY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        repoId = arguments.getString(DownloadFragment.PARAM_REPOID)
        repoName = arguments.getString(DownloadFragment.PARAM_REPONAME)
        directory = arguments.getString(DownloadFragment.PARAM_DIRECTORY)
        address = seafAccountManager.getServerAddress(seafAccountManager.getCurrentAccount())!!
    }

}
