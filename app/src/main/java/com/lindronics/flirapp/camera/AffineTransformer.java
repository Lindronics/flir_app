package com.lindronics.flirapp.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AffineTransformer {

    private Mat transformationMatrix;
    private int width;
    private int height;

    public AffineTransformer(Context context) throws IOException {

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("ModelHandler", "OpenCV not initialized!");
        }

        // Reader for config file
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        context.getAssets().open("transformation.txt")));

        transformationMatrix = new Mat(2, 3, CvType.CV_32F);

        // Image dimensions assumed by transformation
        String row = reader.readLine();
        String[] dimensions = row.split(",");
        width = Integer.valueOf(dimensions[0]);
        height = Integer.valueOf(dimensions[1]);

        // Read matrix values
        int i = 0;
        while ((row = reader.readLine()) != null) {
            String[] data = row.split(",");
            for (int j = 0; j < 3; j++) {
                transformationMatrix.put(i, j, Float.valueOf(data[j]));
            }
            i++;
        }
    }

    /**
     * Performs affine transformation on an image
     * @param image The image to be transformed
     * @return The transformed image
     */
    public Bitmap transform(Bitmap image) {
        Mat rgbMat = new Mat(width, height, CvType.CV_32F);
        Utils.bitmapToMat(image, rgbMat);
        Imgproc.warpAffine(rgbMat, rgbMat, transformationMatrix, rgbMat.size());
        Utils.matToBitmap(rgbMat, image);
        return image;
    }
}
