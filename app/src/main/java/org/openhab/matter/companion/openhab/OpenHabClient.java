package org.openhab.matter.companion.openhab;

public interface OpenHabClient {
    default OpenHabStatus checkReadiness(String baseUrl) throws Exception {
        return checkReadiness(baseUrl, "");
    }

    OpenHabStatus checkReadiness(String baseUrl, String apiToken) throws Exception;
}
