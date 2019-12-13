package com.lindronics.flirapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatus;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CameraHandler cameraHandler;
    private Identity connectedIdentity = null;

    private ListView cameraListView;
    private ArrayList<Identity> foundCameras;
    CameraArrayAdapter cameraArrayAdapter;

    /**
     * Executed when activity is created
     *
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

        foundCameras = new ArrayList<>();
        cameraArrayAdapter = new CameraArrayAdapter(this, foundCameras);
        cameraListView = findViewById(R.id.camera_list);
        cameraListView.setAdapter(cameraArrayAdapter);

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
    private final CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
        @Override
        public void started() {
            showMessage.showOnUI("Starting discovery.");
        }

        @Override
        public void stopped() {
            showMessage.showOnUI("Stopped discovery.");
        }
    };

    /**
     * Callback for camera discovered
     */
    private final DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {

        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);
            foundCameras.add(identity);

            // Stop discovery when connected
            stopDiscovery();

            // Already connected
            if (connectedIdentity != null) {
                Log.d(TAG, "connect(), already connected to a camera!");
                showMessage.showOnUI("connect(), already connected to a camera!");
                return;
            }

            // No camera available
            if (identity == null) {
                Log.d(TAG, "connect(), no camera available!");
                showMessage.showOnUI("connect(), no camera available!");
                return;
            }

            // If no errors, connect to camera
            showMessage.showOnUI("connecting to " + identity);
            connectedIdentity = identity;
        }

        @Override
        public void onCameraLost(Identity identity) {
            foundCameras.remove(identity);
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);

            runOnUiThread(() ->
                    showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode)
            );
        }
    };

    /**
     * Show message on the screen
     */
    interface ShowMessage {
        void show(String message);

        void showOnUI(String message);
    }

    private ShowMessage showMessage = new ShowMessage() {
        @Override
        public void show(String message) {
//            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            TextView view = findViewById(R.id.textBox);
            view.setText(view.getText() + "\n" + message);
        }

        @Override
        public void showOnUI(String message) {
            runOnUiThread(() -> show(message));
        }
    };
}