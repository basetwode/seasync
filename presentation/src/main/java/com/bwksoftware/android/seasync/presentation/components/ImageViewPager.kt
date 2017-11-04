package com.bwksoftware.android.seasync.presentation.components

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import it.sephiroth.android.library.imagezoom.ImageViewTouch


class ImageViewPager : ViewPager {

    private var previousPosition: Int = 0

    private var onPageSelectedListener: OnPageSelectedListener? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    fun setOnPageSelectedListener(listener: OnPageSelectedListener) {
        onPageSelectedListener = listener
    }

    override fun canScroll(v: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        return if (v is ImageViewTouch) {
            (v as ImageViewTouch).canScroll(dx)
        } else {
            super.canScroll(v, checkV, dx, x, y)
        }
    }

    interface OnPageSelectedListener {

        fun onPageSelected(position: Int)

    }

    private fun init() {
        previousPosition = currentItem

        setOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (onPageSelectedListener != null) {
                    onPageSelectedListener!!.onPageSelected(position)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_SETTLING && previousPosition != currentItem) {
                    try {
                        val imageViewTouch = findViewWithTag<View>(
                                VIEW_PAGER_OBJECT_TAG + currentItem) as ImageViewTouch
                        imageViewTouch?.zoomTo(1f, 300)

                        previousPosition = currentItem
                    } catch (ex: ClassCastException) {
                        Log.e(TAG, "This view pager should have only ImageViewTouch as a children.",
                                ex)
                    }

                }
            }
        })
    }

    companion object {

        private val TAG = "ImageViewTouchViewPager"
        val VIEW_PAGER_OBJECT_TAG = "image#"
    }
}