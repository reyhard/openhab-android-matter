package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public final class ConnectedHomeIpAttestationTrustStoreDelegate {
    private final Object proxy;

    public ConnectedHomeIpAttestationTrustStoreDelegate(
            Class<?> delegateClass,
            Class<?> deviceAttestationClass,
            ConnectedHomeIpAttestationTrustStore store) {
        if (delegateClass == null) {
            throw new IllegalArgumentException("delegateClass is required");
        }
        if (deviceAttestationClass == null) {
            throw new IllegalArgumentException("deviceAttestationClass is required");
        }
        if (store == null) {
            throw new IllegalArgumentException("store is required");
        }
        this.proxy = Proxy.newProxyInstance(
                delegateClass.getClassLoader(),
                new Class<?>[] {delegateClass},
                new Handler(deviceAttestationClass, store));
    }

    public Object proxy() {
        return proxy;
    }

    private static final class Handler implements InvocationHandler {
        private final Method extractSkid;
        private final ConnectedHomeIpAttestationTrustStore store;

        private Handler(Class<?> deviceAttestationClass, ConnectedHomeIpAttestationTrustStore store) {
            try {
                this.extractSkid = deviceAttestationClass.getMethod("extractSkidFromPaaCert", byte[].class);
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("DeviceAttestation.extractSkidFromPaaCert not found", exception);
            }
            this.store = store;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getProductAttestationAuthorityCert".equals(method.getName())) {
                byte[] requestedSkid = args != null && args.length > 0 ? (byte[]) args[0] : null;
                if (requestedSkid == null) {
                    return null;
                }
                for (byte[] certificate : store.paaCertificates()) {
                    byte[] certificateSkid = (byte[]) extractSkid.invoke(null, certificate);
                    if (Arrays.equals(certificateSkid, requestedSkid)) {
                        return certificate;
                    }
                }
                return null;
            }
            return null;
        }
    }
}
