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
        ChipMatterController nativeController = new ChipMatterController(name -> { },
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
}
