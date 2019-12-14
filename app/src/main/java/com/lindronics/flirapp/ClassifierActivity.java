package com.lindronics.flirapp;

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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.LinkedBlockingQueue;

public class ClassifierActivity extends AppCompatActivity {

    private static final String TAG = "ClassifierActivity";

    private CameraHandler cameraHandler;

    private ImageView rgbImage;
    private ImageView firImage;

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue<>(21);

    private ImageWriter imageWriter = null;

    ModelHandler modelHandler;


    /**
     * Executed when activity is created.
     * Get camera identity from intent and connect to camera.
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);

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

        modelHandler = new ModelHandler(this, ModelHandler.Device.GPU, 1);
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
                        cameraHandler.startStream(streamDataListener);
                    }
                    break;
                    case DISCONNECTING:
                        break;
                    case DISCONNECTED: {
                        finish();
                    }
                    break;
                }
            });
        }
    };
}
