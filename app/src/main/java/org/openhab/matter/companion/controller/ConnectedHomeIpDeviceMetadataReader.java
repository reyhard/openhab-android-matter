package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpDeviceMetadataReader {
    MatterDeviceMetadata readVendorAndProduct(Object controller, long nodeId) throws Exception;

    MatterDeviceDetails readDeviceDetails(Object controller, long nodeId) throws Exception;

    static ConnectedHomeIpDeviceMetadataReader none() {
        return new ConnectedHomeIpDeviceMetadataReader() {
            @Override
            public MatterDeviceMetadata readVendorAndProduct(Object controller, long nodeId) {
                return MatterDeviceMetadata.empty();
            }

            @Override
            public MatterDeviceDetails readDeviceDetails(Object controller, long nodeId) {
                return MatterDeviceDetails.empty();
            }
        };
    }
}
