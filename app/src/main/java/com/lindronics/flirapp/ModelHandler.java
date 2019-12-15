package com.lindronics.flirapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Trace;

import androidx.annotation.NonNull;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

class ModelHandler {

    /**
     * Interpreter for inference
     */
    private Interpreter tflite;

    /**
     * Loaded TF model
     */
    private MappedByteBuffer tfliteModel;

    /**
     * Labels corresponding to the output of the model.
     */
    private List<String> labels;

    /**
     * Optional GPU delegate for hardware acceleration.
     */
    private GpuDelegate gpuDelegate = null;

    /**
     * Optional NNAPI delegate for hardware acceleration.
     */
    private NnApiDelegate nnApiDelegate = null;

    /**
     * Output probability TensorBuffer.
     */
    private TensorBuffer outputProbabilityBuffer;

    /**
     * Processor to apply post-processing of the output probability.
     */
    private TensorProcessor probabilityProcessor;

    /**
     * Image size along the x axis.
     */
    private int imageHeight;

    /**
     * Image size along the y axis.
     */
    private int imageWidth;

    /**
     * Number of results to show in the UI.
     */
    private static final int MAX_RESULTS = 3;

    /**
     * Possible devices to run the model on
     */
    public enum Device {
        CPU,
        NNAPI,
        GPU
    }

    ModelHandler(Activity activity, Device device, int numThreads) throws IOException {

        // Initialize OpenCV
        Loader.load(opencv_java.class);

        // Load model
        tfliteModel = FileUtil.loadMappedFile(activity, "model.tflite");

        // Select device
        Interpreter.Options tfliteOptions = new Interpreter.Options();
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

        // Load labels out from the label file.
        labels = FileUtil.loadLabels(activity, "labels.txt");

        // Read type and shape of input and output tensors, respectively.
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape();
        imageWidth = imageShape[1];
        imageHeight = imageShape[2];

        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape();
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        // Create the output tensor and its processor.
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        // Create the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(0.0f, 1.0f)).build();
    }


    /**
     * Runs inference and returns the classification results.
     */
    List<Recognition> recognizeImage(final Bitmap rgb, final Bitmap fir) {
        Trace.beginSection("recognizeImage");

        // Load images
        Trace.beginSection("loadImage");
        FloatBuffer inputImageBuffer;
        try {
            inputImageBuffer = loadImage(rgb, fir);
        } catch (IOException e) {
            return null;
        }
        Trace.endSection();

        // Runs the inference call.
        Trace.beginSection("runInference");
        tflite.run(inputImageBuffer, outputProbabilityBuffer.getBuffer().rewind());
        Trace.endSection();

        // Gets the map of label and probability.
        Map<String, Float> labeledProbability =
            new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                .getMapWithFloatValue();
        Trace.endSection();

        // Gets top-k results.
        return getTopKProbability(labeledProbability);
    }

    /**
     * Loads input image, and applies pre-processing.
     */
    private FloatBuffer loadImage(final Bitmap rgb, final Bitmap fir) throws IOException {

        // Convert to OpenCV Mat and resize to required input size
        Mat rgbMat = new Mat(rgb.getWidth(), rgb.getHeight(), CvType.CV_32F);
        Utils.bitmapToMat(rgb, rgbMat);

        Mat firMat = new Mat();
        Utils.bitmapToMat(fir, firMat);

        Imgproc.resize(rgbMat, rgbMat, new Size(imageWidth, imageHeight));
        Imgproc.resize(firMat, firMat, new Size(imageWidth, imageHeight));

        // Convert to NdArray
        NativeImageLoader loader = new NativeImageLoader(imageHeight, imageWidth, 3);

        INDArray rgbArray = loader.asMatrix(rgbMat);
        INDArray firArray = loader.asMatrix(rgbMat);

        // Collapse FIR to 1 channel, stack FIR and RGB together
        INDArray firImageMean = firArray.mean(1);
        firImageMean = Nd4j.expandDims(firImageMean, 0);

        INDArray stackedImage = Nd4j.concat(1, rgbArray, firImageMean);
        stackedImage = stackedImage.divi(255);

        return stackedImage.data().asNio().asFloatBuffer();
    }

    /**
     * Gets the top-k results.
     */
    private static List<Recognition> getTopKProbability(Map<String, Float> labelProb) {
        // Find the best classifications.
        PriorityQueue<Recognition> pq = new PriorityQueue<>(
            MAX_RESULTS,
            (lhs, rhs) -> {
                // Intentionally reversed to put high confidence at the head of the queue.
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
            });

        for (Map.Entry<String, Float> entry : labelProb.entrySet()) {
            pq.add(new Recognition("" + entry.getKey(), entry.getKey(), entry.getValue()));
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    /** Closes the interpreter and model to release resources. */
    void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (nnApiDelegate != null) {
            nnApiDelegate.close();
            nnApiDelegate = null;
        }
        tfliteModel = null;
    }


    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    static class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final String id;

        /**
         * Display name for the recognition.
         */
        private final String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;


        Recognition(
                final String id, final String title, final Float confidence) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
        }

        String getId() {
            return id;
        }

        String getTitle() {
            return title;
        }

        Float getConfidence() {
            return confidence;
        }

        @Override
        @NonNull
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format(Locale.UK, "(%.1f%%) ", confidence * 100.0f);
            }

            return resultString.trim();
        }
    }

}
