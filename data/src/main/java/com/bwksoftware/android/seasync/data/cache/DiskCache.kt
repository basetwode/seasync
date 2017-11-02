package com.bwksoftware.android.seasync.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import java.io.File
import javax.inject.Inject



class DiskCache @Inject constructor(context: Context, sharedPreferences: SharedPreferences) {

    private val cacheDir: File = context.cacheDir
    private val gson = Gson()


    fun readDirectoryList(account: String, repoId: String, path: String): Observable<List<Item>>? {
        return Observable.create<List<Item>> { e ->
            run {

                val accountDir = File(cacheDir, account)
                val repoDir = File(accountDir, repoId)
                val directoryFile = File(repoDir, path + ".txt")
                val listType = object : TypeToken<ArrayList<Item>>() {}.type

                e.onNext(gson.fromJson<List<Item>>(directoryFile.readText(), listType))

            }
        }


    }

    fun writeDirectoryList(account: String, repoId: String, path: String,
                           directoryContent: List<Item>) {
        val accountDir = File(cacheDir, account)
        val repoDir = File(accountDir, repoId)
        val directoryFile = File(repoDir, path + ".txt")
        directoryFile.mkdirs()
        directoryFile.writeText(gson.toJson(directoryContent))
    }

    fun readRepoList(account: String): Observable<List<Repo>>? {

        return Observable.create<List<Repo>> { e ->
            run {
                val accountDir = File(cacheDir, account)
                val reposFile = File(accountDir, "repos.txt")
                val listType = object : TypeToken<ArrayList<Repo>>() {}.type
                e.onNext(gson.fromJson<List<Repo>>(reposFile.readText(), listType))
            }
        }

    }

    fun writeRepoList(account: String, repos: List<Repo>) {
        val accountDir = File(cacheDir, account)
        val reposFile = File(accountDir, "repos.txt")
        reposFile.mkdirs()
        reposFile.writeText(gson.toJson(repos))
    }

}