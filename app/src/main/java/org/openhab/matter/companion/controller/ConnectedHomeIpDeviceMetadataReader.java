package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpDeviceMetadataReader {
    MatterDeviceDetails readDeviceDetails(Object controller, long nodeId) throws Exception;

    static ConnectedHomeIpDeviceMetadataReader none() {
        return (controller, nodeId) -> MatterDeviceDetails.empty();
    }
}
