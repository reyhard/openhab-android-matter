package org.openhab.matter.companion.controller;

public interface NativeChipBridge {
    void load(String libraryName);

    String metadata();

    long commissionBleThread(String datasetHex, long pin, int discriminator);

    String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
}
