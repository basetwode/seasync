package com.bwksoftware.android.seasync.domain.interactor

import com.bwksoftware.android.seasync.domain.ItemTemplate
import com.bwksoftware.android.seasync.domain.executor.PostExecutionThread
import com.bwksoftware.android.seasync.domain.executor.ThreadExecutor
import com.bwksoftware.android.seasync.domain.repository.Repository
import io.reactivex.Observable
import javax.inject.Inject

/**
 * This class is an implementation of [UseCase] that represents a use case for
 * retrieving a collection of all [WorkoutTemplate].
 */
class CacheFile @Inject
internal constructor(val repository: Repository, threadExecutor: ThreadExecutor,
                     postExecutionThread: PostExecutionThread) : UseCase<ItemTemplate, CacheFile.Params>(
        threadExecutor, postExecutionThread) {

    override fun buildUseCaseObservable(params: Params): Observable<ItemTemplate> {
        return repository.cacheFile(params.authToken, params.repoID, params.directory,
                params.fileName)
    }

    class Params(val authToken: String, val repoID: String, val directory: String,
                 val fileName: String)

}