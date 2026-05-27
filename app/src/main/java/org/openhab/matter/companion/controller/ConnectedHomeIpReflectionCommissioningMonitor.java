package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpReflectionCommissioningMonitor implements ConnectedHomeIpCommissioningMonitor {
    private static final long DEFAULT_ICD_STAY_ACTIVE_DURATION_MILLIS = 30_000L;

    private final ConnectedHomeIpReflectionCommandFactory commandFactory;
    private final long timeoutMillis;
    private ConnectedHomeIpCommissioningCompletionListener listener;

    public ConnectedHomeIpReflectionCommissioningMonitor(ConnectedHomeIpReflectionCommandFactory commandFactory) {
        this(commandFactory, ConnectedHomeIpCommissioningCompletionListener.DEFAULT_TIMEOUT_MILLIS);
    }

    public ConnectedHomeIpReflectionCommissioningMonitor(
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            long timeoutMillis) {
        this.commandFactory = require(commandFactory, "commandFactory");
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void prepare(Object controller) throws Exception {
        listener = commandFactory.newCommissioningCompletionListener(
                timeoutMillis,
                () -> {
                    Object icdRegistrationInfo =
                            commandFactory.newIcdRegistrationInfoForStayActive(DEFAULT_ICD_STAY_ACTIVE_DURATION_MILLIS);
                    commandFactory.invokeUpdateCommissioningIcdRegistrationInfo(controller, icdRegistrationInfo);
                });
        commandFactory.invokeSetCompletionListener(controller, listener.proxy());
    }

    @Override
    public MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) throws Exception {
        if (listener == null) {
            throw new IllegalStateException("Commissioning listener has not been prepared");
        }
        return listener.awaitCommissioned(nodeId, controllerState);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
