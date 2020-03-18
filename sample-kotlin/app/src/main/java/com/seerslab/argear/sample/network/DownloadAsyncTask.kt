package com.seerslab.argear.sample.network

import android.os.AsyncTask
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadAsyncTask(
    private val targetPath: String,
    private val achieveUrl: String,
    private val responseListener: DownloadAsyncResponse?
) :
    AsyncTask<Int, Int, Boolean>() {

    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun doInBackground(vararg params: Int?): Boolean {
        var success = false
        var fileOutput: FileOutputStream? = null
        var inputStream: InputStream? = null

        try {
            val url = URL(achieveUrl)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.doOutput = false
            urlConnection.connect()

            val file = File(targetPath)
            file.createNewFile()

            fileOutput = FileOutputStream(file)
            inputStream = urlConnection.inputStream

            val buffer = ByteArray(1024)
            var bufferLength: Int
            var read = 0
            val total = urlConnection.contentLength
            while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                fileOutput.write(buffer, 0, bufferLength)
                read += bufferLength
                if (total > 0) {
                    val progress = (100 * (read / total.toFloat())).toInt()
                    publishProgress(progress)
                }
            }
            success = true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fileOutput?.let {
                try {
                    fileOutput.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            inputStream?.let {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return success
    }

    override fun onProgressUpdate(vararg progress: Int?) {
    }

    override fun onPostExecute(result: Boolean) {
        responseListener?.processFinish(result)
    }
}