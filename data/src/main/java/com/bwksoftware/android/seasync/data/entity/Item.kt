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

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*


class Item : Syncable() {
    @SerializedName("id")
    @Expose
    open var id: String? = null


    @SerializedName("modifier_name")
    @Expose
    open var modifierName: String? = null

    @SerializedName("modifier_email")
    @Expose
    open var modifierEmail: String? = null

    @SerializedName("permission")
    @Expose
    open var permission: String? = null

    @SerializedName("modifier_contact_email")
    @Expose
    open var modifierContactEmail: String? = null

    @SerializedName("type")
    @Expose
    open var type: String? = null

    @SerializedName("mtime")
    @Expose
    open var mtime: Long? = null

    @SerializedName("name")
    @Expose
    open var name: String? = null

    @SerializedName("size")
    @Expose
    open var size: Long = 0


    var parent: Item? = null
    open var repoId: Long? = null

    val childItems = LinkedList<Item>()
    var parentItem: Item? = null
    var isCached: Boolean = false

    fun areChildItemsSynced(mtime: Long): Boolean {
        var result = true
        for (child in childItems)
            result = if (result) mtime == child.mtime else false
        return result
    }

    fun setAndUpdateParent(parent: Item) {
        parentItem = parent
        parent.childItems.add(this)
    }

    companion object {
        fun copy(item: Item): Item {
            val newItem = Item()
            newItem.id = item.id
            newItem.modifierName = item.modifierName
            newItem.modifierEmail = item.modifierEmail
            newItem.permission = item.permission
            newItem.modifierContactEmail = item.modifierContactEmail
            newItem.type = item.type
            newItem.mtime = item.mtime
            newItem.name = item.name
            newItem.size = item.size
            newItem.path = item.path
            newItem.parent = item.parent
            newItem.synced = item.synced
            newItem.storage = item.storage
            newItem.repoId = item.repoId
            return newItem
        }
    }

}