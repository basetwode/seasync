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

package com.bwksoftware.android.seasync.data.net

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bwksoftware.android.seasync.data.entity.Account
import com.bwksoftware.android.seasync.data.entity.Avatar
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.entity.Repo
import com.bwksoftware.android.seasync.data.utils.FileUtils
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Observable
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RestApiImpl @Inject constructor(val context: Context) {
    private val service: RestAPI

    init {
        val logging = HttpLoggingInterceptor()
// set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val builder = OkHttpClient.Builder()
//                .addInterceptor(logging)
        val retro = Retrofit.Builder().baseUrl("https://cloud.bwk-technik.de")
                .addConverterFactory(
                        GsonConverterFactory.create(GsonBuilder().setLenient().create()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(builder.build()).build()
        service = retro.create(RestAPI::class.java)
    }

    fun getAccountToken(username: String, serverAddress: String,
                        password: String): Observable<Account> {
        val requestBody = RequestBody.create(
            "application/x-www-form-urlencoded".toMediaTypeOrNull(),
                "username=$username&password=$password")
        return service.postAccountToken(parseUrl(serverAddress, "auth-token/"), requestBody)
    }

    fun getRepoList(authToken: String, serverAddress: String): Observable<List<Repo>> {
        return service.getRepoList(parseUrl(serverAddress, "repos/"), "Token " + authToken)
    }

    fun getRepoListSync(authToken: String, serverAddress: String): Call<List<Repo>> {
        return service.getRepoListSync(parseUrl(serverAddress, "repos/"), "Token " + authToken)
    }

    fun getAvatar(username: String, serverAddress: String, authToken: String): Observable<Avatar> {
        return service.getAvatar(parseUrl(serverAddress, "avatars/user/$username/resized/128/"),
                "Token " + authToken)
    }

    fun getDirectoryEntries(authToken: String, serverAddress: String, repoID: String,
                            directory: String): Observable<List<Item>> {
        if (directory.isEmpty())
            return service.getDirectoryEntries(parseUrl(serverAddress, "repos/$repoID/dir/"),
                    "Token " + authToken)
        return service.getDirectoryEntries(parseUrl(serverAddress, "repos/$repoID/dir/"),
                "Token " + authToken, directory)
    }

    fun getDirectoryEntriesSync(authToken: String, serverAddress: String, repoID: String,
                                directory: String): Call<List<Item>> {
        Log.d("FileSyncService", repoID)
        if (directory.isEmpty())
            return service.getDirectoryEntriesSync(parseUrl(serverAddress, "repos/$repoID/dir/"),
                    "Token " + authToken)
        return service.getDirectoryEntriesSync(parseUrl(serverAddress, "repos/$repoID/dir/"),
                "Token " + authToken, directory)
    }

    fun getUpdateLink(authToken: String, serverAddress: String, repoID: String,
                      directory: String): Call<String> {
        return service.getUpdateLink(parseUrl(serverAddress, "repos/$repoID/update-link/"),
                "Token " + authToken, directory)
    }

    fun getUploadLink(authToken: String, serverAddress: String, repoID: String,
                      directory: String): Call<String> {
        return service.getUploadLink(parseUrl(serverAddress, "repos/$repoID/update-link/"),
                "Token " + authToken, directory)
    }

    fun uploadFile(url: String, authToken: String, parentDir: String, relativeDir: String,
                   file: File): Call<String> {
        val requestFile = RequestBody.create(context.contentResolver.getType(Uri.fromFile(file))?.toMediaTypeOrNull(),
                file
        )
        // MultipartBody.Part is used to send also the actual file name
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        return service.uploadFile(url, "Token " + authToken, parentDir, relativeDir, body)
    }

    fun updateFile(url: String, authToken: String, targetFilePath: String,
                   targetFile: File): Call<String> {
        val requestFile = RequestBody.create(
                FileUtils.getMimeType(
                        targetFile.name)?.toMediaTypeOrNull(),
                targetFile
        )
        // MultipartBody.Part is used to send also the actual file name
        val body = MultipartBody.Builder()

        body.addFormDataPart("target_file", targetFilePath)
        body.addFormDataPart("file", targetFile.name, requestFile)

        val bodyParts = body.build()
//  val body = MultipartBody.Part.createFormData("target_file", targetFile.name, requestFile)
        return service.updateFile(url,
                "Token " + authToken,
                bodyParts.part(0), bodyParts.part(1))
    }

    fun getFileDownloadLink( authToken: String,serverAddress: String, repoID: String,
                            directory: String): Call<String> {
        return service.getFileDownloadLink(parseUrl(serverAddress,
                "repos/$repoID/file/"), "Token " + authToken, directory)
    }

    fun downloadFile(url: String): Call<ResponseBody> {
        return service.downloadFile(url)
    }

    fun getFileDetail(authToken: String, serverAddress: String, repoID: String, directory: String,
                      filename: String): Call<Item> {
        return service.getFileDetail(parseUrl(serverAddress, "repos/$repoID/file/detail/"),
                directory + filename, "Token " + authToken)
    }

    fun parseUrl(serverAddress: String, path: String): String {
        return "https://$serverAddress/api2/$path"
    }

}
