package com.example.flirapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.log.ThermalLog;
//import com.flir.thermalsdk.Disc

public class MainActivity extends AppCompatActivity {

    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set logging level (DEBUG or NONE)
        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;

        // Initialize SDK
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);
//        DiscoveryManager.getInstance().scan(aDiscoveryEventListener, CommunicationInterface.USB);
    }
}
