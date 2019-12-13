package com.example.flirapp;

import android.graphics.Bitmap;

class FrameDataHolder {

    final Bitmap firBitmap;
    final Bitmap rgbBitmap;

    FrameDataHolder(Bitmap firBitmap, Bitmap rgbBitmap){
        this.firBitmap = firBitmap;
        this.rgbBitmap = rgbBitmap;
    }
}
