package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConnectedHomeIpCommissioningCompletionListener {
    static final long DEFAULT_TIMEOUT_MILLIS = 300_000L;

    private final Object proxy;
    private final long timeoutMillis;
    private final CountDownLatch latch = new CountDownLatch(1);
    private long commissionedNodeId;
    private IllegalStateException error;

    public ConnectedHomeIpCommissioningCompletionListener(Class<?> completionListenerClass) {
        this(completionListenerClass, DEFAULT_TIMEOUT_MILLIS);
    }

    public ConnectedHomeIpCommissioningCompletionListener(Class<?> completionListenerClass, long timeoutMillis) {
        if (completionListenerClass == null) {
            throw new IllegalArgumentException("completionListenerClass is required");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.timeoutMillis = timeoutMillis;
        this.proxy = Proxy.newProxyInstance(
                completionListenerClass.getClassLoader(),
                new Class<?>[] {completionListenerClass},
                new CompletionHandler());
    }

    public Object proxy() {
        return proxy;
    }

    public MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) throws Exception {
        if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("Commissioning completion callback timed out for node " + nodeId);
        }
        if (error != null) {
            throw error;
        }
        if (commissionedNodeId != nodeId) {
            throw new IllegalStateException(
                    "Commissioning completed for node " + commissionedNodeId + " instead of expected node " + nodeId);
        }
        return new MatterCommissioningResult(commissionedNodeId, controllerState);
    }

    private final class CompletionHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("onCommissioningComplete".equals(name)) {
                long nodeId = longArg(args, 0, -1L);
                long errorCode = longArg(args, 1, -1L);
                commissionedNodeId = nodeId;
                if (errorCode != 0L) {
                    error = new IllegalStateException(
                            "Commissioning failed for node " + nodeId + " with error " + errorCode);
                }
                latch.countDown();
                return null;
            }
            if ("onPairingComplete".equals(name)) {
                long errorCode = longArg(args, 0, 0L);
                if (errorCode != 0L) {
                    error = new IllegalStateException("Pairing failed with error " + errorCode);
                    latch.countDown();
                }
                return null;
            }
            if ("onError".equals(name)) {
                Throwable cause = args != null && args.length > 0 && args[0] instanceof Throwable
                        ? (Throwable) args[0]
                        : null;
                error = new IllegalStateException("Commissioning listener reported an error", cause);
                latch.countDown();
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static long longArg(Object[] args, int index, long defaultValue) {
        if (args == null || args.length <= index || !(args[index] instanceof Number)) {
            return defaultValue;
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
