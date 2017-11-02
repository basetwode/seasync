package com.bwksoftware.android.seasync.data.datastore

import com.bwksoftware.android.seasync.data.cache.DiskCache
import com.bwksoftware.android.seasync.domain.ItemTemplate
import com.bwksoftware.android.seasync.domain.RepoTemplate
import io.reactivex.Observable

/**
 * Created by anselm.binninger on 02/11/2017.
 */
class DiskDataStore(val cache: DiskCache) : DataStore {
    override fun getRepoList(authToken: String): Observable<List<RepoTemplate>> {
        return Any() as Observable<List<RepoTemplate>>

    }

    override fun getDirectoryEntries(authToken: String, repoId: String,
                                     directory: String): Observable<List<ItemTemplate>> {
        return Any() as Observable<List<ItemTemplate>>
    }

}