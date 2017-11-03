package com.bwksoftware.android.seasync.data.sync

import android.accounts.Account
import android.content.Context
import android.os.FileObserver
import android.util.Log
import com.bwksoftware.android.seasync.data.datamanager.StorageManager
import com.bwksoftware.android.seasync.data.net.RestApiImpl
import com.bwksoftware.android.seasync.data.provider.FileRepoContract
import java.io.File
import java.util.*


class RecursiveFileObserver constructor(private val restApi: RestApiImpl,
                                        private val account: Account,
                                        private val context: Context,
                                        private var mPath: String,
                                        private var mMask: Int = FileObserver.ALL_EVENTS)
    : FileObserver(mPath, mMask) {

    private var TAG = "RecursiveFileObserver"

    private var mObservers: MutableList<SingleFileObserver>? = null

    private val storageManager = StorageManager(context, null, restApi)

    override fun startWatching() {
        storageManager.setAccount(account)
        Log.d(TAG, "started watching $mPath")
        if (mObservers != null) return
        mObservers = ArrayList()
        val stack = Stack<String>()
        stack.push(mPath)

        while (!stack.empty()) {
            val parent = stack.pop()
            mObservers!!.add(SingleFileObserver(parent, mMask))
            val path = File(parent)
            val files = path.listFiles() ?: continue
            files.indices
                    .filter { files[it].isDirectory && files[it].name != "." && files[it].name != ".." }
                    .forEach { stack.push(files[it].path) }
        }
        for (i in mObservers!!.indices)
            mObservers!![i].startWatching()
    }

    override fun stopWatching() {
        if (mObservers == null) return

        for (i in mObservers!!.indices)
            mObservers!![i].stopWatching()

        mObservers!!.clear()
        mObservers = null
    }

    fun extractItemFromPath(filePath: String): List<String> {
        val path = filePath.removePrefix(mPath).removePrefix("/")
        val repoName = path.substringBefore("/")
        val filePath = path.removePrefix(repoName)
        val fileName = filePath.substringAfterLast("/")
        val directory = filePath.substringBefore(fileName)
        return listOf(repoName, directory, fileName)

    }

    override fun onEvent(event: Int, path: String?) {
        when (event) {
            FileObserver.CREATE -> Log.d(TAG, "CREATE:" + mPath + path)
            FileObserver.DELETE -> Log.d(TAG, "DELETE:" + mPath + path)
            FileObserver.DELETE_SELF -> Log.d(TAG, "DELETE_SELF:" + mPath + path)
            FileObserver.MODIFY -> {
                val (repo, directory, name) = extractItemFromPath(path!!)
                val repoItem = storageManager.getRepo(FileRepoContract.RepoColumns.NAME, repo)
                if (repoItem != null) {
                    val item = storageManager.getFile(repoItem.dbId!!.toString(), directory, name)
                    if (item != null) {
                        val file = File(path)
                        Log.d(TAG, "updating path ${item.mtime} -> ${file.lastModified()}")
                        item.mtime = file.lastModified() / 1000
                        storageManager.updateItemFromBottomToTop(repoItem, item)
                        storageManager.saveItemInstance(item)
                        repoItem.mtime = item.mtime
                        storageManager.saveRepoInstance(repoItem)
                    }
                }

                Log.d(TAG, "MODIFY: $repo $directory $name")
                Log.d(TAG, "MODIFY: $repo " + mPath + " - " + path)
                //TODO retrieve account from path
                //todo inject storagemanager and update stuff
            }
            FileObserver.MOVED_FROM -> Log.d(TAG, "MOVED_FROM:" + mPath + path)
            FileObserver.MOVED_TO -> Log.d(TAG, "MOVED_TO:" + path)
            FileObserver.MOVE_SELF -> Log.d(TAG, "MOVE_SELF:" + path)
            else -> {
            }
        }// just ignore
    }

    inner class SingleFileObserver(private val mPath: String, mask: Int) : FileObserver(
            mPath, mask) {

        override fun onEvent(event: Int, path: String?) {
            val newPath = mPath + "/" + path
            this@RecursiveFileObserver.onEvent(event, newPath)
        }

    }

    companion object {

        var CHANGES_ONLY = FileObserver.CLOSE_WRITE or FileObserver.MOVE_SELF or FileObserver.MOVED_FROM
    }
}