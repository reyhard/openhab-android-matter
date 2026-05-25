package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConnectedHomeIpOpenCommissioningWindowCallback {
    private final Object proxy;
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
                String manualPairingCode = stringArg(args, 1);
                String qrCode = stringArg(args, 2);
                try {
                    result = new MatterOpenCommissioningWindowResult(
                            manualPairingCode.isEmpty() ? qrCode : manualPairingCode,
                            controllerState);
                } catch (IllegalArgumentException exception) {
                    error = new IllegalStateException(
                            "OpenCommissioningWindow returned blank manual and QR setup codes",
                            exception);
                } finally {
                    latch.countDown();
                }
                return null;
            }
            if ("onError".equals(method.getName())) {
                int status = args != null && args.length > 0 && args[0] instanceof Number
                        ? ((Number) args[0]).intValue()
                        : -1;
                long deviceId = args != null && args.length > 1 && args[1] instanceof Number
                        ? ((Number) args[1]).longValue()
                        : -1L;
                error = new IllegalStateException(
                        "OpenCommissioningWindow failed for node " + deviceId + " with status " + status);
                latch.countDown();
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static String stringArg(Object[] args, int index) {
        if (args == null || args.length <= index || args[index] == null) {
            return "";
        }
        return args[index].toString();
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
