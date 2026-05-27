package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpControllerGateway extends ConnectedHomeIpFabricRestoreChecker, ConnectedHomeIpRuntimePreflightChecker {
    MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) throws Exception;

    MatterOpenCommissioningWindowResult openCommissioningWindow(
            ConnectedHomeIpOpenCommissioningWindowRequest request) throws Exception;
}
