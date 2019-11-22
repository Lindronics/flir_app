package com.example.flirapp;

import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;

class CameraHandler {

    // Connected FLIR Camera
    private Camera camera;

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
    CameraHandler() { }

    /**
     * Start discovery of USB and Emulators
     * @param cameraDiscoveryListener -
     * @param discoveryStatus Current discovery status
     */
    void startDiscovery(DiscoveryEventListener cameraDiscoveryListener, DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().scan(cameraDiscoveryListener, CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.started();
    }

    /**
     * Stop discovery of USB and Emulators
     * @param discoveryStatus Current discovery status
     */
    void stopDiscovery(DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().stop(CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.stopped();
    }

    /**
     * Connect to a FLIR One camera
     * @param identity Identity of a discovered FLIR camera
     * @param connectionStatusListener Event listener
     */
    void connect(Identity identity, ConnectionStatusListener connectionStatusListener) {
        camera = new Camera();
        camera.connect(identity, connectionStatusListener);
    }

    /**
     * Disconnect a FLIR One camera
     */
    void disconnect() {
        if (camera == null) {
            return;
        }
        if (camera.isGrabbing()) {
            camera.unsubscribeAllStreams();
        }
        camera.disconnect();
        camera = null;
    }


    Boolean isEmulator(Identity identity) {
        return identity.deviceId.contains("EMULATED FLIR ONE");
    }

    Boolean isCamera(Identity identity) {
        boolean isFlirOneEmulator = identity.deviceId.contains("EMULATED FLIR ONE");
        boolean isCppEmulator = identity.deviceId.contains("C++ Emulator");
        return !isFlirOneEmulator && !isCppEmulator;
    }
}
