package com.example.flirapp;

import android.graphics.Bitmap;

class FrameDataHolder {

    public final Bitmap firBitmap;
    public final Bitmap rgbBitmap;

    FrameDataHolder(Bitmap firBitmap, Bitmap rgbBitmap){
        this.firBitmap = firBitmap;
        this.rgbBitmap = rgbBitmap;
    }
}
