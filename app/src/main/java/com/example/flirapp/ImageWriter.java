package com.example.flirapp;

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

class ImageWriter {

    private Context context;

    ImageWriter(Context context) {
        this.context = context;
    }

//    private void writeImage(Bitmap image, String path) {
//        try {
//            FileOutputStream out = new FileOutputStream(path);
//            MediaStore.Images.Media.insertImage(context.getContentResolver(), image, path , "test");
//
////            image.compress(Bitmap.CompressFormat.PNG, 100, out);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//    }

//    private void writeImage(Bitmap image, String name) {
//        OutputStream out = null;
//        String strDirectory = Environment.getExternalStorageDirectory().toString();
//
//        File f = new File(strDirectory, name);
//        try {
//            out = new FileOutputStream(f);
//
//            image.compress(Bitmap.CompressFormat.JPEG, 85, out);
//            out.flush();
//            out.close();
//
//
//            MediaStore.Images.Media.insertImage(context.getContentResolver(),
//                    f.getAbsolutePath(), f.getName(), f.getName());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void writeImage(Bitmap image, String name, String time) {
        String IMAGES_FOLDER_NAME = "FLIR_App";
        OutputStream out = null;

        try {
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
                    out = resolver.openOutputStream(imageUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM).toString() + File.separator + IMAGES_FOLDER_NAME;

                File file = new File(imagesDir);

                if (!file.exists()) {
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

    void saveImages(Bitmap fir, Bitmap rgb) {
        Date now = new Date();
        String timeStamp = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss_SSS").format(now);

        writeImage(fir, "fir_" + timeStamp + ".png", Long.toString(now.getTime()));
        writeImage(rgb, "rgb_" + timeStamp + ".png", Long.toString(now.getTime()));
    }

}
