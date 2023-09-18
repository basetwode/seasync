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

package com.bwksoftware.android.seasync.presentation.view.activity

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.core.content.FileProvider
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import com.bwksoftware.android.seasync.data.provider.FileRepoContract
import com.bwksoftware.android.seasync.data.utils.FileUtils
import com.bwksoftware.android.seasync.presentation.App
import com.bwksoftware.android.seasync.presentation.R
import com.bwksoftware.android.seasync.presentation.internal.di.components.ActivityComponent
import com.bwksoftware.android.seasync.presentation.internal.di.modules.ActivityModule
import com.bwksoftware.android.seasync.presentation.model.Account
import com.bwksoftware.android.seasync.presentation.model.NavBaseItem
import com.bwksoftware.android.seasync.presentation.navigation.Navigator
import com.bwksoftware.android.seasync.presentation.presenter.AccountPresenter
import com.bwksoftware.android.seasync.presentation.view.adapter.AccountAdapter
import com.bwksoftware.android.seasync.presentation.view.fragment.AddAccountFragment
import com.bwksoftware.android.seasync.presentation.view.fragment.BaseFragment
import com.bwksoftware.android.seasync.presentation.view.fragment.DirectoryFragment
import com.bwksoftware.android.seasync.presentation.view.fragment.ReposFragment
import com.bwksoftware.android.seasync.presentation.view.views.AccountView
import java.io.File
import javax.inject.Inject
import android.accounts.Account as AndroidAccount


class AccountActivity : AppCompatActivity(), AccountView, AccountAdapter.OnItemClickListener, AddAccountFragment.OnAddAccountListener, ReposFragment.OnRepoClickedListener, DirectoryFragment.OnDirectoryClickedListener {

    lateinit private var navRecyclerView: RecyclerView
    lateinit var toolbar: Toolbar
    lateinit private var drawerLayout: DrawerLayout
    lateinit var coordinator: CoordinatorLayout
    lateinit private var accountAdapter: AccountAdapter
    lateinit private var navMenu: Menu
    lateinit private var gridItem: MenuItem


    private lateinit var reposFragment: ReposFragment

    @Inject
    lateinit var presenter: AccountPresenter
    @Inject
    lateinit var navigator: Navigator
    @Inject
    lateinit var sharedPreferences: SharedPrefsController

    private val component: ActivityComponent
        get() = DaggerActivityComponent.builder()
                .applicationComponent((application as App).component)
                .activityModule(ActivityModule(this))
                .build()


    val Activity.app: App
        get() = application as App

    override fun activity() = this

    override fun onHeaderButtonClicked() = if (presenter.showsAccounts) {
        navMenu.clear()
        menuInflater.inflate(R.menu.nav_menu, navMenu)
        presenter.showNavList(navMenu)
    } else {
        navMenu.clear()
        menuInflater.inflate(R.menu.nav_menu_accounts, navMenu)
        presenter.showAccountList(navMenu)
    }

    override fun onRepoClicked(fragment: BaseFragment, repoId: String, repoName: String) {
        navigator.navigateToDirectory(this, fragment.childFragmentManager, presenter.currentAccount,
                repoId, repoName, "")
    }

    override fun onDirectoryClicked(fragment: BaseFragment, repoId: String, repoName: String,
                                    directory: String) {
        navigator.navigateToDirectory(this, fragment.childFragmentManager, presenter.currentAccount,
                repoId, repoName, directory)
    }

    override fun onRemoteFileClicked(fragment: BaseFragment, repoId: String, repoName: String,
                                     directory: String, storage: String,
                                     filename: String) {
        navigator.navigateToDownload(this, fragment.childFragmentManager, presenter.currentAccount,
                repoId, repoName, directory)
    }


    override fun onRevealClicked(fragment: BaseFragment, repoId: String, repoName: String,
                                 directory: String, storage: String,
                                 filename: String) {

        val intent = Intent(Intent.ACTION_VIEW)

        val file = File(File(storage, presenter.currentAccount.name + "/" + repoId), directory)
        val uri = Uri.parse("${file.absolutePath}")
        intent.setDataAndType(uri,"resource/folder")
        startActivity(Intent.createChooser(intent, "Select app to open"))
    }

