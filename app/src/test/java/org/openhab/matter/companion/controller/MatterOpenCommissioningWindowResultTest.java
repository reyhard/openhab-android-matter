package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MatterOpenCommissioningWindowResultTest {
    @Test
    public void exposesManualCodeQrCodeAndControllerState() {
        MatterOpenCommissioningWindowResult result = new MatterOpenCommissioningWindowResult(
                "3497-0112-332",
                "MT:Y.K9042C00KA0648G00",
                "updated-controller-state");

        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("3497-0112-332", result.manualCode());
        assertEquals("MT:Y.K9042C00KA0648G00", result.qrCode());
        assertEquals(true, result.hasQrCode());
        assertEquals("updated-controller-state", result.controllerState());
    }

    @Test
    public void acceptsQrCodeWhenManualCodeIsMissing() {
        MatterOpenCommissioningWindowResult result = new MatterOpenCommissioningWindowResult(
                "",
                "MT:Y.K9042C00KA0648G00",
                "updated-controller-state");

        assertEquals("", result.temporaryCode());
        assertEquals("", result.manualCode());
        assertEquals("MT:Y.K9042C00KA0648G00", result.qrCode());
        assertEquals(true, result.hasQrCode());
    }

    @Test
    public void rejectsBlankManualAndQrCodes() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatterOpenCommissioningWindowResult("", "", "updated-controller-state"));
    }
}
