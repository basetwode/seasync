package com.bwksoftware.android.seasync.presentation.view.adapter

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import com.bwksoftware.android.seasync.data.utils.FileUtils
import com.bwksoftware.android.seasync.presentation.R
import com.bwksoftware.android.seasync.presentation.components.TouchImageView
import com.bwksoftware.android.seasync.presentation.model.Item
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader


class ImageViewerAdapter(val context: Context,
                         val address: String,
                         val directory: String,
                         val repoId: String,
                         val account: String,
                         val token: String) : PagerAdapter() {

    private val mItems: ArrayList<Item> = ArrayList()

    fun setItems(newItems: List<Item>) {
        mItems.clear()
        mItems.addAll(newItems)
    }

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
        return view === `object` as RelativeLayout
    }

    override fun getCount(): Int {
        return mItems.size
    }


    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
        val item = mItems[position]
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewLayout = inflater.inflate(R.layout.fragment_imageviewerpage, container,
                false)
        val imgDisplay: TouchImageView = viewLayout.findViewById(R.id.imgDisplay)
        val btnClose: Button = viewLayout.findViewById(R.id.btnClose)

        val url = FileUtils.getThumbnailUrl(address, repoId, item.name!!, item.storage,directory,account,600)
        ImageLoader.getInstance().displayImage(url, imgDisplay, getDisplayImageOptions);

        (container as ViewPager).addView(viewLayout)
        return viewLayout
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)
    }

    var getDisplayImageOptions: DisplayImageOptions? =
            DisplayImageOptions.Builder()
                    .extraForDownloader(token)
                    .delayBeforeLoading(0)
                    .resetViewBeforeLoading(true)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .build()
}