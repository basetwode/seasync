package com.bwksoftware.android.seasync.data.sync

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import com.bwksoftware.android.seasync.data.entity.Item
import com.bwksoftware.android.seasync.data.service.OnBootReceiver
import okhttp3.ResponseBody
import java.io.*


class SyncManager {

    companion object {
        val DOWNLOAD_PROGRESS = "com.bwksoftware.android.seasync.data.service.sync.syncmanager.DOWNLOAD_PROGRESS"
        val PROGRESS_VALUE = "downloadtask_progress"
        val DOWNLOAD_RATE = "downloadtask_rate"
    }

    class DownloadTask(val localItem: Item, val path: String, internal val context: Context,
                       val responseBody: ResponseBody) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg p0: Void?): Boolean {
            //todo change path to respect repo too
            return writeResponseBodyToDisk(path, localItem.name!!, responseBody)

        }

        private fun writeResponseBodyToDisk(path: String, filename: String,
                                            body: ResponseBody): Boolean {
            val futureStudioIconFile = File(path, filename)

            try {
                Log.d("DownloadTask", "Downloading file: " + path)
                // todo change the file location/name according to your needs
                futureStudioIconFile.parentFile.mkdirs()
                Log.d("Downloading ", futureStudioIconFile.absolutePath)
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null

                try {
                    val fileReader = ByteArray(4096)

                    val fileSize = body.contentLength()
                    var fileSizeDownloaded: Long = 0
                    var oldProgress = 0
                    var timeStart = System.currentTimeMillis()
                    var timeEnd = timeStart
                    var bytesReadSinceLast: Long = 0

                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(futureStudioIconFile)

                    while (true) {
                        val read = inputStream!!.read(fileReader)

                        if (read == -1) {
                            break
                        }

                        outputStream!!.write(fileReader, 0, read)
                        val bytesRead = read.toLong()
                        fileSizeDownloaded += bytesRead
                        bytesReadSinceLast += bytesRead

                        val currentProgress = (fileSizeDownloaded.toFloat() / fileSize * 100).toInt()
                        if (currentProgress >= oldProgress + 2) {
                            oldProgress = currentProgress

                            timeEnd = System.currentTimeMillis()
                            val rate = calcSpeedInMB(timeStart, timeEnd, bytesReadSinceLast)
                            timeStart = System.currentTimeMillis()
                            bytesReadSinceLast = 0
                            val progressIntent = Intent(DOWNLOAD_PROGRESS)
                            progressIntent.putExtra(PROGRESS_VALUE, currentProgress)
                            progressIntent.putExtra(DOWNLOAD_RATE, rate)
                            context.sendBroadcast(progressIntent)

                        }
                        //  Log.d("bla", "file download: $fileSizeDownloaded of $fileSize")
                    }

                    outputStream!!.flush()


                    return true
                } catch (e: IOException) {
                    // Delete file on failure
                    futureStudioIconFile.delete()
                    Log.d("DownloadTask", e.localizedMessage)
                    return false
                } finally {
                    if (inputStream != null) {
                        inputStream!!.close()
                    }

                    if (outputStream != null) {
                        outputStream!!.close()


                    }

                    val progressIntent = Intent(DOWNLOAD_PROGRESS)
                    progressIntent.putExtra(PROGRESS_VALUE, 100)
                    context.sendBroadcast(progressIntent)
                    val restartObserverIntent = Intent(OnBootReceiver.ACTION_RESTART_CACHE_OBSERVER)
                    context.sendBroadcast(restartObserverIntent)
                }
            } catch (e: IOException) {
                Log.d("DownloadTask", e.localizedMessage)
                futureStudioIconFile.delete()
                return false
            }

        }


        fun calcSpeedInMB(start: Long, end: Long, bytes: Long): Float {
            val timeElapsed = end - start
            return (bytes.toFloat() / (timeElapsed.toFloat() / 1000)) / 1000 / 1000
        }
    }


}