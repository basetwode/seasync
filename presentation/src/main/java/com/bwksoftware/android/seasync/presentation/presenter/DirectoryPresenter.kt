/*
 *    Copyright 2018 BWK Technik GbR
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.bwksoftware.android.seasync.presentation.presenter

import android.util.Log
import com.bwksoftware.android.seasync.data.authentication.Authenticator
import com.bwksoftware.android.seasync.domain.ItemTemplate
import com.bwksoftware.android.seasync.domain.interactor.*
import com.bwksoftware.android.seasync.presentation.mapper.ModelMapper
import com.bwksoftware.android.seasync.presentation.model.Item
import com.bwksoftware.android.seasync.presentation.view.views.DirectoryView
import javax.inject.Inject


class DirectoryPresenter @Inject constructor(val getDirectoryEntries: GetDirectoryEntries,
                                             val createSync: CreateSync,
                                             val deleteSync: DeleteSync,
                                             val cacheFile: CacheFile,
                                             val modelMapper: ModelMapper) {

    internal lateinit var directoryView: DirectoryView
    @Inject lateinit var authenticator: Authenticator


    fun getDirectoryEntries(accountName: String, repoId: String, directory: String) {

        val authToken = authenticator.getCurrentUserAuthToken(accountName, directoryView.activity())
        this.getDirectoryEntries.execute(
                DirectoryObserver(), GetDirectoryEntries.Params(authToken, repoId, directory))
    }

    fun fileClicked(position: Int, accountName: String, repoId: String, item: Item,
                    directory: String) {
        val authToken = authenticator.getCurrentUserAuthToken(accountName, directoryView.activity())
        cacheFile.execute(CacheFileObserver(position),
                CacheFile.Params(authToken, repoId, directory, item.name!!))
    }

    fun directoryLongClicked(position: Int, accountName: String, repoId: String, item: Item,
                             directory: String, storage: String,
                             isSynced: Boolean) {
        if (isSynced)
            deleteSync.execute(CreateDeleteSyncObserver(position),
                    DeleteSync.Params(repoId, directory, item.name!!))
        else {
            val authToken = authenticator.getCurrentUserAuthToken(accountName,
                    directoryView.activity())
            createSync.execute(CreateDeleteSyncObserver(position),
                    CreateSync.Params(authToken, repoId, directory, item.name!!,
                            storage, "dir"))
        }
    }

    fun fileLongClicked(position: Int, accountName: String, repoId: String, item: Item,
                        directory: String, storage: String,
                        isSynced: Boolean) {
        if (isSynced)
            deleteSync.execute(CreateDeleteSyncObserver(position),
                    DeleteSync.Params(repoId, directory, item.name!!))
        else {
            val authToken = authenticator.getCurrentUserAuthToken(accountName,
                    directoryView.activity())
            createSync.execute(CreateDeleteSyncObserver(position),
                    CreateSync.Params(authToken, repoId, directory, item.name!!,
                            storage, "file"))
        }
    }

    private inner class CreateDeleteSyncObserver(
            val position: Int) : DefaultObserver<ItemTemplate>() {
        override fun onNext(t: ItemTemplate) {
            Log.d("DirectoryPresenter", "Sync created")
            directoryView.updateItem(position, modelMapper.transformItem(t))
        }
    }

    private inner class CacheFileObserver(val position: Int) : DefaultObserver<ItemTemplate>() {

        override fun onComplete() {
            Log.d("AccountPresenter", "yolo complete")
        }

        override fun onError(exception: Throwable) {
            directoryView.showNoInternet()
            Log.d("AccountPresenter", "yolo error" + exception.localizedMessage)
        }

        override fun onNext(cachedItem: ItemTemplate) {
            val item = modelMapper.transformItem(cachedItem)
            directoryView.updateItem(position, item)
            directoryView.fileDownloadComplete(item)
            Log.d("DirectoryPresenter", "File download successful")
        }
    }

    private inner class DirectoryObserver : DefaultObserver<List<ItemTemplate>>() {

        override fun onComplete() {
            Log.d("AccountPresenter", "yolo complete")
        }

        override fun onError(exception: Throwable) {
            directoryView.showNoInternet()
            Log.d("AccountPresenter", "yolo error" + exception.localizedMessage)
        }

        override fun onNext(directoryEntries: List<ItemTemplate>) {
            directoryView.renderDirectoryEntries(modelMapper.transformItemList(directoryEntries))
        }
    }
}