package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConnectedHomeIpConnectedDeviceCallback {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final DevicePointerReleaser lateDevicePointerReleaser;
    private final Object lock = new Object();
    private long devicePtr;
    private IllegalStateException error;
    private boolean abandoned;

    public ConnectedHomeIpConnectedDeviceCallback(DevicePointerReleaser lateDevicePointerReleaser) {
        if (lateDevicePointerReleaser == null) {
            throw new IllegalArgumentException("lateDevicePointerReleaser is required");
        }
        this.lateDevicePointerReleaser = lateDevicePointerReleaser;
    }

    public Object proxy(Class<?> callbackClass) {
        if (callbackClass == null) {
            throw new IllegalArgumentException("callbackClass is required");
        }
        return Proxy.newProxyInstance(
                callbackClass.getClassLoader(),
                new Class<?>[] {callbackClass},
                new CallbackHandler());
    }

    public long awaitDevicePointer(long nodeId, long timeoutMillis) throws InterruptedException {
        boolean completed;
        try {
            completed = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            abandon();
            throw exception;
        }
        if (!completed) {
            abandon();
            throw new IllegalStateException("Connected device pointer callback timed out for node " + nodeId);
        }
        if (error != null) {
            throw error;
        }
        return devicePtr;
    }

    private void abandon() {
        synchronized (lock) {
            abandoned = true;
        }
    }

    private final class CallbackHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            if ("onDeviceConnected".equals(method.getName())) {
                long connectedDevicePtr = args != null && args.length > 0 && args[0] instanceof Number
                        ? ((Number) args[0]).longValue()
                        : 0L;
                synchronized (lock) {
                    if (abandoned) {
                        lateDevicePointerReleaser.release(connectedDevicePtr);
                        return null;
                    }
                    devicePtr = connectedDevicePtr;
                }
                latch.countDown();
                return null;
            }
            if ("onConnectionFailure".equals(method.getName())) {
                long nodeId = args != null && args.length > 0 && args[0] instanceof Number
                        ? ((Number) args[0]).longValue()
                        : -1L;
                Throwable cause = args != null && args.length > 1 && args[1] instanceof Throwable
                        ? (Throwable) args[1]
                        : null;
                error = new IllegalStateException("Connected device pointer failed for node " + nodeId, cause);
                latch.countDown();
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    public interface DevicePointerReleaser {
        void release(long devicePtr) throws Exception;
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
