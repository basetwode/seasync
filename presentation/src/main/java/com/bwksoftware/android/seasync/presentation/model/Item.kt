package com.bwksoftware.android.seasync.presentation.model


open class Item(val id: String?, val name: String?, val mtime: Long?, val size: Long,
                val type: Int, var storage: String, var synced: Boolean, var isCached: Boolean,
                var isRootSync:Boolean) {
    companion object {
        val TYPE_FILE = 0
        val TYPE_DIRECTORY = 1
        val UNKNOWN = -1
    }

}