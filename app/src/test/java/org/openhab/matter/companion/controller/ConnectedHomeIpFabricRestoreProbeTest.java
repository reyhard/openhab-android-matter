package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpFabricRestoreProbeTest {
    @Test
    public void returnsSkippedWhenBootstrapNodeIdMissing() {
        ConnectedHomeIpFabricRestoreProbe probe = new ConnectedHomeIpFabricRestoreProbe(
                () -> {
                    throw new AssertionError("controller should not be requested");
                },
                (controller, nodeId) -> {
                    throw new AssertionError("pointer should not be requested");
                });

        ConnectedHomeIpFabricRestoreStatus status = probe.check(-1L);

        assertFalse(status.checked());
        assertFalse(status.ready());
        assertEquals(-1L, status.nodeId());
        assertEquals("No connectedhomeip bootstrap fabric has been commissioned yet.", status.message());
    }

    @Test
    public void returnsReadyWhenDevicePointerCanBeAcquiredAndReleased() {
        Object controller = new Object();
        CapturingPointerProvider pointerProvider = new CapturingPointerProvider(new CapturingPointer(1234L));
        ConnectedHomeIpFabricRestoreProbe probe = new ConnectedHomeIpFabricRestoreProbe(
                () -> controller,
                pointerProvider);

        ConnectedHomeIpFabricRestoreStatus status = probe.check(987654321L);

        assertTrue(status.checked());
        assertTrue(status.ready());
        assertEquals(987654321L, status.nodeId());
        assertEquals("connectedhomeip fabric restore ready for node 987654321.", status.message());
        assertSame(controller, pointerProvider.controller);
        assertEquals(987654321L, pointerProvider.nodeId);
        assertTrue(pointerProvider.pointer.closed);
    }

    @Test
    public void returnsNotReadyWhenAcquireThrows() {
        ConnectedHomeIpFabricRestoreProbe probe = new ConnectedHomeIpFabricRestoreProbe(
                Object::new,
                (controller, nodeId) -> {
                    throw new IllegalStateException("fabric missing");
                });

        ConnectedHomeIpFabricRestoreStatus status = probe.check(987654321L);

        assertTrue(status.checked());
        assertFalse(status.ready());
        assertEquals(987654321L, status.nodeId());
        assertEquals(
                "connectedhomeip fabric restore is not ready for node 987654321: fabric missing",
                status.message());
    }

    private static final class CapturingPointerProvider implements ConnectedHomeIpDevicePointerProvider {
        private final CapturingPointer pointer;
        private Object controller;
        private long nodeId;

        private CapturingPointerProvider(CapturingPointer pointer) {
            this.pointer = pointer;
        }

        @Override
        public ConnectedHomeIpDevicePointer acquire(Object controller, long nodeId) {
            this.controller = controller;
            this.nodeId = nodeId;
            return pointer;
        }
    }

    private static final class CapturingPointer extends ConnectedHomeIpDevicePointer {
        private boolean closed;

        private CapturingPointer(long value) {
            super(value, () -> { });
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
