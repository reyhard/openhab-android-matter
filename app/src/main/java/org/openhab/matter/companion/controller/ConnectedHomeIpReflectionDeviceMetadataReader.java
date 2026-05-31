package org.openhab.matter.companion.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConnectedHomeIpReflectionDeviceMetadataReader implements ConnectedHomeIpDeviceMetadataReader {
    private static final int DEVICE_DETAILS_ENDPOINT = 0;
    private static final long ATTRIBUTE_TIMEOUT_MILLIS = 1_500L;
    private static final String CLUSTER_PREFIX = "chip.devicecontroller.ChipClusters$";
    private static final String BASIC_INFORMATION_CLUSTER_CLASS = CLUSTER_PREFIX + "BasicInformationCluster";
    private static final String POWER_SOURCE_CLUSTER_CLASS = CLUSTER_PREFIX + "PowerSourceCluster";
    private static final String GENERAL_DIAGNOSTICS_CLUSTER_CLASS = CLUSTER_PREFIX + "GeneralDiagnosticsCluster";
    private static final String THREAD_NETWORK_DIAGNOSTICS_CLUSTER_CLASS =
            CLUSTER_PREFIX + "ThreadNetworkDiagnosticsCluster";
    private static final String OTA_SOFTWARE_UPDATE_REQUESTOR_CLUSTER_CLASS =
            CLUSTER_PREFIX + "OtaSoftwareUpdateRequestorCluster";
    private static final String CHAR_STRING_CALLBACK_CLASS = CLUSTER_PREFIX + "CharStringAttributeCallback";
    private static final String INTEGER_CALLBACK_CLASS = CLUSTER_PREFIX + "IntegerAttributeCallback";
    private static final String BOOLEAN_CALLBACK_CLASS = CLUSTER_PREFIX + "BooleanAttributeCallback";
    private static final String NETWORK_INTERFACES_CALLBACK_CLASS =
            CLUSTER_PREFIX + "NetworkInterfacesAttributeCallback";

    private final ConnectedHomeIpDevicePointerProvider devicePointerProvider;
    private final ClassLoader classLoader;
    private final long attributeTimeoutMillis;

    public ConnectedHomeIpReflectionDeviceMetadataReader(
            ConnectedHomeIpDevicePointerProvider devicePointerProvider) {
        this(devicePointerProvider, ATTRIBUTE_TIMEOUT_MILLIS);
    }

    ConnectedHomeIpReflectionDeviceMetadataReader(
            ConnectedHomeIpDevicePointerProvider devicePointerProvider,
            long attributeTimeoutMillis) {
        if (devicePointerProvider == null) {
            throw new IllegalArgumentException("devicePointerProvider is required");
        }
        if (attributeTimeoutMillis <= 0L) {
            throw new IllegalArgumentException("attributeTimeoutMillis must be positive");
        }
        this.devicePointerProvider = devicePointerProvider;
        this.classLoader = ConnectedHomeIpReflectionDeviceMetadataReader.class.getClassLoader();
        this.attributeTimeoutMillis = attributeTimeoutMillis;
    }

    @Override
    public MatterDeviceMetadata readVendorAndProduct(Object controller, long nodeId) throws Exception {
        MatterDeviceDetails.Builder builder = new MatterDeviceDetails.Builder();
        try (ConnectedHomeIpDevicePointer pointer = devicePointerProvider.acquire(controller, nodeId)) {
            readBasicVendorAndProduct(pointer.value(), builder);
        }
        MatterDeviceDetails details = builder.build();
        return new MatterDeviceMetadata(details.vendorName(), details.productName());
    }

    @Override
    public MatterDeviceDetails readDeviceDetails(Object controller, long nodeId) throws Exception {
        MatterDeviceDetails.Builder builder = new MatterDeviceDetails.Builder();
        try (ConnectedHomeIpDevicePointer pointer = devicePointerProvider.acquire(controller, nodeId)) {
            readBasicInformation(pointer.value(), builder);
            readPowerSource(pointer.value(), builder);
            readThreadNetworkDiagnostics(pointer.value(), builder);
            readOtaSoftwareUpdateRequestor(pointer.value(), builder);
            readGeneralDiagnostics(pointer.value(), builder);
        }
        return builder.build();
    }

    private void readBasicInformation(long devicePointer, MatterDeviceDetails.Builder builder) {
        Object cluster = newCluster(BASIC_INFORMATION_CLUSTER_CLASS, devicePointer, DEVICE_DETAILS_ENDPOINT);
        if (cluster == null) {
            return;
        }
        readBasicVendorAndProduct(cluster, builder);
        builder.softwareVersionString(stringAttribute(
                cluster,
                "readSoftwareVersionStringAttribute",
                "software version string"));
        builder.hardwareVersionString(stringAttribute(
                cluster,
                "readHardwareVersionStringAttribute",
                "hardware version string"));
        builder.partNumber(stringAttribute(cluster, "readPartNumberAttribute", "part number"));
    }

    private void readBasicVendorAndProduct(long devicePointer, MatterDeviceDetails.Builder builder) {
        Object cluster = newCluster(BASIC_INFORMATION_CLUSTER_CLASS, devicePointer, DEVICE_DETAILS_ENDPOINT);
        if (cluster == null) {
            return;
        }
        readBasicVendorAndProduct(cluster, builder);
    }

    private void readBasicVendorAndProduct(Object cluster, MatterDeviceDetails.Builder builder) {
        builder.vendorName(stringAttribute(cluster, "readVendorNameAttribute", "vendor name"));
        builder.productName(stringAttribute(cluster, "readProductNameAttribute", "product name"));
    }

    private void readPowerSource(long devicePointer, MatterDeviceDetails.Builder builder) {
        Object cluster = newCluster(POWER_SOURCE_CLUSTER_CLASS, devicePointer, DEVICE_DETAILS_ENDPOINT);
        if (cluster == null) {
            return;
        }
        builder.batteryPercentRemaining(integerAttribute(
                cluster,
                "readBatPercentRemainingAttribute",
                "battery percent remaining"));
        builder.batteryQuantity(integerAttribute(cluster, "readBatQuantityAttribute", "battery quantity"));
        String replacementDescription = stringAttribute(
                cluster,
                "readBatReplacementDescriptionAttribute",
                "battery replacement description");
        String commonDesignation = stringAttribute(cluster, "readBatCommonDesignationAttribute", "battery designation");
        builder.batteryDesignation(replacementDescription.isEmpty() ? commonDesignation : replacementDescription);
    }

    private void readThreadNetworkDiagnostics(long devicePointer, MatterDeviceDetails.Builder builder) {
        Object cluster = newCluster(THREAD_NETWORK_DIAGNOSTICS_CLUSTER_CLASS, devicePointer, DEVICE_DETAILS_ENDPOINT);
        if (cluster == null) {
            return;
        }
        builder.threadNetworkName(stringAttribute(cluster, "readNetworkNameAttribute", "Thread network name"));
        builder.threadChannel(integerAttribute(cluster, "readChannelAttribute", "Thread channel"));
    }

    private void readOtaSoftwareUpdateRequestor(long devicePointer, MatterDeviceDetails.Builder builder) {
        Object cluster = newCluster(OTA_SOFTWARE_UPDATE_REQUESTOR_CLUSTER_CLASS, devicePointer, DEVICE_DETAILS_ENDPOINT);
        if (cluster == null) {
            return;
        }
        builder.otaUpdatePossible(booleanAttribute(cluster, "readUpdatePossibleAttribute", "OTA update possible"));
    }

    private void readGeneralDiagnostics(long devicePointer, MatterDeviceDetails.Builder builder) {
        Object cluster = newCluster(GENERAL_DIAGNOSTICS_CLUSTER_CLASS, devicePointer, DEVICE_DETAILS_ENDPOINT);
        if (cluster == null) {
            return;
        }
        Object value = attribute(cluster, "readNetworkInterfacesAttribute", NETWORK_INTERFACES_CALLBACK_CLASS,
                "network interfaces");
        String address = ipv6Address(value);
        if (!address.isEmpty()) {
            builder.ipv6Address(address);
        }
    }

    private String stringAttribute(Object cluster, String methodName, String label) {
        Object value = attribute(cluster, methodName, CHAR_STRING_CALLBACK_CLASS, label);
        return value == null ? "" : value.toString().trim();
    }

    private Integer integerAttribute(Object cluster, String methodName, String label) {
        Object value = attribute(cluster, methodName, INTEGER_CALLBACK_CLASS, label);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.valueOf(((String) value).trim());
            } catch (NumberFormatException exception) {
                ConnectedHomeIpDiagnostics.emit("Unable to decode Matter " + label + ": " + safeMessage(exception));
            }
        }
        return null;
    }

    private Boolean booleanAttribute(Object cluster, String methodName, String label) {
        Object value = attribute(cluster, methodName, BOOLEAN_CALLBACK_CLASS, label);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.valueOf(((String) value).trim());
        }
        return null;
    }

    private Object attribute(Object cluster, String methodName, String callbackClassName, String label) {
        Class<?> callbackClass = classOrNull(callbackClassName, label + " callback");
        if (callbackClass == null) {
            return null;
        }
        Method readMethod;
        try {
            readMethod = cluster.getClass().getMethod(methodName, callbackClass);
        } catch (NoSuchMethodException exception) {
            ConnectedHomeIpDiagnostics.emit("Matter attribute read method unavailable for " + label + ": " + methodName);
            return null;
        }
        AttributeCallback callback = new AttributeCallback(label);
        try {
            readMethod.invoke(cluster, callback.proxy(callbackClass));
        } catch (ReflectiveOperationException | LinkageError exception) {
            ConnectedHomeIpDiagnostics.emit("Unable to request Matter " + label + ": " + safeMessage(exception));
            return null;
        }
        try {
            return callback.await(attributeTimeoutMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            ConnectedHomeIpDiagnostics.emit("Interrupted reading Matter " + label);
            return null;
        }
    }

    private Object newCluster(String clusterClassName, long devicePointer, int endpoint) {
        Class<?> clusterClass = classOrNull(clusterClassName, shortClassName(clusterClassName));
        if (clusterClass == null) {
            return null;
        }
        try {
            Constructor<?> constructor = clusterClass.getConstructor(long.class, int.class);
            return constructor.newInstance(devicePointer, endpoint);
        } catch (ReflectiveOperationException | LinkageError exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Unable to create Matter cluster " + shortClassName(clusterClassName) + ": "
                            + safeMessage(exception));
            return null;
        }
    }

    private Class<?> classOrNull(String className, String label) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException | LinkageError exception) {
            ConnectedHomeIpDiagnostics.emit("Matter " + label + " unavailable: " + safeMessage(exception));
            return null;
        }
    }

    private static String ipv6Address(Object value) {
        if (!(value instanceof List<?>)) {
            return "";
        }
        ConnectedHomeIpDiagnostics.emit("Matter network interfaces were read, but IPv6 decoding is not yet mapped safely");
        return "";
    }

    private static String shortClassName(String className) {
        int index = className.lastIndexOf('$');
        return index < 0 ? className : className.substring(index + 1);
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    private static final class AttributeCallback implements InvocationHandler {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final String label;
        private Object value;

        private AttributeCallback(String label) {
            this.label = label;
        }

        private Object proxy(Class<?> callbackClass) {
            return Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[] {callbackClass},
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("onSuccess".equals(method.getName()) && args != null && args.length >= 1) {
                value = args[0];
                latch.countDown();
                return null;
            }
            if ("onError".equals(method.getName())) {
                ConnectedHomeIpDiagnostics.emit("Unable to read Matter " + label + ": " + callbackMessage(args));
                latch.countDown();
                return null;
            }
            return defaultValue(method.getReturnType());
        }

        private Object await(long timeoutMillis) throws InterruptedException {
            if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                ConnectedHomeIpDiagnostics.emit("Timed out reading Matter " + label);
            }
            return value;
        }

        private static String callbackMessage(Object[] args) {
            if (args == null || args.length == 0 || args[0] == null) {
                return "unknown error";
            }
            return args[0].toString();
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType == void.class || returnType == Void.class) {
                return null;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0F;
            }
            if (returnType == double.class) {
                return 0D;
            }
            if (returnType == char.class) {
                return (char) 0;
            }
            return null;
        }
    }
}
