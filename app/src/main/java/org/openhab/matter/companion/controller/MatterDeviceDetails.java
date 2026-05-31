package org.openhab.matter.companion.controller;

public final class MatterDeviceDetails {
    private final String vendorName;
    private final String productName;
    private final String softwareVersionString;
    private final String hardwareVersionString;
    private final String partNumber;
    private final Integer batteryPercentRemaining;
    private final Integer batteryQuantity;
    private final String batteryDesignation;
    private final String threadNetworkName;
    private final Integer threadChannel;
    private final String ipv6Address;
    private final Boolean otaUpdatePossible;

    private MatterDeviceDetails(Builder builder) {
        vendorName = clean(builder.vendorName);
        productName = clean(builder.productName);
        softwareVersionString = clean(builder.softwareVersionString);
        hardwareVersionString = clean(builder.hardwareVersionString);
        partNumber = clean(builder.partNumber);
        batteryPercentRemaining = builder.batteryPercentRemaining;
        batteryQuantity = builder.batteryQuantity;
        batteryDesignation = clean(builder.batteryDesignation);
        threadNetworkName = clean(builder.threadNetworkName);
        threadChannel = builder.threadChannel;
        ipv6Address = clean(builder.ipv6Address);
        otaUpdatePossible = builder.otaUpdatePossible;
    }

    public static MatterDeviceDetails empty() {
        return new Builder().build();
    }

    public boolean isEmpty() {
        return vendorName.isEmpty()
                && productName.isEmpty()
                && softwareVersionString.isEmpty()
                && hardwareVersionString.isEmpty()
                && partNumber.isEmpty()
                && batteryPercentRemaining == null
                && batteryQuantity == null
                && batteryDesignation.isEmpty()
                && threadNetworkName.isEmpty()
                && threadChannel == null
                && ipv6Address.isEmpty()
                && otaUpdatePossible == null;
    }

    public String vendorName() {
        return vendorName;
    }

    public String productName() {
        return productName;
    }

    public String softwareVersionString() {
        return softwareVersionString;
    }

    public String hardwareVersionString() {
        return hardwareVersionString;
    }

    public String partNumber() {
        return partNumber;
    }

    public Integer batteryPercentRemaining() {
        return batteryPercentRemaining;
    }

    public Integer batteryQuantity() {
        return batteryQuantity;
    }

    public String batteryDesignation() {
        return batteryDesignation;
    }

    public String threadNetworkName() {
        return threadNetworkName;
    }

    public Integer threadChannel() {
        return threadChannel;
    }

    public String ipv6Address() {
        return ipv6Address;
    }

    public Boolean otaUpdatePossible() {
        return otaUpdatePossible;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {
        private String vendorName;
        private String productName;
        private String softwareVersionString;
        private String hardwareVersionString;
        private String partNumber;
        private Integer batteryPercentRemaining;
        private Integer batteryQuantity;
        private String batteryDesignation;
        private String threadNetworkName;
        private Integer threadChannel;
        private String ipv6Address;
        private Boolean otaUpdatePossible;

        public Builder vendorName(String vendorName) {
            this.vendorName = vendorName;
            return this;
        }

        public Builder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder softwareVersionString(String softwareVersionString) {
            this.softwareVersionString = softwareVersionString;
            return this;
        }

        public Builder hardwareVersionString(String hardwareVersionString) {
            this.hardwareVersionString = hardwareVersionString;
            return this;
        }

        public Builder partNumber(String partNumber) {
            this.partNumber = partNumber;
            return this;
        }

        public Builder batteryPercentRemaining(Integer batteryPercentRemaining) {
            this.batteryPercentRemaining = batteryPercentRemaining;
            return this;
        }

        public Builder batteryQuantity(Integer batteryQuantity) {
            this.batteryQuantity = batteryQuantity;
            return this;
        }

        public Builder batteryDesignation(String batteryDesignation) {
            this.batteryDesignation = batteryDesignation;
            return this;
        }

        public Builder threadNetworkName(String threadNetworkName) {
            this.threadNetworkName = threadNetworkName;
            return this;
        }

        public Builder threadChannel(Integer threadChannel) {
            this.threadChannel = threadChannel;
            return this;
        }

        public Builder ipv6Address(String ipv6Address) {
            this.ipv6Address = ipv6Address;
            return this;
        }

        public Builder otaUpdatePossible(Boolean otaUpdatePossible) {
            this.otaUpdatePossible = otaUpdatePossible;
            return this;
        }

        public MatterDeviceDetails build() {
            return new MatterDeviceDetails(this);
        }
    }
}
