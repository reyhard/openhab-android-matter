package chip.devicecontroller;

import java.util.ArrayList;
import java.util.List;

public final class ChipClusters {
    public static String vendorName = "";
    public static String productName = "";
    public static String softwareVersionString = "";
    public static String hardwareVersionString = "";
    public static String partNumber = "";
    public static Integer batPercentRemaining = null;
    public static Integer batQuantity = null;
    public static String batReplacementDescription = "";
    public static String batCommonDesignation = "";
    public static String threadNetworkName = "";
    public static Integer threadChannel = null;
    public static Boolean updatePossible = null;
    public static List<Object> networkInterfaces = new ArrayList<>();

    private ChipClusters() {}

    public static void reset() {
        vendorName = "";
        productName = "";
        softwareVersionString = "";
        hardwareVersionString = "";
        partNumber = "";
        batPercentRemaining = null;
        batQuantity = null;
        batReplacementDescription = "";
        batCommonDesignation = "";
        threadNetworkName = "";
        threadChannel = null;
        updatePossible = null;
        networkInterfaces = new ArrayList<>();
    }

    public interface CharStringAttributeCallback {
        void onSuccess(String value);

        void onError(String value);
    }

    public interface IntegerAttributeCallback {
        void onSuccess(Integer value);

        void onError(String value);
    }

    public interface BooleanAttributeCallback {
        void onSuccess(Boolean value);

        void onError(String value);
    }

    public interface NetworkInterfacesAttributeCallback {
        void onSuccess(List<Object> value);

        void onError(String value);
    }

    public static final class NetworkInterfaceStruct {
        public final List<byte[]> iPv6Addresses;

        public NetworkInterfaceStruct(List<byte[]> iPv6Addresses) {
            this.iPv6Addresses = iPv6Addresses;
        }
    }

    public static final class BasicInformationCluster {
        public BasicInformationCluster(long devicePointer, int endpoint) {}

        public void readVendorNameAttribute(CharStringAttributeCallback callback) {
            callback.onSuccess(vendorName);
        }

        public void readProductNameAttribute(CharStringAttributeCallback callback) {
            callback.onSuccess(productName);
        }

        public void readSoftwareVersionStringAttribute(CharStringAttributeCallback callback) {
            callback.onSuccess(softwareVersionString);
        }

        public void readHardwareVersionStringAttribute(CharStringAttributeCallback callback) {
            callback.onSuccess(hardwareVersionString);
        }

        public void readPartNumberAttribute(CharStringAttributeCallback callback) {
            callback.onSuccess(partNumber);
        }
    }

    public static final class PowerSourceCluster {
        public PowerSourceCluster(long devicePointer, int endpoint) {}

        public interface BatPercentRemainingAttributeCallback {
            void onSuccess(Integer value);

            void onError(String value);
        }

        public void readBatPercentRemainingAttribute(BatPercentRemainingAttributeCallback callback) {
            callback.onSuccess(batPercentRemaining);
        }

        public void readBatQuantityAttribute(IntegerAttributeCallback callback) {
            callback.onSuccess(batQuantity);
        }

        public void readBatReplacementDescriptionAttribute(CharStringAttributeCallback callback) {
            callback.onSuccess(batReplacementDescription);
        }

        public void readBatCommonDesignationAttribute(CharStringAttributeCallback callback) {
            callback.onSuccess(batCommonDesignation);
        }
    }

    public static final class GeneralDiagnosticsCluster {
        public GeneralDiagnosticsCluster(long devicePointer, int endpoint) {}

        public interface NetworkInterfacesAttributeCallback {
            void onSuccess(List<Object> value);

            void onError(String value);
        }

        public void readNetworkInterfacesAttribute(NetworkInterfacesAttributeCallback callback) {
            callback.onSuccess(networkInterfaces);
        }
    }

    public static final class ThreadNetworkDiagnosticsCluster {
        public ThreadNetworkDiagnosticsCluster(long devicePointer, int endpoint) {}

        public interface NetworkNameAttributeCallback {
            void onSuccess(String value);

            void onError(String value);
        }

        public interface ChannelAttributeCallback {
            void onSuccess(Integer value);

            void onError(String value);
        }

        public void readNetworkNameAttribute(NetworkNameAttributeCallback callback) {
            callback.onSuccess(threadNetworkName);
        }

        public void readChannelAttribute(ChannelAttributeCallback callback) {
            callback.onSuccess(threadChannel);
        }
    }

    public static final class OtaSoftwareUpdateRequestorCluster {
        public OtaSoftwareUpdateRequestorCluster(long devicePointer, int endpoint) {}

        public void readUpdatePossibleAttribute(BooleanAttributeCallback callback) {
            callback.onSuccess(updatePossible);
        }
    }
}
