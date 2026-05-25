package org.openhab.matter.companion.controller;

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
    public long commissionBleThread(String datasetHex, long pin, int discriminator) {
        return nativeCommissionBleThread(datasetHex, pin, discriminator);
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator) {
        return nativeOpenCommissioningWindow(nodeId, timeoutSeconds, discriminator);
    }

    private static native String nativeControllerMetadata();

    private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);

    private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
}
