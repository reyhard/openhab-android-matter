package org.openhab.matter.companion.openhab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public final class HttpOpenHabInboxClient implements OpenHabInboxClient {
    private static final int TIMEOUT_MILLIS = 3000;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;

    @Override
    public OpenHabInboxStatus checkInbox(String baseUrl) {
        return checkInbox(baseUrl, "");
    }

    @Override
    public OpenHabInboxStatus checkInbox(String baseUrl, String apiToken) {
        HttpURLConnection connection = null;
        try {
            URL url = inboxUrl(baseUrl);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return unreachable("openHAB Inbox URL is invalid", "Unsupported protocol: " + url.getProtocol());
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            applyBearerToken(connection, apiToken);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                ReadResult response = readBoundedResponse(connection.getInputStream());
                if (response.truncated) {
                    String message = "openHAB Inbox response exceeded the "
                            + MAX_RESPONSE_BYTES + "-byte limit";
                    String details = "HTTP " + responseCode + " from " + url
                            + "; response exceeded the " + MAX_RESPONSE_BYTES
                            + "-byte limit and was not checked";
                    return new OpenHabInboxStatus(
                            true,
                            false,
                            message,
                            details,
                            Collections.emptyList(),
                            true);
                }
                OpenHabInboxJsonScanner.ScanResult scan = OpenHabInboxJsonScanner.scan(response.text);
                List<OpenHabInboxEntry> entries = scan.entries();
                boolean detected = containsMatterEntry(entries);
                String message = detected ? "Matter Inbox entry detected" : "No Matter Inbox entry detected";
                String details = "HTTP " + responseCode + " from " + url;
                if (scan.truncated()) {
                    details += "; openHAB Inbox response was truncated or incomplete, so only complete entries were considered";
                }
                return new OpenHabInboxStatus(
                        true,
                        detected,
                        message,
                        details,
                        entries,
                        scan.truncated());
            }
            return unreachable("openHAB Inbox is not reachable", "HTTP " + responseCode + " from " + url);
        } catch (MalformedURLException e) {
            return unreachable("openHAB Inbox URL is invalid", e.getMessage());
        } catch (IOException e) {
            return unreachable("openHAB Inbox is not reachable", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void applyBearerToken(HttpURLConnection connection, String apiToken) {
        if (apiToken != null && !apiToken.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiToken.trim());
        }
    }

    private static URL inboxUrl(String baseUrl) throws MalformedURLException {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return new URL(normalizedBaseUrl + "/rest/inbox");
    }

    private static boolean containsMatterEntry(List<OpenHabInboxEntry> entries) {
        for (OpenHabInboxEntry entry : entries) {
            if (entry.matterEntry()) {
                return true;
            }
        }
        return false;
    }

    private static ReadResult readBoundedResponse(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            boolean truncated = false;
            while (true) {
                int remaining = MAX_RESPONSE_BYTES + 1 - totalBytes;
                if (remaining <= 0) {
                    truncated = true;
                    break;
                }
                int readBytes = stream.read(buffer, 0, Math.min(buffer.length, remaining));
                if (readBytes == -1) {
                    break;
                }
                output.write(buffer, 0, readBytes);
                totalBytes += readBytes;
                if (totalBytes > MAX_RESPONSE_BYTES) {
                    truncated = true;
                    break;
                }
            }
            byte[] bytes = output.toByteArray();
            int textLength = Math.min(bytes.length, MAX_RESPONSE_BYTES);
            return new ReadResult(new String(bytes, 0, textLength, StandardCharsets.UTF_8), truncated);
        } finally {
            stream.close();
        }
    }

    private static OpenHabInboxStatus unreachable(String message, String details) {
        return new OpenHabInboxStatus(false, false, sanitize(message), sanitize(details));
    }

    private static String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "No details available";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static final class ReadResult {
        private final String text;
        private final boolean truncated;

        ReadResult(String text, boolean truncated) {
            this.text = text;
            this.truncated = truncated;
        }
    }
}
