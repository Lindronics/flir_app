package com.lindronics.flirapp.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatus;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.google.gson.Gson;
import com.lindronics.flirapp.R;
import com.lindronics.flirapp.camera.CameraHandler;
import com.lindronics.flirapp.camera.FrameDataHolder;
import com.lindronics.flirapp.camera.ImageWriter;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.LinkedBlockingQueue;

abstract class AbstractCameraActivity extends AppCompatActivity implements CameraHandler.StreamDataListener {

    private static final String TAG = "AbstractCameraActivity";

    private CameraHandler cameraHandler;

    private ImageView rgbImage;
    private ImageView firImage;

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue<>(21);

    private ToggleButton cameraButton;

    private ImageWriter imageWriter = null;


    /**
     * Executed when activity is created.
     * Get camera identity from intent and connect to camera.
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraHandler = new CameraHandler();

        rgbImage = findViewById(R.id.rgb_view);
        firImage = findViewById(R.id.fir_view);

        Bundle extras = getIntent().getExtras();
        Gson gson = new Gson();

        if (extras == null) {
            finish();
            return;
        }

        String identityString = extras.getString("cameraIdentity");
        Identity cameraIdentity = gson.fromJson(identityString, Identity.class);
        cameraHandler.connect(cameraIdentity, connectionStatusListener);
    }

    /**
     * Receive receiveImages from camera handler
     * @param images RGB and FIR receiveImages
     */
    @Override
    public void receiveImages(FrameDataHolder images) {

        try {
            framesBuffer.put(images);
        } catch (InterruptedException e) {
            // If interrupted while waiting for adding a new item in the queue
            Log.e(TAG, "receiveImages(), unable to add incoming receiveImages to frames buffer, exception:" + e);
        }

        runOnUiThread(() -> {
            Log.d(TAG, "framebuffer size:" + framesBuffer.size());
            FrameDataHolder poll = framesBuffer.poll();
            if (poll != null) {
                firImage.setImageBitmap(poll.firBitmap);
                rgbImage.setImageBitmap(poll.rgbBitmap);
            }
        });
    }



    /**
     * Defines behaviour for changes of the connection status
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
                        cameraHandler.startStream(AbstractCameraActivity.this);
                    }
                    break;
                    case DISCONNECTING:
                        break;
                    case DISCONNECTED: {
                        onDisconnected();
                        finish();
                    }
                    break;
                }
            });
        }
    };

    abstract void onDisconnected();
}
