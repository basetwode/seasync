package com.bwksoftware.android.seasync.data.datastore

import com.bwksoftware.android.seasync.data.cache.DiskCache
import com.bwksoftware.android.seasync.data.entity.Avatar
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import io.reactivex.rxjava3.core.Observable


class DiskDataStore(val cache: DiskCache) : DataStore {
    override fun getAvatar(username: String,serverAddress:String, authToken: String): Observable<Avatar> {
        return Observable.create<Avatar> { e ->
            run {
                e.onNext(cache.readAvatar(username))
            }
        }
    }

    override fun getRepoList(account: String,serverAddress:String, authToken: String): Observable<List<Repo>> {
        return Observable.create<List<Repo>> { e ->
            run {
                e.onNext(
                        cache.readRepoList(account))
            }
        }
    }

    override fun getDirectoryEntries(account: String,serverAddress:String, authToken: String, repoId: String,
                                     directory: String): Observable<List<Item>> {
        return Observable.create<List<Item>> { e ->
            run {
                e.onNext(cache.readDirectoryList(account, repoId, directory))
            }
        }
    }


}
