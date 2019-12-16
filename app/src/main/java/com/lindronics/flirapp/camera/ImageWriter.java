package com.lindronics.flirapp.camera;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Handles writing images to the file system
 */
public class ImageWriter {

    private final Context context;

    public ImageWriter(Context context) {
        this.context = context;
    }

    /**
     * Writes an image to file system.
     *
     * @param image Bitmap to write
     * @param name  File name of the image
     * @param time  Current timestamp
     */
    private void writeImage(Bitmap image, String name, String time) {
        String IMAGES_FOLDER_NAME = "FLIR_App";
        OutputStream out = null;

        try {

            // If Android Q or later, use MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();

                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + IMAGES_FOLDER_NAME);
                contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, time);
                contentValues.put(MediaStore.MediaColumns.DATE_ADDED, time);

                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                try {
                    out = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                // *** UNTESTED ***
                // If before android Q, use legacy file storage
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM).toString() + File.separator + IMAGES_FOLDER_NAME;

                File file = new File(imagesDir);

                if (!file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.mkdir();
                }

                File imageFile = new File(imagesDir, name + ".png");
                out = new FileOutputStream(imageFile);

            }
            if (out != null) {
                image.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves FIR and RGB image to file system
     *
     * @param fir FIR Bitmap
     * @param rgb RGB Bitmap
     */
    public void saveImages(Bitmap fir, Bitmap rgb) {
        Date now = new Date();

        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss_SSS").format(now);

        writeImage(fir, "fir_" + timeStamp + ".png", Long.toString(now.getTime()));
        writeImage(rgb, "rgb_" + timeStamp + ".png", Long.toString(now.getTime()));
    }

}
