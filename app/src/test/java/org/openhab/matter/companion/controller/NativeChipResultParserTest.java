package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class NativeChipResultParserTest {
    @Test
    public void parsesCommissioningResultWithUpdatedControllerState() {
        NativeCommissioningResult result = NativeChipResultParser.parseCommissioningResult(
                "nodeId=1234;controllerStateBase64=ZmFicmljLXYy");

        assertEquals(1234L, result.nodeId());
        assertEquals("fabric-v2", result.controllerState());
    }

    @Test
    public void parsesCommissioningResultWithBase64EncodedOpaqueControllerState() {
        NativeCommissioningResult result = NativeChipResultParser.parseCommissioningResult(
                "nodeId=1234;controllerStateBase64=ZmFicmljO3YyCmxpbmUyPW9r");

        assertEquals(1234L, result.nodeId());
        assertEquals("fabric;v2\nline2=ok", result.controllerState());
    }

    @Test
    public void rejectsCommissioningResultWithoutNodeId() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NativeChipResultParser.parseCommissioningResult("controllerStateBase64=ZmFicmljLXYy"));

        assertEquals("Native CHIP commissioning result is missing nodeId", exception.getMessage());
    }

    @Test
    public void rejectsCommissioningResultWithoutEncodedControllerState() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NativeChipResultParser.parseCommissioningResult("nodeId=1234;controllerState=fabric-v2"));

        assertEquals("Native CHIP result is missing controllerStateBase64", exception.getMessage());
    }

    @Test
    public void rejectsCommissioningResultWithInvalidEncodedControllerState() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NativeChipResultParser.parseCommissioningResult("nodeId=1234;controllerStateBase64=not-valid-@@"));

        assertEquals("Native CHIP result has invalid controllerStateBase64", exception.getMessage());
    }

    @Test
    public void parsesOpenCommissioningWindowResultWithUpdatedControllerState() {
        NativeOpenCommissioningWindowResult result = NativeChipResultParser.parseOpenCommissioningWindowResult(
                "temporaryCode=3497-0112-332;controllerStateBase64=ZmFicmljLXYz");

        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("fabric-v3", result.controllerState());
    }

    @Test
    public void parsesOpenCommissioningWindowResultWithBase64EncodedOpaqueControllerState() {
        NativeOpenCommissioningWindowResult result = NativeChipResultParser.parseOpenCommissioningWindowResult(
                "temporaryCode=3497-0112-332;controllerStateBase64=ZmFicmljO3YzCmxpbmUyPW9r");

        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("fabric;v3\nline2=ok", result.controllerState());
    }

    @Test
    public void rejectsOpenCommissioningWindowResultWithoutTemporaryCode() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NativeChipResultParser.parseOpenCommissioningWindowResult("controllerStateBase64=ZmFicmljLXYz"));

        assertEquals("Native CHIP OpenCommissioningWindow result is missing temporaryCode", exception.getMessage());
    }

    @Test
    public void rejectsOpenCommissioningWindowResultWithoutEncodedControllerState() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NativeChipResultParser.parseOpenCommissioningWindowResult(
                        "temporaryCode=3497-0112-332;controllerState=fabric-v3"));

        assertEquals("Native CHIP result is missing controllerStateBase64", exception.getMessage());
    }
}
