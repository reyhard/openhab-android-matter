package org.openhab.matter.companion.controller;

import chip.devicecontroller.OpenCommissioningCallback;

public final class ConnectedHomeIpConcreteOpenCommissioningCallback implements OpenCommissioningCallback {
    private final ConnectedHomeIpOpenCommissioningWindowCallback delegate;

    public ConnectedHomeIpConcreteOpenCommissioningCallback(
            ConnectedHomeIpOpenCommissioningWindowCallback delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate is required");
        }
        this.delegate = delegate;
    }

    @Override
    public void onError(int status, long deviceId) {
        delegate.onError(status, deviceId);
    }

    @Override
    public void onSuccess(long deviceId, String manualPairingCode, String qrCode) {
        delegate.onSuccess(deviceId, manualPairingCode, qrCode);
    }
}
