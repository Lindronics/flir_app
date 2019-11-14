package com.example.flirapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ThermalSdkAndroid.init(this);
    }
}
