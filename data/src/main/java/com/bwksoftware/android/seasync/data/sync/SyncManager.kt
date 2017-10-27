package com.bwksoftware.android.seasync.data.sync

import android.os.AsyncTask
import android.util.Log
import com.bwksoftware.android.seasync.data.entity.Item
import okhttp3.ResponseBody
import java.io.*


class SyncManager {

    class DownloadTask(val localItem: Item, val path: String,
                       val responseBody: ResponseBody) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg p0: Void?): Boolean {
            //todo change path to respect repo too
            writeResponseBodyToDisk(path, localItem.name!!, responseBody)
            return true
        }

        private fun writeResponseBodyToDisk(path: String, filename: String,
                                            body: ResponseBody): Boolean {
            try {
                Log.d("DownloadTask", "Downloading file: " + path)
                // todo change the file location/name according to your needs
                val futureStudioIconFile = File(path, filename)
                Log.d("Files", futureStudioIconFile.absolutePath)
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null

                try {
                    val fileReader = ByteArray(4096)

                    val fileSize = body.contentLength()
                    var fileSizeDownloaded: Long = 0

                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(futureStudioIconFile)

                    while (true) {
                        val read = inputStream!!.read(fileReader)

                        if (read == -1) {
                            break
                        }

                        outputStream!!.write(fileReader, 0, read)

                        fileSizeDownloaded += read.toLong()

                        Log.d("bla", "file download: $fileSizeDownloaded of $fileSize")
                    }

                    outputStream!!.flush()

                    return true
                } catch (e: IOException) {
                    Log.d("DownloadTask", e.localizedMessage)
                    return false
                } finally {
                    if (inputStream != null) {
                        inputStream!!.close()
                    }

                    if (outputStream != null) {
                        outputStream!!.close()
                    }
                }
            } catch (e: IOException) {
                Log.d("DownloadTask", e.localizedMessage)

                return false
            }

        }

    }

}