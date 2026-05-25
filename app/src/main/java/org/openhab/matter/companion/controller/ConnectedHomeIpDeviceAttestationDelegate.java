package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class ConnectedHomeIpDeviceAttestationDelegate {
    private final Object proxy;

    public ConnectedHomeIpDeviceAttestationDelegate(
            Class<?> delegateClass,
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            Object controller,
            boolean attestationBypassEnabled) {
        if (delegateClass == null) {
            throw new IllegalArgumentException("delegateClass is required");
        }
        if (commandFactory == null) {
            throw new IllegalArgumentException("commandFactory is required");
        }
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        this.proxy = Proxy.newProxyInstance(
                delegateClass.getClassLoader(),
                new Class<?>[] {delegateClass},
                new AttestationHandler(commandFactory, controller, attestationBypassEnabled));
    }

    public Object proxy() {
        return proxy;
    }

    private static final class AttestationHandler implements InvocationHandler {
        private final ConnectedHomeIpReflectionCommandFactory commandFactory;
        private final Object controller;
        private final boolean attestationBypassEnabled;

        private AttestationHandler(
                ConnectedHomeIpReflectionCommandFactory commandFactory,
                Object controller,
                boolean attestationBypassEnabled) {
            this.commandFactory = commandFactory;
            this.controller = controller;
            this.attestationBypassEnabled = attestationBypassEnabled;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("onDeviceAttestationCompleted".equals(method.getName())) {
                long devicePtr = args != null && args.length > 0 && args[0] instanceof Number
                        ? ((Number) args[0]).longValue()
                        : -1L;
                commandFactory.invokeContinueCommissioning(controller, devicePtr, attestationBypassEnabled);
                return null;
            }
            return defaultValue(method.getReturnType());
        }
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
