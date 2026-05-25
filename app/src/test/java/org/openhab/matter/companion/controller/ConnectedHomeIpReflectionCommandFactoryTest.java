package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;

import org.junit.Test;
import org.openhab.matter.companion.domain.ThreadDataset;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public final class ConnectedHomeIpReflectionCommandFactoryTest {
    @Test
    public void buildsThreadCommissionParametersWithNetworkCredentials() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = new ConnectedHomeIpReflectionCommandFactory(
                FakeNetworkCredentials.class,
                FakeNetworkCredentials.ThreadCredentials.class,
                FakeCommissionParameters.Builder.class,
                FakeChipDeviceController.class,
                FakeOpenCommissioningCallback.class);

        Object params = factory.newThreadCommissionParameters(ThreadDataset.parse("hex:0E08FF1000000000"));

        assertArrayEquals(new byte[] {
                0x0E,
                0x08,
                (byte) 0xFF,
                0x10,
                0x00,
                0x00,
                0x00,
                0x00
        }, FakeNetworkCredentials.lastThreadCredentials.operationalDataset);
        assertSame(FakeCommissionParameters.BUILT, params);
        assertNull(FakeCommissionParameters.lastBuilder.csrNonce);
        assertEquals("network:8", FakeCommissionParameters.lastBuilder.networkCredentials);
        assertNull(FakeCommissionParameters.lastBuilder.icdRegistrationInfo);
    }

    @Test
    public void locatesConnectedHomeIpControllerCommandMethods() throws Exception {
        ConnectedHomeIpReflectionCommandFactory factory = new ConnectedHomeIpReflectionCommandFactory(
                FakeNetworkCredentials.class,
                FakeNetworkCredentials.ThreadCredentials.class,
                FakeCommissionParameters.Builder.class,
                FakeChipDeviceController.class,
                FakeOpenCommissioningCallback.class);

        Method pair = factory.pairDeviceThroughBleMethod();
        Method ocw = factory.openPairingWindowWithPinCallbackMethod();

        assertEquals("pairDeviceThroughBLE", pair.getName());
        assertEquals(BluetoothGatt.class, pair.getParameterTypes()[0]);
        assertEquals("openPairingWindowWithPINCallback", ocw.getName());
        assertEquals(FakeOpenCommissioningCallback.class, ocw.getParameterTypes()[5]);
    }

    public static final class FakeNetworkCredentials {
        private static ThreadCredentials lastThreadCredentials;

        public static Object forThread(ThreadCredentials threadCredentials) {
            lastThreadCredentials = threadCredentials;
            return "network:" + threadCredentials.operationalDataset.length;
        }

        public static final class ThreadCredentials {
            private final byte[] operationalDataset;

            public ThreadCredentials(byte[] operationalDataset) {
                this.operationalDataset = operationalDataset;
            }
        }
    }

    public static final class FakeCommissionParameters {
        private static final Object BUILT = new Object();
        private static Builder lastBuilder;

        public static final class Builder {
            private Object csrNonce = "unset";
            private Object networkCredentials;
            private Object icdRegistrationInfo = "unset";

            public Builder() {
                lastBuilder = this;
            }

            public Builder setCsrNonce(byte[] csrNonce) {
                this.csrNonce = csrNonce;
                return this;
            }

            public Builder setNetworkCredentials(Object networkCredentials) {
                this.networkCredentials = networkCredentials;
                return this;
            }

            public Builder setICDRegistrationInfo(Object icdRegistrationInfo) {
                this.icdRegistrationInfo = icdRegistrationInfo;
                return this;
            }

            public Object build() {
                return BUILT;
            }
        }
    }

    public static final class FakeChipDeviceController {
        public void pairDeviceThroughBLE(
                BluetoothGatt bleServer,
                int connId,
                long deviceId,
                long setupPincode,
                Object params) {
        }

        public boolean openPairingWindowWithPINCallback(
                long devicePtr,
                int duration,
                long iteration,
                int discriminator,
                Long setupPinCode,
                FakeOpenCommissioningCallback callback) {
            return true;
        }
    }

    public interface FakeOpenCommissioningCallback {
    }
}
