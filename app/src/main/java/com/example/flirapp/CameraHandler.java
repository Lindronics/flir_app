package com.example.flirapp;

import androidx.annotation.Nullable;

import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CameraHandler {

    // Discovered FLIR cameras
    private LinkedList<Identity> foundCameraIdentities = new LinkedList<>();

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
    public CameraHandler() { }

    /**
     * Start discovery of USB and Emulators
     * @param cameraDiscoveryListener -
     * @param discoveryStatus Current discovery status
     */
    public void startDiscovery(DiscoveryEventListener cameraDiscoveryListener, DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().scan(cameraDiscoveryListener, CommunicationInterface.USB);
        discoveryStatus.started();
    }

    /**
     * Stop discovery of USB and Emulators
     * @param discoveryStatus Current discovery status
     */
    public void stopDiscovery(DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().stop(CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.stopped();
    }

    /**
     * Connect to a FLIR One camera
     * @param identity Identity of a discovered FLIR camera
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
    }
}
