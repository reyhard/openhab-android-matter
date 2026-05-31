package org.openhab.matter.companion.openhab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpOpenHabInboxClient implements OpenHabInboxClient {
    private static final int TIMEOUT_MILLIS = 3000;
    private static final int MAX_RESPONSE_BYTES = 16 * 1024;
    private static final Pattern THING_UID_PATTERN = Pattern.compile(
            "\"thingUID\"\\s*:\\s*\"([^\"]+)\"");

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
                String responseText = readBoundedResponse(connection.getInputStream());
                Set<String> matterEntryIds = extractMatterEntryIds(responseText);
                boolean detected = !matterEntryIds.isEmpty() || containsMatterEntry(responseText);
                String message = detected ? "Matter Inbox entry detected" : "No Matter Inbox entry detected";
                return new OpenHabInboxStatus(true, detected, message, "HTTP " + responseCode + " from " + url,
                        matterEntryIds);
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

    private static boolean containsMatterEntry(String responseText) {
        return responseText.contains("matter:") || responseText.contains("\"bindingId\":\"matter\"");
    }

    private static Set<String> extractMatterEntryIds(String responseText) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = THING_UID_PATTERN.matcher(responseText);
        while (matcher.find()) {
            String thingUid = matcher.group(1);
            if (thingUid != null && thingUid.startsWith("matter:")) {
                ids.add(thingUid);
            }
        }
        if (ids.isEmpty() && containsMatterEntry(responseText)) {
            ids.add("response:" + Integer.toHexString(responseText.hashCode()));
        }
        return ids;
    }

    private static String readBoundedResponse(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
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

    private static OpenHabInboxStatus unreachable(String message, String details) {
        return new OpenHabInboxStatus(false, false, sanitize(message), sanitize(details));
    }

    private static String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "No details available";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
