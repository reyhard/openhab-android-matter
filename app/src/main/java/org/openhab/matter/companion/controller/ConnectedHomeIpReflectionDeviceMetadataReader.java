package org.openhab.matter.companion.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConnectedHomeIpReflectionDeviceMetadataReader implements ConnectedHomeIpDeviceMetadataReader {
    private static final int BASIC_INFORMATION_ENDPOINT = 0;
    private static final long ATTRIBUTE_TIMEOUT_MILLIS = 1_500L;
    private static final String BASIC_INFORMATION_CLUSTER_CLASS =
            "chip.devicecontroller.ChipClusters$BasicInformationCluster";
    private static final String CHAR_STRING_CALLBACK_CLASS =
            "chip.devicecontroller.ChipClusters$CharStringAttributeCallback";

    private final ConnectedHomeIpDevicePointerProvider devicePointerProvider;
    private final Constructor<?> basicInformationClusterConstructor;
    private final Class<?> charStringCallbackClass;
    private final long attributeTimeoutMillis;

    public ConnectedHomeIpReflectionDeviceMetadataReader(
            ConnectedHomeIpDevicePointerProvider devicePointerProvider) throws ClassNotFoundException, NoSuchMethodException {
        this(devicePointerProvider, ATTRIBUTE_TIMEOUT_MILLIS);
    }

    ConnectedHomeIpReflectionDeviceMetadataReader(
            ConnectedHomeIpDevicePointerProvider devicePointerProvider,
            long attributeTimeoutMillis) throws ClassNotFoundException, NoSuchMethodException {
        if (devicePointerProvider == null) {
            throw new IllegalArgumentException("devicePointerProvider is required");
        }
        if (attributeTimeoutMillis <= 0L) {
            throw new IllegalArgumentException("attributeTimeoutMillis must be positive");
        }
        ClassLoader classLoader = ConnectedHomeIpReflectionDeviceMetadataReader.class.getClassLoader();
        Class<?> basicInformationClusterClass = Class.forName(
                BASIC_INFORMATION_CLUSTER_CLASS,
                false,
                classLoader);
        this.devicePointerProvider = devicePointerProvider;
        this.basicInformationClusterConstructor =
                basicInformationClusterClass.getConstructor(long.class, int.class);
        this.charStringCallbackClass = Class.forName(CHAR_STRING_CALLBACK_CLASS, false, classLoader);
        this.attributeTimeoutMillis = attributeTimeoutMillis;
    }

    @Override
    public MatterDeviceMetadata readVendorAndProduct(Object controller, long nodeId) throws Exception {
        try (ConnectedHomeIpDevicePointer pointer = devicePointerProvider.acquire(controller, nodeId)) {
            Object cluster = basicInformationClusterConstructor.newInstance(
                    pointer.value(),
                    BASIC_INFORMATION_ENDPOINT);
            AttributeStringCallback vendorCallback = new AttributeStringCallback("vendor name");
            AttributeStringCallback productCallback = new AttributeStringCallback("product name");
            cluster.getClass()
                    .getMethod("readVendorNameAttribute", charStringCallbackClass)
                    .invoke(cluster, vendorCallback.proxy(charStringCallbackClass));
            cluster.getClass()
                    .getMethod("readProductNameAttribute", charStringCallbackClass)
                    .invoke(cluster, productCallback.proxy(charStringCallbackClass));
            long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(attributeTimeoutMillis);
            return new MatterDeviceMetadata(
                    vendorCallback.awaitUntil(deadlineNanos),
                    productCallback.awaitUntil(deadlineNanos));
        }
    }

    private static final class AttributeStringCallback implements InvocationHandler {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final String label;
        private String value = "";

        private AttributeStringCallback(String label) {
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
            if ("onSuccess".equals(method.getName()) && args != null && args.length == 1) {
                value = args[0] == null ? "" : args[0].toString().trim();
                latch.countDown();
                return null;
            }
            if ("onError".equals(method.getName())) {
                String message = args != null && args.length > 0 && args[0] != null
                        ? args[0].toString()
                        : "unknown error";
                ConnectedHomeIpDiagnostics.emit("Unable to read Matter " + label + ": " + message);
                latch.countDown();
                return null;
            }
            return defaultValue(method.getReturnType());
        }

        private String awaitUntil(long deadlineNanos) throws InterruptedException {
            long remainingNanos = Math.max(0L, deadlineNanos - System.nanoTime());
            if (!latch.await(remainingNanos, TimeUnit.NANOSECONDS)) {
                ConnectedHomeIpDiagnostics.emit("Timed out reading Matter " + label);
            }
            return value;
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
