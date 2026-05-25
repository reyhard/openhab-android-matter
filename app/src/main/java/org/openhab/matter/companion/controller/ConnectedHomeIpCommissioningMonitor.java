package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpCommissioningMonitor {
    void prepare(Object controller) throws Exception;

    MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) throws Exception;
}
