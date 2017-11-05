package com.bwksoftware.android.seasync.data.cache

import android.content.Context
import android.util.Log
import com.bwksoftware.android.seasync.data.entity.Avatar
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*
import javax.inject.Inject


class DiskCache @Inject constructor(context: Context) {

    private val cacheDir: File = context.cacheDir
    private val gson = Gson()
    private var sharedPreferences: SharedPrefsController = SharedPrefsController(context)

    fun readDirectoryList(account: String, repoId: String, path: String): List<Item> {


        val accountDir = File(cacheDir, account)
        val repoDir = File(accountDir, repoId)
        val directoryFile = File(repoDir, path + SEASYNC_CACHE_EXTENSION)
        val listType = object : TypeToken<ArrayList<Item>>() {}.type

        return gson.fromJson<List<Item>>(directoryFile.readText(), listType)

    }

    fun writeDirectoryList(account: String, repoId: String, path: String,
                           directoryContent: List<Item>, merge: Boolean) {
        val accountDir = File(cacheDir, account)
        val repoDir = File(accountDir, repoId)
        val directoryFile = File(repoDir, path + SEASYNC_CACHE_EXTENSION)
        val directory = File(directoryFile.absolutePath.substringBeforeLast("/"))
        directory.mkdirs()

        val listType = object : TypeToken<ArrayList<Item>>() {}.type
        var localList: List<Item>? = null
        if (directoryFile.exists() && merge) {
            localList = gson.fromJson<List<Item>>(directoryFile.readText(), listType)
            directoryFile.writeText(
                    gson.toJson(mergeDirectoryLists(account, repoId, localList, directoryContent)))
        } else
            directoryFile.writeText(gson.toJson(directoryContent))
        setLastUpdateTime(System.currentTimeMillis())
    }

    fun mergeDirectoryLists(account: String, repoId: String, cachedList: List<Item>,
                            remoteList: List<Item>): List<Item> {
        val cachedMap = cachedList.associateBy({ it.name }, { it })
        val remoteMap = remoteList.associateBy({ it.name }, { it })
        val mergedList = ArrayList<Item>()
        val invalidFiles = LinkedList<Item>()
        val remainingFiles = cachedMap.toMutableMap()

        for (item in remoteList) {
            val localItem = cachedMap[item.name]
            val remoteItem = remoteMap[item.name]
            remainingFiles.remove(item.name)

            if (localItem != null && remoteItem != null) {
                // If both exists merge them
                if (remoteItem.mtime != localItem.mtime) {
                    if (localItem.synced == true) {
                        //if the item is synced we merge them anyway
                        remoteItem.synced = localItem.synced
                        remoteItem.isRootSync = localItem.isRootSync
                        remoteItem.storage = localItem.storage
                    } else if (localItem.isCached)
                        invalidFiles.add(localItem)
                    //Do not merge since the local item is outdated
                    mergedList.add(remoteItem)
                } else {
                    remoteItem.isCached = localItem.isCached
                    remoteItem.isRootSync = localItem.isRootSync
                    remoteItem.synced = localItem.synced
                    remoteItem.storage = localItem.storage ?: ""
                    mergedList.add(remoteItem)
                }
            } else if (localItem != null && localItem.isCached) {
                invalidFiles.add(localItem)
                //Ignoe the local item since it is no longer on remote
            } else if (remoteItem != null && localItem == null) {
                //New item! add it
                mergedList.add(remoteItem)
            }
        }
        deleteFilesForItems(account, repoId, invalidFiles + remainingFiles.values)
        return mergedList
    }

    fun deleteFilesForItems(account: String, repoId: String, items: List<Item>) {
        items
                .map { "${it.storage}/$account/$repoId/${it.path}/${it.name}" }
                .map { File(it) }
                .forEach {
                    Log.d("DiskCache", "Deleting file ${it.absolutePath} since it became invalid!")
                    it.delete()
                }
    }

    fun readRepoList(account: String): List<Repo> {
        val accountDir = File(cacheDir, account)
        val reposFile = File(accountDir, "repos$SEASYNC_CACHE_EXTENSION")
        val listType = object : TypeToken<ArrayList<Repo>>() {}.type
        return gson.fromJson<List<Repo>>(reposFile.readText(), listType)
    }

    fun writeRepoList(account: String, repos: List<Repo>) {
        val accountDir = File(cacheDir, account)
        val reposFile = File(accountDir, "repos$SEASYNC_CACHE_EXTENSION")
        val directory = File(reposFile.absolutePath.substringBeforeLast("/"))
        directory.mkdirs()
        reposFile.writeText(gson.toJson(repos))
        setLastUpdateTime(System.currentTimeMillis())
    }

    fun writeAvatar(userName: String, avatar: Avatar) {
        val avatarFile = File(cacheDir, userName + SEASYNC_CACHE_AVATAR_EXTENSION)
        avatarFile.writeText(gson.toJson(avatar))
    }

    fun readAvatar(userName: String): Avatar {
        val avatarFile = File(cacheDir, userName + SEASYNC_CACHE_AVATAR_EXTENSION)
        val type = object : TypeToken<Avatar>() {}.type
        return gson.fromJson<Avatar>(avatarFile.readText(), type)
    }

    fun setLastUpdateTime(time: Long) {
        sharedPreferences.setPreference(SharedPrefsController.Preference.CACHE_LAST_UPDATE,
                time.toString())
    }

    fun getLastUpdateTime(): Long {
        return sharedPreferences.getPreferenceValue(
                SharedPrefsController.Preference.CACHE_LAST_UPDATE).toLong()
    }

    fun isDirectoryExpired(account: String, repoId: String, path: String): Boolean {
        val accountDir = File(cacheDir, account)
        val repoDir = File(accountDir, repoId)
        val directoryFile = File(repoDir, path + SEASYNC_CACHE_EXTENSION)
        return ((System.currentTimeMillis() - directoryFile.lastModified()) > EXPIRATION_TIME)
    }

    fun isAvatarExpired(userName: String): Boolean {
        val avatarFile = File(cacheDir, userName + SEASYNC_CACHE_AVATAR_EXTENSION)
        return ((System.currentTimeMillis() - avatarFile.lastModified()) > EXPIRATION_TIME)
    }

    fun isRepoExpired(account: String): Boolean {
        val accountDir = File(cacheDir, account)
        val reposFile = File(accountDir, "repos$SEASYNC_CACHE_EXTENSION")
        return ((System.currentTimeMillis() - reposFile.lastModified()) > EXPIRATION_TIME)
    }


    fun isAvatarCached(userName: String): Boolean {
        val avatarFile = File(cacheDir, userName + SEASYNC_CACHE_AVATAR_EXTENSION)
        return avatarFile.exists()
    }

    fun isRepoCached(account: String): Boolean {
        val accountDir = File(cacheDir, account)
        val reposFile = File(accountDir, "repos$SEASYNC_CACHE_EXTENSION")
        return reposFile.exists()
    }

    fun isDirectoryCached(account: String, repoId: String, path: String): Boolean {
        val accountDir = File(cacheDir, account)
        val repoDir = File(accountDir, repoId)
        val directoryFile = File(repoDir, path + SEASYNC_CACHE_EXTENSION)
        return directoryFile.exists()
    }

    companion object {
        val EXPIRATION_TIME = 1 * 60 * 1000
        val SEASYNC_CACHE_EXTENSION = ".seasynctxt"
        val SEASYNC_CACHE_AVATAR_EXTENSION = ".avatar"
    }

}