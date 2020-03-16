package com.lindronics.flirapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.UiThread;

import com.lindronics.flirapp.R;
import com.lindronics.flirapp.camera.FrameDataHolder;
import com.lindronics.flirapp.classification.ModelHandler;

import java.io.IOException;
import java.util.List;

public class ClassifierActivity extends AbstractCameraActivity {

    private static final String TAG = "ClassifierActivity";

    private ModelHandler modelHandler;

    private TextView firstPredictionBox;
    private TextView secondPredictionBox;
    private TextView thirdPredictionBox;
    private ProgressBar firstPredictionPb;
    private ProgressBar secondPredictionPb;
    private ProgressBar thirdPredictionPb;

    /**
     * Used so that classification is not run on every frame.
     */
    private int frameCounter;

    /**
     * Frames to skip before classifying.
     */
    private static final int skipFrames = 3;


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
        thirdPredictionBox = findViewById(R.id.third_prediction_box);
        firstPredictionPb = findViewById(R.id.first_prediction_bar);
        secondPredictionPb = findViewById(R.id.second_prediction_bar);
        thirdPredictionPb = findViewById(R.id.third_prediction_bar);

        frameCounter = 0;
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        recreateModelHandler();
    }

    @Override
    public void receiveImages(FrameDataHolder images) {
        super.receiveImages(images);

        // Skip frames
        frameCounter = (frameCounter + 1) % skipFrames;
        if (frameCounter != 0) {
            return;
        }

        // Run classification
        runInBackground(() -> {
            long t1 = System.currentTimeMillis();
            if (modelHandler != null) {
                final List<ModelHandler.Recognition> results =
                        modelHandler.recognizeImage(images);
                Log.i("UPDATE", "Confidence: " + results.get(0).getConfidence());

                runOnUiThread(() -> showResultsInBottomSheet(results));
            }
            long t2 = System.currentTimeMillis();
            Log.i("ELAPSED", (t2 - t1) + " ms");
        });
    }

    /**
     * Display classification results
     *
     * @param results List of results
     */
    @UiThread
    private void showResultsInBottomSheet(List<ModelHandler.Recognition> results) {
        if (results != null && results.size() >= 2) {
            ModelHandler.Recognition recognition = results.get(0);
            if (recognition != null) {
                firstPredictionBox.setText(recognition.toString());
                firstPredictionPb.setProgress((int)(float) (recognition.getConfidence() * 100));
            }

            ModelHandler.Recognition recognition2 = results.get(1);
            if (recognition2 != null) {
                secondPredictionBox.setText(recognition2.toString());
                secondPredictionPb.setProgress((int)(float) (recognition2.getConfidence() * 100));
            }

            ModelHandler.Recognition recognition3 = results.get(2);
            if (recognition3 != null) {
                thirdPredictionBox.setText(recognition3.toString());
                thirdPredictionPb.setProgress((int)(float) (recognition3.getConfidence() * 100));
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
            modelHandler = new ModelHandler(this, ModelHandler.Device.GPU, 2, false);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
        frameCounter = 0;
    }


    /**
     * Behaviour already covered by superclass
     */
    @Override
    void onDisconnected() {
    }
}
