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

package com.bwksoftware.android.seasync.presentation.components

import android.app.Activity
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bwksoftware.android.seasync.data.authentication.SeafAccountManager
import com.bwksoftware.android.seasync.data.utils.FileUtils
import com.bwksoftware.android.seasync.presentation.R
import com.bwksoftware.android.seasync.presentation.model.BottomSheetItem
import com.bwksoftware.android.seasync.presentation.model.DirectoryItem
import com.bwksoftware.android.seasync.presentation.model.Item
import com.bwksoftware.android.seasync.presentation.view.fragment.BaseFragment
import com.bwksoftware.android.seasync.presentation.view.fragment.DirectoryFragment
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import java.net.URLEncoder

/**
 * Created by ansel on 11/7/2017.
 */
class BottomSheet(item: BottomSheetItem, view: View, fragment: BaseFragment,
                  repoName: String, repoId: String, directory: String,
                  private val seafAccountManager: SeafAccountManager, address: String) {
    val options: LinearLayout = view.findViewById(R.id.bottom_sheet_sync_options)

    val openButton: LinearLayout = view.findViewById(R.id.bottom_sheet_open)
    val syncButton: LinearLayout = view.findViewById(R.id.bottom_sheet_sync)
    val syncButtonImg: ImageView = view.findViewById(R.id.bottom_sheet_sync_img)
    val syncButtonText: TextView = view.findViewById(R.id.bottom_sheet_sync_text)
    val title: TextView = view.findViewById(R.id.bottom_sheet_title)
    val details: TextView = view.findViewById(R.id.bottom_sheet_details)
    val syncExternalStorage: LinearLayout = view.findViewById(R.id.bottom_sheet_sync_external)
    val syncInternalStorage: LinearLayout = view.findViewById(R.id.bottom_sheet_sync_internal)
    val localDetails: TextView = view.findViewById(R.id.bottom_sheet_local_details)
    val itemImage: ImageView = view.findViewById(R.id.bottom_sheet_image)
    val revealInFinder: LinearLayout = view.findViewById(R.id.bottom_sheet_reveal_finder)

    val activity = fragment.activity

    init {
        title.text = item.name
        if (item.synced && !item.isRootSync) {
            syncButton.isEnabled = false
            val filter = LightingColorFilter(Color.WHITE,
                    fragment.resources.getColor(R.color.disabledGreyDark))
            syncButtonImg.colorFilter = filter
            syncButtonText.setTextColor(fragment.resources.getColor(R.color.disabledGreyDark))
            syncButton.setBackgroundColor(fragment.resources.getColor(R.color.disabledGrey))
        }
        if (item.synced) {
            localDetails.text = "Location: ${item.storage}"
            syncButtonImg.setImageDrawable(fragment.resources.getDrawable(R.drawable.unsync))
            syncButtonText.text = "Unsync item"
        } else {
            syncButtonImg.setImageDrawable(fragment.resources.getDrawable(R.drawable.sync))
            syncButtonText.text = "Sync item"
        }
        if (item.isCached)
            localDetails.text = "Location: ${item.storage}"

        if (item.storage == activity.getExternalFilesDir(null).absolutePath)
            revealInFinder.visibility = View.VISIBLE

        revealInFinder.setOnClickListener {
            val attachedActivity = activity
            when (attachedActivity) {
                is DirectoryFragment.OnDirectoryClickedListener -> {
                    attachedActivity.onRevealClicked(fragment,
                            repoId,
                            repoName,
                            directory, item.storage, item.name!!)
                }
            }
        }

        details.text = FileUtils.readableFileSize(
                item.size!!) + ", " + FileUtils.translateCommitTime(item.mtime!! * 1000,
                activity.baseContext)
        if (FileUtils.isViewableImage(item.name!!)) {
            val dir = directory
            val repo = repoId

            val file = URLEncoder.encode(dir + "/" + item.name, "UTF-8")
            val url = FileUtils.getThumbnailUrl(address, repo, file, 100)
            ImageLoader.getInstance().displayImage(url, itemImage, getDisplayImageOptions())
        } else if (item.drawableId>0)
            itemImage.setImageDrawable(fragment.resources.getDrawable(item.drawableId))

    }


    fun getDisplayImageOptions(): DisplayImageOptions? =
            DisplayImageOptions.Builder()
                    .extraForDownloader(seafAccountManager.getCurrentAccountToken())
                    .delayBeforeLoading(50)
                    .resetViewBeforeLoading(true)
                    .showImageForEmptyUri(R.drawable.empty_profile)
                    .showImageOnFail(R.drawable.empty_profile)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .build()
}