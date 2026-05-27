package org.openhab.matter.companion.controller;

import android.content.Context;

import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ConnectedHomeIpMatterControllerFactory {
    private static final int ATTESTATION_FAIL_SAFE_EXPIRY_SECONDS = 120;
    private static final long DEVICE_POINTER_TIMEOUT_MILLIS = 300_000L;
    private static final Object DEFAULT_GATEWAY_LOCK = new Object();
    private static ConnectedHomeIpControllerGateway defaultGateway;

    private final Context context;
    private final ConnectedHomeIpControllerArtifacts artifacts;
    private final GatewayFactory gatewayFactory;

    public ConnectedHomeIpMatterControllerFactory(Context context) {
        this(context, new ConnectedHomeIpControllerArtifacts(), ConnectedHomeIpMatterControllerFactory::cachedDefaultGateway);
    }

    public ConnectedHomeIpMatterControllerFactory(
            Context context,
            ConnectedHomeIpControllerArtifacts artifacts,
            GatewayFactory gatewayFactory) {
        this.context = context;
        this.artifacts = artifacts == null ? new ConnectedHomeIpControllerArtifacts() : artifacts;
        this.gatewayFactory = gatewayFactory == null ? ConnectedHomeIpMatterControllerFactory::newDefaultGateway : gatewayFactory;
    }

    public MatterControllerCandidate create(boolean attestationBypassEnabled) {
        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();
        if (!status.ready()) {
            return new UnavailableCandidate(status, attestationBypassEnabled);
        }
        try {
            return new ConnectedHomeIpMatterController(
                    artifacts,
                    gatewayFactory.create(context),
                    attestationBypassEnabled);
        } catch (Exception | LinkageError ex) {
            return new UnavailableCandidate(
                    new ConnectedHomeIpControllerArtifactsStatus(
                            false,
                            status.libraryName(),
                            "connectedhomeip Android controller initialization failed: " + safeMessage(ex)),
                    attestationBypassEnabled);
        }
    }

    public interface GatewayFactory {
        ConnectedHomeIpControllerGateway create(Context context) throws Exception;
    }

    static ConnectedHomeIpControllerGateway cachedDefaultGateway(Context context, GatewayFactory gatewayFactory)
            throws Exception {
        synchronized (DEFAULT_GATEWAY_LOCK) {
            if (defaultGateway == null) {
                Context stableContext = applicationContext(context);
                defaultGateway = gatewayFactory.create(stableContext);
                ConnectedHomeIpDiagnostics.emit("Created shared connectedhomeip Android gateway");
            } else {
                ConnectedHomeIpDiagnostics.emit("Reusing shared connectedhomeip Android gateway");
            }
            return defaultGateway;
        }
    }

    static void resetCachedDefaultGatewayForTesting() {
        synchronized (DEFAULT_GATEWAY_LOCK) {
            defaultGateway = null;
        }
    }

    private static ConnectedHomeIpControllerGateway cachedDefaultGateway(Context context) throws Exception {
        return cachedDefaultGateway(context, ConnectedHomeIpMatterControllerFactory::newDefaultGateway);
    }

    private static ConnectedHomeIpControllerGateway newDefaultGateway(Context context) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        ConnectedHomeIpPlatformControllerProvider platformProvider = new ConnectedHomeIpPlatformControllerProvider(context);
        ConnectedHomeIpReflectionCommandFactory commandFactory = ConnectedHomeIpReflectionCommandFactory.fromDefaultClassLoader();
        return new ConnectedHomeIpReflectionGateway(
                platformProvider,
                new ConnectedHomeIpAndroidBleConnectionProvider(context, platformProvider),
                new ConnectedHomeIpRandomNodeIdAllocator(),
                new ConnectedHomeIpReflectionCommissioningMonitor(commandFactory),
                new ConnectedHomeIpReflectionAttestationHandler(commandFactory, ATTESTATION_FAIL_SAFE_EXPIRY_SECONDS),
                new ConnectedHomeIpReflectionDevicePointerProvider(commandFactory, DEVICE_POINTER_TIMEOUT_MILLIS),
                commandFactory);
    }

    private static Context applicationContext(Context context) {
        if (context == null) {
            return null;
        }
        Context applicationContext = context.getApplicationContext();
        return applicationContext == null ? context : applicationContext;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    private static final class UnavailableCandidate implements MatterControllerCandidate {
        private final ConnectedHomeIpControllerArtifactsStatus status;
        private final boolean attestationBypassEnabled;

        private UnavailableCandidate(
                ConnectedHomeIpControllerArtifactsStatus status,
                boolean attestationBypassEnabled) {
            this.status = status;
            this.attestationBypassEnabled = attestationBypassEnabled;
        }

        @Override
        public ChipMatterControllerStatus readiness() {
            return new ChipMatterControllerStatus(
                    false,
                    status.libraryName(),
                    attestationBypassEnabled,
                    "connectedhomeip-java",
                    false,
                    status.message());
        }

        @Override
        public MatterCommissioningResult commissionBleThread(
                ThreadDataset dataset,
                MatterSetupPayload payload,
                String controllerState,
                ProgressListener listener) {
            throw unavailable();
        }

        @Override
        public MatterOpenCommissioningWindowResult openCommissioningWindow(
                long nodeId,
                int timeoutSeconds,
                int discriminator,
                String controllerState,
                ProgressListener listener) {
            throw unavailable();
        }

        private IllegalStateException unavailable() {
            return new IllegalStateException(status.message());
        }
    }
}
