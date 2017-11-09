package com.bwksoftware.android.seasync.presentation.utils

import android.content.Context
import com.bwksoftware.android.seasync.presentation.R

/**
 * Created by anselm.binninger on 09/11/2017.
 */
class IconUtils {
    companion object {
        fun getIconFromName(context: Context, name: String): Int {
            val extension = name.substringAfterLast(".")
            val resId = context.resources.getIdentifier("drawable/$extension", null,
                    context.packageName)
            return if (resId > 0)
                resId
            else R.drawable._blank
        }

    }
}