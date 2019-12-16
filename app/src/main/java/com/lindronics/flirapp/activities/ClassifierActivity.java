package com.lindronics.flirapp.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.UiThread;

import com.lindronics.flirapp.R;
import com.lindronics.flirapp.camera.FrameDataHolder;
import com.lindronics.flirapp.classification.ModelHandler;


import java.io.IOException;
import java.util.List;

public class ClassifierActivity extends AbstractCameraActivity {

    private static final String TAG = "ClassifierActivity";

    private Handler handler;
    private HandlerThread handlerThread;

    private ModelHandler modelHandler;

    private TextView firstPredictionBox;
    private TextView secondPredictionBox;


    /**
     * Executed when activity is created.
     * Get camera identity from intent and connect to camera.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_classifier);
        super.onCreate(savedInstanceState);

        recreateModelHandler();

        firstPredictionBox = findViewById(R.id.first_prediction_box);
        secondPredictionBox = findViewById(R.id.second_prediction_box);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        recreateModelHandler();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    /**
     * Run procedure in background
     * @param r Runnable to run
     */
    private synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void receiveImages(FrameDataHolder images) {
        super.receiveImages(images);

        // Run classification
        runInBackground(() -> {
            if (modelHandler != null) {
                final List<ModelHandler.Recognition> results =
                        modelHandler.recognizeImage(images);
                Log.i("UPDATE", "Confidence: " + results.get(0).getConfidence());

                runOnUiThread(() -> showResultsInBottomSheet(results));
            }
        });
    }

    /**
     * Display classification results
     * @param results List of results
     */
    @UiThread
    private void showResultsInBottomSheet(List<ModelHandler.Recognition> results) {
        if (results != null && results.size() >= 2) {
            ModelHandler.Recognition recognition = results.get(0);
            if (recognition != null) {
                firstPredictionBox.setText(recognition.toString());
            }

            ModelHandler.Recognition recognition2 = results.get(1);
            if (recognition != null) {
                secondPredictionBox.setText(recognition2.toString());
            }
        }
    }

    /**
     * Reload model handler.
     * Finish activity if unsuccessful.
     */
    private void recreateModelHandler() {
        if (modelHandler != null) {
            modelHandler.close();
            modelHandler = null;
        }

        try {
            modelHandler = new ModelHandler(this, ModelHandler.Device.CPU, 2, true);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }


    /**
     * Behaviour already covered by superclass
     */
    @Override
    void onDisconnected() { }
}
