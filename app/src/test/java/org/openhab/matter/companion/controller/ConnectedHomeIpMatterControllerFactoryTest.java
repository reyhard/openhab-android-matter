package org.openhab.matter.companion.controller;

import android.content.Context;

import org.junit.Test;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpMatterControllerFactoryTest {
    @Test
    public void missingArtifactsReturnUnavailableCandidateWithoutBuildingGateway() {
        CapturingGatewayFactory gatewayFactory = new CapturingGatewayFactory();
        ConnectedHomeIpMatterControllerFactory factory = new ConnectedHomeIpMatterControllerFactory(
                null,
                missingArtifacts(),
                gatewayFactory);

        MatterControllerCandidate candidate = factory.create(true);
        ChipMatterControllerStatus status = candidate.readiness();

        assertFalse(status.ready());
        assertEquals("CHIPController", status.libraryName());
        assertTrue(status.attestationBypassEnabled());
        assertTrue(status.message().contains("Missing connectedhomeip controller class"));
        assertEquals(0, gatewayFactory.calls);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> candidate.commissionBleThread(
                        ThreadDataset.parse("hex:0e080000000000010000"),
                        new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                        "controller-state",
                        ignored -> { }));
        assertTrue(exception.getMessage().contains("Missing connectedhomeip controller class"));
    }

    @Test
    public void readyArtifactsReturnConnectedHomeIpControllerCandidate() {
        CapturingGatewayFactory gatewayFactory = new CapturingGatewayFactory();
        ConnectedHomeIpMatterControllerFactory factory = new ConnectedHomeIpMatterControllerFactory(
                null,
                readyArtifacts(),
                gatewayFactory);

        MatterControllerCandidate candidate = factory.create(false);
        ChipMatterControllerStatus status = candidate.readiness();

        assertTrue(candidate instanceof ConnectedHomeIpMatterController);
        assertTrue(status.ready());
        assertEquals("CHIPController", status.libraryName());
        assertEquals(1, gatewayFactory.calls);
    }

    @Test
    public void gatewayConstructionFailureReturnsUnavailableCandidate() {
        ConnectedHomeIpMatterControllerFactory factory = new ConnectedHomeIpMatterControllerFactory(
                null,
                readyArtifacts(),
                context -> {
                    throw new IllegalStateException("boom");
                });

        MatterControllerCandidate candidate = factory.create(false);
        ChipMatterControllerStatus status = candidate.readiness();

        assertFalse(status.ready());
        assertTrue(status.message().contains("connectedhomeip Android controller initialization failed"));
        assertTrue(status.message().contains("boom"));
    }

    @Test
    public void gatewayConstructionLinkageFailureReturnsUnavailableCandidate() {
        ConnectedHomeIpMatterControllerFactory factory = new ConnectedHomeIpMatterControllerFactory(
                null,
                readyArtifacts(),
                context -> {
                    throw new NoClassDefFoundError("missing reflected dependency");
                });

        MatterControllerCandidate candidate = factory.create(false);
        ChipMatterControllerStatus status = candidate.readiness();

        assertFalse(status.ready());
        assertTrue(status.message().contains("connectedhomeip Android controller initialization failed"));
        assertTrue(status.message().contains("missing reflected dependency"));
    }

    @Test
    public void cachedDefaultGatewayReusesSameGatewayInstance() throws Exception {
        ConnectedHomeIpMatterControllerFactory.resetCachedDefaultGatewayForTesting();
        CapturingGatewayFactory gatewayFactory = new CapturingGatewayFactory();

        ConnectedHomeIpControllerGateway first = ConnectedHomeIpMatterControllerFactory.cachedDefaultGateway(
                null,
                gatewayFactory);
        ConnectedHomeIpControllerGateway second = ConnectedHomeIpMatterControllerFactory.cachedDefaultGateway(
                null,
                gatewayFactory);

        assertSame(first, second);
        assertEquals(1, gatewayFactory.calls);
    }

    private static ConnectedHomeIpControllerArtifacts readyArtifacts() {
        return new ConnectedHomeIpControllerArtifacts(name -> true, name -> { });
    }

    private static ConnectedHomeIpControllerArtifacts missingArtifacts() {
        return new ConnectedHomeIpControllerArtifacts(
                name -> !"chip.devicecontroller.ChipDeviceController".equals(name),
                name -> { });
    }

    private static final class CapturingGatewayFactory implements ConnectedHomeIpMatterControllerFactory.GatewayFactory {
        private int calls;

        @Override
        public ConnectedHomeIpControllerGateway create(Context context) {
            calls++;
            return new ConnectedHomeIpControllerGateway() {
                @Override
                public MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) {
                    return new MatterCommissioningResult(987654321L, request.controllerState());
                }

                @Override
                public MatterOpenCommissioningWindowResult openCommissioningWindow(
                        ConnectedHomeIpOpenCommissioningWindowRequest request) {
                    return new MatterOpenCommissioningWindowResult("3497-0112-332", request.controllerState());
                }

                @Override
                public ConnectedHomeIpFabricRestoreStatus checkFabricRestore(long bootstrapNodeId) {
                    return new ConnectedHomeIpFabricRestoreStatus(true, true, bootstrapNodeId, "restore-ok");
                }

                @Override
                public ConnectedHomeIpRuntimePreflightStatus checkRuntimePreflight() {
                    return new ConnectedHomeIpRuntimePreflightStatus(true, "runtime-ok");
                }
            };
        }
    }
}
