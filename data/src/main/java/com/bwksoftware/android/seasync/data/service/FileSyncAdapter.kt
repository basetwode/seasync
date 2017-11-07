package com.bwksoftware.android.seasync.data.service

import android.accounts.Account
import android.accounts.AccountManager
import android.app.NotificationManager
import android.content.*
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.bwksoftware.android.seasync.data.R
import com.bwksoftware.android.seasync.data.authentication.SeafAccountManager
import com.bwksoftware.android.seasync.data.cache.DiskCache
import com.bwksoftware.android.seasync.data.datamanager.StorageManager
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import com.bwksoftware.android.seasync.data.entity.Syncable
import com.bwksoftware.android.seasync.data.net.RestApiImpl
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import java.io.File


class FileSyncAdapter constructor(val mContext: Context) : AbstractThreadedSyncAdapter(
        mContext, true) {

    var restApi = RestApiImpl(mContext)

    var prefs = SharedPrefsController(mContext)

    lateinit var storageManager: StorageManager

    var cache = DiskCache(mContext)


    private val accountManager = AccountManager.get(mContext)

    fun printFilesRecursive(filesDir: File) {
        for (strFile in filesDir.list()) {
            val file = File(filesDir, strFile)
            Log.d("File", file.absolutePath)
            if (file.isDirectory)
                printFilesRecursive(file)
        }
    }

    override fun onPerformSync(p0: Account?, p1: Bundle?, p2: String?, p3: ContentProviderClient?,
                               p4: SyncResult?) {
        Log.d("FileSyncService", "onPerformSync")
        storageManager = StorageManager(context, SeafAccountManager(context,
                SharedPrefsController(context)), restApi)

        storageManager.contentProviderClient = p3!!
        val serverAddress = AccountManager.get(context).getUserData(p0, "Server")
        val authToken = accountManager.blockingGetAuthToken(p0, "full_access",
                true)


        // retrieve list of repos and compare their modified time to local repos from db
        val repos = restApi.getRepoListSync(authToken, serverAddress).execute().body()
        printFilesRecursive(mContext.filesDir)
        if (repos == null)
            return
        val notificationMgr = mContext.getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context)
        notificationBuilder.setContentTitle("Synchronizing ${p0!!.name}")
        notificationBuilder.setContentText("Synchronization in progress")
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_round)
        notificationBuilder.setProgress(0, 0, true)
        notificationMgr.notify(0, notificationBuilder.build())

        for (remoteRepo in repos!!) {

            var localRepo = storageManager.getRepo(remoteRepo)
//            Log.d("FileSyncService", remoteRepo.id + " " + remoteRepo.name)
//            if (localRepo == null && remoteRepo.name == "Public") {
//                remoteRepo.synced = true
//                remoteRepo.storage = mContext.filesDir.absolutePath
//                storageManager.saveRepoInstance(remoteRepo)
//                localRepo = storageManager.getRepo(remoteRepo)
//                localRepo!!.mtime = 0
//                storageManager.saveRepoInstance(localRepo)
//
//            }

            if (localRepo != null) {
                Log.d("FileSyncService", localRepo.id + " " + localRepo.name)
                compareReposAndSync(p3!!, authToken, p0, serverAddress, localRepo, remoteRepo)
            } else {
                Log.d("FileSyncService", remoteRepo.name + " not marked for sync")
            }
        }
        context.sendBroadcast(Intent(OnBootReceiver.ACTION_RESTART_FILE_OBSERVER))
        notificationBuilder.setContentText("Synchronization complete")
                // Removes the progress bar
                .setProgress(0, 0, false)
        notificationMgr.notify(0, notificationBuilder.build())
    }

    fun compareReposAndSync(contentProviderClient: ContentProviderClient, authToken: String,
                            account: Account, serverAddress: String,
                            localRepo: Repo, remoteRepo: Repo) {
        if (localRepo.mtime!! != remoteRepo.mtime!!) {
            // Retrieve list of content from server
            val syncSuccess = syncDirectoryRecursive(contentProviderClient, authToken, account,
                    serverAddress,
                    localRepo,
                    localRepo, "/")
            if (syncSuccess) updateCreateItemIfSyncSuccessful(syncSuccess, localRepo,
                    remoteRepo.mtime!!, remoteRepo.size!!, "")
        }
    }

    fun syncDirectoryRecursive(contentProviderClient: ContentProviderClient, authToken: String,
                               account: Account, serverAddress: String,
                               parent: Syncable,
                               repo: Repo, path: String): Boolean {
        val localItemsForRepo = storageManager.getItemsForRepo(repo,
                path).toMutableMap()
        val remoteItemsForRepo = restApi.getDirectoryEntriesSync(authToken, serverAddress,
                repo.id!!,
                path).execute().body() ?: return false

        cache.writeDirectoryList(account.name, repo.id!!, path.removeSuffix("/"),
                remoteItemsForRepo, true)

        //TODO: add accountname to path and extract path building into a method in storagemgr
        // If we're in the root directory we have to check if the repo should be synced
        // if we're in a folder we have to check if the parent folder was marked to be synced.

        Log.d("FileSyncAdapter", "local items: " + localItemsForRepo.size.toString())
        var syncSuccessful = true

        for (remoteItem in remoteItemsForRepo) {
            remoteItem.storage = parent.storage
            remoteItem.path = path

            val localItem = storageManager.getFile(repo, remoteItem)

            //Remove item from to sync list
            localItemsForRepo.remove(remoteItem.name)

            val (itemToSync, type) = SyncType.determineType(remoteItem, localItem, parent, path,
                    repo.dbId!!)
            val item = itemToSync as Item

            when (type) {
                SyncType.DIR_SYNCED_AND_MODIFIED, SyncType.DIR_SYNCED_FRESH -> {
                    Log.d("FileSyncAdapter", type.toString() + " - " + item.path + item.name)
                    val directorySynced = syncDirectoryRecursive(contentProviderClient,
                            authToken, account, serverAddress, item, repo, path + item.name + "/")
                    syncSuccessful = if (syncSuccessful) directorySynced else false
                    updateCreateItemIfSyncSuccessful(directorySynced, item,
                            remoteItem.mtime!!,
                            remoteItem.size, remoteItem.id!!)
                }
                SyncType.FILE_SYNCED_AND_MODIFIED, SyncType.FILE_SYNCED_FRESH -> {

                    Log.d("FileSyncAdapter", type.toString() + " - " + item.path + item.name)
                    val itemSynced = storageManager.syncItem(authToken, repo, item,
                            remoteItem)
                    syncSuccessful = if (syncSuccessful) itemSynced else false
                    updateCreateItemIfSyncSuccessful(itemSynced, item,
                            remoteItem.mtime!!,
                            remoteItem.size, remoteItem.id!!)

                }
                SyncType.FILE_SYNCED, SyncType.DIR_SYNCED, SyncType.NOT_SYNCED -> {
                    Log.d("FileSyncAdapter", type.toString() + " - " + item.path + item.name)
                    //Do nothing
                }
            }

        }
        for ((_, itemToDelete) in localItemsForRepo) {
            storageManager.deleteItemRecursive(itemToDelete, repo)
        }
        return syncSuccessful
    }

    enum class SyncType {
        DIR_SYNCED,
        DIR_SYNCED_AND_MODIFIED,
        DIR_SYNCED_FRESH,
        FILE_SYNCED,
        FILE_SYNCED_FRESH,
        FILE_SYNCED_AND_MODIFIED,
        NOT_SYNCED;

        companion object {
            fun determineType(remoteItem: Item, localItem: Item?, parent: Syncable, path: String,
                              repoId: Long): List<Any> {
                return when {
                    localItem != null ->

                        if (localItem.type == "dir")
                            if (localItem.mtime != remoteItem.mtime)
                                listOf(localItem, DIR_SYNCED_AND_MODIFIED)
                            else
                                listOf(localItem, DIR_SYNCED)
                        else
                            if (localItem.mtime != remoteItem.mtime)
                                listOf(localItem, FILE_SYNCED_AND_MODIFIED)
                            else
                                listOf(localItem, FILE_SYNCED)

                    parent.synced!! -> {

                        remoteItem.storage = parent.storage
                        remoteItem.synced = true
                        remoteItem.repoId = repoId
                        if (remoteItem.type == "dir")
                            listOf(remoteItem, DIR_SYNCED_FRESH)
                        else
                            listOf(remoteItem, FILE_SYNCED_FRESH)
                    }
                    else -> listOf(remoteItem, NOT_SYNCED)
                }
            }
        }
    }


    fun updateCreateItemIfSyncSuccessful(syncSuccess: Boolean, item: Syncable, newMtime: Long,
                                         newSize: Long, newHash: String) {
        when (item) {
            is Item -> {
                if (syncSuccess) {
                    item.mtime = newMtime
                    item.size = newSize
                    item.id = newHash
                    storageManager.saveItemInstance(item)
                }
            }
            is Repo -> {
                if (syncSuccess) {
                    item.mtime = newMtime
                    item.size = newSize
                    storageManager.saveRepoInstance(item)
                }
            }
        }

    }

    //TODO: When a user selects a folder, repo or file to be synced, the ui has to
    // create all necessary db entries.
    // The syncservice will then be triggered and download things
}