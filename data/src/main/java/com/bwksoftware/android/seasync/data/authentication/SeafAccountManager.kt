package com.bwksoftware.android.seasync.data.authentication

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.content.SyncRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.bwksoftware.android.seasync.data.R
import com.bwksoftware.android.seasync.data.prefs.SharedPrefsController
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SeafAccountManager @Inject constructor(val context: Context,
                                             val sharedPrefsController: SharedPrefsController) {

    fun createAccount(email: String, password: String, serverAddress: String,
                      authToken: String): Account {
        val account = Account(email, context.getString(R.string.authtype))
        Log.d("SeafAccMgr", account.toString())
        val am = AccountManager.get(context)

        am.addAccountExplicitly(account, password, null)
        am.setAuthToken(account, "full_access", authToken)
        am.setUserData(account, "Server", serverAddress)

        ContentResolver.setMasterSyncAutomatically(true)
        ContentResolver.setIsSyncable(account, context.getString(R.string.authtype), 1)
        ContentResolver.setSyncAutomatically(account, context.getString(R.string.authtype), true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val b  = SyncRequest.Builder().syncPeriodic(60, 1)
            b.setSyncAdapter(account, context.getString(R.string.authtype))
            b.setExtras(Bundle())
            ContentResolver.requestSync(b.build())
        } else {
            ContentResolver.addPeriodicSync(account, context.getString(R.string.authtype), Bundle.EMPTY,
                    120 )
        }

        Log.d("SeafAccountMgr",ContentResolver.getIsSyncable(account,
                context.getString(R.string.authtype)).toString())
        Log.d("SeafAccountMgr",ContentResolver.getPeriodicSyncs(account,
                context.getString(R.string.authtype)).toString())
        return account
    }

    fun getAllAccounts(): List<Account> {
        val accountManager = AccountManager.get(context)
        val currentAccountName = sharedPrefsController.getPreferenceValue(
                SharedPrefsController.Preference.CURRENT_USER_ACCOUNT)
        var accounts = accountManager.getAccountsByType(context.getString(R.string.authtype))
        if (accounts.isEmpty() || currentAccountName == "None")
            accounts += Account("None", context.getString(R.string.authtype))
        return accounts.sortedWith(Comparator { o1, _ ->
            if (o1.name == currentAccountName) -1 else 0
        })
    }

    fun getAccountByName(accountName: String): Account? {

        val accounts = getAllAccounts()
        var currentAndroidAccount: Account? = null
        accounts.filter { it.name == accountName }
                .forEach { currentAndroidAccount = it }
        return currentAndroidAccount
    }

    fun getServerAddress(account: Account): String? {
        return AccountManager.get(context).getUserData(account, "Server")
    }

    fun getCurrentAccount(): Account {
        var account = getAccountByName(sharedPrefsController.getPreferenceValue(
                SharedPrefsController.Preference.CURRENT_USER_ACCOUNT))
        if (account == null) {
            account = Account("None", context.getString(R.string.authtype))
        }
        return account
    }

    fun getCurrentAccountToken(): String {
        val currentAccountName = sharedPrefsController.getPreferenceValue(
                SharedPrefsController.Preference.CURRENT_USER_ACCOUNT)
        val currentAccount = getAccountByName(currentAccountName)
        if (currentAccount != null && currentAccount.name != "None") {
            return AccountManager.get(context).peekAuthToken(currentAccount, "full_access")
        } else {
            return ""
        }
    }
    fun deleteAccount(accountName: String){
        val accountManager = AccountManager.get(context)
        val account = Account(accountName, context.getString(R.string.authtype))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            accountManager.removeAccountExplicitly(account)
            //TODO implement it for earlier versions
        }
    }

}