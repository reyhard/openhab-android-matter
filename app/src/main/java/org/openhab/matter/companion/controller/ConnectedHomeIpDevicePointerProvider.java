package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpDevicePointerProvider {
    ConnectedHomeIpDevicePointer acquire(Object controller, long nodeId) throws Exception;
}
