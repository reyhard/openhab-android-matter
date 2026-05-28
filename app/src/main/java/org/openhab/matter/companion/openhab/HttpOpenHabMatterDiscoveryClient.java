package org.openhab.matter.companion.openhab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class HttpOpenHabMatterDiscoveryClient implements OpenHabMatterDiscoveryClient {
    private static final int TIMEOUT_MILLIS = 3000;
    private static final int MAX_RESPONSE_BYTES = 1024;
    private static final int DEFAULT_SCAN_TIMEOUT_SECONDS = 120;

    @Override
    public OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode, String apiToken) {
        HttpURLConnection connection = null;
        try {
            String safePairingCode = pairingCode == null ? "" : pairingCode.trim();
            if (safePairingCode.isEmpty()) {
                return failed(false, "openHAB Matter scan code is missing", "No pairing code was available.");
            }

            URL url = scanUrl(baseUrl, safePairingCode);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return failed(false, "openHAB Matter scan URL is invalid",
                        "Unsupported protocol: " + url.getProtocol());
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod("POST");
            applyBearerToken(connection, apiToken);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                int timeoutSeconds = parseTimeoutSeconds(readBoundedResponse(connection.getInputStream()));
                return new OpenHabMatterDiscoveryScanStatus(true, true, "openHAB Matter scan started",
                        "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl), timeoutSeconds);
            }
            if (responseCode == 401 || responseCode == 403) {
                return failed(true, "openHAB Matter scan authentication failed",
                        "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl));
            }
            if (responseCode == 404) {
                return failed(true, "openHAB Matter discovery service was not found",
                        "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl));
            }
            return failed(true, "openHAB Matter scan could not be started",
                    "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl));
        } catch (MalformedURLException e) {
            return failed(false, "openHAB Matter scan URL is invalid", "Malformed openHAB Matter scan URL");
        } catch (IOException e) {
            return failed(false, "openHAB Matter scan is not reachable", sanitize(e.getMessage()));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static URL scanUrl(String baseUrl, String pairingCode) throws IOException {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        String encodedCode = URLEncoder.encode(pairingCode, StandardCharsets.UTF_8.name());
        return new URL(normalizedBaseUrl + "/rest/discovery/bindings/matter/scan?input=" + encodedCode);
    }

    private static void applyBearerToken(HttpURLConnection connection, String apiToken) {
        if (apiToken != null && !apiToken.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiToken.trim());
        }
    }

    private static int parseTimeoutSeconds(String responseText) {
        try {
            return Math.max(1, Integer.parseInt(responseText.trim()));
        } catch (RuntimeException ignored) {
            return DEFAULT_SCAN_TIMEOUT_SECONDS;
        }
    }

    private static String readBoundedResponse(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int totalBytes = 0;
            int readBytes;
            while (totalBytes < MAX_RESPONSE_BYTES
                    && (readBytes = stream.read(buffer, 0, Math.min(buffer.length, MAX_RESPONSE_BYTES - totalBytes))) != -1) {
                output.write(buffer, 0, readBytes);
                totalBytes += readBytes;
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } finally {
            stream.close();
        }
    }

    private static OpenHabMatterDiscoveryScanStatus failed(boolean reachable, String message, String details) {
        return new OpenHabMatterDiscoveryScanStatus(reachable, false, message, sanitize(details), 0);
    }

    private static String safeEndpointForDetails(String baseUrl) {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        try {
            URL url = new URL(normalizedBaseUrl);
            String port = url.getPort() >= 0 ? ":" + url.getPort() : "";
            return url.getProtocol() + "://" + url.getHost() + port + "/rest/discovery/bindings/matter/scan";
        } catch (MalformedURLException e) {
            return "openHAB Matter discovery scan endpoint";
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "No details available";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
