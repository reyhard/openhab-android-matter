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
        ChipMatterController controller = new ChipMatterController(name -> { }, new ChipMatterControllerConfig(
                "custom_chip",
                true));

        ChipMatterControllerStatus status = controller.readiness();

        assertTrue(status.ready());
        assertEquals("custom_chip", status.libraryName());
        assertTrue(status.attestationBypassEnabled());
    }

    @Test
    public void missingNativeEntryPointIsReportedAsIllegalState() {
        ChipMatterController controller = new ChipMatterController(name -> { }, ChipMatterControllerConfig.defaultConfig());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> controller.commissionBleThread(
                ThreadDataset.parse("0E080000000000010000"),
                new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                ignored -> { }));

        assertTrue(exception.getMessage().contains("Native CHIP controller entry point is missing"));
    }

    @Test
    public void readinessLoadsNativeLibraryOnlyOnce() {
        int[] loadCount = new int[] {0};
        ChipMatterController controller = new ChipMatterController(name -> loadCount[0]++,
                ChipMatterControllerConfig.defaultConfig());

        controller.readiness();
        controller.readiness();

        assertEquals(1, loadCount[0]);
    }
}
