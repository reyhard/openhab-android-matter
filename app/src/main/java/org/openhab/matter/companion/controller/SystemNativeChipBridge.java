package org.openhab.matter.companion.controller;

import java.util.Objects;

public final class SystemNativeChipBridge implements NativeChipBridge {
    @Override
    public void load(String libraryName) {
        System.loadLibrary(libraryName);
    }

    @Override
    public String metadata() {
        return nativeControllerMetadata();
    }

    @Override
    public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
        Objects.requireNonNull(request, "request");
        String rawResult = nativeCommissionBleThread(
                request.datasetHex(),
                request.setupPin(),
                request.discriminator(),
                request.attestationBypassEnabled(),
                request.controllerState());
        return NativeChipResultParser.parseCommissioningResult(rawResult);
    }

    @Override
    public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
        Objects.requireNonNull(request, "request");
        String rawResult = nativeOpenCommissioningWindow(
                request.nodeId(),
                request.timeoutSeconds(),
                request.discriminator(),
                request.controllerState());
        return NativeChipResultParser.parseOpenCommissioningWindowResult(rawResult);
    }

    private static native String nativeControllerMetadata();

    private static native String nativeCommissionBleThread(
            String datasetHex,
            long pin,
            int discriminator,
            boolean attestationBypassEnabled,
            String controllerState);

    private static native String nativeOpenCommissioningWindow(
            long nodeId,
            int timeoutSeconds,
            int discriminator,
            String controllerState);
}
