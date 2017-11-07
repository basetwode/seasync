/*
 *    Copyright 2018 BWK Technik GbR
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.bwksoftware.android.seasync.presentation.view.fragment

import android.accounts.Account
import android.app.Activity
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bwksoftware.android.seasync.data.authentication.SeafAccountManager
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import com.bwksoftware.android.seasync.data.utils.FileUtils
import com.bwksoftware.android.seasync.presentation.R
import com.bwksoftware.android.seasync.presentation.components.BottomSheet
import com.bwksoftware.android.seasync.presentation.components.GridLayoutManager
import com.bwksoftware.android.seasync.presentation.model.BottomSheetItem
import com.bwksoftware.android.seasync.presentation.model.DirectoryItem
import com.bwksoftware.android.seasync.presentation.model.Item
import com.bwksoftware.android.seasync.presentation.presenter.DirectoryPresenter
import com.bwksoftware.android.seasync.presentation.view.adapter.DirectoryAdapter
import com.bwksoftware.android.seasync.presentation.view.views.DirectoryView
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import java.net.URLEncoder
import javax.inject.Inject


class DirectoryFragment : BaseFragment(), DirectoryView, DirectoryAdapter.OnItemClickListener {

    interface OnDirectoryClickedListener {
        fun onDirectoryClicked(fragment: BaseFragment, repoId: String, repoName: String,
                               directory: String)

        fun onFileClicked(fragment: BaseFragment, repoId: String, repoName: String,
                          directory: String, storage: String, file: String)

        fun onImageClicked(fragment: BaseFragment, repoId: String, repoName: String,
                           directory: String, file: String)

        fun onRevealClicked(fragment: BaseFragment, repoId: String, repoName: String,
                            directory: String, storage: String, file: String)
    }

    companion object {
        private const val PARAM_ACCOUNT = "param_account"
        private const val PARAM_DIRECTORY = "param_directory"
        private const val PARAM_REPOID = "param_repoid"
        private const val PARAM_REPONAME = "param_reponame"

        fun forAccountRepoAndDir(account: Account, repoId: String,
                                 repoName: String,
                                 directory: String): DirectoryFragment {
            val reposFragment = DirectoryFragment()
            val arguments = Bundle()
            arguments.putString(PARAM_ACCOUNT, account.name)
            arguments.putString(PARAM_DIRECTORY, directory)
            arguments.putString(PARAM_REPOID, repoId)
            arguments.putString(PARAM_REPONAME, repoName)
            reposFragment.arguments = arguments
            return reposFragment
        }
    }

    @Inject lateinit var directoryPresenter: DirectoryPresenter
    @Inject lateinit var seafAccountManager: SeafAccountManager
    @Inject lateinit var seafPreferences: SharedPrefsController

    lateinit var directoryAdapter: DirectoryAdapter

    lateinit var rvDirectory: RecyclerView

    private lateinit var address: String

    lateinit var repoId : String
    lateinit var repoName : String
    lateinit var directory : String

    override fun layoutId() = R.layout.fragment_directory

    override fun name() = arguments.getString(PARAM_REPONAME) + arguments.getString(
            PARAM_DIRECTORY)

    override fun activity() = activity


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        repoId = arguments.getString(PARAM_REPOID)
        repoName = arguments.getString(PARAM_REPONAME)
        directory = arguments.getString(PARAM_DIRECTORY)
        address = seafAccountManager.getServerAddress(seafAccountManager.getCurrentAccount())!!
        directoryAdapter = DirectoryAdapter(this,
                isGridView(),
                address!!,
                arguments.getString(PARAM_REPOID),
                arguments.getString(
                        PARAM_DIRECTORY), seafAccountManager.getCurrentAccountToken(), context)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvDirectory = view?.findViewById(R.id.rv_directory)!!
        rvDirectory.adapter = directoryAdapter

        if (firstTimeCreated(savedInstanceState)) {
            initializeView()
            loadDirectory()
        }
    }

    override fun renderDirectoryEntries(entries: List<Item>) {
        directoryAdapter.setItems(entries)
        directoryAdapter.notifyDataSetChanged()
    }

    override fun updateItem(position: Int, item: Item) {
        val currItem = directoryAdapter.getItem(position)
        currItem.synced = item.synced
        currItem.isCached = item.isCached
        currItem.storage = item.storage
        currItem.isRootSync = item.isRootSync
        directoryAdapter.notifyItemChanged(position)
    }


    override fun onDirectoryClicked(item: Item, position: Int) {
        val attachedActivity = activity
        when (attachedActivity) {
            is OnDirectoryClickedListener -> attachedActivity.onDirectoryClicked(this,
                    arguments.getString(PARAM_REPOID),
                    arguments.getString(PARAM_REPONAME),
                    arguments.getString(PARAM_DIRECTORY) + "/" + item.name)
        }
    }

    override fun onFileClicked(item: Item, position: Int) {
        val attachedActivity = activity
        when (attachedActivity) {
            is OnDirectoryClickedListener -> {
                if (FileUtils.isViewableImage(item.name!!))
                    attachedActivity.onImageClicked(this,
                            arguments.getString(PARAM_REPOID),
                            arguments.getString(PARAM_REPONAME),
                            arguments.getString(PARAM_DIRECTORY), item.name)
                else {
                    if (item.synced) {
                        attachedActivity.onFileClicked(this,
                                arguments.getString(PARAM_REPOID),
                                arguments.getString(PARAM_REPONAME),
                                arguments.getString(PARAM_DIRECTORY), item.storage!!, item.name)
                    } else if (item.isCached) {
                        attachedActivity.onFileClicked(this,
                                arguments.getString(PARAM_REPOID),
                                arguments.getString(PARAM_REPOID),
                                arguments.getString(PARAM_DIRECTORY), item.storage!!, item.name)
                    } else {
                        directoryPresenter.fileClicked(position, arguments.getString(PARAM_ACCOUNT),
                                arguments.getString(PARAM_REPOID), item, arguments.getString(
                                PARAM_DIRECTORY))
                    }
                }
            }
        }
    }

    override fun onDirectoryLongClicked(item: Item, position: Int) {


        val mBottomSheetDialog = BottomSheetDialog(activity)
        val sheetView = activity.layoutInflater.inflate(R.layout.item_bottom_sheet,
                null)
        mBottomSheetDialog.setContentView(sheetView)
        val bottomSheet = BottomSheet(BottomSheetItem(item), sheetView, this, repoName,repoId,directory,seafAccountManager,address)
        bottomSheet.openButton.setOnClickListener({
            onDirectoryClicked(item, position)
            mBottomSheetDialog.dismiss()
        })
        bottomSheet.syncExternalStorage.setOnClickListener({
            syncUnsyncDir(position, item, context.getExternalFilesDir(null).absolutePath)
            mBottomSheetDialog.dismiss()
        })
        bottomSheet.syncInternalStorage.setOnClickListener({
            syncUnsyncDir(position, item, context.filesDir.absolutePath)
            mBottomSheetDialog.dismiss()
        })
        bottomSheet.syncButton.setOnClickListener({
            if (item.synced) {
                syncUnsyncDir(position, item, "")
                mBottomSheetDialog.dismiss()
            } else {
                bottomSheet.options.visibility = if (bottomSheet.options.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        })
        mBottomSheetDialog.show()


    }

    override fun onFileLongClicked(item: Item, position: Int) {

        val mBottomSheetDialog = BottomSheetDialog(activity)
        val sheetView = activity.layoutInflater.inflate(R.layout.item_bottom_sheet,
                null)
        mBottomSheetDialog.setContentView(sheetView)
        val bottomSheet = BottomSheet(BottomSheetItem(item), sheetView, this, repoName,repoId,directory,seafAccountManager,address)

        bottomSheet.openButton.setOnClickListener({
            onFileClicked(item, position)
            mBottomSheetDialog.dismiss()
        })
        bottomSheet.syncExternalStorage.setOnClickListener({
            syncUnsyncFile(position, item, context.getExternalFilesDir(null).absolutePath)
            mBottomSheetDialog.dismiss()
        })
        bottomSheet.syncInternalStorage.setOnClickListener({
            syncUnsyncFile(position, item, context.filesDir.absolutePath)
            mBottomSheetDialog.dismiss()
        })
        bottomSheet.syncButton.setOnClickListener({
            if (item.synced) {
                syncUnsyncFile(position, item, "")
                mBottomSheetDialog.dismiss()
            } else {
                bottomSheet.options.visibility = if (bottomSheet.options.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        })
        mBottomSheetDialog.show()

    }

    override fun fileDownloadComplete(item: Item) {
        val attachedActivity = activity
        when (attachedActivity) {
            is OnDirectoryClickedListener -> {
                attachedActivity.onFileClicked(this,
                        arguments.getString(PARAM_REPOID),
                        arguments.getString(PARAM_REPOID),
                        arguments.getString(PARAM_DIRECTORY), item.storage!!, item.name!!)
            }
        }
    }

    private fun syncUnsyncDir(position: Int, item: Item, storage: String) {
        directoryPresenter.directoryLongClicked(position, arguments.getString(PARAM_ACCOUNT),
                arguments.getString(
                        PARAM_REPOID), item,
                arguments.getString(PARAM_DIRECTORY), storage, item.synced)
    }

    private fun syncUnsyncFile(position: Int, item: Item, storage: String) {
        directoryPresenter.fileLongClicked(position, arguments.getString(PARAM_ACCOUNT),
                arguments.getString(
                        PARAM_REPOID), item,
                arguments.getString(PARAM_DIRECTORY), storage, item.synced)
    }


    private fun loadDirectory() {
        directoryPresenter.getDirectoryEntries(arguments.getString(PARAM_ACCOUNT),
                arguments.getString(
                        PARAM_REPOID), arguments.getString(PARAM_DIRECTORY))
    }

    private fun getGridColumns(isGridView: Boolean): Int {
        return if (!isGridView()) 1 else context.resources.getInteger(
                R.integer.gv_number_of_columns)
    }

    private var mDividerItemDecoration: DividerItemDecoration? = null

    fun switchView(isGridView: Boolean) {
        val layoutMgr = GridLayoutManager(context, getGridColumns(isGridView))
        rvDirectory.layoutManager = layoutMgr
        if (!isGridView) {
            mDividerItemDecoration = DividerItemDecoration(
                    rvDirectory.context,
                    (rvDirectory.layoutManager as GridLayoutManager).orientation
            )

            rvDirectory.addItemDecoration(mDividerItemDecoration)
        } else {
            rvDirectory.removeItemDecoration(mDividerItemDecoration)
        }

        directoryAdapter.isGridView = isGridView
        rvDirectory.adapter = directoryAdapter
        directoryAdapter.notifyDataSetChanged()
    }

    private fun isGridView(): Boolean {
        val pref = seafPreferences.getPreferenceValue(
                SharedPrefsController.Preference.GRID_VIEW_DIRECTORIES)
        return pref.toBoolean()
    }


    private fun initializeView() {
        directoryPresenter.directoryView = this
        rvDirectory.setHasFixedSize(true)
        rvDirectory.setItemViewCacheSize(20)
        rvDirectory.isDrawingCacheEnabled = true
        rvDirectory.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        switchView(isGridView())
    }


}