package com.seerslab.argear.sample.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaStoreUtil {

    @RequiresApi(Build.VERSION_CODES.Q)
    public static void writeImageToMediaStoreForQ(Context context, String filePath, String relativePath) {
        Uri uri = Uri.parse(filePath);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, uri.getLastPathSegment());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        File file = new File(filePath);
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = context.getContentResolver().insert(collection, values);
        if (item != null) {
            try {
                ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(item, "w", null);
                FileOutputStream outputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                byte[] buffer = new byte[1024];
                FileInputStream inputStream = new FileInputStream(file);
                while (true) {
                    int data = inputStream.read(buffer, 0, buffer.length);
                    if (data == -1) {
                        inputStream.close();
                        outputStream.close();
                        break;
                    }
                    outputStream.write(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(item, values, null, null);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public static void writeVideoToMediaStoreForQ(Context context, String path, String relativePath) {
        Uri uri = Uri.parse(path);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, uri.getLastPathSegment());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/*");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, relativePath);
        values.put(MediaStore.Video.Media.IS_PENDING, 1);

        File file = new File(path);
        Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = context.getContentResolver().insert(collection, values);

        if (item != null) {
            try {
                ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(item, "w", null);
                FileOutputStream outputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                byte[] buffer = new byte[1024];
                FileInputStream inputStream = new FileInputStream(file);
                while (true) {
                    int data = inputStream.read(buffer, 0, buffer.length);
                    if (data == -1) {
                        inputStream.close();
                        outputStream.close();
                        break;
                    }
                    outputStream.write(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(item, values, null, null);
        }
    }
}
