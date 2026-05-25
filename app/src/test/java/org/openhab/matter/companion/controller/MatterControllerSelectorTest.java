package org.openhab.matter.companion.controller;

import org.junit.Test;

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
        assertTrue(selection.message().contains("Using native CHIP controller: custom_chip"));
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
        assertTrue(selection.message().contains("Native CHIP controller not ready"));
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
        assertTrue(selection.message().contains("Native CHIP controller not ready"));
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
}
