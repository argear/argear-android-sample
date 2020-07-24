package com.seerslab.argear.sample.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MediaStoreUtil {

    companion object {

        fun writeImageToMediaStore(context: Context, path: String) {
            val uri = Uri.parse(path)
            val file = File(path)

            val inputStream = FileInputStream(file)
            val filePath = Environment.DIRECTORY_DCIM + File.separator + "ARGEAR" +
                    File.separator + uri.pathSegments.last()
            val outputStream = FileOutputStream(path)

            val buffer = ByteArray(1024)
            while (true) {
                val data = inputStream.read(buffer, 0, buffer.size)
                if (data == -1) {
                    break
                }
                outputStream.write(buffer)
            }
            inputStream.close()
            outputStream.close()

            val values= ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, uri.pathSegments.last())
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(MediaStore.Images.Media.DATA, filePath)
            }

            val item = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        @Throws(IOException::class)
        fun writeImageToMediaStoreForQ(context: Context, path: String) {
            val uri = Uri.parse(path)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, uri.pathSegments.last())
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ARGEAR")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val file = File(path)
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val item = context.contentResolver.insert(collection, values)
            item?.let { insertUri ->
                context.contentResolver.openFileDescriptor(insertUri, "w", null).use {
                    it?.let { parcelFileDescriptor ->
                        FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
                            val buffer = ByteArray(1024)
                            val imageInputStream = FileInputStream(file)
                            while (true) {
                                val data = imageInputStream.read(buffer, 0, buffer.size)
                                if (data == -1) {
                                    break
                                }
                                outputStream.write(buffer)
                            }
                            imageInputStream.close()
                            outputStream.close()
                        }
                    }
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(insertUri, values, null, null)
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        @Throws(IOException::class)
        fun writeVideoToMediaStoreForQ(context: Context, path: String) {
            val uri = Uri.parse(path)
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, uri.pathSegments.last())
                put(MediaStore.Video.Media.MIME_TYPE, "video/*")
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/ARGEAR")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val file = File(path)
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val item = context.contentResolver.insert(collection, values)
            item?.let { insertUri ->
                context.contentResolver.openFileDescriptor(insertUri, "w", null).use {
                    it?.let { parcelFileDescriptor ->
                        FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
                            val buffer = ByteArray(1024)
                            val imageInputStream = FileInputStream(file)
                            while (true) {
                                val data = imageInputStream.read(buffer, 0, buffer.size)
                                if (data == -1) {
                                    break
                                }
                                outputStream.write(buffer)
                            }
                            imageInputStream.close()
                            outputStream.close()
                        }
                    }
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(insertUri, values, null, null)
            }
        }
    }
}