package com.lindronics.flirapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;
import com.lindronics.flirapp.R;
import com.lindronics.flirapp.camera.FrameDataHolder;
import com.lindronics.flirapp.camera.ImageWriter;

public class CameraActivity extends AbstractCameraActivity {

    private static final String TAG = "CameraActivity";

    private ToggleButton cameraButton;

    private ImageWriter imageWriter = null;


    /**
     * Executed when activity is created.
     * Get camera identity from intent and connect to camera.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_camera);
        super.onCreate(savedInstanceState);

        cameraButton = findViewById(R.id.camera_button);
    }

    @Override
    public void receiveImages(FrameDataHolder images) {

        super.receiveImages(images);

        runInBackground(() -> {
            if (imageWriter != null) {
                imageWriter.saveImages(images);
            }
        });
    }

    /**
     * Event listener for starting or stopping camera capture/recording
     *
     * @param view Toggle button
     */
    public void toggleCapture(View view) {
        ToggleButton button = (ToggleButton) view;
        if (button.isChecked()) {
            startCapture();
        } else {
            endCapture();
        }
    }

    /**
     * Start capturing/recording data
     */
    private void startCapture() {
        cameraButton.setBackground(getDrawable(R.drawable.ic_camera_capture_recording));
        imageWriter = new ImageWriter(this);
    }

    /**
     * Finish capturing/recording data
     */
    private void endCapture() {
        cameraButton.setBackground(getDrawable(R.drawable.ic_camera_capture_ready));
        imageWriter = null;
    }

    /**
     * End capture when disconnected
     */
    @Override
    void onDisconnected() {
        endCapture();
    }
}
