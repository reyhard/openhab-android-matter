package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpAndroidBleConnectionProviderTest {
    @Test
    public void matterScanFilterUsesFullDiscriminatorServiceData() {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(3840);

        assertEquals(UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB"), filter.serviceUuid());
        assertArrayEquals(new byte[] {0x00, 0x00, 0x0F}, filter.serviceData());
        assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, filter.serviceDataMask());
    }

    @Test
    public void connectScansConnectsRegistersBleManagerAndReturnsConnection() throws Exception {
        FakeScanner scanner = new FakeScanner("matter-device");
        FakeGattConnector connector = new FakeGattConnector();
        FakeBleManager bleManager = new FakeBleManager(42);
        ConnectedHomeIpAndroidBleConnectionProvider provider = new ConnectedHomeIpAndroidBleConnectionProvider(
                scanner,
                connector,
                bleManager,
                1000L);

        ConnectedHomeIpBleConnection connection = provider.connect(3840);

        assertEquals(3840, scanner.filter.discriminator());
        assertEquals(1000L, scanner.timeoutMillis);
        assertEquals("matter-device", connector.device);
        assertSame(bleManager.gattCallback, connector.chipGattCallback);
        assertSame(connector.gatt, bleManager.gatt);
        assertTrue(bleManager.bleCallbackSet);
        assertEquals(42, connection.connectionId());
        assertSame(connector.gatt, connection.gatt());
        assertFalse(connector.connection.closed);
    }

    @Test
    public void closeRemovesConnectionAndClosesGatt() throws Exception {
        FakeGattConnector connector = new FakeGattConnector();
        FakeBleManager bleManager = new FakeBleManager(42);
        ConnectedHomeIpAndroidBleConnectionProvider provider = new ConnectedHomeIpAndroidBleConnectionProvider(
                new FakeScanner("matter-device"),
                connector,
                bleManager,
                1000L);

        ConnectedHomeIpBleConnection connection = provider.connect(3840);
        connection.close();

        assertEquals(42, bleManager.removedConnectionId);
        assertTrue(connector.connection.closed);
    }

    @Test
    public void connectFailsWhenScanTimesOut() {
        ConnectedHomeIpAndroidBleConnectionProvider provider = new ConnectedHomeIpAndroidBleConnectionProvider(
                new FakeScanner(null),
                new FakeGattConnector(),
                new FakeBleManager(42),
                1000L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> provider.connect(3840));

        assertTrue(exception.getMessage().contains("3840"));
    }

    @Test
    public void connectClosesGattWhenRegistrationFails() {
        FakeGattConnector connector = new FakeGattConnector();
        ConnectedHomeIpAndroidBleConnectionProvider provider = new ConnectedHomeIpAndroidBleConnectionProvider(
                new FakeScanner("matter-device"),
                connector,
                new FakeBleManager(0),
                1000L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> provider.connect(3840));

        assertTrue(exception.getMessage().contains("connection id"));
        assertTrue(connector.connection.closed);
    }

    @Test
    public void connectClosesGattWhenBleCallbackRegistrationFails() {
        FakeGattConnector connector = new FakeGattConnector();
        FakeBleManager bleManager = new FakeBleManager(42);
        bleManager.failSetBleCallback = true;
        ConnectedHomeIpAndroidBleConnectionProvider provider = new ConnectedHomeIpAndroidBleConnectionProvider(
                new FakeScanner("matter-device"),
                connector,
                bleManager,
                1000L);

        assertThrows(Exception.class, () -> provider.connect(3840));

        assertEquals(42, bleManager.removedConnectionId);
        assertTrue(connector.connection.closed);
    }

    @Test
    public void connectClosesGattWhenRemoveConnectionFailsDuringErrorCleanup() {
        FakeGattConnector connector = new FakeGattConnector();
        FakeBleManager bleManager = new FakeBleManager(42);
        bleManager.failSetBleCallback = true;
        bleManager.failRemoveConnection = true;
        ConnectedHomeIpAndroidBleConnectionProvider provider = new ConnectedHomeIpAndroidBleConnectionProvider(
                new FakeScanner("matter-device"),
                connector,
                bleManager,
                1000L);

        assertThrows(Exception.class, () -> provider.connect(3840));

        assertTrue(connector.connection.closed);
    }

    private static final class FakeScanner implements ConnectedHomeIpAndroidBleConnectionProvider.BleScanner {
        private final Object device;
        private ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter;
        private long timeoutMillis;

        private FakeScanner(Object device) {
            this.device = device;
        }

        @Override
        public Object scan(
                ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter,
                long timeoutMillis) {
            this.filter = filter;
            this.timeoutMillis = timeoutMillis;
            return device;
        }
    }

    private static final class FakeGattConnector implements ConnectedHomeIpAndroidBleConnectionProvider.GattConnector {
        private final BluetoothGatt gatt = null;
        private final FakeGattConnection connection = new FakeGattConnection(gatt);
        private Object device;
        private BluetoothGattCallback chipGattCallback;

        @Override
        public ConnectedHomeIpAndroidBleConnectionProvider.GattConnection connect(
                Object device,
                BluetoothGattCallback chipGattCallback) {
            this.device = device;
            this.chipGattCallback = chipGattCallback;
            return connection;
        }
    }

    private static final class FakeGattConnection implements ConnectedHomeIpAndroidBleConnectionProvider.GattConnection {
        private final BluetoothGatt gatt;
        private boolean closed;

        private FakeGattConnection(BluetoothGatt gatt) {
            this.gatt = gatt;
        }

        @Override
        public BluetoothGatt gatt() {
            return gatt;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class FakeBleManager implements ConnectedHomeIpAndroidBleConnectionProvider.BleManagerBridge {
        private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() { };
        private final int connectionId;
        private BluetoothGatt gatt;
        private boolean bleCallbackSet;
        private boolean failSetBleCallback;
        private boolean failRemoveConnection;
        private int removedConnectionId;

        private FakeBleManager(int connectionId) {
            this.connectionId = connectionId;
        }

        @Override
        public BluetoothGattCallback gattCallback() {
            return gattCallback;
        }

        @Override
        public int addConnection(BluetoothGatt gatt) {
            this.gatt = gatt;
            return connectionId;
        }

        @Override
        public void setBleCallback() {
            if (failSetBleCallback) {
                throw new IllegalStateException("setBleCallback failed");
            }
            bleCallbackSet = true;
        }

        @Override
        public void removeConnection(int connectionId) {
            if (failRemoveConnection) {
                throw new IllegalStateException("removeConnection failed");
            }
            removedConnectionId = connectionId;
        }
    }
}
