package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpFabricRestoreChecker {
    ConnectedHomeIpFabricRestoreStatus checkFabricRestore(long bootstrapNodeId) throws Exception;
}
