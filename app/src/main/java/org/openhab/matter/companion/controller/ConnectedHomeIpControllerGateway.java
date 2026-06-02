package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpControllerGateway extends ConnectedHomeIpFabricRestoreChecker, ConnectedHomeIpRuntimePreflightChecker {
    MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) throws Exception;

    MatterOpenCommissioningWindowResult openCommissioningWindow(
            ConnectedHomeIpOpenCommissioningWindowRequest request) throws Exception;

    default MatterDeviceDetails readDeviceDetails(long nodeId) throws Exception {
        throw new UnsupportedOperationException("Matter device details are not supported by this gateway.");
    }

    default void unpair(long nodeId) throws Exception {
        throw new UnsupportedOperationException("Matter unpair is not supported by this gateway.");
    }
}
