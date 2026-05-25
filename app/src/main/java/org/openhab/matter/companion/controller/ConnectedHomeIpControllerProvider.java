package org.openhab.matter.companion.controller;

public interface ConnectedHomeIpControllerProvider {
    Object controller() throws Exception;
}
