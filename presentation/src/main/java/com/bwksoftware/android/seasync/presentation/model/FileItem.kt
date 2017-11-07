package com.bwksoftware.android.seasync.presentation.model

/**
 * Created by anselm.binninger on 12/10/2017.
 */
class FileItem(id: String?, name: String?, mtime: Long?, size: Long, storage: String,
               synced: Boolean
               , isCached: Boolean, isRootSync: Boolean) : Item(id,
        name, mtime,
        size, TYPE_FILE, storage, synced, isCached,isRootSync,0) {

}