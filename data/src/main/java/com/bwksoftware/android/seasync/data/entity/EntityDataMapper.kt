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

package com.bwksoftware.android.seasync.data.entity

import com.bwksoftware.android.seasync.data.datamanager.StorageManager
import com.bwksoftware.android.seasync.domain.AccountTemplate
import com.bwksoftware.android.seasync.domain.AvatarTemplate
import com.bwksoftware.android.seasync.domain.ItemTemplate
import com.bwksoftware.android.seasync.domain.RepoTemplate
import javax.inject.Inject


class EntityDataMapper @Inject constructor(val storageManager: StorageManager) {


    fun transformAccountToken(account: Account): AccountTemplate {
        return AccountTemplate(account.token!!, account.username, account.imgUrl)
    }

    fun transformAvatar(avatar: Avatar): AvatarTemplate {
        return AvatarTemplate(avatar.url!!.replace("http://", "https://"))
    }

    fun transformRepo(repo: Repo): RepoTemplate {
        return RepoTemplate(repo.id, repo.name, repo.permission, repo.owner, repo.encrypted,
                repo.mtime, repo.size, repo.synced ?:false, repo.storage ?: "")
    }

    fun transformRepoList(repoList: List<Repo>): List<RepoTemplate> {
        return repoList.mapTo(ArrayList()) { transformRepo(it) }
    }

    fun transformItemList(itemList: List<Item>, repoId: String, path: String): List<ItemTemplate> {
        return itemList.mapTo(ArrayList()) { transformItem(it, repoId, path) }
    }

    fun transformItem(item: Item, repoId: String, path: String): ItemTemplate {

        //TODO: fix for cached files since they have props too
        return ItemTemplate(item.id, item.type, item.name, item.mtime, item.size,
                item.storage ?: "", item.synced ?: false, item.isCached,
                item.isRootSync ?: false)
    }
}
