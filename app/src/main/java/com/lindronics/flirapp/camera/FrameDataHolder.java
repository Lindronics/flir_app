package com.lindronics.flirapp.camera;

import android.graphics.Bitmap;

public class FrameDataHolder {

    public Bitmap rgbBitmap;
    public Bitmap firBitmap;

    public FrameDataHolder(Bitmap rgbBitmap, Bitmap firBitmap){
        this.rgbBitmap = rgbBitmap;
        this.firBitmap = firBitmap;
    }
}
