package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NativeChipControllerSessionTest {
    @Test
    public void unchangedAttestationBypassPreservesNativeSelection() {
        MatterController fallback = new FakeMatterController();
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                productionFactory());
        NativeChipControllerSession.SelectionRequest request = session.selectionRequest();
        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                request.nativeController(),
                true);
        assertTrue(session.applySelection(request, selection));

        assertFalse(session.syncAttestationBypass(false));

        assertTrue(session.nativeSelected());
        assertSame(selection.controller(), session.controller());
    }

    @Test
    public void changedAttestationBypassInvalidatesNativeSelection() {
        MatterController fallback = new FakeMatterController();
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                productionFactory());
        NativeChipControllerSession.SelectionRequest request = session.selectionRequest();
        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                request.nativeController(),
                true);
        assertTrue(session.applySelection(request, selection));

        assertTrue(session.syncAttestationBypass(true));

        assertFalse(session.nativeSelected());
        assertSame(fallback, session.controller());
        assertEquals(true, session.attestationBypassEnabled());
    }

    @Test
    public void staleSelectionResultIsRejectedAfterAttestationBypassChanges() {
        MatterController fallback = new FakeMatterController();
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                productionFactory());
        NativeChipControllerSession.SelectionRequest staleRequest = session.selectionRequest();
        MatterControllerSelection staleSelection = MatterControllerSelector.select(
                fallback,
                staleRequest.nativeController(),
                true);

        assertTrue(session.syncAttestationBypass(true));

        assertFalse(session.applySelection(staleRequest, staleSelection));
        assertFalse(session.nativeSelected());
        assertSame(fallback, session.controller());
        assertEquals(true, session.attestationBypassEnabled());
    }

    @Test
    public void selectionRequestStopsBeingCurrentAfterAttestationBypassChanges() {
        MatterController fallback = new FakeMatterController();
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                productionFactory());
        NativeChipControllerSession.SelectionRequest staleRequest = session.selectionRequest();

        session.syncAttestationBypass(true);

        assertFalse(session.isCurrent(staleRequest));
        assertTrue(session.isCurrent(session.selectionRequest()));
    }

    @Test
    public void selectionRequestUsesCurrentAttestationBypass() {
        MatterController fallback = new FakeMatterController();
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                productionFactory());

        session.syncAttestationBypass(true);
        ChipMatterControllerStatus status = session.selectionRequest().nativeController().readiness();

        assertTrue(status.attestationBypassEnabled());
    }

    private static NativeChipControllerSession.NativeControllerFactory productionFactory() {
        return attestationBypassEnabled -> new ChipMatterController(
                productionBridge(),
                new ChipMatterControllerConfig("test_chip", attestationBypassEnabled));
    }

    private static NativeChipBridge productionBridge() {
        return new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
            }

            @Override
            public String metadata() {
                return "kind=connectedhomeip;version=2026.05;production=true";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                return new NativeCommissioningResult(1234L, request.controllerState());
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                return new NativeOpenCommissioningWindowResult("3497-0112-332", request.controllerState());
            }
        };
    }
}
