package com.bwksoftware.android.seasync.data.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.FileObserver
import android.util.Log
import com.bwksoftware.android.seasync.data.authentication.SeafAccountManager
import com.bwksoftware.android.seasync.data.cache.DiskCache
import com.bwksoftware.android.seasync.data.cache.DiskCache.Companion.SEASYNC_CACHE_AVATAR_EXTENSION
import com.bwksoftware.android.seasync.data.cache.DiskCache.Companion.SEASYNC_CACHE_EXTENSION
import com.bwksoftware.android.seasync.data.datamanager.StorageManager
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import com.bwksoftware.android.seasync.data.net.RestApiImpl
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import com.bwksoftware.android.seasync.data.provider.FileRepoContract
import java.io.File
import java.util.*


class RecursiveFileObserver constructor(private val restApi: RestApiImpl,
                                        private val account: Account,
                                        private val context: Context,
                                        private var mPath: String,
                                        private val isCacheObserver: Boolean,
                                        private var mMask: Int = FileObserver.ALL_EVENTS)
    : FileObserver(mPath, mMask) {

    private var TAG = "RecursiveFileObserver"

    private var mObservers: MutableList<SingleFileObserver>? = null

    private val storageManager = StorageManager(context, SeafAccountManager(context,
            SharedPrefsController(context)), restApi)

    private val accountManager = AccountManager.get(context)


    val cache: DiskCache = DiskCache(context)

    override fun startWatching() {
        Log.d(TAG, "started watching $mPath")
        if (mObservers != null) return

        val bla = mObservers ?: false

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
        Log.d(TAG, "Stopped watching")
        if (mObservers == null) return

        for (i in mObservers!!.indices)
            mObservers!![i].stopWatching()

        mObservers!!.clear()
        mObservers = null
    }

    fun extractItemFromPath(filePath: String): List<String> {
        val path = filePath.removePrefix(mPath).removePrefix("/")
        Log.d(TAG, path)
        val repoName = path.substringBefore("/")
        Log.d(TAG, repoName)
        val filePath = path.removePrefix(repoName)
        Log.d(TAG, filePath)

        val fileName = filePath.substringAfterLast("/")
        Log.d(TAG, fileName)

        val directory = filePath.substringBefore(fileName)
        return listOf(repoName, directory, fileName)
    }

    fun syncSyncedItem(path: String) {
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
    }

    fun syncCachedItem(path: String) {
        Log.d(TAG, "!!!!!: " + path)
        if (path.contains(SEASYNC_CACHE_EXTENSION) || path.contains(SEASYNC_CACHE_AVATAR_EXTENSION))
            return
        val (repoID, directory, name) = extractItemFromPath(path!!)
        val repo = Repo()
        repo.id = repoID
        repo.name = repoID
        val item = Item()
        item.name = name
        item.path = directory
        item.storage = mPath.substringBeforeLast(account.name)

        val authToken = accountManager.blockingGetAuthToken(account, "full_access",
                true)
        val uploadSuccess = storageManager.uploadAndUpdateItem(authToken, repo, item, item)
        if (uploadSuccess) {
            val cachedItems: List<Item> = cache.readDirectoryList(account.name, repoID,
                    item.path!!.removeSuffix("/"))
            cachedItems
                    .filter { it.name == item.name }
                    .forEach {
                        Log.d(TAG, it.mtime.toString() + " " + item.mtime.toString())
                        it.mtime = item.mtime
                        it.size = item.size
                    }
            Log.d(TAG, "pathhhhh " + item.path)
            cache.writeDirectoryList(account.name, repoID, item.path!!.removeSuffix("/"),
                    cachedItems,
                    false)
        }
        // TODO: upload item
        // TODO: update local cache json
    }

    override fun onEvent(event: Int, path: String?) {
        when (event) {
            FileObserver.CREATE -> return //Log.d(TAG, "CREATE:" + mPath + path)
            FileObserver.DELETE -> return //Log.d(TAG, "DELETE:" + mPath + path)
            FileObserver.DELETE_SELF -> return //Log.d(TAG, "DELETE_SELF:" + mPath + path)
            FileObserver.MODIFY -> {
                if (isCacheObserver) {
                    syncCachedItem(path!!)
                } else
                    syncSyncedItem(path!!)
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