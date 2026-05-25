package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpControllerGateway {
    MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) throws Exception;

    MatterOpenCommissioningWindowResult openCommissioningWindow(
            ConnectedHomeIpOpenCommissioningWindowRequest request) throws Exception;
}
