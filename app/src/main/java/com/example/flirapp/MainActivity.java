package com.example.flirapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CameraHandler cameraHandler;
    private Identity connectedIdentity = null;
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();


    /**
     * Executed when activity is created
     * @param savedInstanceState -
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set logging level (DEBUG or NONE)
        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;

        // Initialize SDK
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);

        cameraHandler = new CameraHandler();

        startDiscovery();
    }


    /**
     * Start camera discovery
     */
    private void startDiscovery() {
        cameraHandler.startDiscovery(cameraDiscoveryListener, discoveryStatusListener);
    }

    /**
     * Stop camera discovery
     */
    private void stopDiscovery() {
        cameraHandler.stopDiscovery(discoveryStatusListener);
    }

    /**
     * Callback for discovery status
     */
    private CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
        @Override
        public void started() {
            showMessage.show("Starting discovery.");
        }

        @Override
        public void stopped() {
            showMessage.show("Stopping discovery.");
        }
    };

    /**
     * Callback for camera discovered
     */
    private DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {

        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showMessage.show("Device discovered!");
                    cameraHandler.add(identity);
                }
            });
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopDiscovery();
                    showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
                }
            });
        }
    };

    /**
     * Connect to a Camera
     */
    private void connect(Identity identity) {

        // Stop discovery when connected
        cameraHandler.stopDiscovery(discoveryStatusListener);

        // Already connected
        if (connectedIdentity != null) {
            Log.d(TAG, "connect(), already connected to a camera!");
            showMessage.show("connect(), already connected to a camera!");
            return;
        }

        // No camera available
        if (identity == null) {
            Log.d(TAG, "connect(), no camera available!");
            showMessage.show("connect(), no camera available!");
            return;
        }

        connectedIdentity = identity;
    }

    /**
     * Show message on the screen
     */
    public interface ShowMessage {
        void show(String message);
    }

    private ShowMessage showMessage = new ShowMessage() {
        @Override
        public void show(String message) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };
}
