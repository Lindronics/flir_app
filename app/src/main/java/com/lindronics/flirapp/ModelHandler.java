package com.lindronics.flirapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.linalg.api.ndarray.INDArray;

import org.nd4j.linalg.factory.Nd4j;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class ModelHandler {

    /**
     * Interpreter for inference
     */
    private Interpreter tflite;

    /**
     * Options for configuring the Interpreter.
     */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

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
     * Input image TensorBuffer.
     */
    private TensorImage inputImageBuffer;

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
    private int imageSizeX;

    /**
     * Image size along the y axis.
     */
    private int imageSizeY;

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

    public ModelHandler(Activity activity, Device device, int numThreads) throws IOException {

//        if (!OpenCVLoader.initDebug()) {
//            Log.e(">>>", "OpenCV not initialized!");
//        }
        Loader.load(opencv_java.class);



        tfliteModel = FileUtil.loadMappedFile(activity, "model.tflite");

        // Select device
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

        // Loads labels out from the label file.
        labels = FileUtil.loadLabels(activity, "labels.txt");

        // Reads type and shape of input and output tensors, respectively.
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape();
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape();
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(imageDataType);

        // Creates the output tensor and its processor.
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        // Creates the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(0.0f, 1.0f)).build();

    }


    /**
     * Runs inference and returns the classification results.
     */
    public List<Recognition> recognizeImage(final Bitmap rgb, final Bitmap fir, int sensorOrientation) {
        Trace.beginSection("recognizeImage");

        Trace.beginSection("loadImage");
        long startTimeForLoadImage = SystemClock.uptimeMillis();


//        inputImageBuffer = loadImage(bitmap, sensorOrientation);
        float[] inputImageBuffer = null;
        try {
            inputImageBuffer = loadImage(rgb, fir, sensorOrientation);
        } catch (IOException e) {
            return null;
        }

        byte[] i = new byte[inputImageBuffer.length];

        ByteBuffer buffer = ByteBuffer.wrap(i);
        FloatBuffer fb = buffer.asFloatBuffer();

        float[] floatArray = new float[fb.limit()];
        fb.get(floatArray);


        long endTimeForLoadImage = SystemClock.uptimeMillis();
        Trace.endSection();

        // Runs the inference call.
        Trace.beginSection("runInference");
//        long startTimeForReference = SystemClock.uptimeMillis();
//        tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());

        tflite.run(fb, outputProbabilityBuffer.getBuffer().rewind());

//        long endTimeForReference = SystemClock.uptimeMillis();
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
    private float[] loadImage(final Bitmap rgb, final Bitmap fir, int sensorOrientation) throws IOException {


//        int[] rgbArray = new int[rgb.getWidth() * rgb.getHeight() * 3];
//        rgb.getPixels(rgbArray, 0, rgb.getWidth(), 0, 0, rgb.getWidth(), rgb.getHeight());
//        int[] shape = {rgb.getWidth(), rgb.getHeight(), 3};
//        Mat test = converter.convert(rgbArray);
//
//        int[] firArray = new int[fir.getWidth() * fir.getHeight() * 3];
//        rgb.getPixels(rgbArray, 0, rgb.getWidth(), 0, 0, rgb.getWidth(), rgb.getHeight());
//        int[] shape = {rgb.getWidth(), rgb.getHeight(), 3};

        Log.i(">>>>>>>TAG", "GOT HERE" );

        Mat rgbArray = new Mat(rgb.getWidth(), rgb.getHeight(), CvType.CV_32F);
        Utils.bitmapToMat(rgb, rgbArray);

        Mat firArray = new Mat();
        Utils.bitmapToMat(fir, firArray);

        Imgproc.resize(rgbArray, rgbArray, firArray.size());

//        ArrayList<Mat> channels = new ArrayList<>();
//        channels.add(rgbArray);
//        channels.add(firArray);
        Log.i(">>>>>>>TAG", "SHAPE: " + firArray.size() + ", " + rgbArray.size());
        Log.i(">>>>>>>TAG", "CHANS: " + firArray.channels() + ", " + rgbArray.channels());

        NativeImageLoader loader = new NativeImageLoader();

        INDArray rgbImage = loader.asMatrix(rgbArray);
        INDArray firImage = loader.asMatrix(rgbArray);

        INDArray firImageMean = firImage.mean(2);

        INDArray stackedImage = Nd4j.stack(2, rgbImage, firImage);
        stackedImage = stackedImage.divi(255);

        float[] result = stackedImage.data().asFloat();

//        Core.merge(channels, )

        return result;
//        // Loads bitmap into a TensorImage.
//        inputImageBuffer.load(bitmap);
//        inputImageBuffer.load(bitmap);
//
//
//
//        // Creates processor for the TensorImage.
//        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
//        int numRoration = sensorOrientation / 90;
//
//        ImageProcessor imageProcessor =
//            new ImageProcessor.Builder()
//                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
//                .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
//                .add(new Rot90Op(numRoration))
//                .build();
//        return imageProcessor.process(inputImageBuffer);
    }

    /**
     * Gets the top-k results.
     */
    private static List<Recognition> getTopKProbability(Map<String, Float> labelProb) {
        // Find the best classifications.
        PriorityQueue<Recognition> pq = new PriorityQueue<>(
            MAX_RESULTS,
            new Comparator<Recognition>() {
                @Override
                public int compare(Recognition lhs, Recognition rhs) {
                    // Intentionally reversed to put high confidence at the head of the queue.
                    return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                }
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
    public void close() {
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
    public static class Recognition {
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


        public Recognition(
                final String id, final String title, final Float confidence) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            return resultString.trim();
        }
    }

}
