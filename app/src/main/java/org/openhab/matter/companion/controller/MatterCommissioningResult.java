package org.openhab.matter.companion.controller;

public final class MatterCommissioningResult {
    private final long nodeId;
    private final String controllerState;
    private final String vendorName;
    private final String productName;

    public MatterCommissioningResult(long nodeId, String controllerState) {
        this(nodeId, controllerState, "", "");
    }

    public MatterCommissioningResult(long nodeId, String controllerState, String vendorName, String productName) {
        this.nodeId = nodeId;
        this.controllerState = nullToEmpty(controllerState);
        this.vendorName = nullToEmpty(vendorName);
        this.productName = nullToEmpty(productName);
    }

    public long nodeId() {
        return nodeId;
    }

    public String controllerState() {
        return controllerState;
    }

    public String vendorName() {
        return vendorName;
    }

    public String productName() {
        return productName;
    }

    public MatterCommissioningResult withMetadata(MatterDeviceMetadata metadata) {
        MatterDeviceMetadata safeMetadata = metadata == null ? MatterDeviceMetadata.empty() : metadata;
        return new MatterCommissioningResult(
                nodeId,
                controllerState,
                safeMetadata.vendorName(),
                safeMetadata.productName());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
