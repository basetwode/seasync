package com.bwksoftware.android.seasync.presentation.mapper

import com.bwksoftware.android.seasync.domain.AvatarTemplate
import com.bwksoftware.android.seasync.domain.ItemTemplate
import com.bwksoftware.android.seasync.presentation.model.Avatar
import com.bwksoftware.android.seasync.presentation.model.DirectoryItem
import com.bwksoftware.android.seasync.presentation.model.FileItem
import com.bwksoftware.android.seasync.presentation.model.Item
import javax.inject.Inject


class ModelMapper @Inject constructor() {
    fun transformAvatar(avatar: AvatarTemplate): Avatar {
        return Avatar(avatar.url)
    }

    fun transformItem(item: ItemTemplate): Item {
        return when (item.type) {
            "file" -> FileItem(item.id, item.name, item.mtime, item.size,item.storage,item.synced)
            "dir" -> DirectoryItem(item.id, item.name, item.mtime, item.size, item.storage, item.synced)
            else -> Item(item.id, item.name, item.mtime, item.size, Item.UNKNOWN, item.storage, item.synced)
        }
    }

    fun transformItemList(itemList: List<ItemTemplate>): List<Item> {
        return itemList.mapTo(ArrayList<Item>()) { transformItem(it) }
    }

}