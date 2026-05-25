package org.openhab.matter.companion.controller;

import android.bluetooth.BluetoothGatt;

public class ConnectedHomeIpBleConnection implements AutoCloseable {
    private final BluetoothGatt gatt;
    private final int connectionId;
    private final AutoCloseable closeAction;

    public ConnectedHomeIpBleConnection(BluetoothGatt gatt, int connectionId, AutoCloseable closeAction) {
        this.gatt = gatt;
        this.connectionId = connectionId;
        this.closeAction = requireCloseAction(closeAction);
    }

    public BluetoothGatt gatt() {
        return gatt;
    }

    public int connectionId() {
        return connectionId;
    }

    @Override
    public void close() throws Exception {
        closeAction.close();
    }

    private static AutoCloseable requireCloseAction(AutoCloseable closeAction) {
        if (closeAction == null) {
            throw new IllegalArgumentException("closeAction is required");
        }
        return closeAction;
    }
}
