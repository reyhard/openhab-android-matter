package org.openhab.matter.companion.openhab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

public final class OpenHabInboxSseClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 15000;

    public void observe(String baseUrl, OpenHabInboxEventListener listener) throws IOException {
        observe(baseUrl, listener, () -> true);
    }

    public void observe(String baseUrl, OpenHabInboxEventListener listener, BooleanSupplier shouldContinue) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = eventsUrl(baseUrl);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                throw new IOException("Unsupported protocol: " + url.getProtocol());
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/event-stream");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP " + responseCode + " from " + url);
            }

            readEvents(connection, listener, shouldContinue);
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

    private static void readEvents(HttpURLConnection connection, OpenHabInboxEventListener listener,
            BooleanSupplier shouldContinue) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder block = new StringBuilder();
            String line;
            while (shouldContinue.getAsBoolean() && (line = readLine(reader, shouldContinue)) != null) {
                if (line.isEmpty()) {
                    if (block.length() > 0 && !dispatchEvent(block.toString(), listener)) {
                        return;
                    }
                    block.setLength(0);
                } else {
                    block.append(line).append('\n');
                }
            }
            if (shouldContinue.getAsBoolean() && block.length() > 0) {
                dispatchEvent(block.toString(), listener);
            }
        }
    }

    private static boolean dispatchEvent(String block, OpenHabInboxEventListener listener) {
        OpenHabInboxEvent event = OpenHabInboxSseParser.parse(block);
        if (event.topic().isEmpty()) {
            return true;
        }
        return listener.onEvent(event);
    }

    private static String readLine(BufferedReader reader, BooleanSupplier shouldContinue) throws IOException {
        while (shouldContinue.getAsBoolean()) {
            try {
                return reader.readLine();
            } catch (SocketTimeoutException ignored) {
                // SSE streams are long-lived; delayed keep-alives should not stop observation.
            }
        }
        return null;
    }
}
