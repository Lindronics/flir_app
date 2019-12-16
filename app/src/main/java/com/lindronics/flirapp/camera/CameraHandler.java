package com.lindronics.flirapp.camera;

import android.graphics.Bitmap;
import android.util.Log;

import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.streaming.ThermalImageStreamListener;

import java.util.Objects;

public class CameraHandler {

    private static final String TAG = "Camera handler";

    // Connected FLIR Camera
    private Camera camera;

    public interface StreamDataListener {
        void images(FrameDataHolder dataHolder);

        void images(Bitmap msxBitmap, Bitmap dcBitmap);
    }

    private StreamDataListener streamDataListener;

    /**
     * Possible discovery statuses
     */
    public interface DiscoveryStatus {
        void started();

        void stopped();
    }

    /**
     * Empty constructor for now
     */
    public CameraHandler() {
    }

    /**
     * Start discovery of USB and Emulators
     *
     * @param cameraDiscoveryListener -
     * @param discoveryStatus         Current discovery status
     */
    public void startDiscovery(DiscoveryEventListener cameraDiscoveryListener, DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().scan(cameraDiscoveryListener, CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.started();
    }

    /**
     * Stop discovery of USB and Emulators
     *
     * @param discoveryStatus Current discovery status
     */
    public void stopDiscovery(DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().stop(CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.stopped();
    }

    /**
     * Connect to a FLIR One camera
     *
     * @param identity                 Identity of a discovered FLIR camera
     * @param connectionStatusListener Event listener
     */
    public void connect(Identity identity, ConnectionStatusListener connectionStatusListener) {
        camera = new Camera();
        camera.connect(identity, connectionStatusListener);
    }

    /**
     * Disconnect a FLIR One camera
     */
    public void disconnect() {
        if (camera == null) {
            return;
        }
        if (camera.isGrabbing()) {
            camera.unsubscribeAllStreams();
        }
        camera.disconnect();
        camera = null;
    }

    /**
     * Determines whether a device is the FLIR One emulator
     *
     * @param identity identity of the device
     * @return true if the device is the emulator
     */
    public Boolean isEmulator(Identity identity) {
        return identity.deviceId.contains("EMULATED FLIR ONE");
    }

    /**
     * Determines whether a device is a FLIR One camera
     *
     * @param identity identity of the device
     * @return true if the device is a FLIR One camera
     */
    public Boolean isCamera(Identity identity) {
        boolean isFlirOneEmulator = identity.deviceId.contains("EMULATED FLIR ONE");
        boolean isCppEmulator = identity.deviceId.contains("C++ Emulator");
        return !isFlirOneEmulator && !isCppEmulator;
    }

    /**
     * Called when a new image is available (non-UI-thread)
     */
    private final ThermalImageStreamListener thermalImageStreamListener = new ThermalImageStreamListener() {
        @Override
        public void onImageReceived() {
            Log.d(TAG, "onImageReceived(), we got another ThermalImage");
            camera.withImage(this, handleIncomingImage);
        }
    };

    /**
     * Subscribes to camera stream
     */
    public void startStream(StreamDataListener listener) {
        this.streamDataListener = listener;
        camera.subscribeStream(thermalImageStreamListener);
    }

    /**
     * For processing images and updating UI
     */
    private final Camera.Consumer<ThermalImage> handleIncomingImage = new Camera.Consumer<ThermalImage>() {

        @Override
        public void accept(ThermalImage thermalImage) {

            // Get a bitmap with only IR data
            Objects.requireNonNull(thermalImage.getFusion()).setFusionMode(FusionMode.THERMAL_ONLY);
            Bitmap firBitmap = BitmapAndroid.createBitmap(thermalImage.getImage()).getBitMap();

            // Get a bitmap with the visual image
            Bitmap rgbBitmap = BitmapAndroid.createBitmap(Objects.requireNonNull(thermalImage.getFusion().getPhoto())).getBitMap();

            // Add images to cache
            streamDataListener.images(firBitmap, rgbBitmap);
        }
    };
}
