package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class NativeChipResultParserTest {
    @Test
    public void parsesCommissioningResultWithUpdatedControllerState() {
        NativeCommissioningResult result = NativeChipResultParser.parseCommissioningResult(
                "nodeId=1234;controllerState=fabric-v2");

        assertEquals(1234L, result.nodeId());
        assertEquals("fabric-v2", result.controllerState());
    }

    @Test
    public void rejectsCommissioningResultWithoutNodeId() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NativeChipResultParser.parseCommissioningResult("controllerState=fabric-v2"));

        assertEquals("Native CHIP commissioning result is missing nodeId", exception.getMessage());
    }

    @Test
    public void parsesOpenCommissioningWindowResultWithUpdatedControllerState() {
        NativeOpenCommissioningWindowResult result = NativeChipResultParser.parseOpenCommissioningWindowResult(
                "temporaryCode=3497-0112-332;controllerState=fabric-v3");

        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("fabric-v3", result.controllerState());
    }

    @Test
    public void rejectsOpenCommissioningWindowResultWithoutTemporaryCode() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NativeChipResultParser.parseOpenCommissioningWindowResult("controllerState=fabric-v3"));

        assertEquals("Native CHIP OpenCommissioningWindow result is missing temporaryCode", exception.getMessage());
    }
}
