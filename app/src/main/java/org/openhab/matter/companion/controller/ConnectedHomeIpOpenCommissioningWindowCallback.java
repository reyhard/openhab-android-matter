package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConnectedHomeIpOpenCommissioningWindowCallback {
    private Object proxy;
    private final String controllerState;
    private final CountDownLatch latch = new CountDownLatch(1);
    private MatterOpenCommissioningWindowResult result;
    private IllegalStateException error;

    public ConnectedHomeIpOpenCommissioningWindowCallback(Class<?> callbackInterface, String controllerState) {
        if (callbackInterface == null) {
            throw new IllegalArgumentException("callbackInterface is required");
        }
        this.controllerState = controllerState == null ? "" : controllerState;
        this.proxy = Proxy.newProxyInstance(
                callbackInterface.getClassLoader(),
                new Class<?>[] {callbackInterface},
                new CallbackHandler());
    }

    public Object proxy() {
        return proxy;
    }

    public void setProxy(Object proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy is required");
        }
        this.proxy = proxy;
    }

    public MatterOpenCommissioningWindowResult awaitResult(long timeoutMillis) throws InterruptedException {
        if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("OpenCommissioningWindow callback timed out");
        }
        if (error != null) {
            throw error;
        }
        return result;
    }

    private final class CallbackHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("onSuccess".equals(method.getName())) {
                onSuccess(longArg(args, 0), stringArg(args, 1), stringArg(args, 2));
                return defaultValue(method.getReturnType());
            }
            if ("onError".equals(method.getName())) {
                onError(intArg(args, 0), longArg(args, 1));
                return defaultValue(method.getReturnType());
            }
            return defaultValue(method.getReturnType());
        }
    }

    public void onSuccess(long deviceId, String manualPairingCode, String qrCode) {
        try {
            result = new MatterOpenCommissioningWindowResult(
                    manualPairingCode,
                    qrCode,
                    controllerState);
        } catch (IllegalArgumentException exception) {
            error = new IllegalStateException(
                    "OpenCommissioningWindow returned blank manual and QR setup codes",
                    exception);
        } finally {
            latch.countDown();
        }
    }

    public void onError(int status, long deviceId) {
        error = new IllegalStateException(
                "OpenCommissioningWindow failed for node " + deviceId + " with status " + status);
        latch.countDown();
    }

    private static String stringArg(Object[] args, int index) {
        if (args == null || args.length <= index || args[index] == null) {
            return "";
        }
        return args[index].toString();
    }

    private static int intArg(Object[] args, int index) {
        if (args == null || args.length <= index || !(args[index] instanceof Number)) {
            return -1;
        }
        return ((Number) args[index]).intValue();
    }

    private static long longArg(Object[] args, int index) {
        if (args == null || args.length <= index || !(args[index] instanceof Number)) {
            return -1L;
        }
        return ((Number) args[index]).longValue();
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || void.class.equals(returnType)) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return 0;
    }
}
