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

package com.bwksoftware.android.seasync.presentation.model

import com.bwksoftware.android.seasync.presentation.R

class BottomSheetItem(val storage: String, val name: String, val size: Long, val synced: Boolean,
                      val isRootSync: Boolean, val isCached: Boolean, val mtime: Long,
                      val drawableId: Int) {

    constructor(repo: Repo) : this(repo.storage, repo.name!!, repo.size!!, repo.synced,
            repo.synced, false, repo.mtime!!, R.drawable.repo)

    constructor(item: Item) : this(item.storage, item.name!!, item.size, item.synced,
            item.isRootSync, item.isCached, item.mtime!!, item.drawableId)

}