    override fun onFileClicked(fragment: BaseFragment, repoId: String, repoName: String,
                               directory: String, storage: String,
                               filename: String) {
        if (storage.isEmpty())
            return
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val file = File(
                File(File(storage, presenter.currentAccount.name + "/" + repoName), directory),
                filename)
        val uri = FileProvider.getUriForFile(this,
                "com.bwksoftware.android.seasync.data.fileprovider", file)
        intent.setDataAndType(uri, FileUtils.getMimeType(filename))
        Log.d("AccountActivity", FileUtils.getMimeType(filename))
        startActivity(Intent.createChooser(intent, "Select app to open"))
    }

    override fun onImageClicked(fragment: BaseFragment, repoId: String, repoName: String,
                                directory: String,
                                file: String) {
        navigator.navigateToImageViewer(this, fragment.childFragmentManager,
                presenter.currentAccount, repoId, repoName, directory, file)
    }


    override fun onButtonClicked(itemId: Int) {
        when (itemId) {
            R.id.repos -> {
                navigator.navigateToReposView(this, supportFragmentManager,
                        presenter.currentAccount)
            }
            R.id.uploads -> {
                navigator.navigateToUploadsView(this, supportFragmentManager)
            }
            R.id.add_account -> {
                navigator.navigateToAddAccountView(this, supportFragmentManager)
            }
            R.id.sync -> {
                val params = Bundle()
                params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                ContentResolver.requestSync(presenter.currentAccount,
                        "com.bwksoftware.android.seasync.data.sync", params)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)

        Toast.makeText(app.baseContext, navMenu.findItem(itemId).title,
                Toast.LENGTH_SHORT).show()
    }

    override fun onAccountClicked(account: Account) {
        navMenu.clear()
        menuInflater.inflate(R.menu.nav_menu, navMenu)
        presenter.selectAccount(account)
        presenter.showNavList(navMenu)
    }

    override fun showNavList(items: List<NavBaseItem>) {
        accountAdapter.setItems(items)
        accountAdapter.notifyDataSetChanged()
    }

    override fun selectAccount(account: Account) {

        contentResolver.notifyChange(FileRepoContract.CONTENT_URI, null)
        initNavigationDrawer()
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        initScreen()
    }

    override fun onAccountComplete(account: Account) {
        refreshAccountList()
        onAccountClicked(account)
        supportFragmentManager.popBackStack()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        component.inject(this)
        accountAdapter = AccountAdapter(this, this)
        presenter.view = this
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        coordinator = findViewById(R.id.coordinator)
        setSupportActionBar(toolbar)
        if (savedInstanceState == null) {
            initNavigationDrawer()
            presenter.init()


            ContentResolver.setIsSyncable(presenter.currentAccount, baseContext.getString(
                    com.bwksoftware.android.seasync.data.R.string.authtype), 1)
            ContentResolver.setSyncAutomatically(presenter.currentAccount, baseContext.getString(
                    com.bwksoftware.android.seasync.data.R.string.authtype), true);
            ContentResolver.addPeriodicSync(presenter.currentAccount, baseContext.getString(
                    com.bwksoftware.android.seasync.data.R.string.authtype), Bundle.EMPTY,
                    120)
            ContentResolver.setMasterSyncAutomatically(true)

            Log.d("SeafAccountMgr", ContentResolver.getPeriodicSyncs(presenter.currentAccount,
                    baseContext.getString(
                            com.bwksoftware.android.seasync.data.R.string.authtype)).toString())

            initScreen()
        }

    }

    fun updateViewInFragmentsRecursive(fragment: BaseFragment, isGrid: Boolean) {
        if (fragment.childFragmentManager.backStackEntryCount > 0) {

            val fragmentTag = fragment.childFragmentManager.getBackStackEntryAt(
                    fragment.childFragmentManager.backStackEntryCount - 1).name
            val activeFragment = fragment.childFragmentManager.findFragmentByTag(
                    fragmentTag) as DirectoryFragment
            activeFragment.switchView(!isGrid)
            updateViewInFragmentsRecursive(activeFragment, isGrid)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_grid -> {
                val isGrid = sharedPreferences.getPreferenceValue(
                        SharedPrefsController.Preference.GRID_VIEW_DIRECTORIES).toBoolean()
                sharedPreferences.setPreference(
                        SharedPrefsController.Preference.GRID_VIEW_DIRECTORIES,
                        (!isGrid).toString())
                gridItem.icon = if (isGrid) resources.getDrawable(
                        R.drawable.grid_off) else resources.getDrawable(
                        R.drawable.grid_on)

                if (reposFragment.childFragmentManager.backStackEntryCount > 0) {

                    val fragmentTag = reposFragment.childFragmentManager.getBackStackEntryAt(
                            reposFragment.childFragmentManager.backStackEntryCount - 1).name
                    val activeFragment = reposFragment.childFragmentManager.findFragmentByTag(
                            fragmentTag) as DirectoryFragment
                    activeFragment.switchView(!isGrid)
                    updateViewInFragmentsRecursive(activeFragment, isGrid)
                }
//
                true
            }
            else -> false
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater

        inflater.inflate(R.menu.action_bar, menu)
        if (menu != null) {
            val isGrid = sharedPreferences.getPreferenceValue(
                    SharedPrefsController.Preference.GRID_VIEW_DIRECTORIES).toBoolean()
            gridItem = menu.findItem(R.id.action_grid)
            gridItem.icon = if (isGrid) resources.getDrawable(
                    R.drawable.grid_off) else resources.getDrawable(
                    R.drawable.grid_on)
        }
        val pMenu = PopupMenu(baseContext, null)

        navMenu = pMenu.menu
        pMenu.menuInflater.inflate(R.menu.nav_menu, pMenu.menu)
        presenter.showNavList(pMenu.menu)
        return true
    }

    fun setTitle(title: String) {
        val titleView: TextView = toolbar.findViewById(R.id.title)
        titleView.text = title
        //toolbar.title = title

    }

    private fun initNavigationDrawer() {
        navRecyclerView = findViewById(R.id.nav_menu__rvlist)
        with(navRecyclerView) {
            this.layoutManager = LinearLayoutManager(this.context)
            this.setAdapter(accountAdapter)
        }
        drawerLayout = findViewById<View>(R.id.drawer) as DrawerLayout
        val actionBarDrawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
        }
        drawerLayout!!.addDrawerListener(actionBarDrawerToggle)
        val mDividerItemDecoration = DividerItemDecoration(
                navRecyclerView.getContext(),
                (navRecyclerView.layoutManager as LinearLayoutManager).orientation
        )
        navRecyclerView.addItemDecoration(mDividerItemDecoration)
        actionBarDrawerToggle.syncState()
    }

    private fun initScreen() {
        if (presenter.currentAccount.name != "None") {
            reposFragment = ReposFragment.forAccount(presenter.currentAccount)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, reposFragment, ReposFragment::class.java.name)
                    .commit()
            setTitle(reposFragment!!.name())
        }

//        val intent = Intent(this,FileSyncService::class.java)
//        startService(intent)

//        Intent i = new Intent(context, MyActivity.class);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);
//
//        /***** For start Service  ****/
//        Intent myIntent = new Intent(context, ServiceClassName.class);
//        context.startService(myIntent)
    }

    private var doubleBackToExitPressedOnce: Boolean = false

    private var onBackPressedOnMainFragmentPressedOnce: Boolean = false

    override fun onBackPressed() {

        if (!getActiveFragment()?.onBackPressed()!!) {
            if (!onBackPressedOnMainFragmentPressedOnce) {
                onBackPressedOnMainFragmentPressedOnce = true
                return
            }
            // container Fragment or its associates couldn't handle the back pressed task
            // delegating the task to super class
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return
            }

            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click back again to logout", Toast.LENGTH_SHORT).show()

            Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)

        } else {
            // carousel handled the back pressed task
            // do not call super
        }
    }

    fun getActiveFragment(): BaseFragment? {
        if (supportFragmentManager.backStackEntryCount == 0) {
            return reposFragment
        }
        val tag = supportFragmentManager.getBackStackEntryAt(
                supportFragmentManager.backStackEntryCount - 1).name
        return supportFragmentManager.findFragmentByTag(tag) as BaseFragment?
    }


}
