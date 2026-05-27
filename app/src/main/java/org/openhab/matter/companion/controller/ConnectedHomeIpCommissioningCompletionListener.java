package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConnectedHomeIpCommissioningCompletionListener {
    static final long DEFAULT_TIMEOUT_MILLIS = 300_000L;

    private final Object proxy;
    private final long timeoutMillis;
    private final IcdRegistrationInfoRequiredHandler icdRegistrationInfoRequiredHandler;
    private final CountDownLatch latch = new CountDownLatch(1);
    private long commissionedNodeId;
    private IllegalStateException error;

    public ConnectedHomeIpCommissioningCompletionListener(Class<?> completionListenerClass) {
        this(completionListenerClass, DEFAULT_TIMEOUT_MILLIS);
    }

    public ConnectedHomeIpCommissioningCompletionListener(Class<?> completionListenerClass, long timeoutMillis) {
        this(completionListenerClass, timeoutMillis, null);
    }

    public ConnectedHomeIpCommissioningCompletionListener(
            Class<?> completionListenerClass,
            long timeoutMillis,
            IcdRegistrationInfoRequiredHandler icdRegistrationInfoRequiredHandler) {
        if (completionListenerClass == null) {
            throw new IllegalArgumentException("completionListenerClass is required");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.timeoutMillis = timeoutMillis;
        this.icdRegistrationInfoRequiredHandler = icdRegistrationInfoRequiredHandler;
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
                    ConnectedHomeIpDiagnostics.emit(
                            "connectedhomeip commissioning failed for node " + nodeId + " with error " + errorCode);
                } else {
                    ConnectedHomeIpDiagnostics.emit("connectedhomeip commissioning complete for node " + nodeId);
                }
                latch.countDown();
                return null;
            }
            if ("onPairingComplete".equals(name)) {
                long errorCode = longArg(args, 0, 0L);
                if (errorCode != 0L) {
                    error = new IllegalStateException("Pairing failed with error " + errorCode);
                    ConnectedHomeIpDiagnostics.emit("connectedhomeip pairing failed with error " + errorCode);
                    latch.countDown();
                } else {
                    ConnectedHomeIpDiagnostics.emit("connectedhomeip pairing complete");
                }
                return null;
            }
            if ("onCommissioningStageStart".equals(name)) {
                String stage = stringArg(args, 1, "unknown");
                ConnectedHomeIpDiagnostics.emit("connectedhomeip commissioning stage started: " + stage);
                return null;
            }
            if ("onCommissioningStatusUpdate".equals(name)) {
                String stage = stringArg(args, 1, "unknown");
                long errorCode = longArg(args, 2, 0L);
                ConnectedHomeIpDiagnostics.emit(
                        "connectedhomeip commissioning stage update: "
                                + stage
                                + (errorCode == 0L ? " succeeded" : " error " + errorCode));
                return null;
            }
            if ("onStatusUpdate".equals(name)) {
                long status = longArg(args, 0, -1L);
                ConnectedHomeIpDiagnostics.emit("connectedhomeip status update: " + status);
                return null;
            }
            if ("onReadCommissioningInfo".equals(name)) {
                ConnectedHomeIpDiagnostics.emit("connectedhomeip read commissioning info");
                return null;
            }
            if ("onICDRegistrationInfoRequired".equals(name)) {
                ConnectedHomeIpDiagnostics.emit("connectedhomeip ICD registration info required");
                if (icdRegistrationInfoRequiredHandler != null) {
                    try {
                        icdRegistrationInfoRequiredHandler.onRequired();
                        ConnectedHomeIpDiagnostics.emit("connectedhomeip ICD registration info submitted");
                    } catch (Exception exception) {
                        String message = "Failed to submit connectedhomeip ICD registration info: "
                                + safeMessage(exception);
                        ConnectedHomeIpDiagnostics.emit(message);
                        error = new IllegalStateException(message, exception);
                        latch.countDown();
                    }
                }
                return null;
            }
            if ("onICDRegistrationComplete".equals(name)) {
                long nodeId = longArg(args, 0, -1L);
                ConnectedHomeIpDiagnostics.emit("connectedhomeip ICD registration complete for node " + nodeId);
                return null;
            }
            if ("onCloseBleComplete".equals(name)) {
                ConnectedHomeIpDiagnostics.emit("connectedhomeip BLE close complete");
                return null;
            }
            if ("onNotifyChipConnectionClosed".equals(name)) {
                ConnectedHomeIpDiagnostics.emit("connectedhomeip CHIP connection closed");
                return null;
            }
            if ("onError".equals(name)) {
                Throwable cause = args != null && args.length > 0 && args[0] instanceof Throwable
                        ? (Throwable) args[0]
                        : null;
                String causeMessage = cause == null ? "" : safeMessage(cause);
                String message = causeMessage.isEmpty()
                        ? "Commissioning listener reported an error"
                        : "Commissioning listener reported an error: " + causeMessage;
                ConnectedHomeIpDiagnostics.emit("connectedhomeip commissioning error: " + causeMessage);
                error = new IllegalStateException(message, cause);
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

    private static String stringArg(Object[] args, int index, String defaultValue) {
        if (args == null || args.length <= index || !(args[index] instanceof String)) {
            return defaultValue;
        }
        String value = (String) args[index];
        return value == null || value.isEmpty() ? defaultValue : value;
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

    private static String safeMessage(Throwable throwable) {
        if (throwable instanceof InvocationTargetException
                && ((InvocationTargetException) throwable).getTargetException() != null) {
            return safeMessage(((InvocationTargetException) throwable).getTargetException());
        }
        if (throwable.getCause() != null && throwable.getCause() != throwable) {
            String message = throwable.getMessage();
            if (message == null || message.isEmpty()) {
                return throwable.getClass().getSimpleName() + ": " + safeMessage(throwable.getCause());
            }
            return throwable.getClass().getSimpleName() + ": " + message + " / " + safeMessage(throwable.getCause());
        }
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    public interface IcdRegistrationInfoRequiredHandler {
        void onRequired() throws Exception;
    }
}
