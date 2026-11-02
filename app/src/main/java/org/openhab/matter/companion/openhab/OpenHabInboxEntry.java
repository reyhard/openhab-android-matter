package org.openhab.matter.companion.openhab;

import java.math.BigInteger;
import java.util.Locale;

public final class OpenHabInboxEntry {
    private final String thingUid;
    private final String label;
    private final String flag;
    private final String nodeId;

    public OpenHabInboxEntry(String thingUid, String label, String flag, String nodeId) {
        this.thingUid = nullToEmpty(thingUid);
        this.label = nullToEmpty(label);
        this.flag = nullToEmpty(flag);
        this.nodeId = nullToEmpty(nodeId);
    }

    public String thingUid() {
        return thingUid;
    }

    public String label() {
        return label;
    }

    public String flag() {
        return flag;
    }

    public String nodeId() {
        return nodeId;
    }

    public boolean matterEntry() {
        return thingUid.startsWith("matter:");
    }

    public boolean ignored() {
        return "IGNORED".equalsIgnoreCase(flag.trim());
    }

    public boolean eligibleDiscovery() {
        return matterEntry() && !flag.trim().isEmpty() && !ignored();
    }

    public boolean matchesNodeId(long nodeId) {
        if (!matterEntry()) {
            return false;
        }
        if (matchesNodeIdText(this.nodeId, nodeId)) {
            return true;
        }
        int separator = thingUid.lastIndexOf(':');
        return separator >= 0 && separator + 1 < thingUid.length()
                && matchesNodeIdText(thingUid.substring(separator + 1), nodeId);
    }

    public boolean labelContains(String vendorName, String productName) {
        String vendor = nullToEmpty(vendorName).trim().toLowerCase(Locale.ROOT);
        String product = nullToEmpty(productName).trim().toLowerCase(Locale.ROOT);
        if (vendor.isEmpty() || product.isEmpty()) {
            return false;
        }
        String normalizedLabel = label.toLowerCase(Locale.ROOT);
        return normalizedLabel.contains(vendor) && normalizedLabel.contains(product);
    }

    private static boolean matchesNodeIdText(String value, long nodeId) {
        String text = nullToEmpty(value).trim();
        if (text.isEmpty()) {
            return false;
        }

        boolean hexadecimal = false;
        String candidate = text;
        if (text.length() > 2 && (text.startsWith("0x") || text.startsWith("0X"))) {
            hexadecimal = true;
            candidate = text.substring(2);
        } else if (!isDecimal(text)) {
            if (!isHexadecimal(text)) {
                return false;
            }
            hexadecimal = true;
        }

        if (hexadecimal) {
            String normalized = stripLeadingZeroes(candidate.toLowerCase(Locale.ROOT));
            return !normalized.isEmpty()
                    && normalized.equals(stripLeadingZeroes(Long.toHexString(nodeId)));
        }

        try {
            return new BigInteger(text).equals(new BigInteger(Long.toUnsignedString(nodeId)));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isDecimal(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return !value.isEmpty();
    }

    private static boolean isHexadecimal(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean hexadecimal = (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F');
            if (!hexadecimal) {
                return false;
            }
        }
        return !value.isEmpty();
    }

    private static String stripLeadingZeroes(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
