package org.openhab.matter.companion.controller;

import org.junit.Test;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class MatterControllerSelectorTest {
    @Test
    public void keepsFallbackControllerWhenNativeIsNotRequested() {
        MatterController fallback = new FakeMatterController();
        ChipMatterController nativeController = new ChipMatterController(name -> { },
                ChipMatterControllerConfig.defaultConfig());

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                false);

        assertSame(fallback, selection.controller());
        assertFalse(selection.nativeSelected());
        assertTrue(selection.message().contains("Using simulated Matter controller"));
    }

    @Test
    public void selectsNativeControllerWhenReadyAndRequested() {
        MatterController fallback = new FakeMatterController();
        ChipMatterController nativeController = new ChipMatterController(productionBridge(),
                new ChipMatterControllerConfig("custom_chip", true));

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                true);

        assertSame(nativeController, selection.controller());
        assertTrue(selection.nativeSelected());
        assertTrue(selection.message().contains("Using native Matter controller: custom_chip"));
    }

    @Test
    public void selectsGenericReadyControllerCandidateWhenRequested() {
        MatterController fallback = new FakeMatterController();
        MatterControllerCandidate nativeController = new ReadyCandidate("connectedhomeip-java");

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                true);

        assertSame(nativeController, selection.controller());
        assertTrue(selection.nativeSelected());
        assertTrue(selection.message().contains("Using native Matter controller: connectedhomeip-java"));
    }

    @Test
    public void fallsBackWhenRequestedNativeControllerIsNotReady() {
        MatterController fallback = new FakeMatterController();
        ChipMatterController nativeController = new ChipMatterController(name -> {
            throw new UnsatisfiedLinkError("missing " + name);
        }, ChipMatterControllerConfig.defaultConfig());

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                true);

        assertSame(fallback, selection.controller());
        assertFalse(selection.nativeSelected());
        assertTrue(selection.message().contains("Native Matter controller not ready"));
        assertTrue(selection.message().contains("Continuing with simulated Matter controller"));
    }

    @Test
    public void fallsBackWhenNativeBridgeIsOnlyStub() {
        MatterController fallback = new FakeMatterController();
        ChipMatterController nativeController = new ChipMatterController(stubBridge(),
                ChipMatterControllerConfig.defaultConfig());

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                true);

        assertSame(fallback, selection.controller());
        assertFalse(selection.nativeSelected());
        assertTrue(selection.message().contains("Native Matter controller not ready"));
        assertTrue(selection.message().contains("stub"));
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

    private static NativeChipBridge stubBridge() {
        return new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
            }

            @Override
            public String metadata() {
                return "kind=stub;version=0.1.0;production=false;message=packaging stub";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                throw new AssertionError("selector must not call commissioning");
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                throw new AssertionError("selector must not call OCW");
            }
        };
    }

    private static final class ReadyCandidate implements MatterControllerCandidate {
        private final String libraryName;

        private ReadyCandidate(String libraryName) {
            this.libraryName = libraryName;
        }

        @Override
        public ChipMatterControllerStatus readiness() {
            return new ChipMatterControllerStatus(
                    true,
                    libraryName,
                    false,
                    "connectedhomeip-java",
                    true,
                    "ready");
        }

        @Override
        public MatterCommissioningResult commissionBleThread(
                ThreadDataset dataset,
                MatterSetupPayload payload,
                String controllerState,
                ProgressListener listener) {
            return new MatterCommissioningResult(1L, controllerState);
        }

        @Override
        public MatterOpenCommissioningWindowResult openCommissioningWindow(
                long nodeId,
                int timeoutSeconds,
                int discriminator,
                String controllerState,
                ProgressListener listener) {
            return new MatterOpenCommissioningWindowResult("3497-0112-332", controllerState);
        }
    }
}
