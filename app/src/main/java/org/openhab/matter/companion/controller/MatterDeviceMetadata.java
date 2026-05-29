package org.openhab.matter.companion.controller;

public final class MatterDeviceMetadata {
    private final String vendorName;
    private final String productName;

    public MatterDeviceMetadata(String vendorName, String productName) {
        this.vendorName = nullToEmpty(vendorName);
        this.productName = nullToEmpty(productName);
    }

    public static MatterDeviceMetadata empty() {
        return new MatterDeviceMetadata("", "");
    }

    public String vendorName() {
        return vendorName;
    }

    public String productName() {
        return productName;
    }

    public boolean isEmpty() {
        return vendorName.isEmpty() && productName.isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
