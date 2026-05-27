package org.openhab.matter.companion.controller;

import org.junit.Test;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import java.util.concurrent.atomic.AtomicInteger;

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
    public void constructorDoesNotCreateNativeCandidateUntilSelectionRequest() {
        AtomicInteger factoryCalls = new AtomicInteger();

        NativeChipControllerSession session = new NativeChipControllerSession(
                new FakeMatterController(),
                false,
                attestationBypassEnabled -> {
                    factoryCalls.incrementAndGet();
                    return new ReadyCandidate("connectedhomeip-java", attestationBypassEnabled);
                });

        assertEquals(0, factoryCalls.get());

        session.selectionRequest();

        assertEquals(1, factoryCalls.get());
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
    public void newerSelectionRequestInvalidatesEarlierPendingSelection() {
        MatterController fallback = new FakeMatterController();
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                productionFactory());
        NativeChipControllerSession.SelectionRequest staleRequest = session.selectionRequest();
        NativeChipControllerSession.SelectionRequest currentRequest = session.selectionRequest();
        MatterControllerSelection staleSelection = MatterControllerSelector.select(
                fallback,
                staleRequest.nativeController(),
                true);
        MatterControllerSelection currentSelection = MatterControllerSelector.select(
                fallback,
                currentRequest.nativeController(),
                true);

        assertFalse(session.applySelection(staleRequest, staleSelection));
        assertTrue(session.applySelection(currentRequest, currentSelection));
        assertTrue(session.nativeSelected());
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

    @Test
    public void selectionRequestCanUseGenericNativeCandidate() {
        MatterController fallback = new FakeMatterController();
        ReadyCandidate candidate = new ReadyCandidate("connectedhomeip-java", false);
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                attestationBypassEnabled -> candidate);

        NativeChipControllerSession.SelectionRequest request = session.selectionRequest();
        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                request.nativeController(),
                true);

        assertSame(candidate, request.nativeController());
        assertTrue(session.applySelection(request, selection));
        assertSame(candidate, session.controller());
        assertTrue(session.nativeSelected());
    }

    @Test
    public void selectNativeIfReadySelectsReadyCandidate() {
        MatterController fallback = new FakeMatterController();
        ReadyCandidate candidate = new ReadyCandidate("connectedhomeip-java", false);
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                attestationBypassEnabled -> candidate);

        MatterControllerSelection selection = session.selectNativeIfReady();

        assertTrue(selection.nativeSelected());
        assertTrue(session.nativeSelected());
        assertSame(candidate, session.controller());
    }

    @Test
    public void selectNativeIfReadyKeepsFallbackWhenCandidateIsNotReady() {
        MatterController fallback = new FakeMatterController();
        NativeChipControllerSession session = new NativeChipControllerSession(
                fallback,
                false,
                attestationBypassEnabled -> new NotReadyCandidate());

        MatterControllerSelection selection = session.selectNativeIfReady();

        assertFalse(selection.nativeSelected());
        assertFalse(session.nativeSelected());
        assertSame(fallback, session.controller());
        assertTrue(selection.message().contains("Native Matter controller not ready"));
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

    private static final class ReadyCandidate implements MatterControllerCandidate {
        private final String libraryName;
        private final boolean attestationBypassEnabled;

        private ReadyCandidate(String libraryName, boolean attestationBypassEnabled) {
            this.libraryName = libraryName;
            this.attestationBypassEnabled = attestationBypassEnabled;
        }

        @Override
        public ChipMatterControllerStatus readiness() {
            return new ChipMatterControllerStatus(
                    true,
                    libraryName,
                    attestationBypassEnabled,
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

    private static final class NotReadyCandidate implements MatterControllerCandidate {
        @Override
        public ChipMatterControllerStatus readiness() {
            return new ChipMatterControllerStatus(
                    false,
                    "connectedhomeip-java",
                    false,
                    "connectedhomeip-java",
                    true,
                    "test native controller unavailable");
        }

        @Override
        public MatterCommissioningResult commissionBleThread(
                ThreadDataset dataset,
                MatterSetupPayload payload,
                String controllerState,
                ProgressListener listener) {
            throw new AssertionError("not-ready controller must not be used");
        }

        @Override
        public MatterOpenCommissioningWindowResult openCommissioningWindow(
                long nodeId,
                int timeoutSeconds,
                int discriminator,
                String controllerState,
                ProgressListener listener) {
            throw new AssertionError("not-ready controller must not be used");
        }
    }
}
