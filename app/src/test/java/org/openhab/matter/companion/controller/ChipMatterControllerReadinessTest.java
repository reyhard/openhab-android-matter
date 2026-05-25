package org.openhab.matter.companion.controller;

import org.junit.Test;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public final class ChipMatterControllerReadinessTest {
    @Test
    public void reportsNativeLibraryMissingWithoutThrowingFromConstructor() {
        NativeLibraryLoader loader = name -> {
            throw new UnsatisfiedLinkError("missing " + name);
        };

        ChipMatterController controller = new ChipMatterController(loader, ChipMatterControllerConfig.defaultConfig());
        ChipMatterControllerStatus status = controller.readiness();

        assertFalse(status.ready());
        assertEquals("openhab_matter_chip", status.libraryName());
        assertTrue(status.message().contains("missing openhab_matter_chip"));
    }

    @Test
    public void reportsReadyWhenNativeLibraryLoads() {
        ChipMatterController controller = new ChipMatterController(productionBridge(), new ChipMatterControllerConfig(
                "custom_chip",
                true));

        ChipMatterControllerStatus status = controller.readiness();

        assertTrue(status.ready());
        assertEquals("custom_chip", status.libraryName());
        assertTrue(status.attestationBypassEnabled());
    }

    @Test
    public void reportsStubBridgeAsNotReadyEvenWhenLibraryLoads() {
        NativeChipBridge bridge = new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
            }

            @Override
            public String metadata() {
                return "kind=stub;version=0.1.0;production=false;message=JNI stub only";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                throw new AssertionError("stub commissioning must not be called");
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                throw new AssertionError("stub OCW must not be called");
            }
        };

        ChipMatterController controller = new ChipMatterController(bridge, ChipMatterControllerConfig.defaultConfig());
        ChipMatterControllerStatus status = controller.readiness();

        assertFalse(status.ready());
        assertEquals("stub", status.bridgeKind());
        assertFalse(status.productionReady());
        assertTrue(status.message().contains("JNI stub only"));
    }

    @Test
    public void reportsReadyOnlyForProductionConnectedhomeipBridge() {
        NativeChipBridge bridge = new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
            }

            @Override
            public String metadata() {
                return "kind=connectedhomeip;version=2026.05;production=true;message=connectedhomeip controller ready";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                return new NativeCommissioningResult(1234L, request.controllerState());
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                return new NativeOpenCommissioningWindowResult("MT:PRODUCTION", request.controllerState());
            }
        };

        ChipMatterController controller = new ChipMatterController(bridge, new ChipMatterControllerConfig(
                "custom_chip",
                true));

        ChipMatterControllerStatus status = controller.readiness();

        assertTrue(status.ready());
        assertEquals("connectedhomeip", status.bridgeKind());
        assertTrue(status.productionReady());
        assertEquals("custom_chip", status.libraryName());
        assertTrue(status.attestationBypassEnabled());
    }

    @Test
    public void missingNativeEntryPointIsReportedAsIllegalState() {
        ChipMatterController controller = new ChipMatterController(new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
            }

            @Override
            public String metadata() {
                return "kind=connectedhomeip;version=2026.05;production=true;message=connectedhomeip controller ready";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                throw new UnsatisfiedLinkError("missing nativeCommissionBleThread");
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                return new NativeOpenCommissioningWindowResult("MT:PRODUCTION", request.controllerState());
            }
        }, ChipMatterControllerConfig.defaultConfig());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> controller.commissionBleThread(
                ThreadDataset.parse("0E080000000000010000"),
                new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                "incoming-controller-state",
                ignored -> { }));

        assertTrue(exception.getMessage().contains("Native CHIP controller entry point is missing"));
    }

    @Test
    public void readinessLoadsNativeLibraryOnlyOnce() {
        int[] loadCount = new int[] {0};
        ChipMatterController controller = new ChipMatterController(new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
                loadCount[0]++;
            }

            @Override
            public String metadata() {
                return "kind=connectedhomeip;version=2026.05;production=true;message=connectedhomeip controller ready";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                return new NativeCommissioningResult(1234L, request.controllerState());
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                return new NativeOpenCommissioningWindowResult("MT:PRODUCTION", request.controllerState());
            }
        }, ChipMatterControllerConfig.defaultConfig());

        controller.readiness();
        controller.readiness();

        assertEquals(1, loadCount[0]);
    }

    @Test
    public void loaderBackedControllerRequiresNativeMetadataEntryPoint() {
        ChipMatterController controller = new ChipMatterController(name -> { }, ChipMatterControllerConfig.defaultConfig());

        ChipMatterControllerStatus status = controller.readiness();

        assertFalse(status.ready());
        assertFalse(status.productionReady());
        assertEquals("unknown", status.bridgeKind());
    }

    private static NativeChipBridge productionBridge() {
        return new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
            }

            @Override
            public String metadata() {
                return "kind=connectedhomeip;version=2026.05;production=true;message=connectedhomeip controller ready";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                return new NativeCommissioningResult(1234L, request.controllerState());
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                return new NativeOpenCommissioningWindowResult("MT:PRODUCTION", request.controllerState());
            }
        };
    }
}
