package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpNodeIdAllocator {
    long nextNodeId() throws Exception;
}
