package com.bwksoftware.android.seasync.presentation.components

import android.content.Context
import android.support.v7.widget.GridLayoutManager

/**
 * Created by anselm.binninger on 04/11/2017.
 */
class GridLayoutManager(context: Context, spanCount: Int) : GridLayoutManager(context,
        spanCount) {

    override fun getSpanSizeLookup(): SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return spanCount
            }

        }
    }
}