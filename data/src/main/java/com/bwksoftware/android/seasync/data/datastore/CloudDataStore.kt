package com.bwksoftware.android.seasync.data.datastore

import com.bwksoftware.android.seasync.data.cache.DiskCache
import com.bwksoftware.android.seasync.data.entity.Avatar
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import com.bwksoftware.android.seasync.data.net.RestApiImpl
import io.reactivex.Observable


class CloudDataStore(val cache: DiskCache, val restAPI: RestApiImpl) : DataStore {
    override fun getAvatar(username: String, serverAddress: String,
                           authToken: String): Observable<Avatar> {
        return restAPI.getAvatar(username,serverAddress, authToken).doOnNext {
            cache.writeAvatar(username, it)
        }
    }

    override fun getRepoList(account: String, serverAddress: String,
                             authToken: String): Observable<List<Repo>> {
        return restAPI.getRepoList(authToken,serverAddress).doOnNext {
            cache.writeRepoList(account, it)
        }
    }

    override fun getDirectoryEntries(account: String, serverAddress: String, authToken: String,
                                     repoId: String,
                                     directory: String): Observable<List<Item>> {
        return restAPI.getDirectoryEntries(authToken,serverAddress, repoId, directory).doOnNext {
            cache.writeDirectoryList(account, repoId, directory, it, true)
        }
    }

}