package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpBleConnectionProvider {
    ConnectedHomeIpBleConnection connect(int discriminator) throws Exception;
}
