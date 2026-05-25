package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;

import org.openhab.matter.companion.domain.ThreadDataset;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ConnectedHomeIpReflectionCommandFactory {
    private static final String NETWORK_CREDENTIALS_CLASS = "chip.devicecontroller.NetworkCredentials";
    private static final String THREAD_CREDENTIALS_CLASS = "chip.devicecontroller.NetworkCredentials$ThreadCredentials";
    private static final String COMMISSION_PARAMETERS_BUILDER_CLASS = "chip.devicecontroller.CommissionParameters$Builder";
    private static final String CHIP_DEVICE_CONTROLLER_CLASS = "chip.devicecontroller.ChipDeviceController";
    private static final String OPEN_COMMISSIONING_CALLBACK_CLASS = "chip.devicecontroller.OpenCommissioningCallback";

    private final Class<?> networkCredentialsClass;
    private final Class<?> threadCredentialsClass;
    private final Class<?> commissionParametersBuilderClass;
    private final Class<?> chipDeviceControllerClass;
    private final Class<?> openCommissioningCallbackClass;

    public ConnectedHomeIpReflectionCommandFactory(
            Class<?> networkCredentialsClass,
            Class<?> threadCredentialsClass,
            Class<?> commissionParametersBuilderClass,
            Class<?> chipDeviceControllerClass,
            Class<?> openCommissioningCallbackClass) {
        this.networkCredentialsClass = requireClass(networkCredentialsClass, "networkCredentialsClass");
        this.threadCredentialsClass = requireClass(threadCredentialsClass, "threadCredentialsClass");
        this.commissionParametersBuilderClass = requireClass(
                commissionParametersBuilderClass,
                "commissionParametersBuilderClass");
        this.chipDeviceControllerClass = requireClass(chipDeviceControllerClass, "chipDeviceControllerClass");
        this.openCommissioningCallbackClass = requireClass(
                openCommissioningCallbackClass,
                "openCommissioningCallbackClass");
    }

    public static ConnectedHomeIpReflectionCommandFactory fromDefaultClassLoader() throws ClassNotFoundException {
        ClassLoader classLoader = ConnectedHomeIpReflectionCommandFactory.class.getClassLoader();
        return new ConnectedHomeIpReflectionCommandFactory(
                Class.forName(NETWORK_CREDENTIALS_CLASS, false, classLoader),
                Class.forName(THREAD_CREDENTIALS_CLASS, false, classLoader),
                Class.forName(COMMISSION_PARAMETERS_BUILDER_CLASS, false, classLoader),
                Class.forName(CHIP_DEVICE_CONTROLLER_CLASS, false, classLoader),
                Class.forName(OPEN_COMMISSIONING_CALLBACK_CLASS, false, classLoader));
    }

    public Object newThreadCommissionParameters(ThreadDataset dataset) throws ReflectiveOperationException {
        Constructor<?> threadCredentialsConstructor = threadCredentialsClass.getConstructor(byte[].class);
        Object threadCredentials = threadCredentialsConstructor.newInstance((Object) dataset.bytes());
        Method forThread = networkCredentialsClass.getMethod("forThread", threadCredentialsClass);
        Object networkCredentials = forThread.invoke(null, threadCredentials);
        Object builder = commissionParametersBuilderClass.getConstructor().newInstance();
        findSingleParameterMethod(commissionParametersBuilderClass, "setCsrNonce").invoke(builder, new Object[] {null});
        findSingleParameterMethod(commissionParametersBuilderClass, "setNetworkCredentials").invoke(builder, networkCredentials);
        findSingleParameterMethod(commissionParametersBuilderClass, "setICDRegistrationInfo").invoke(builder, new Object[] {null});
        return findNoParameterMethod(commissionParametersBuilderClass, "build").invoke(builder);
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
        return new ConnectedHomeIpOpenCommissioningWindowCallback(openCommissioningCallbackClass, controllerState);
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

    private Class<?> commissionParametersClass() {
        Class<?> declaringClass = commissionParametersBuilderClass.getDeclaringClass();
        if (declaringClass == null) {
            throw new IllegalStateException("CommissionParameters.Builder must be a nested class");
        }
        return declaringClass;
    }
}
