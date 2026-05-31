package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;

import org.openhab.matter.companion.domain.ThreadDataset;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ConnectedHomeIpReflectionCommandFactory {
    private static final String NETWORK_CREDENTIALS_CLASS = "chip.devicecontroller.NetworkCredentials";
    private static final String THREAD_CREDENTIALS_CLASS = "chip.devicecontroller.NetworkCredentials$ThreadCredentials";
    private static final String COMMISSION_PARAMETERS_BUILDER_CLASS = "chip.devicecontroller.CommissionParameters$Builder";
    private static final String CHIP_DEVICE_CONTROLLER_CLASS = "chip.devicecontroller.ChipDeviceController";
    private static final String OPEN_COMMISSIONING_CALLBACK_CLASS = "chip.devicecontroller.OpenCommissioningCallback";
    private static final String COMPLETION_LISTENER_CLASS =
            "chip.devicecontroller.ChipDeviceController$CompletionListener";
    private static final String DEVICE_ATTESTATION_DELEGATE_CLASS = "chip.devicecontroller.DeviceAttestationDelegate";
    private static final String ATTESTATION_TRUST_STORE_DELEGATE_CLASS =
            "chip.devicecontroller.AttestationTrustStoreDelegate";
    private static final String DEVICE_ATTESTATION_CLASS = "chip.devicecontroller.DeviceAttestation";
    private static final String GET_CONNECTED_DEVICE_CALLBACK_CLASS =
            "chip.devicecontroller.GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback";
    private static final String ICD_REGISTRATION_INFO_CLASS = "chip.devicecontroller.ICDRegistrationInfo";
    private static final String CONCRETE_DEVICE_ATTESTATION_DELEGATE_CLASS =
            "org.openhab.matter.companion.controller.ConnectedHomeIpConcreteDeviceAttestationDelegate";
    private static final String CONCRETE_ATTESTATION_TRUST_STORE_DELEGATE_CLASS =
            "org.openhab.matter.companion.controller.ConnectedHomeIpConcreteAttestationTrustStoreDelegate";
    private static final String CONCRETE_CONNECTED_DEVICE_CALLBACK_CLASS =
            "org.openhab.matter.companion.controller.ConnectedHomeIpConcreteConnectedDeviceCallback";
    private static final String CONCRETE_OPEN_COMMISSIONING_WINDOW_CALLBACK_CLASS =
            "org.openhab.matter.companion.controller.ConnectedHomeIpConcreteOpenCommissioningWindowCallback";
    private static final String CONCRETE_OPEN_COMMISSIONING_CALLBACK_CLASS =
            "org.openhab.matter.companion.controller.ConnectedHomeIpConcreteOpenCommissioningCallback";

    private final Class<?> networkCredentialsClass;
    private final Class<?> threadCredentialsClass;
    private final Class<?> commissionParametersBuilderClass;
    private final Class<?> chipDeviceControllerClass;
    private final Class<?> openCommissioningCallbackClass;
    private final Class<?> completionListenerClass;
    private final Class<?> deviceAttestationDelegateClass;
    private final Class<?> attestationTrustStoreDelegateClass;
    private final Class<?> deviceAttestationClass;
    private final Class<?> getConnectedDeviceCallbackClass;
    private final Class<?> icdRegistrationInfoClass;

    public ConnectedHomeIpReflectionCommandFactory(
            Class<?> networkCredentialsClass,
            Class<?> threadCredentialsClass,
            Class<?> commissionParametersBuilderClass,
            Class<?> chipDeviceControllerClass,
            Class<?> openCommissioningCallbackClass) {
        this(
                networkCredentialsClass,
                threadCredentialsClass,
                commissionParametersBuilderClass,
                chipDeviceControllerClass,
                openCommissioningCallbackClass,
                nestedClass(chipDeviceControllerClass, "CompletionListener"),
                null,
                null,
                null,
                null,
                null);
    }

    public ConnectedHomeIpReflectionCommandFactory(
            Class<?> networkCredentialsClass,
            Class<?> threadCredentialsClass,
            Class<?> commissionParametersBuilderClass,
            Class<?> chipDeviceControllerClass,
            Class<?> openCommissioningCallbackClass,
            Class<?> completionListenerClass,
            Class<?> deviceAttestationDelegateClass,
            Class<?> getConnectedDeviceCallbackClass) {
        this(
                networkCredentialsClass,
                threadCredentialsClass,
                commissionParametersBuilderClass,
                chipDeviceControllerClass,
                openCommissioningCallbackClass,
                completionListenerClass,
                deviceAttestationDelegateClass,
                getConnectedDeviceCallbackClass,
                null,
                null,
                null);
    }

    public ConnectedHomeIpReflectionCommandFactory(
            Class<?> networkCredentialsClass,
            Class<?> threadCredentialsClass,
            Class<?> commissionParametersBuilderClass,
            Class<?> chipDeviceControllerClass,
            Class<?> openCommissioningCallbackClass,
            Class<?> completionListenerClass,
            Class<?> deviceAttestationDelegateClass,
            Class<?> getConnectedDeviceCallbackClass,
            Class<?> icdRegistrationInfoClass) {
        this(
                networkCredentialsClass,
                threadCredentialsClass,
                commissionParametersBuilderClass,
                chipDeviceControllerClass,
                openCommissioningCallbackClass,
                completionListenerClass,
                deviceAttestationDelegateClass,
                getConnectedDeviceCallbackClass,
                icdRegistrationInfoClass,
                null,
                null);
    }

    public ConnectedHomeIpReflectionCommandFactory(
            Class<?> networkCredentialsClass,
            Class<?> threadCredentialsClass,
            Class<?> commissionParametersBuilderClass,
            Class<?> chipDeviceControllerClass,
            Class<?> openCommissioningCallbackClass,
            Class<?> completionListenerClass,
            Class<?> deviceAttestationDelegateClass,
            Class<?> getConnectedDeviceCallbackClass,
            Class<?> icdRegistrationInfoClass,
            Class<?> attestationTrustStoreDelegateClass,
            Class<?> deviceAttestationClass) {
        this.networkCredentialsClass = requireClass(networkCredentialsClass, "networkCredentialsClass");
        this.threadCredentialsClass = requireClass(threadCredentialsClass, "threadCredentialsClass");
        this.commissionParametersBuilderClass = requireClass(
                commissionParametersBuilderClass,
                "commissionParametersBuilderClass");
        this.chipDeviceControllerClass = requireClass(chipDeviceControllerClass, "chipDeviceControllerClass");
        this.openCommissioningCallbackClass = requireClass(
                openCommissioningCallbackClass,
                "openCommissioningCallbackClass");
        this.completionListenerClass = completionListenerClass;
        this.deviceAttestationDelegateClass = deviceAttestationDelegateClass;
        this.attestationTrustStoreDelegateClass = attestationTrustStoreDelegateClass;
        this.deviceAttestationClass = deviceAttestationClass;
        this.getConnectedDeviceCallbackClass = getConnectedDeviceCallbackClass;
        this.icdRegistrationInfoClass = icdRegistrationInfoClass;
    }

    public static ConnectedHomeIpReflectionCommandFactory fromDefaultClassLoader() throws ClassNotFoundException {
        ClassLoader classLoader = ConnectedHomeIpReflectionCommandFactory.class.getClassLoader();
        return new ConnectedHomeIpReflectionCommandFactory(
                Class.forName(NETWORK_CREDENTIALS_CLASS, false, classLoader),
                Class.forName(THREAD_CREDENTIALS_CLASS, false, classLoader),
                Class.forName(COMMISSION_PARAMETERS_BUILDER_CLASS, false, classLoader),
                Class.forName(CHIP_DEVICE_CONTROLLER_CLASS, false, classLoader),
                Class.forName(OPEN_COMMISSIONING_CALLBACK_CLASS, false, classLoader),
                Class.forName(COMPLETION_LISTENER_CLASS, false, classLoader),
                Class.forName(DEVICE_ATTESTATION_DELEGATE_CLASS, false, classLoader),
                Class.forName(GET_CONNECTED_DEVICE_CALLBACK_CLASS, false, classLoader),
                Class.forName(ICD_REGISTRATION_INFO_CLASS, false, classLoader),
                Class.forName(ATTESTATION_TRUST_STORE_DELEGATE_CLASS, false, classLoader),
                Class.forName(DEVICE_ATTESTATION_CLASS, false, classLoader));
    }

    public Object newThreadCommissionParameters(ThreadDataset dataset) throws ReflectiveOperationException {
        Constructor<?> threadCredentialsConstructor = threadCredentialsClass.getConstructor(byte[].class);
        Object threadCredentials = threadCredentialsConstructor.newInstance((Object) dataset.bytes());
        Method forThread = networkCredentialsClass.getMethod("forThread", threadCredentialsClass);
        Object networkCredentials = forThread.invoke(null, threadCredentials);
        Object builder = commissionParametersBuilderClass.getConstructor().newInstance();
        findSingleParameterMethod(commissionParametersBuilderClass, "setCsrNonce").invoke(builder, new Object[] {null});
        findSingleParameterMethod(commissionParametersBuilderClass, "setNetworkCredentials").invoke(builder, networkCredentials);
        Object icdRegistrationInfo = newDeferredIcdRegistrationInfo();
        findSingleParameterMethod(commissionParametersBuilderClass, "setICDRegistrationInfo")
                .invoke(builder, new Object[] {icdRegistrationInfo});
        return findNoParameterMethod(commissionParametersBuilderClass, "build").invoke(builder);
    }

    public Object newIcdRegistrationInfoForStayActive(long stayActiveDurationMillis)
            throws ReflectiveOperationException {
        if (stayActiveDurationMillis <= 0L) {
            throw new IllegalArgumentException("stayActiveDurationMillis must be positive");
        }
        Object builder = requireAvailable(icdRegistrationInfoClass, "icdRegistrationInfoClass")
                .getMethod("newBuilder")
                .invoke(null);
        findSingleParameterMethod(builder.getClass(), "setICDStayActiveDurationMsec")
                .invoke(builder, Long.valueOf(stayActiveDurationMillis));
        return findNoParameterMethod(builder.getClass(), "build").invoke(builder);
    }

    public void invokeUpdateCommissioningIcdRegistrationInfo(Object controller, Object icdRegistrationInfo)
            throws ReflectiveOperationException {
        try {
            chipDeviceControllerClass
                    .getMethod(
                            "updateCommissioningICDRegistrationInfo",
                            requireAvailable(icdRegistrationInfoClass, "icdRegistrationInfoClass"))
                    .invoke(controller, icdRegistrationInfo);
        } catch (InvocationTargetException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "connectedhomeip updateCommissioningICDRegistrationInfo failed: "
                            + safeMessage(exception));
            throw exception;
        }
    }

    public Method pairDeviceThroughBleMethod() throws NoSuchMethodException {
        for (Method method : chipDeviceControllerClass.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals("pairDeviceThroughBLE")
                    && parameterTypes.length == 5
                    && BluetoothGatt.class.equals(parameterTypes[0])
                    && int.class.equals(parameterTypes[1])
                    && long.class.equals(parameterTypes[2])
                    && long.class.equals(parameterTypes[3])
                    && commissionParametersClass().equals(parameterTypes[4])) {
                return method;
            }
        }
        throw new NoSuchMethodException("pairDeviceThroughBLE");
    }

    public void invokePairDeviceThroughBle(
            Object controller,
            BluetoothGatt bleServer,
            int connId,
            long deviceId,
            long setupPin,
            Object commissionParameters) throws ReflectiveOperationException {
        pairDeviceThroughBleMethod().invoke(controller, bleServer, connId, deviceId, setupPin, commissionParameters);
    }

    public Method openPairingWindowWithPinCallbackMethod() throws NoSuchMethodException {
        return chipDeviceControllerClass.getMethod(
                "openPairingWindowWithPINCallback",
                long.class,
                int.class,
                long.class,
                int.class,
                Long.class,
                openCommissioningCallbackClass);
    }

    public ConnectedHomeIpOpenCommissioningWindowCallback newOpenCommissioningWindowCallback(
            String controllerState) {
        ConnectedHomeIpOpenCommissioningWindowCallback callback =
                new ConnectedHomeIpOpenCommissioningWindowCallback(openCommissioningCallbackClass, controllerState);
        Object concreteCallback = newConcreteOpenCommissioningWindowCallback(callback);
        if (concreteCallback != null) {
            callback.setProxy(concreteCallback);
        }
        return callback;
    }

    public boolean invokeOpenPairingWindowWithPinCallback(
            Object controller,
            long devicePtr,
            ConnectedHomeIpOpenCommissioningWindowRequest request,
            Long setupPinCode,
            Object callbackProxy) throws ReflectiveOperationException {
        Object result = openPairingWindowWithPinCallbackMethod().invoke(
                controller,
                devicePtr,
                request.timeoutSeconds(),
                request.iteration(),
                request.discriminator(),
                setupPinCode,
                callbackProxy);
        return Boolean.TRUE.equals(result);
    }

    public ConnectedHomeIpCommissioningCompletionListener newCommissioningCompletionListener() {
        return new ConnectedHomeIpCommissioningCompletionListener(
                requireAvailable(completionListenerClass, "completionListenerClass"));
    }

    public ConnectedHomeIpCommissioningCompletionListener newCommissioningCompletionListener(long timeoutMillis) {
        return new ConnectedHomeIpCommissioningCompletionListener(
                requireAvailable(completionListenerClass, "completionListenerClass"),
                timeoutMillis);
    }

    public ConnectedHomeIpCommissioningCompletionListener newCommissioningCompletionListener(
            long timeoutMillis,
            ConnectedHomeIpCommissioningCompletionListener.IcdRegistrationInfoRequiredHandler icdRegistrationInfoRequiredHandler) {
        return new ConnectedHomeIpCommissioningCompletionListener(
                requireAvailable(completionListenerClass, "completionListenerClass"),
                timeoutMillis,
                icdRegistrationInfoRequiredHandler);
    }

    public Object newDeviceAttestationDelegate(Object controller, boolean attestationBypassEnabled) {
        Object concreteDelegate = newConcreteDeviceAttestationDelegate(controller, attestationBypassEnabled);
        if (concreteDelegate != null) {
            return concreteDelegate;
        }
        return new ConnectedHomeIpDeviceAttestationDelegate(
                requireAvailable(deviceAttestationDelegateClass, "deviceAttestationDelegateClass"),
                this,
                controller,
                attestationBypassEnabled)
                .proxy();
    }

    public Object newAttestationTrustStoreDelegate(ConnectedHomeIpAttestationTrustStore store) {
        Object concreteDelegate = newConcreteAttestationTrustStoreDelegate(store);
        if (concreteDelegate != null) {
            return concreteDelegate;
        }
        return new ConnectedHomeIpAttestationTrustStoreDelegate(
                requireAvailable(attestationTrustStoreDelegateClass, "attestationTrustStoreDelegateClass"),
                requireAvailable(deviceAttestationClass, "deviceAttestationClass"),
                store)
                .proxy();
    }

    public Object newGetConnectedDeviceCallback(ConnectedHomeIpConnectedDeviceCallback callback) {
        Object concreteCallback = newConcreteConnectedDeviceCallback(callback);
        if (concreteCallback != null) {
            return concreteCallback;
        }
        return callback.proxy(requireAvailable(getConnectedDeviceCallbackClass, "getConnectedDeviceCallbackClass"));
    }

    public void invokeSetCompletionListener(Object controller, Object listenerProxy)
            throws ReflectiveOperationException {
        chipDeviceControllerClass
                .getMethod("setCompletionListener", requireAvailable(completionListenerClass, "completionListenerClass"))
                .invoke(controller, listenerProxy);
    }

    public void invokeSetDeviceAttestationDelegate(
            Object controller,
            int failSafeExpiryTimeoutSeconds,
            Object delegateProxy) throws ReflectiveOperationException {
        chipDeviceControllerClass
                .getMethod(
                        "setDeviceAttestationDelegate",
                        int.class,
                        requireAvailable(deviceAttestationDelegateClass, "deviceAttestationDelegateClass"))
                .invoke(controller, failSafeExpiryTimeoutSeconds, delegateProxy);
    }

    public void invokeSetAttestationTrustStoreDelegate(
            Object controller,
            Object delegateProxy,
            java.util.List<byte[]> cdTrustKeys) throws ReflectiveOperationException {
        try {
            chipDeviceControllerClass
                    .getMethod(
                            "setAttestationTrustStoreDelegate",
                            requireAvailable(attestationTrustStoreDelegateClass, "attestationTrustStoreDelegateClass"),
                            java.util.List.class)
                    .invoke(controller, delegateProxy, cdTrustKeys);
        } catch (NoSuchMethodException noTwoArgMethod) {
            chipDeviceControllerClass
                    .getMethod(
                            "setAttestationTrustStoreDelegate",
                            requireAvailable(attestationTrustStoreDelegateClass, "attestationTrustStoreDelegateClass"))
                    .invoke(controller, delegateProxy);
        }
    }

    public void invokeContinueCommissioning(
            Object controller,
            long devicePtr,
            boolean ignoreAttestationFailure) throws ReflectiveOperationException {
        chipDeviceControllerClass
                .getMethod("continueCommissioning", long.class, boolean.class)
                .invoke(controller, devicePtr, ignoreAttestationFailure);
    }

    public void invokeGetConnectedDevicePointer(Object controller, long nodeId, Object callbackProxy)
            throws ReflectiveOperationException {
        chipDeviceControllerClass
                .getMethod(
                        "getConnectedDevicePointer",
                        long.class,
                        requireAvailable(getConnectedDeviceCallbackClass, "getConnectedDeviceCallbackClass"))
                .invoke(controller, nodeId, callbackProxy);
    }

    public void invokeReleaseConnectedDevicePointer(Object controller, long devicePtr)
            throws ReflectiveOperationException {
        chipDeviceControllerClass
                .getMethod("releaseConnectedDevicePointer", long.class)
                .invoke(controller, devicePtr);
    }

    public void invokeClose(Object controller) throws ReflectiveOperationException {
        chipDeviceControllerClass
                .getMethod("close")
                .invoke(controller);
    }

    private static Method findSingleParameterMethod(Class<?> targetClass, String name) throws NoSuchMethodException {
        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Method findNoParameterMethod(Class<?> targetClass, String name) throws NoSuchMethodException {
        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == 0) {
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Class<?> requireClass(Class<?> type, String name) {
        if (type == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return type;
    }

    private static Class<?> requireAvailable(Class<?> type, String name) {
        if (type == null) {
            throw new IllegalStateException(name + " is not configured");
        }
        return type;
    }

    private static Class<?> nestedClass(Class<?> targetClass, String simpleName) {
        if (targetClass == null) {
            return null;
        }
        for (Class<?> nested : targetClass.getClasses()) {
            if (simpleName.equals(nested.getSimpleName())) {
                return nested;
            }
        }
        return null;
    }

    private Class<?> commissionParametersClass() {
        Class<?> declaringClass = commissionParametersBuilderClass.getDeclaringClass();
        if (declaringClass == null) {
            throw new IllegalStateException("CommissionParameters.Builder must be a nested class");
        }
        return declaringClass;
    }

    private Object newConcreteDeviceAttestationDelegate(Object controller, boolean attestationBypassEnabled) {
        Class<?> delegateInterface = requireAvailable(deviceAttestationDelegateClass, "deviceAttestationDelegateClass");
        try {
            ClassLoader classLoader = ConnectedHomeIpReflectionCommandFactory.class.getClassLoader();
            Class<?> delegateClass = Class.forName(
                    CONCRETE_DEVICE_ATTESTATION_DELEGATE_CLASS,
                    false,
                    classLoader);
            Constructor<?> constructor = delegateClass.getConstructor(chipDeviceControllerClass, boolean.class);
            Object delegate = constructor.newInstance(controller, attestationBypassEnabled);
            if (!delegateInterface.isInstance(delegate)) {
                ConnectedHomeIpDiagnostics.emit("Concrete device attestation delegate is not compatible; using proxy fallback");
                return null;
            }
            ConnectedHomeIpDiagnostics.emit("Using concrete connectedhomeip device attestation delegate");
            return delegate;
        } catch (ClassNotFoundException exception) {
            ConnectedHomeIpDiagnostics.emit("Concrete connectedhomeip device attestation delegate not packaged; using proxy fallback");
            return null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Concrete connectedhomeip device attestation delegate unavailable: "
                            + safeMessage(exception)
                            + "; using proxy fallback");
            return null;
        }
    }

    private Object newConcreteAttestationTrustStoreDelegate(ConnectedHomeIpAttestationTrustStore store) {
        Class<?> delegateInterface = requireAvailable(
                attestationTrustStoreDelegateClass,
                "attestationTrustStoreDelegateClass");
        try {
            ClassLoader classLoader = ConnectedHomeIpReflectionCommandFactory.class.getClassLoader();
            Class<?> delegateClass = Class.forName(
                    CONCRETE_ATTESTATION_TRUST_STORE_DELEGATE_CLASS,
                    false,
                    classLoader);
            Constructor<?> constructor = delegateClass.getConstructor(ConnectedHomeIpAttestationTrustStore.class);
            Object delegate = constructor.newInstance(store);
            if (!delegateInterface.isInstance(delegate)) {
                ConnectedHomeIpDiagnostics.emit(
                        "Concrete attestation trust-store delegate is not compatible; using proxy fallback");
                return null;
            }
            ConnectedHomeIpDiagnostics.emit("Using concrete connectedhomeip attestation trust-store delegate");
            return delegate;
        } catch (ClassNotFoundException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Concrete connectedhomeip attestation trust-store delegate not packaged; using proxy fallback");
            return null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Concrete connectedhomeip attestation trust-store delegate unavailable: "
                            + safeMessage(exception)
                            + "; using proxy fallback");
            return null;
        }
    }

    private Object newConcreteConnectedDeviceCallback(ConnectedHomeIpConnectedDeviceCallback callback) {
        Class<?> callbackInterface = requireAvailable(getConnectedDeviceCallbackClass, "getConnectedDeviceCallbackClass");
        try {
            ClassLoader classLoader = ConnectedHomeIpReflectionCommandFactory.class.getClassLoader();
            Class<?> callbackClass = Class.forName(
                    CONCRETE_CONNECTED_DEVICE_CALLBACK_CLASS,
                    false,
                    classLoader);
            Constructor<?> constructor = callbackClass.getConstructor(ConnectedHomeIpConnectedDeviceCallback.class);
            Object concreteCallback = constructor.newInstance(callback);
            if (!callbackInterface.isInstance(concreteCallback)) {
                ConnectedHomeIpDiagnostics.emit("Concrete connected device callback is not compatible; using proxy fallback");
                return null;
            }
            ConnectedHomeIpDiagnostics.emit("Using concrete connectedhomeip connected device callback");
            return concreteCallback;
        } catch (ClassNotFoundException exception) {
            ConnectedHomeIpDiagnostics.emit("Concrete connectedhomeip connected device callback not packaged; using proxy fallback");
            return null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Concrete connectedhomeip connected device callback unavailable: "
                            + safeMessage(exception)
                            + "; using proxy fallback");
            return null;
        }
    }

    private Object newConcreteOpenCommissioningWindowCallback(
            ConnectedHomeIpOpenCommissioningWindowCallback callback) {
        Class<?> callbackInterface = requireAvailable(openCommissioningCallbackClass, "openCommissioningCallbackClass");
        Object concreteCallback = newConcreteOpenCommissioningWindowCallback(
                callback,
                callbackInterface,
                CONCRETE_OPEN_COMMISSIONING_WINDOW_CALLBACK_CLASS,
                "open commissioning window");
        if (concreteCallback != null) {
            return concreteCallback;
        }
        return newConcreteOpenCommissioningWindowCallback(
                callback,
                callbackInterface,
                CONCRETE_OPEN_COMMISSIONING_CALLBACK_CLASS,
                "open commissioning");
    }

    private Object newConcreteOpenCommissioningWindowCallback(
            ConnectedHomeIpOpenCommissioningWindowCallback callback,
            Class<?> callbackInterface,
            String callbackClassName,
            String callbackDescription) {
        try {
            ClassLoader classLoader = ConnectedHomeIpReflectionCommandFactory.class.getClassLoader();
            Class<?> callbackClass = Class.forName(
                    callbackClassName,
                    false,
                    classLoader);
            Constructor<?> constructor = callbackClass.getConstructor(
                    ConnectedHomeIpOpenCommissioningWindowCallback.class);
            Object concreteCallback = constructor.newInstance(callback);
            if (!callbackInterface.isInstance(concreteCallback)) {
                ConnectedHomeIpDiagnostics.emit(
                        "Concrete " + callbackDescription + " callback is not compatible; using proxy fallback");
                return null;
            }
            ConnectedHomeIpDiagnostics.emit("Using concrete connectedhomeip " + callbackDescription + " callback");
            return concreteCallback;
        } catch (ClassNotFoundException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Concrete connectedhomeip " + callbackDescription + " callback not packaged; using proxy fallback");
            return null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Concrete connectedhomeip " + callbackDescription + " callback unavailable: "
                            + safeMessage(exception)
                            + "; using proxy fallback");
            return null;
        }
    }

    private Object newDeferredIcdRegistrationInfo() throws ReflectiveOperationException {
        if (icdRegistrationInfoClass == null) {
            ConnectedHomeIpDiagnostics.emit("connectedhomeip ICD registration info class unavailable; ICD registration disabled");
            return null;
        }
        try {
            return icdRegistrationInfoClass
                    .getMethod("createForDeferredConfiguration")
                    .invoke(null);
        } catch (NoSuchMethodException exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "connectedhomeip ICD deferred factory unavailable; using empty ICD registration info builder");
            Object builder = icdRegistrationInfoClass
                    .getMethod("newBuilder")
                    .invoke(null);
            return findNoParameterMethod(builder.getClass(), "build").invoke(builder);
        }
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
}
