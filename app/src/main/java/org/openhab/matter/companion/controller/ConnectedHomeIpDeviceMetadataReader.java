package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpDeviceMetadataReader {
    MatterDeviceMetadata readVendorAndProduct(Object controller, long nodeId) throws Exception;

    static ConnectedHomeIpDeviceMetadataReader none() {
        return (controller, nodeId) -> MatterDeviceMetadata.empty();
    }
}
