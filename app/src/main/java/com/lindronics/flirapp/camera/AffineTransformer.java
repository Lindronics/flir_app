package com.lindronics.flirapp.camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class AffineTransformer {

    private Mat transformationMatrix;
    private int width;
    private int height;

    public AffineTransformer(int width, int height) {

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("ModelHandler", "OpenCV not initialized!");
        }

        this.width = width;
        this.height = height;

        transformationMatrix = new Mat(2, 3, CvType.CV_32F);
        transformationMatrix.put(0, 0, (float) 1.22e+00);
        transformationMatrix.put(0, 1, (float) -1.56e-02);
        transformationMatrix.put(0, 2, (float) -5.93e+01);
        transformationMatrix.put(1, 0, (float) -1.26e-02);
        transformationMatrix.put(1, 1, (float) 1.20e+00);
        transformationMatrix.put(1, 2, (float) -6.85e+01);

    }

    public Bitmap transform(Bitmap image) {
        Mat rgbMat = new Mat(width, height, CvType.CV_32F);
        Utils.bitmapToMat(image, rgbMat);
        Imgproc.warpAffine(rgbMat, rgbMat, transformationMatrix, rgbMat.size());
        Utils.matToBitmap(rgbMat, image);
        return image;
    }
}
