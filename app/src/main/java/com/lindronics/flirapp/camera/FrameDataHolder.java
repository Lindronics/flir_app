package com.lindronics.flirapp.camera;

import android.graphics.Bitmap;

public class FrameDataHolder {

    public final Bitmap firBitmap;
    public final Bitmap rgbBitmap;

    public FrameDataHolder(Bitmap firBitmap, Bitmap rgbBitmap){
        this.firBitmap = firBitmap;
        this.rgbBitmap = rgbBitmap;
    }
}
