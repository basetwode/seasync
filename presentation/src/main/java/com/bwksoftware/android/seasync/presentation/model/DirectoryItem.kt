package com.bwksoftware.android.seasync.presentation.model

import com.bwksoftware.android.seasync.presentation.R


class DirectoryItem(id: String?, name: String?, mtime: Long?, size: Long, storage: String,
                    synced: Boolean, isCached: Boolean, isRootSync: Boolean) : Item(id, name, mtime,
        size, TYPE_DIRECTORY, storage, synced, isCached, isRootSync, R.drawable.folder)