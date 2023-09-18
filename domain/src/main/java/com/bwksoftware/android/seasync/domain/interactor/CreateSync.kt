package com.bwksoftware.android.seasync.domain.interactor

import com.bwksoftware.android.seasync.domain.ItemTemplate
import com.bwksoftware.android.seasync.domain.executor.PostExecutionThread
import com.bwksoftware.android.seasync.domain.executor.ThreadExecutor
import com.bwksoftware.android.seasync.domain.repository.Repository
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

/**
 * This class is an implementation of [UseCase] that represents a use case for
 * retrieving a collection of all [WorkoutTemplate].
 */
class CreateSync @Inject
internal constructor(val repository: Repository, threadExecutor: ThreadExecutor,
                     postExecutionThread: PostExecutionThread) : UseCase<ItemTemplate, CreateSync.Params>(
        threadExecutor, postExecutionThread) {

    override fun buildUseCaseObservable(params: Params): Observable<ItemTemplate> {
        return repository.syncItem(params.authToken, params.repoID, params.directory, params.name,
                params.storage, params.type)
    }

    class Params(val authToken: String, val repoID: String, val directory: String, val name: String,
                 val storage: String, val type: String)

}
