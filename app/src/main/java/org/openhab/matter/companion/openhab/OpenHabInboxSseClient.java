package org.openhab.matter.companion.openhab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class OpenHabInboxSseClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 15000;

    public void observe(String baseUrl, OpenHabInboxEventListener listener) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = eventsUrl(baseUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/event-stream");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP " + responseCode + " from " + url);
            }

            readEvents(connection, listener);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static URL eventsUrl(String baseUrl) throws MalformedURLException {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return new URL(normalizedBaseUrl + "/rest/events?topics=openhab/inbox/*");
    }

    private static void readEvents(HttpURLConnection connection, OpenHabInboxEventListener listener) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder block = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (block.length() > 0 && !listener.onEvent(OpenHabInboxSseParser.parse(block.toString()))) {
                        return;
                    }
                    block.setLength(0);
                } else {
                    block.append(line).append('\n');
                }
            }
            if (block.length() > 0) {
                listener.onEvent(OpenHabInboxSseParser.parse(block.toString()));
            }
        }
    }
}
