package com.bwksoftware.android.seasync.data.datastore

import android.content.Context
import android.net.ConnectivityManager
import com.bwksoftware.android.seasync.data.cache.DiskCache
import com.bwksoftware.android.seasync.data.net.RestApiImpl
import javax.inject.Inject


class DataStoreFactory @Inject constructor(val context: Context, val cache: DiskCache,
                                           val restAPI: RestApiImpl) {


    fun createRepoDataStore(account: String): DataStore {
        val isCached = cache.isRepoCached(account)
        return if (isCached && !isInternetAvailable())
            DiskDataStore(cache)
        else if (isCached && !cache.isRepoExpired(account)) {
            DiskDataStore(cache)
        } else
            CloudDataStore(cache, restAPI)
    }

    fun createDirectoryDataStore(account: String, repoId: String,
                                 path: String): DataStore {
        val isCached = cache.isDirectoryCached(account, repoId, path)
        return if (isCached && !isInternetAvailable())
            DiskDataStore(cache)
        else if (isCached && !cache.isDirectoryExpired(account,repoId,path)) {
            DiskDataStore(cache)
        } else
            CloudDataStore(cache, restAPI)
    }

    fun createAvatarDataStore(username: String): DataStore {
        val isCached = cache.isAvatarCached(username)
        return if (isCached && !isInternetAvailable())
            DiskDataStore(cache)
        else if (isCached && !cache.isAvatarExpired(username)) {
            DiskDataStore(cache)
        } else
            CloudDataStore(cache, restAPI)
    }


    fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null
    }
}