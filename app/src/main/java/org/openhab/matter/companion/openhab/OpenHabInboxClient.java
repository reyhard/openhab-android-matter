package org.openhab.matter.companion.openhab;

public interface OpenHabInboxClient {
    default OpenHabInboxStatus checkInbox(String baseUrl) throws Exception {
        return checkInbox(baseUrl, "");
    }

    OpenHabInboxStatus checkInbox(String baseUrl, String apiToken) throws Exception;
}
