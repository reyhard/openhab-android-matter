package org.openhab.matter.companion.openhab;

public interface OpenHabClient {
    OpenHabStatus checkReadiness(String baseUrl) throws Exception;
}
