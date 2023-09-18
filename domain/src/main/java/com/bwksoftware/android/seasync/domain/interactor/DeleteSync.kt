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
class DeleteSync @Inject
internal constructor(val repository: Repository, threadExecutor: ThreadExecutor,
                     postExecutionThread: PostExecutionThread) : UseCase<ItemTemplate, DeleteSync.Params>(
        threadExecutor, postExecutionThread) {

    override fun buildUseCaseObservable(params: Params): Observable<ItemTemplate> {
        return repository.unsyncItem(params.repoID,params.directory,params.name)
    }

    class Params(val repoID: String, val directory: String, val name: String)

}
