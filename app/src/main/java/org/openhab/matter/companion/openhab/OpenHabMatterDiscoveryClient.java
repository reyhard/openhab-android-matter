package org.openhab.matter.companion.openhab;

public interface OpenHabMatterDiscoveryClient {
    default OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode) throws Exception {
        return startMatterScan(baseUrl, pairingCode, "");
    }

    OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode, String apiToken)
            throws Exception;
}
