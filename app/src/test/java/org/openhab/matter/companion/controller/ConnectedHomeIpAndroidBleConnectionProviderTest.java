package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        assertArrayEquals(new byte[] {0x00, (byte) 0xFF, (byte) 0xFF}, filter.serviceDataMask());
    }

    @Test
    public void matterScanFilterMatchesLongDiscriminatorInServiceDataWithTrailingMatterFields() {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(3840);

        assertTrue(filter.matchesServiceData(new byte[] {
                0x00,
                0x00,
                0x0F,
                0x34,
                0x12,
                0x78,
                0x56,
                0x00
        }));
    }

    @Test
    public void matterScanFilterMatchesConnectedHomeIpAndAndroidObservedServiceDataOpcodes() {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(3840);

        assertTrue(filter.matchesServiceData(new byte[] {
                0x00,
                0x00,
                0x0F,
                0x34,
                0x12,
                0x78,
                0x56,
                0x00
        }));
        assertTrue(filter.matchesServiceData(new byte[] {
                0x01,
                0x00,
                0x0F,
                0x34,
                0x12,
                0x78,
                0x56,
                0x00
        }));
    }

    @Test
    public void matterScanFilterRejectsDifferentDiscriminatorServiceData() {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(3840);

        assertFalse(filter.matchesServiceData(new byte[] {
                0x01,
                0x01,
                0x0F,
                0x34,
                0x12,
                0x78,
                0x56,
                0x00
        }));
    }

    @Test
    public void matterScanFilterRejectsMissingServiceData() {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(3840);

        assertFalse(filter.matchesServiceData(null));
        assertFalse(filter.matchesServiceData(new byte[] {0x00, 0x00}));
    }

    @Test
    public void matterScanFilterExtractsAdvertisedDiscriminatorForDiagnostics() {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(3840);

        assertEquals(3840, filter.advertisedDiscriminator(new byte[] {0x01, 0x00, 0x0F, 0x34}));
        assertEquals(3841, filter.advertisedDiscriminator(new byte[] {0x01, 0x01, 0x0F, 0x34}));
        assertEquals(-1, filter.advertisedDiscriminator(null));
        assertEquals(-1, filter.advertisedDiscriminator(new byte[] {0x00, 0x00}));
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

        assertEquals(
                "No Matter BLE advertisement found for discriminator 3840. Put the Matter device into pairing mode near this phone, keep Bluetooth enabled, and retry before the pairing window expires.",
                exception.getMessage());
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

    @Test
    public void androidBleScannerTriesBroadParsedServiceDataBeforeExactHardwareFilter() throws Exception {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(1740);
        List<String> calls = new ArrayList<>();

        Object device = ConnectedHomeIpAndroidBleConnectionProvider.AndroidBleScanner.firstMatch(
                filter,
                1000L,
                new RecordingScanPhase("broad", "matter-device", calls),
                new RecordingScanPhase("exact", "late-device", calls));

        assertEquals("matter-device", device);
        assertEquals(Arrays.asList("broad"), calls);
    }

    @Test
    public void androidBleScannerUsesExactHardwareFilterIfBroadParsingFindsNoDevice() throws Exception {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(1740);
        List<String> calls = new ArrayList<>();

        Object device = ConnectedHomeIpAndroidBleConnectionProvider.AndroidBleScanner.firstMatch(
                filter,
                1000L,
                new RecordingScanPhase("broad", null, calls),
                new RecordingScanPhase("exact", "matter-device", calls));

        assertEquals("matter-device", device);
        assertEquals(Arrays.asList("broad", "exact"), calls);
    }

    @Test
    public void androidBleScannerCanRetryScanPhasesAcrossRounds() throws Exception {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(1740);
        List<String> calls = new ArrayList<>();

        Object device = ConnectedHomeIpAndroidBleConnectionProvider.AndroidBleScanner.firstMatch(
                filter,
                1000L,
                2,
                new RecordingScanPhase("broad", null, calls),
                new DeviceOnAttemptScanPhase("exact", 2, "matter-device", calls));

        assertEquals("matter-device", device);
        assertEquals(Arrays.asList("broad", "exact", "broad", "exact"), calls);
    }

    @Test
    public void androidBleScannerReportsScanRoundsToDiagnosticsSink() throws Exception {
        ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter =
                ConnectedHomeIpAndroidBleConnectionProvider.matterScanFilter(1740);
        List<String> diagnostics = new ArrayList<>();

        ConnectedHomeIpDiagnostics.withListener(diagnostics::add, () ->
                ConnectedHomeIpAndroidBleConnectionProvider.AndroidBleScanner.firstMatch(
                        filter,
                        1000L,
                        2,
                        new RecordingScanPhase("broad", null, new ArrayList<>()),
                        new DeviceOnAttemptScanPhase("exact", 2, "matter-device", new ArrayList<>())));

        assertEquals(Arrays.asList(
                "Matter BLE scan round 1 of 2 for discriminator 1740",
                "Matter BLE scan round 2 of 2 for discriminator 1740"),
                diagnostics);
    }

    @Test
    public void androidBleScannerFormatsServiceDataForDiagnostics() {
        Map<UUID, byte[]> serviceData = new LinkedHashMap<>();
        serviceData.put(UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB"), new byte[] {0x01, (byte) 0xCC, 0x06});

        assertEquals(
                "0000fff6-0000-1000-8000-00805f9b34fb=01CC06",
                ConnectedHomeIpAndroidBleConnectionProvider.AndroidBleScanner.describeServiceData(serviceData));
    }

    @Test
    public void androidGattConnectorRetriesObservedLinkLayerConnectionFailures() {
        assertTrue(ConnectedHomeIpAndroidBleConnectionProvider.AndroidGattConnector.isRetryableConnectionFailureStatus(62));
        assertTrue(ConnectedHomeIpAndroidBleConnectionProvider.AndroidGattConnector.isRetryableConnectionFailureStatus(133));
        assertFalse(ConnectedHomeIpAndroidBleConnectionProvider.AndroidGattConnector.isRetryableConnectionFailureStatus(0));
    }

    @Test
    public void diagnosticsListenerIsClearedAfterScopedCall() throws Exception {
        List<String> diagnostics = new ArrayList<>();

        ConnectedHomeIpDiagnostics.withListener(diagnostics::add, () -> {
            ConnectedHomeIpDiagnostics.emit("inside");
            return null;
        });
        ConnectedHomeIpDiagnostics.emit("outside");

        assertEquals(Arrays.asList("inside"), diagnostics);
    }

    @Test
    public void diagnosticsListenerReceivesMessagesFromCallbackThreadDuringScopedCall() throws Exception {
        List<String> diagnostics = new ArrayList<>();
        Thread callbackThread = new Thread(() -> ConnectedHomeIpDiagnostics.emit("callback-thread"), "callback-thread");

        ConnectedHomeIpDiagnostics.withListener(diagnostics::add, () -> {
            callbackThread.start();
            callbackThread.join();
            return null;
        });

        assertEquals(Arrays.asList("callback-thread"), diagnostics);
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

    private static final class RecordingScanPhase implements ConnectedHomeIpAndroidBleConnectionProvider.AndroidBleScanner.ScanPhase {
        private final String name;
        private final Object device;
        private final List<String> calls;

        private RecordingScanPhase(String name, Object device, List<String> calls) {
            this.name = name;
            this.device = device;
            this.calls = calls;
        }

        @Override
        public Object scan(
                ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter,
                long timeoutMillis) {
            calls.add(name);
            return device;
        }
    }

    private static final class DeviceOnAttemptScanPhase implements ConnectedHomeIpAndroidBleConnectionProvider.AndroidBleScanner.ScanPhase {
        private final String name;
        private final int attemptWithDevice;
        private final Object device;
        private final List<String> calls;
        private int attempts;

        private DeviceOnAttemptScanPhase(String name, int attemptWithDevice, Object device, List<String> calls) {
            this.name = name;
            this.attemptWithDevice = attemptWithDevice;
            this.device = device;
            this.calls = calls;
        }

        @Override
        public Object scan(
                ConnectedHomeIpAndroidBleConnectionProvider.MatterBleScanFilter filter,
                long timeoutMillis) {
            calls.add(name);
            attempts++;
            return attempts == attemptWithDevice ? device : null;
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
