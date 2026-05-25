package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpCommissioningMonitor {
    MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) throws Exception;
}
