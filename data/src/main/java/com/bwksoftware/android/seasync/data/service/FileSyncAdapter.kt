package com.bwksoftware.android.seasync.data.service

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.util.Log
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


    private val accountManager = AccountManager.get(mContext)

    fun printFilesRecursive(filesDir: File) {
        Log.d("File", filesDir.absolutePath)

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
        storageManager = StorageManager(mContext, null, restApi)
        storageManager.setAccount(p0!!)
//        storageManager.contentProviderClient = p3!!
        val authToken = accountManager.blockingGetAuthToken(p0, "full_access",
                true)

        // retrieve list of repos and compare their modified time to local repos from db
        val repos = restApi.getRepoListSync(authToken).execute().body()
        printFilesRecursive(mContext.filesDir)
        for (remoteRepo in repos!!) {

            var localRepo = storageManager.getRepo(remoteRepo)
            Log.d("FileSyncService", remoteRepo.id + " " + remoteRepo.name)
            if (localRepo == null && remoteRepo.name == "Public") {
                remoteRepo.synced = true
                remoteRepo.storage = mContext.filesDir.absolutePath
                storageManager.saveRepoInstance(remoteRepo)
                localRepo = storageManager.getRepo(remoteRepo)
                localRepo!!.mtime = 0
                storageManager.saveRepoInstance(localRepo)

            }

            if (localRepo != null) {
                Log.d("FileSyncService", localRepo.id + " " + localRepo.name)

                compareReposAndSync(p3!!, authToken, localRepo, remoteRepo)
            } else {
                Log.d("FileSyncService", remoteRepo.name + " not marked for sync")
            }

        }
        // if not equal retrieve list of entries for that repo and compare to local files
    }

    fun compareReposAndSync(contentProviderClient: ContentProviderClient, authToken: String,
                            localRepo: Repo, remoteRepo: Repo) {
        if (localRepo.mtime!! != remoteRepo.mtime!!) {
            // Retrieve list of content from server
            syncDirectoryRecursive(contentProviderClient, authToken, localRepo, localRepo, "/")
        }
    }

    fun syncDirectoryRecursive(contentProviderClient: ContentProviderClient, authToken: String,
                               parent: Syncable,
                               repo: Repo, path: String): Boolean {
        val localItemsForRepo = storageManager.getItemsForRepo(repo, "dir").toMutableList()
        val remoteItemsForRepo = restApi.getDirectoryEntriesSync(authToken, repo.id!!,
                path).execute().body()
        //TODO: check if remoteITemsForRepos is not null

        //TODO: update repo instance if sync successful

        //TODO: add accountname to path and extract path building into a method in storagemgr
        // If we're in the root directory we have to check if the repo should be synced
        // if we're in a folder we have to check if the parent folder was marked to be synced.


        Log.d("FileSyncAdapter", repo.toString())
        var syncSuccessful = true

        for (remoteItem in remoteItemsForRepo!!) {
            remoteItem.storage = parent.storage
            //TODO: we need to remove every item we've checked from the localitem list so we can check
            //TODO if there are any items remaining which then have to be deleted as they no longer
            //TODO exist remotely
            val localItem = storageManager.getFile(repo.dbId!!, path, remoteItem.name!!)
            if (localItem != null) {
                // if the folder locally exists and is on remote
                // we need check its contents whether they are synced if their mtime differs
                if (localItem.type == "dir") {
                    if (localItem.mtime != remoteItem.mtime) {
                        val directorySynced = syncDirectoryRecursive(contentProviderClient,
                                authToken, localItem, repo, path + "/" + localItem.name)
                        syncSuccessful = if (syncSuccessful) directorySynced else false
                        updateCreateItemIfSyncSuccessfull(directorySynced, remoteItem,
                                remoteItem.mtime!!,
                                remoteItem.size)
                    }

                } else {
                    // if its a file and exists locally we have to compare the time stamps
                    if (localItem.mtime != remoteItem.mtime) {
                        Log.d("FileSyncAdapter", "File exists but time differs")
                        val itemSynced = storageManager.syncItem(authToken, repo, localItem,
                                remoteItem)
                        syncSuccessful = if (syncSuccessful) itemSynced else false
                        updateCreateItemIfSyncSuccessfull(itemSynced, remoteItem,
                                remoteItem.mtime!!,
                                remoteItem.size)
                    } else {
                        Log.d("FileSyncAdapter", "File exists and is synced")
                    }
                }
            } else if (parent.synced!!) {
                // we don't have this file and all files of this folder should be synced
                // -> we need to download it
                // test if file is a dir or a file and sync
                remoteItem.path = path
                remoteItem.storage = parent.storage
                remoteItem.synced = true

                if (remoteItem.type == "dir") {
                    val directorySynced = syncDirectoryRecursive(contentProviderClient, authToken,
                            remoteItem, repo, path + "/" + remoteItem.name)
                    syncSuccessful = if (syncSuccessful) directorySynced else false
                    updateCreateItemIfSyncSuccessfull(directorySynced, remoteItem,
                            remoteItem.mtime!!,
                            remoteItem.size)
                } else {
                    val itemSynced = storageManager.syncItem(authToken, repo, localItem, remoteItem)
                    syncSuccessful = if (syncSuccessful) itemSynced else false
                    updateCreateItemIfSyncSuccessfull(itemSynced, remoteItem, remoteItem.mtime!!,
                            remoteItem.size)
                }
            } else {
                // Nothing, repo or directory are not marked to be synced, thus we ignore it
            }

        }
        return syncSuccessful
    }

    fun updateCreateItemIfSyncSuccessfull(syncSuccess: Boolean, item: Item, newMtime: Long,
                                          newSize: Long) {
        if (syncSuccess) {
            item.mtime = newMtime
            item.size = newSize
            storageManager.saveItemInstance(item)
        }
    }

    // TODO: create a synctask which downloads/uploads a file and creates/updates db entries accordingly
    // -

    //TODO: When a user selects a folder, repo or file to be synced, the ui has to
    // create all necessary db entries.
    // The syncservice will then be triggered and download things
}