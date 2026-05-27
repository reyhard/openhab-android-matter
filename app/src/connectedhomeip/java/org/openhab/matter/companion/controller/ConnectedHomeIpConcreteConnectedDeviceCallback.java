package org.openhab.matter.companion.controller;

import chip.devicecontroller.GetConnectedDeviceCallbackJni;

public final class ConnectedHomeIpConcreteConnectedDeviceCallback
        implements GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
    private final ConnectedHomeIpConnectedDeviceCallback delegate;

    public ConnectedHomeIpConcreteConnectedDeviceCallback(ConnectedHomeIpConnectedDeviceCallback delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate is required");
        }
        this.delegate = delegate;
    }

    @Override
    public void onDeviceConnected(long devicePtr) {
        try {
            delegate.onDeviceConnected(devicePtr);
        } catch (Exception exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Connected device pointer late release failed: " + safeMessage(exception));
        }
    }

    @Override
    public void onConnectionFailure(long nodeId, Exception exception) {
        delegate.onConnectionFailure(nodeId, exception);
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }
}
