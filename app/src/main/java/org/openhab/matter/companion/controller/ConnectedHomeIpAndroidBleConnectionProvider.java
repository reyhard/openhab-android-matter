package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class ConnectedHomeIpAndroidBleConnectionProvider implements ConnectedHomeIpBleConnectionProvider,
        ConnectedHomeIpRuntimePreflightChecker {
    private static final String TAG = "OpenHabMatterBle";
    private static final UUID MATTER_BLE_SERVICE_UUID = UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB");
    private static final int MAX_SCAN_RECORD_DIAGNOSTICS = 40;
    private static final int MAX_SCAN_ROUNDS = 3;
    private static final long DEFAULT_SCAN_TIMEOUT_MILLIS = 30_000L;
    private static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 15_000L;
    private static final int CHIP_MTU = 247;

    private final BleScanner scanner;
    private final GattConnector connector;
    private final BleManagerBridge bleManager;
    private final long scanTimeoutMillis;

    public ConnectedHomeIpAndroidBleConnectionProvider(
            Context context,
            ConnectedHomeIpPlatformControllerProvider platformControllerProvider) throws ClassNotFoundException {
        this(
                new AndroidBleScanner(BluetoothAdapter.getDefaultAdapter()),
                new AndroidGattConnector(context, DEFAULT_CONNECT_TIMEOUT_MILLIS),
                new ReflectionBleManagerBridge(platformControllerProvider),
                DEFAULT_SCAN_TIMEOUT_MILLIS);
    }

    public ConnectedHomeIpAndroidBleConnectionProvider(
            BleScanner scanner,
            GattConnector connector,
            BleManagerBridge bleManager,
            long scanTimeoutMillis) {
        if (scanner == null) {
            throw new IllegalArgumentException("scanner is required");
        }
        if (connector == null) {
            throw new IllegalArgumentException("connector is required");
        }
        if (bleManager == null) {
            throw new IllegalArgumentException("bleManager is required");
        }
        if (scanTimeoutMillis <= 0) {
            throw new IllegalArgumentException("scanTimeoutMillis must be positive");
        }
        this.scanner = scanner;
        this.connector = connector;
        this.bleManager = bleManager;
        this.scanTimeoutMillis = scanTimeoutMillis;
    }

    @Override
    public ConnectedHomeIpBleConnection connect(int discriminator) throws Exception {
        MatterBleScanFilter filter = matterScanFilter(discriminator);
        Object device = scanner.scan(filter, scanTimeoutMillis);
        if (device == null) {
            throw new IllegalStateException(
                    "No Matter BLE advertisement found for discriminator " + discriminator
                            + ". Put the Matter device into pairing mode near this phone, keep Bluetooth enabled, and retry before the pairing window expires.");
        }
        GattConnection gattConnection = connector.connect(device, bleManager.gattCallback());
        boolean registered = false;
        int connectionId = 0;
        try {
            connectionId = bleManager.addConnection(gattConnection.gatt());
            if (connectionId <= 0) {
                throw new IllegalStateException("connectedhomeip BLE manager returned invalid connection id " + connectionId);
            }
            registered = true;
            bleManager.setBleCallback();
            int registeredConnectionId = connectionId;
            return new ConnectedHomeIpBleConnection(
                    gattConnection.gatt(),
                    registeredConnectionId,
                    () -> {
                        try {
                            bleManager.removeConnection(registeredConnectionId);
                        } finally {
                            gattConnection.close();
                        }
                    });
        } catch (Exception exception) {
            if (registered) {
                try {
                    bleManager.removeConnection(connectionId);
                } finally {
                    gattConnection.close();
                }
            } else {
                gattConnection.close();
            }
            throw exception;
        }
    }

    @Override
    public ConnectedHomeIpRuntimePreflightStatus checkRuntimePreflight() {
        try {
            BluetoothGattCallback callback = bleManager.gattCallback();
            if (callback == null) {
                return new ConnectedHomeIpRuntimePreflightStatus(
                        false,
                        "connectedhomeip BLE manager returned a null GATT callback");
            }
            return new ConnectedHomeIpRuntimePreflightStatus(
                    true,
                    "connectedhomeip BLE manager callback ready: " + callback.getClass().getName());
        } catch (Exception | LinkageError ex) {
            return new ConnectedHomeIpRuntimePreflightStatus(
                    false,
                    "connectedhomeip BLE manager preflight failed: " + safeMessage(ex));
        }
    }

    public static MatterBleScanFilter matterScanFilter(int discriminator) {
        int version = 0;
        int versionDiscriminator = ((version & 0xF) << 12) | (discriminator & 0xFFF);
        return new MatterBleScanFilter(
                discriminator,
                MATTER_BLE_SERVICE_UUID,
                new byte[] {
                        0x00,
                        (byte) versionDiscriminator,
                        (byte) (versionDiscriminator >> 8)
                },
                new byte[] {0x00, (byte) 0xFF, (byte) 0xFF});
    }

    public interface BleScanner {
        Object scan(MatterBleScanFilter filter, long timeoutMillis) throws Exception;
    }

    public interface GattConnector {
        GattConnection connect(Object device, BluetoothGattCallback chipGattCallback) throws Exception;
    }

    public interface GattConnection extends AutoCloseable {
        BluetoothGatt gatt();

        @Override
        void close() throws Exception;
    }

    public interface BleManagerBridge {
        BluetoothGattCallback gattCallback() throws Exception;

        int addConnection(BluetoothGatt gatt) throws Exception;

        void setBleCallback() throws Exception;

        void removeConnection(int connectionId) throws Exception;
    }

    public static final class MatterBleScanFilter {
        private final int discriminator;
        private final UUID serviceUuid;
        private final byte[] serviceData;
        private final byte[] serviceDataMask;

        private MatterBleScanFilter(int discriminator, UUID serviceUuid, byte[] serviceData, byte[] serviceDataMask) {
            this.discriminator = discriminator;
            this.serviceUuid = serviceUuid;
            this.serviceData = serviceData.clone();
            this.serviceDataMask = serviceDataMask.clone();
        }

        public int discriminator() {
            return discriminator;
        }

        public UUID serviceUuid() {
            return serviceUuid;
        }

        public byte[] serviceData() {
            return serviceData.clone();
        }

        public byte[] serviceDataMask() {
            return serviceDataMask.clone();
        }

        public boolean matchesServiceData(byte[] advertisedServiceData) {
            if (advertisedServiceData == null || advertisedServiceData.length < serviceData.length) {
                return false;
            }
            for (int i = 0; i < serviceData.length; i++) {
                if ((advertisedServiceData[i] & serviceDataMask[i]) != (serviceData[i] & serviceDataMask[i])) {
                    return false;
                }
            }
            return true;
        }

        public int advertisedDiscriminator(byte[] advertisedServiceData) {
            if (advertisedServiceData == null || advertisedServiceData.length < 3) {
                return -1;
            }
            int versionDiscriminator = (advertisedServiceData[1] & 0xFF) | ((advertisedServiceData[2] & 0xFF) << 8);
            return versionDiscriminator & 0xFFF;
        }
    }

    public static final class AndroidBleScanner implements BleScanner {
        private final BluetoothAdapter bluetoothAdapter;

        public AndroidBleScanner(BluetoothAdapter bluetoothAdapter) {
            this.bluetoothAdapter = bluetoothAdapter;
        }

        @Override
        public Object scan(MatterBleScanFilter filter, long timeoutMillis) throws Exception {
            if (bluetoothAdapter == null) {
                throw new IllegalStateException("Bluetooth adapter is not available");
            }
            if (!bluetoothAdapter.isEnabled()) {
                throw new IllegalStateException("Bluetooth adapter is disabled");
            }
            BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                throw new IllegalStateException("Bluetooth LE scanner is not available");
            }
            return firstMatch(
                    filter,
                    timeoutMillis,
                    MAX_SCAN_ROUNDS,
                    (scanFilter, scanTimeoutMillis) -> scanByParsedServiceData(bluetoothLeScanner, scanFilter, scanTimeoutMillis),
                    (scanFilter, scanTimeoutMillis) -> scanByExactHardwareFilter(bluetoothLeScanner, scanFilter, scanTimeoutMillis));
        }

        public interface ScanPhase {
            Object scan(MatterBleScanFilter filter, long timeoutMillis) throws Exception;
        }

        public static Object firstMatch(MatterBleScanFilter filter, long timeoutMillis, ScanPhase... scanPhases) throws Exception {
            return firstMatch(filter, timeoutMillis, 1, scanPhases);
        }

        public static Object firstMatch(
                MatterBleScanFilter filter,
                long timeoutMillis,
                int scanRounds,
                ScanPhase... scanPhases) throws Exception {
            if (scanRounds <= 0) {
                throw new IllegalArgumentException("scanRounds must be positive");
            }
            for (int round = 1; round <= scanRounds; round++) {
                if (scanRounds > 1) {
                    String message = "Matter BLE scan round " + round + " of " + scanRounds
                            + " for discriminator " + filter.discriminator();
                    logInfo("Starting " + message);
                    ConnectedHomeIpDiagnostics.emit(message);
                }
                for (ScanPhase scanPhase : scanPhases) {
                    Object device = scanPhase.scan(filter, timeoutMillis);
                    if (device != null) {
                        return device;
                    }
                }
            }
            return null;
        }

        private static void logInfo(String message) {
            try {
                Log.i(TAG, message);
            } catch (RuntimeException ignored) {
                // Android Log is not available in local JVM unit tests.
            }
        }

        private Object scanByExactHardwareFilter(
                BluetoothLeScanner bluetoothLeScanner,
                MatterBleScanFilter filter,
                long timeoutMillis) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<BluetoothDevice> device = new AtomicReference<>();
            AtomicReference<IllegalStateException> scanFailure = new AtomicReference<>();
            ScanCallback callback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (result != null && result.getDevice() != null && device.compareAndSet(null, result.getDevice())) {
                        latch.countDown();
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    scanFailure.compareAndSet(null, new IllegalStateException("Matter BLE scan failed with code " + errorCode));
                    latch.countDown();
                }
            };
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceData(
                            new ParcelUuid(filter.serviceUuid()),
                            filter.serviceData(),
                            filter.serviceDataMask())
                    .build();
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            logAndEmit("Starting exact Matter BLE scan for discriminator " + filter.discriminator());
            bluetoothLeScanner.startScan(Collections.singletonList(scanFilter), scanSettings, callback);
            try {
                boolean completed = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
                if (scanFailure.get() != null) {
                    throw scanFailure.get();
                }
                if (completed && device.get() != null) {
                    logAndEmit("Exact Matter BLE scan matched discriminator " + filter.discriminator()
                            + " at " + safeAddress(device.get()));
                    return device.get();
                }
            } finally {
                bluetoothLeScanner.stopScan(callback);
            }
            logAndEmit("Exact Matter BLE scan timed out for discriminator " + filter.discriminator());
            return null;
        }

        private Object scanByParsedServiceData(
                BluetoothLeScanner bluetoothLeScanner,
                MatterBleScanFilter filter,
                long timeoutMillis) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<BluetoothDevice> device = new AtomicReference<>();
            AtomicReference<IllegalStateException> scanFailure = new AtomicReference<>();
            AtomicInteger diagnosticsCount = new AtomicInteger();
            ScanCallback callback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (result == null || result.getDevice() == null || result.getScanRecord() == null) {
                        return;
                    }
                    ScanRecord record = result.getScanRecord();
                    logScanRecord(filter, result, record, diagnosticsCount);
                    byte[] serviceData = record.getServiceData(new ParcelUuid(filter.serviceUuid()));
                    if (serviceData != null) {
                        logAndEmit("Parsed Matter BLE scan saw service data from " + safeAddress(result.getDevice())
                                + ": discriminator=" + filter.advertisedDiscriminator(serviceData)
                                + ", data=" + hex(serviceData));
                    }
                    if (filter.matchesServiceData(serviceData) && device.compareAndSet(null, result.getDevice())) {
                        logAndEmit("Parsed Matter BLE scan matched discriminator " + filter.discriminator()
                                + " at " + safeAddress(result.getDevice()));
                        latch.countDown();
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    scanFailure.compareAndSet(null, new IllegalStateException("Matter BLE parsed scan failed with code " + errorCode));
                    latch.countDown();
                }
            };
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            logAndEmit("Starting parsed Matter BLE scan for discriminator " + filter.discriminator());
            bluetoothLeScanner.startScan(Collections.emptyList(), scanSettings, callback);
            try {
                boolean completed = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
                if (scanFailure.get() != null) {
                    throw scanFailure.get();
                }
                if (!completed) {
                    logAndEmit("Parsed Matter BLE scan timed out for discriminator " + filter.discriminator());
                    return null;
                }
                return device.get();
            } finally {
                bluetoothLeScanner.stopScan(callback);
            }
        }

        private static void logScanRecord(
                MatterBleScanFilter filter,
                ScanResult result,
                ScanRecord record,
                AtomicInteger diagnosticsCount) {
            int currentCount = diagnosticsCount.getAndIncrement();
            if (currentCount >= MAX_SCAN_RECORD_DIAGNOSTICS) {
                return;
            }
            logInfo("Parsed Matter BLE scan record " + (currentCount + 1)
                    + " for discriminator " + filter.discriminator()
                    + ": address=" + safeAddress(result.getDevice())
                    + ", name=" + safeDeviceName(result.getDevice())
                    + ", rssi=" + result.getRssi()
                    + ", serviceUuids=" + describeServiceUuids(record.getServiceUuids())
                    + ", serviceData=" + describeServiceData(record.getServiceData())
                    + ", manufacturerData=" + describeManufacturerData(record.getManufacturerSpecificData())
                    + ", raw=" + hex(record.getBytes()));
        }

        private static void logAndEmit(String message) {
            logInfo(message);
            ConnectedHomeIpDiagnostics.emit(message);
        }

        static String describeServiceUuids(List<ParcelUuid> serviceUuids) {
            if (serviceUuids == null || serviceUuids.isEmpty()) {
                return "<none>";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < serviceUuids.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(serviceUuids.get(i).getUuid());
            }
            return builder.toString();
        }

        static String describeServiceData(Map<?, byte[]> serviceData) {
            if (serviceData == null || serviceData.isEmpty()) {
                return "<none>";
            }
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Map.Entry<?, byte[]> entry : serviceData.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(entry.getKey()).append('=').append(hex(entry.getValue()));
                first = false;
            }
            return builder.toString();
        }

        static String describeManufacturerData(SparseArray<byte[]> manufacturerData) {
            if (manufacturerData == null || manufacturerData.size() == 0) {
                return "<none>";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < manufacturerData.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(manufacturerData.keyAt(i)).append('=').append(hex(manufacturerData.valueAt(i)));
            }
            return builder.toString();
        }

        private static String safeAddress(BluetoothDevice device) {
            try {
                return device.getAddress();
            } catch (SecurityException ex) {
                return "<address unavailable>";
            }
        }

        private static String safeDeviceName(BluetoothDevice device) {
            try {
                String name = device.getName();
                return name == null || name.isEmpty() ? "<none>" : name;
            } catch (SecurityException ex) {
                return "<name unavailable>";
            }
        }

        private static String hex(byte[] data) {
            if (data == null) {
                return "<null>";
            }
            StringBuilder builder = new StringBuilder(data.length * 2);
            for (byte value : data) {
                int unsigned = value & 0xFF;
                if (unsigned < 0x10) {
                    builder.append('0');
                }
                builder.append(Integer.toHexString(unsigned).toUpperCase());
            }
            return builder.toString();
        }
    }

    public static final class AndroidGattConnector implements GattConnector {
        private static final int MAX_CONNECT_ATTEMPTS = 3;
        private static final int GATT_CONNECTION_FAILED_TO_ESTABLISH = 62;
        private static final int GATT_GENERIC_ERROR = 133;
        private static final long RETRY_DELAY_MILLIS = 350L;

        private final Context context;
        private final long timeoutMillis;

        public AndroidGattConnector(Context context, long timeoutMillis) {
            if (context == null) {
                throw new IllegalArgumentException("context is required");
            }
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("timeoutMillis must be positive");
            }
            this.context = context;
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public GattConnection connect(Object device, BluetoothGattCallback chipGattCallback) throws Exception {
            if (!(device instanceof BluetoothDevice)) {
                throw new IllegalArgumentException("device must be a BluetoothDevice");
            }
            GattConnectionFailure lastRetryableFailure = null;
            for (int attempt = 1; attempt <= MAX_CONNECT_ATTEMPTS; attempt++) {
                try {
                    return connectOnce((BluetoothDevice) device, chipGattCallback);
                } catch (GattConnectionFailure failure) {
                    if (!isRetryableConnectionFailureStatus(failure.status()) || attempt == MAX_CONNECT_ATTEMPTS) {
                        throw failure;
                    }
                    lastRetryableFailure = failure;
                    logAndEmit("Bluetooth GATT connection failed with retryable status "
                            + failure.status()
                            + " on attempt " + attempt
                            + "; retrying");
                    Thread.sleep(RETRY_DELAY_MILLIS);
                }
            }
            throw lastRetryableFailure == null
                    ? new IllegalStateException("Bluetooth GATT connection failed")
                    : lastRetryableFailure;
        }

        static boolean isRetryableConnectionFailureStatus(int status) {
            return status == GATT_CONNECTION_FAILED_TO_ESTABLISH || status == GATT_GENERIC_ERROR;
        }

        private GattConnection connectOnce(BluetoothDevice device, BluetoothGattCallback chipGattCallback) throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<BluetoothGatt> connectedGatt = new AtomicReference<>();
            AtomicReference<IllegalStateException> error = new AtomicReference<>();
            BluetoothGattCallback callback = new ForwardingGattCallback(chipGattCallback, latch, connectedGatt, error);
            logAndEmit("Starting Bluetooth GATT connection to Matter device");
            BluetoothGatt gatt = connectGatt(device, callback);
            if (gatt == null) {
                throw new IllegalStateException("Bluetooth GATT connection could not be started");
            }
            boolean completed = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!completed) {
                gatt.disconnect();
                gatt.close();
                ConnectedHomeIpDiagnostics.emit("Bluetooth GATT connection timed out");
                throw new IllegalStateException("Bluetooth GATT connection timed out");
            }
            if (error.get() != null) {
                gatt.close();
                ConnectedHomeIpDiagnostics.emit(error.get().getMessage());
                throw error.get();
            }
            ConnectedHomeIpDiagnostics.emit("Bluetooth GATT connection ready for connectedhomeip");
            return new AndroidGattConnection(connectedGatt.get() == null ? gatt : connectedGatt.get());
        }

        private BluetoothGatt connectGatt(BluetoothDevice device, BluetoothGattCallback callback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE);
            }
            return device.connectGatt(context, false, callback);
        }

        private static final class GattConnectionFailure extends IllegalStateException {
            private final int status;

            private GattConnectionFailure(int status, int newState) {
                super("Bluetooth GATT disconnected during connection with status " + status + ", newState " + newState);
                this.status = status;
            }

            private int status() {
                return status;
            }
        }
    }

    public static final class ReflectionBleManagerBridge implements BleManagerBridge {
        private static final String BLE_CALLBACK_CLASS = "chip.platform.BleCallback";

        private final ConnectedHomeIpPlatformControllerProvider platformControllerProvider;
        private final Class<?> bleCallbackClass;
        private Object bleManager;
        private Object bleCallbackProxy;

        public ReflectionBleManagerBridge(ConnectedHomeIpPlatformControllerProvider platformControllerProvider)
                throws ClassNotFoundException {
            if (platformControllerProvider == null) {
                throw new IllegalArgumentException("platformControllerProvider is required");
            }
            this.platformControllerProvider = platformControllerProvider;
            this.bleCallbackClass = Class.forName(
                    BLE_CALLBACK_CLASS,
                    false,
                    ConnectedHomeIpAndroidBleConnectionProvider.class.getClassLoader());
        }

        @Override
        public BluetoothGattCallback gattCallback() throws Exception {
            return (BluetoothGattCallback) bleManager().getClass().getMethod("getCallback").invoke(bleManager());
        }

        @Override
        public int addConnection(BluetoothGatt gatt) throws Exception {
            Object result = bleManager().getClass().getMethod("addConnection", BluetoothGatt.class).invoke(bleManager(), gatt);
            return result instanceof Number ? ((Number) result).intValue() : 0;
        }

        @Override
        public void setBleCallback() throws Exception {
            bleManager().getClass().getMethod("setBleCallback", bleCallbackClass).invoke(bleManager(), bleCallbackProxy());
        }

        @Override
        public void removeConnection(int connectionId) throws Exception {
            bleManager().getClass().getMethod("removeConnection", int.class).invoke(bleManager(), connectionId);
        }

        private Object bleManager() throws Exception {
            if (bleManager == null) {
                Object platform = platformControllerProvider.platform();
                bleManager = platform.getClass().getMethod("getBLEManager").invoke(platform);
            }
            return bleManager;
        }

        private Object bleCallbackProxy() {
            if (bleCallbackProxy == null) {
                bleCallbackProxy = Proxy.newProxyInstance(
                        bleCallbackClass.getClassLoader(),
                        new Class<?>[] {bleCallbackClass},
                        new NoOpBleCallbackHandler());
            }
            return bleCallbackProxy;
        }
    }

    private static final class AndroidGattConnection implements GattConnection {
        private final BluetoothGatt gatt;

        private AndroidGattConnection(BluetoothGatt gatt) {
            this.gatt = gatt;
        }

        @Override
        public BluetoothGatt gatt() {
            return gatt;
        }

        @Override
        public void close() {
            if (gatt != null) {
                gatt.disconnect();
                gatt.close();
            }
        }
    }

    private static final class ForwardingGattCallback extends BluetoothGattCallback {
        private static final int STATE_DISCOVER_SERVICE = 2;
        private static final int STATE_REQUEST_MTU = 3;

        private final BluetoothGattCallback wrappedCallback;
        private final CountDownLatch latch;
        private final AtomicReference<BluetoothGatt> connectedGatt;
        private final AtomicReference<IllegalStateException> error;
        private int state;

        private ForwardingGattCallback(
                BluetoothGattCallback wrappedCallback,
                CountDownLatch latch,
                AtomicReference<BluetoothGatt> connectedGatt,
                AtomicReference<IllegalStateException> error) {
            this.wrappedCallback = wrappedCallback == null ? new BluetoothGattCallback() { } : wrappedCallback;
            this.latch = latch;
            this.connectedGatt = connectedGatt;
            this.error = error;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            wrappedCallback.onConnectionStateChange(gatt, status, newState);
            ConnectedHomeIpDiagnostics.emit("Bluetooth GATT state changed: status " + status + ", newState " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                state = STATE_DISCOVER_SERVICE;
                if (gatt != null) {
                    ConnectedHomeIpDiagnostics.emit("Bluetooth GATT connected; discovering Matter services");
                    gatt.discoverServices();
                }
                return;
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                error.compareAndSet(null, new AndroidGattConnector.GattConnectionFailure(status, newState));
                latch.countDown();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            wrappedCallback.onServicesDiscovered(gatt, status);
            if (state != STATE_DISCOVER_SERVICE) {
                return;
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                ConnectedHomeIpDiagnostics.emit("Bluetooth GATT service discovery failed with status " + status);
                error.compareAndSet(null, new IllegalStateException("Bluetooth GATT service discovery failed with status " + status));
                latch.countDown();
                return;
            }
            state = STATE_REQUEST_MTU;
            if (gatt != null) {
                ConnectedHomeIpDiagnostics.emit("Bluetooth GATT services discovered; requesting MTU " + CHIP_MTU);
                gatt.requestMtu(CHIP_MTU);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            wrappedCallback.onMtuChanged(gatt, mtu, status);
            if (state != STATE_REQUEST_MTU) {
                return;
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                ConnectedHomeIpDiagnostics.emit("Bluetooth GATT MTU request failed with status " + status);
                error.compareAndSet(null, new IllegalStateException("Bluetooth GATT MTU request failed with status " + status));
            } else {
                ConnectedHomeIpDiagnostics.emit("Bluetooth GATT MTU ready: " + mtu);
                connectedGatt.set(gatt);
            }
            latch.countDown();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            wrappedCallback.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            wrappedCallback.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            wrappedCallback.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            wrappedCallback.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            wrappedCallback.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            wrappedCallback.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            wrappedCallback.onReliableWriteCompleted(gatt, status);
        }
    }

    private static final class NoOpBleCallbackHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return null;
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    private static void logAndEmit(String message) {
        Log.i(TAG, message);
        ConnectedHomeIpDiagnostics.emit(message);
    }
}
