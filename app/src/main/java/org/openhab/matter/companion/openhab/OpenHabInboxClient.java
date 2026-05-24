package org.openhab.matter.companion.openhab;

public interface OpenHabInboxClient {
    OpenHabInboxStatus checkInbox(String baseUrl) throws Exception;
}
