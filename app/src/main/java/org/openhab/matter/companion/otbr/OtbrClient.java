package org.openhab.matter.companion.otbr;

public interface OtbrClient {
    OtbrStatus checkReadiness(String baseUrl);
}
