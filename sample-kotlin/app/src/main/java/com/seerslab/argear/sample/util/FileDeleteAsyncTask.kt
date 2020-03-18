package com.seerslab.argear.sample.util

import android.os.AsyncTask
import java.io.File

class FileDeleteAsyncTask(
    private val directory: File?,
    private val onAsyncFileDeleteListener: OnAsyncFileDeleteListener?
) :
    AsyncTask<Void, Void, Any>() {

    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun doInBackground(vararg voids: Void): Any? {
        if (directory != null && directory.exists() && directory.listFiles() != null) {
            for (childFile in directory.listFiles()) {
                if (childFile != null) {
                    if (childFile.isDirectory) {
                        deleteDirectory(childFile)
                    } else {
                        childFile.delete()
                    }
                }
            }
            directory.delete()
        }
        return null
    }

    override fun onPostExecute(result: Any?) {
        onAsyncFileDeleteListener?.processFinish(result)
    }

    private fun deleteDirectory(localDirectory: File?) {
        if (localDirectory != null && localDirectory.exists() && localDirectory.listFiles() != null) {
            for (childFile in localDirectory.listFiles()) {
                if (childFile != null) {
                    if (childFile.isDirectory) {
                        deleteDirectory(childFile)
                    } else {
                        childFile.delete()
                    }
                }
            }
        }
    }

    interface OnAsyncFileDeleteListener {
        fun processFinish(result: Any?)
    }
}