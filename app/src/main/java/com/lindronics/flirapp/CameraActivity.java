package com.lindronics.flirapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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

import java.util.concurrent.LinkedBlockingQueue;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private CameraHandler cameraHandler;

    private ImageView rgbImage;
    private ImageView firImage;

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue<>(21);

    private ToggleButton cameraButton;

    private ImageWriter imageWriter = null;


    /**
     * Executed when activity is created
     *
     * @param savedInstanceState -
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraHandler = new CameraHandler();

        rgbImage = findViewById(R.id.rgb_view);
        firImage = findViewById(R.id.fir_view);

        cameraButton = findViewById(R.id.camera_button);
    }

    /**
     * Defines behaviour for when images are received
     */
    private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {

        @Override
        public void images(FrameDataHolder dataHolder) {

            runOnUiThread(() -> {
                firImage.setImageBitmap(dataHolder.firBitmap);
                rgbImage.setImageBitmap(dataHolder.rgbBitmap);
            });
        }

        @Override
        public void images(Bitmap firBitmap, Bitmap rgbBitmap) {

            try {
                framesBuffer.put(new FrameDataHolder(firBitmap, rgbBitmap));
            } catch (InterruptedException e) {
                // If interrupted while waiting for adding a new item in the queue
                Log.e(TAG, "images(), unable to add incoming images to frames buffer, exception:" + e);
            }

            runOnUiThread(() -> {
                Log.d(TAG, "framebuffer size:" + framesBuffer.size());
                FrameDataHolder poll = framesBuffer.poll();
                if (poll != null) {
                    firImage.setImageBitmap(poll.firBitmap);
                    rgbImage.setImageBitmap(poll.rgbBitmap);
                }
            });

            if (imageWriter != null) {
                imageWriter.saveImages(firBitmap, rgbBitmap);
            }
        }
    };


    /**
     * Defines behaviour vor changes of the connection status
     */
    private final ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener() {
        @Override
        public void onConnectionStatusChanged(@NotNull ConnectionStatus connectionStatus, @org.jetbrains.annotations.Nullable ErrorCode errorCode) {
            Log.d(TAG, "onConnectionStatusChanged connectionStatus:" + connectionStatus + " errorCode:" + errorCode);

            runOnUiThread(() -> {

                switch (connectionStatus) {
                    case CONNECTING:
                        break;
                    case CONNECTED: {
                        showMessage.showOnUI("Connected to camera!");
                        cameraHandler.startStream(streamDataListener);
                        cameraButton.setVisibility(View.VISIBLE);
                    }
                    break;
                    case DISCONNECTING:
                        break;
                    case DISCONNECTED: {
//                        disconnect();
                        showMessage.showOnUI("Disconnected from camera!");
                        cameraButton.setVisibility(View.INVISIBLE);
                        endCapture();
                    }
                    break;
                }
            });
        }
    };

    /**
     * Show message on the screen
     */
    interface ShowMessage {
        void show(String message);

        void showOnUI(String message);
    }

    private MainActivity.ShowMessage showMessage = new MainActivity.ShowMessage() {
        @Override
        public void show(String message) {
            Toast.makeText(CameraActivity.this, message, Toast.LENGTH_SHORT).show();
            TextView view = findViewById(R.id.textBox);
            view.setText(view.getText() + "\n" + message);
        }

        @Override
        public void showOnUI(String message) {
            runOnUiThread(() -> show(message));
        }
    };


    /**
     * Event listener for starting or stopping camera capture/recording
     *
     * @param view Toggle button
     */
    public void toggleCapture(View view) {
        ToggleButton button = (ToggleButton) view;
        if (button.isChecked()) {
            button.setBackgroundDrawable(getDrawable(R.drawable.ic_camera_capture_recording));
            startCapture();
        } else {
            button.setBackgroundDrawable(getDrawable(R.drawable.ic_camera_capture_ready));
            endCapture();
        }
    }

    private void startCapture() {
        imageWriter = new ImageWriter(this);
        showMessage.showOnUI("Started recording");

    }

    private void endCapture() {
        imageWriter = null;
        showMessage.showOnUI("Stopped recording");
    }
}
