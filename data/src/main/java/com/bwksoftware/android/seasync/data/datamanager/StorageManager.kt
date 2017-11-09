package com.bwksoftware.android.seasync.data.datamanager

import android.accounts.Account
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.BaseColumns
import android.util.Log
import com.bwksoftware.android.seasync.data.authentication.SeafAccountManager
import com.bwksoftware.android.seasync.data.cache.DiskCache
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import com.bwksoftware.android.seasync.data.net.RestApiImpl
import com.bwksoftware.android.seasync.data.provider.FileRepoContract
import com.bwksoftware.android.seasync.data.provider.FileRepoContract.Companion.CONTENT_URI
import com.bwksoftware.android.seasync.data.service.OnBootReceiver
import com.bwksoftware.android.seasync.data.sync.SyncManager
import java.io.File
import javax.inject.Inject


class StorageManager @Inject constructor(val context: Context,
                                         val seafAccountManager: SeafAccountManager
                                         ,
                                         val restApi: RestApiImpl) {

    init {

    }

    var contentProviderClient = context.contentResolver.acquireContentProviderClient(CONTENT_URI)

    fun currAccount(): Account = seafAccountManager.getCurrentAccount()
    val cache = DiskCache(context)


    fun createItemInstance(cursor: Cursor): Item {
        val item = Item()
        item.dbId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
        item.name = cursor.getString(cursor.getColumnIndex(FileRepoContract.FileColumns.NAME))
        item.mtime = cursor.getLong(cursor.getColumnIndex(FileRepoContract.FileColumns.MOD_DATE))
        item.size = cursor.getLong(cursor.getColumnIndex(FileRepoContract.FileColumns.SIZE))
        item.repoId = cursor.getString(
                cursor.getColumnIndex(FileRepoContract.FileColumns.REPO_ID)).toLong()
        item.path = cursor.getString(cursor.getColumnIndex(FileRepoContract.FileColumns.PATH))
        item.type = cursor.getString(cursor.getColumnIndex(FileRepoContract.FileColumns.TYPE))
        item.id = cursor.getString(cursor.getColumnIndex(FileRepoContract.FileColumns.REMOTE_ID))
        item.storage = cursor.getString(cursor.getColumnIndex(FileRepoContract.FileColumns.STORAGE))
        item.isRootSync = cursor.getInt(
                cursor.getColumnIndex(FileRepoContract.FileColumns.ROOT_SYNC)) == 1
        item.synced = cursor.getInt(cursor.getColumnIndex(FileRepoContract.FileColumns.SYNCED)) == 1
        return item
    }


    fun saveItemInstance(item: Item) {
        updateCache(item)
        val contentValues = ContentValues()
        contentValues.put(FileRepoContract.FileColumns.NAME, item.name)
        contentValues.put(FileRepoContract.FileColumns.SIZE, item.size)
        contentValues.put(FileRepoContract.FileColumns.MOD_DATE, item.mtime)
        contentValues.put(FileRepoContract.FileColumns.PATH, item.path)
        contentValues.put(FileRepoContract.FileColumns.REPO_ID, item.repoId.toString())
        contentValues.put(FileRepoContract.FileColumns.TYPE, item.type)
        contentValues.put(FileRepoContract.FileColumns.REMOTE_ID, item.id)
        contentValues.put(FileRepoContract.FileColumns.SYNCED, item.synced)
        contentValues.put(FileRepoContract.FileColumns.ROOT_SYNC, item.isRootSync)
        contentValues.put(FileRepoContract.FileColumns.STORAGE, item.storage)
        contentValues.put(FileRepoContract.FileColumns.ACCOUNT, currAccount().name)


        if (item.dbId != null) {
            val resultUri = contentProviderClient.update(FileRepoContract.File.buildFileUri(
                    item.dbId!!),
                    contentValues, null, null)
        } else {
            val resultUri = contentProviderClient.insert(FileRepoContract.File.CONTENT_URI,
                    contentValues)
            val id = resultUri.pathSegments[1]
            item.dbId = id.toLong()
        }
    }

    fun updateCache(item: Item) {
        var name = currAccount().name
        if (name == "None")
            name = seafAccountManager?.getCurrentAccount()?.name
        val repo = getRepo(BaseColumns._ID, item.repoId.toString())
        val cachedItems: List<Item> = cache.readDirectoryList(name, repo!!.id!!,
                item.path!!.removeSuffix("/"))
        var newItem: Item? = null
        cachedItems
                .filter { it.name == item.name }
                .forEach {
                    it.isRootSync = item.isRootSync
                    it.synced = item.synced
                    it.path = item.path
                    it.storage = item.storage
                    newItem = it
                }
        cache.writeDirectoryList(currAccount().name, repo.id!!, newItem!!.path!!.removeSuffix("/"),
                cachedItems,
                false)
    }

    fun syncItem(authToken: String, repo: Repo, localItem: Item?, remoteItem: Item): Boolean {
        Log.d("FileSyncAdapter", "syncing " + remoteItem.name)
        return if (localItem == null) {
            Log.d("FileSyncAdapter", "file not synced")
            remoteItem.repoId = repo.dbId
            downloadAndUpdateItem(authToken, repo, remoteItem, remoteItem)
        } else {
            if (localItem.mtime!! > remoteItem.mtime!!) {
                // our item is newer, upload it
                uploadAndUpdateItem(authToken, repo, localItem, remoteItem)
            } else {
                downloadAndUpdateItem(authToken, repo, localItem, remoteItem)
                // remote item is newer download it and update db
            }
        }
    }

    fun downloadAndUpdateItem(authToken: String, repo: Repo, localItem: Item,
                              remoteItem: Item): Boolean {
        val currAccount = seafAccountManager.getCurrentAccount()
        val serverAddress = seafAccountManager.getServerAddress(currAccount)!!
        val call = restApi.getFileDownloadLink(authToken, serverAddress, repo.id!!,
                localItem.path + "/" + localItem.name)
        val response = call.execute()
        if (response.isSuccessful) {
            val downloadLink = response.body() ?: return false
            val downloadCall = restApi.downloadFile(downloadLink)
            val responseDownload = downloadCall.execute()
            if (responseDownload.isSuccessful) {
                val file = responseDownload.body() ?: return false
                val download = SyncManager.DownloadTask(localItem, createFilePath(repo, localItem),
                        file)
                Thread.sleep(100)
                val fileDownloadedSuccessful = download.execute().get()

                return fileDownloadedSuccessful
            }
            return false
        }
        return false
    }

    fun getPathAndNameFromPath(path: String): List<String> {
        val pathWithoutTrailingSlash = if (path.endsWith("/")) path.removeSuffix("/") else path
        val newPath = pathWithoutTrailingSlash.substringBeforeLast("/")
        val name = pathWithoutTrailingSlash.substringAfterLast("/")
        return listOf(newPath, name)
    }

    fun updateItemFromBottomToTop(repo: Repo, item: Item) {
        Log.d("StorageManager", "Updating ${repo.name}: ${item.path} - ${item.name}")
        val (parentPath, parentName) = getPathAndNameFromPath(item.path!!)
        val parentItem = getFile(repo.dbId!!.toString(), parentPath + "/", parentName)
        if (parentPath.isNotEmpty())
            updateItemFromBottomToTop(repo, parentItem!!)
        if (parentItem != null) {
            parentItem!!.mtime = item.mtime
            saveItemInstance(parentItem)
        }
    }

    fun createFilePath(repo: Repo, item: Item): String {
        return """${item.storage}/${currAccount().name}/${repo.id}/${item.path}"""
    }

    fun uploadAndUpdateItem(authToken: String, repo: Repo, localItem: Item,
                            remoteItem: Item): Boolean {
        val currAccount = seafAccountManager.getCurrentAccount()
        val serverAddress = seafAccountManager.getServerAddress(currAccount)!!
        val call = restApi.getUpdateLink(authToken, serverAddress, repo.id!!, remoteItem.path!!)

        val response = call.execute()
        if (response.isSuccessful) {
            val link = response.body() ?: return false
            //TODO refactor file path creation

            val file = File(createFilePath(repo, localItem), localItem.name)
            val uploadCall = restApi.updateFile(link, authToken,
                    File(localItem.path, localItem.name).absolutePath,
                    file)
            val responseUpload = uploadCall.execute()

            val detailCall = restApi.getFileDetail(authToken, serverAddress, repo.id!!,
                    localItem.path!!,
                    localItem.name!!)
            val itemDetails = detailCall.execute().body() ?: return false

            if (responseUpload.isSuccessful) {
                remoteItem.mtime = itemDetails.mtime
                remoteItem.size = itemDetails.size
            }

            return responseUpload.isSuccessful
        }
        return false
    }

    fun createRepoInstance(cursor: Cursor): Repo {
        val repo = Repo()
        repo.dbId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
        repo.id = cursor.getString(cursor.getColumnIndex(FileRepoContract.RepoColumns.REPO_ID))
        repo.name = cursor.getString(cursor.getColumnIndex(FileRepoContract.RepoColumns.NAME))
        repo.mtime = cursor.getLong(cursor.getColumnIndex(FileRepoContract.RepoColumns.MOD_DATE))
        repo.synced = cursor.getInt(cursor.getColumnIndex(
                FileRepoContract.RepoColumns.FULL_SYNCED)) == 1
        repo.encrypted = cursor.getInt(cursor.getColumnIndex(
                FileRepoContract.RepoColumns.ENCRYPTED)) == 1
        repo.size = cursor.getLong(cursor.getColumnIndex(FileRepoContract.RepoColumns.SIZE))
        repo.permission = cursor.getString(
                cursor.getColumnIndex(FileRepoContract.RepoColumns.PERMISSION))
        repo.storage = cursor.getString(cursor.getColumnIndex(FileRepoContract.RepoColumns.STORAGE))
        return repo
    }

    fun getItemsForRepo(repo: Repo): Map<String, Item> {
        val cursor = contentProviderClient.query(FileRepoContract.File.CONTENT_URI,
                null,
                FileRepoContract.FileColumns.REPO_ID + "=? AND " +
                        FileRepoContract.FileColumns.ACCOUNT + "=?",
                arrayOf(repo.dbId.toString(), currAccount().name),
                null)
        val items = HashMap<String, Item>()
        while (cursor.moveToNext()) {
            val item = createItemInstance(cursor)
            items.put(item.name!!, item)
        }
        cursor.close()
        return items
    }

    fun getItemsForRepo(repo: Repo, path: String): Map<String, Item> {
        val cursor = contentProviderClient.query(FileRepoContract.File.CONTENT_URI,
                null,
                FileRepoContract.FileColumns.REPO_ID + "=? AND " +
                        FileRepoContract.FileColumns.PATH + "=? AND " +
                        FileRepoContract.FileColumns.ACCOUNT + "=?",
                arrayOf(repo.dbId.toString(), path, currAccount().name),
                null)
        val items = HashMap<String, Item>()
        while (cursor.moveToNext()) {
            val item = createItemInstance(cursor)
            items.put(item.name!!, item)
        }
        cursor.close()
        return items
    }

    fun getRepo(key: String, value: String): Repo? {
        val cursor = contentProviderClient.query(FileRepoContract.Repo.CONTENT_URI,
                null,
                "$key=? AND " +
                        FileRepoContract.RepoColumns.ACCOUNT + "=?",
                arrayOf(value, currAccount().name),
                null)
        var localRepo: Repo? = null
        if (cursor.moveToFirst())
            localRepo = createRepoInstance(cursor)
        cursor.close()
        return localRepo
    }

    fun getRepo(repoHash: String): Repo? {
        val cursor = contentProviderClient.query(FileRepoContract.Repo.CONTENT_URI,
                null,
                FileRepoContract.RepoColumns.REPO_ID + "=? AND " +
                        FileRepoContract.RepoColumns.ACCOUNT + "=?",
                arrayOf(repoHash, currAccount().name),
                null)
        var localRepo: Repo? = null
        if (cursor.moveToFirst())
            localRepo = createRepoInstance(cursor)
        cursor.close()
        return localRepo
    }

    fun getRepo(repo: Repo): Repo? {
        return getRepo(repo.id!!)
    }

    fun saveRepoInstance(repo: Repo) {
        updateCacheForRepo(repo)
        val contentValues = ContentValues()
        contentValues.put(FileRepoContract.RepoColumns.NAME, repo.name)
        contentValues.put(FileRepoContract.RepoColumns.MOD_DATE, repo.mtime)
        contentValues.put(FileRepoContract.RepoColumns.FULL_SYNCED, repo.synced)
        contentValues.put(FileRepoContract.RepoColumns.REPO_ID, repo.id)
        contentValues.put(FileRepoContract.RepoColumns.STORAGE, repo.storage)
        contentValues.put(FileRepoContract.RepoColumns.ACCOUNT, currAccount().name)
        contentValues.put(FileRepoContract.RepoColumns.ENCRYPTED, repo.encrypted)
        contentValues.put(FileRepoContract.RepoColumns.PERMISSION, repo.permission)
        contentValues.put(FileRepoContract.RepoColumns.SIZE, repo.size)

        if (repo.dbId != null) {
            val resultUri = contentProviderClient.update(FileRepoContract.Repo.buildRepoUri(
                    repo.dbId!!),
                    contentValues, null, null)
        } else {
            val directory = File(repo.storage, repo.name)
            directory.mkdirs()
            val resultUri = contentProviderClient.insert(FileRepoContract.Repo.CONTENT_URI,
                    contentValues)
            val id = resultUri.pathSegments[1]
            repo.dbId = id.toLong()
        }
    }

    fun updateCacheForRepo(repo: Repo) {
        var name = currAccount().name
        if (name == "None")
            name = seafAccountManager.getCurrentAccount().name
        val cachedItems: List<Repo> = cache.readRepoList(name)
        var newItem: Repo? = null
        cachedItems
                .filter { it.id == repo.id }
                .forEach {
                    it.isRootSync = repo.isRootSync
                    it.synced = repo.synced
                    it.storage = repo.storage
                    newItem = it
                }
        cache.writeRepoList(name, cachedItems, false)
    }

    fun getFile(hash: String): Item? {
        val cursor = contentProviderClient.query(FileRepoContract.File.CONTENT_URI,
                null,
                FileRepoContract.FileColumns.REMOTE_ID + "=?",
                arrayOf(hash),
                null
        )
        var item: Item? = null
        if (cursor.moveToFirst())
            item = createItemInstance(cursor)
        cursor.close()
        return item
    }

    fun getFile(repoId: String, filePath: String, fileName: String): Item? {
        val cursor = contentProviderClient.query(FileRepoContract.File.CONTENT_URI,
                null,
                FileRepoContract.FileColumns.REPO_ID + "=? AND " +
                        FileRepoContract.FileColumns.PATH + "=? AND " +
                        FileRepoContract.FileColumns.NAME + "=? AND " +
                        FileRepoContract.FileColumns.ACCOUNT + "=?",
                arrayOf(repoId, filePath, fileName, currAccount().name),
                null
        )
        var item: Item? = null
        if (cursor.moveToFirst())
            item = createItemInstance(cursor)
        cursor.close()
        return item
    }

    fun getCachedFile(repoHash: String, authToken: String, item: Item): Item? {
        val cacheDir = context.cacheDir.absolutePath
        val repo = Repo()
        repo.id = repoHash
        repo.name = repoHash
        item.storage = cacheDir
        val downloadSuccessful = (downloadAndUpdateItem(authToken, repo, item, item))

        if (downloadSuccessful) {
            //Update local cache
            val cachedItems: List<Item> = cache.readDirectoryList(currAccount().name, repoHash,
                    item.path!!)
            var newItem: Item? = null
            cachedItems
                    .filter { it.name == item.name }
                    .forEach {
                        it.isCached = true
                        it.path = item.path
                        it.storage = cacheDir
                        newItem = it
                    }
            cache.writeDirectoryList(currAccount().name, repoHash, newItem!!.path!!, cachedItems,
                    false)
            val restartObserverIntent = Intent(OnBootReceiver.ACTION_RESTART_CACHE_OBSERVER)
            context.sendBroadcast(restartObserverIntent)
            return newItem
        }
        return null
    }

    fun getFile(repo: Repo, item: Item): Item? {
        return getFile(repo.dbId.toString(), item.path!!, item.name!!)
    }

    fun fileExists(path: String): Boolean {
        return fileExists(FileRepoContract.FileColumns.PATH, path)
    }

    fun fileExists(selectionKey: String, selectionValue: String): Boolean {
        val cursor = contentProviderClient.query(FileRepoContract.File.CONTENT_URI,
                null,
                selectionKey + "=?", arrayOf(selectionValue),
                FileRepoContract.File.SORT_ORDER_DEFAULT
        )
        val result = cursor.moveToFirst()
        cursor.close()
        return result
    }

    fun deleteItemRecursive(parent: Item, repo: Repo) {
        Log.d("StorageManager",
                "Deleting item ${parent.path}${parent.name} ")
        val items = getItemsForRepo(repo, parent.path!! + parent.name + "/")
        for ((_, item) in items) {
            if (item.type == "dir")
                deleteItemRecursive(item, repo)
            deleteItem(repo, item)
        }
        cache.writeDirectoryList(currAccount().name, repo.id!!,
                (parent.path!! + parent.name).removeSuffix("/"), items.values.toList(), false)
        deleteItem(repo, parent)
    }

    fun deleteItem(repo: Repo, item: Item) {
        val correspondingFile = File(createFilePath(repo, item), item.name)
        item.synced = false
        correspondingFile.delete()
        contentProviderClient.delete(FileRepoContract.File.buildFileUri(item.dbId!!),
                null, null)
    }

    fun unsyncItem(repoId: String, path: String, name: String): Item {
        val repo = getRepo(repoId)
        val item = getFile(repo!!.dbId.toString(), path + "/", name)
        deleteItemRecursive(item!!, repo)
        deleteParentsIfUnsyncedRecursive(repo, item)
        item.synced = false
        return item
    }

    fun createNewRepoSync(authToken: String, repoHash: String, storage: String): Repo {
        var localRepo = getRepo(repoHash)
        if (localRepo == null) {
            val currAccount = seafAccountManager.getCurrentAccount()
            val serverAddress = seafAccountManager.getServerAddress(currAccount)!!
            restApi.getRepoListSync(authToken, serverAddress).execute().body()!!
                    .filter { it.id == repoHash }
                    .forEach { localRepo = it }
        }
        localRepo!!.mtime = 0
        localRepo!!.isRootSync = true
        localRepo!!.synced = true
        localRepo!!.storage = storage
        saveRepoInstance(localRepo!!)
        return localRepo!!
    }

    fun createNewSync(authToken: String, repoHash: String, path: String, name: String,
                      storage: String, type: String): Item {

        var localRepo = getRepo(repoHash)
        if (localRepo == null) {
            val currAccount = seafAccountManager.getCurrentAccount()
            val serverAddress = seafAccountManager.getServerAddress(currAccount)!!
            restApi.getRepoListSync(authToken, serverAddress).execute().body()!!
                    .filter { it.id == repoHash }
                    .forEach { localRepo = it }

        }
        localRepo!!.mtime = 0
        saveRepoInstance(localRepo!!)

        if (path.isNotEmpty())
            createNewSyncRecursive(authToken, localRepo!!, path, storage)
        val itemToSync = Item()
        itemToSync.name = name
        itemToSync.path = path + "/"
        itemToSync.type = type
        itemToSync.repoId = localRepo!!.dbId
        itemToSync.isRootSync = true
        itemToSync.synced = true
        itemToSync.storage = storage
        saveItemInstance(itemToSync)
        localRepo!!.mtime = 0
        saveRepoInstance(localRepo!!)
        return itemToSync
    }

    fun deleteParentsIfUnsyncedRecursive(repo: Repo, item: Item) {
        val pathWithoutTrailingSlash = item.path!!.substringBeforeLast("/")
        val pathParent = pathWithoutTrailingSlash.substringBeforeLast("/")
        val nameParent = pathWithoutTrailingSlash.substringAfterLast("/")
        val parent = getFile(repo.dbId.toString(), pathParent + "/", nameParent)
        deleteItem(repo, item)
        item.synced = false
        val childs = getItemsForRepo(repo, pathParent + "/")
        if (parent != null)
            cache.writeDirectoryList(currAccount().name, repo.id!!,
                    (parent.path!! + parent.name).removeSuffix("/"),
                    childs.values.toList(), false)
        else {
            val dirList = cache.readDirectoryList(currAccount().name, repo.id!!, "")
            dirList.filter { it.name == item.name }.forEach {
                it.synced = false
                it.storage = ""
                it.isRootSync = false
            }

            cache.writeDirectoryList(currAccount().name, repo.id!!, "/",
                    dirList, false)
        }
        if (childs.isNotEmpty())
            return
        if (pathParent.isNotEmpty())
            deleteParentsIfUnsyncedRecursive(repo, parent!!)

    }

    fun createNewSyncRecursive(authToken: String, repo: Repo, path: String,
                               storage: String): Item {


        val parentPath = path.substringBeforeLast("/")
        val name = path.substringAfterLast("/")

        if (parentPath.isNotEmpty()) {
            createNewSyncRecursive(authToken, repo, parentPath, storage)
        }
        val directory = Item()
        directory.repoId = repo.dbId
        directory.name = name
        directory.path = parentPath + "/"
        directory.type = "dir"
        directory.storage = storage
        var localDirectory = getFile(repo, directory)
        if (localDirectory == null) {
            localDirectory = directory

        }
        directory.mtime = 0
        saveItemInstance(directory)
        return localDirectory

    }
}
