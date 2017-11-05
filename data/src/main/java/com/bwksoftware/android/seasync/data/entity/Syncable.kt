package com.bwksoftware.android.seasync.data.entity

/**
 * Created by anselm.binninger on 27/10/2017.
 */
open class Syncable {
    open var synced: Boolean? = null

    open var storage: String? = null

    open var path: String? = "/"
    open var dbId: Long? = null
    open var isRootSync: Boolean? = null

}