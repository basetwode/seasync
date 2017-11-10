package com.bwksoftware.android.seasync.presentation.presenter

import com.bwksoftware.android.seasync.data.authentication.Authenticator
import com.bwksoftware.android.seasync.presentation.mapper.ModelMapper
import com.bwksoftware.android.seasync.presentation.view.views.DirectoryView
import javax.inject.Inject

class DownloadPresenter @Inject constructor(val modelMapper: ModelMapper) {

    internal lateinit var directoryView: DirectoryView
    @Inject lateinit var authenticator: Authenticator



}