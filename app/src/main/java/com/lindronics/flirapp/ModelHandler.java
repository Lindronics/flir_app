package com.lindronics.flirapp;

import android.app.Activity;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.model.Model;


import java.io.IOException;
import java.nio.MappedByteBuffer;

public class ModelHandler {

    /** Interpreter for inference */
    private Interpreter tflite;

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    /** Loaded TF model */
    private MappedByteBuffer tfliteModel;

    /** Optional GPU delegate for hardware accleration. */
    private GpuDelegate gpuDelegate = null;

    /** Optional NNAPI delegate for hardware accleration. */
    private NnApiDelegate nnApiDelegate = null;

    /** Possible devices to run the model on */
    public enum Device {
        CPU,
        NNAPI,
        GPU
    }

    public ModelHandler(Activity activity, Device device, int numThreads) {
        try {
            tfliteModel = FileUtil.loadMappedFile(activity, "model.tflite");

            switch (device) {
                case NNAPI:
                    nnApiDelegate = new NnApiDelegate();
                    tfliteOptions.addDelegate(nnApiDelegate);
                    break;
                case GPU:
                    gpuDelegate = new GpuDelegate();
                    tfliteOptions.addDelegate(gpuDelegate);
                    break;
                case CPU:
                    break;
            }
            tfliteOptions.setNumThreads(numThreads);
            tflite = new Interpreter(tfliteModel, tfliteOptions);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String getPrediction(Bitmap fir, Bitmap rgb) {
        return "";
    }


}
