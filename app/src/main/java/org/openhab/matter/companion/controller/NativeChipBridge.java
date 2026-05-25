package org.openhab.matter.companion.controller;

public interface NativeChipBridge {
    void load(String libraryName);

    String metadata();

    NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request);

    NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request);
}
