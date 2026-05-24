package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        ChipMatterController controller = new ChipMatterController(name -> { }, new ChipMatterControllerConfig(
                "custom_chip",
                true));

        ChipMatterControllerStatus status = controller.readiness();

        assertTrue(status.ready());
        assertEquals("custom_chip", status.libraryName());
        assertTrue(status.attestationBypassEnabled());
    }
}
