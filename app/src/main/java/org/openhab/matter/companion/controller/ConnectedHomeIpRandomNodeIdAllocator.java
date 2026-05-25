package org.openhab.matter.companion.controller;

import java.security.SecureRandom;

public final class ConnectedHomeIpRandomNodeIdAllocator implements ConnectedHomeIpNodeIdAllocator {
    private final SecureRandom random;

    public ConnectedHomeIpRandomNodeIdAllocator() {
        this(new SecureRandom());
    }

    ConnectedHomeIpRandomNodeIdAllocator(SecureRandom random) {
        this.random = random == null ? new SecureRandom() : random;
    }

    @Override
    public long nextNodeId() {
        long nodeId;
        do {
            nodeId = random.nextLong() & Long.MAX_VALUE;
        } while (nodeId == 0L);
        return nodeId;
    }
}
