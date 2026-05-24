package org.openhab.matter.companion.otbr;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public final class HttpOtbrClient implements OtbrClient {
    private static final int TIMEOUT_MILLIS = 3000;

    @Override
    public OtbrStatus checkReadiness(String baseUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = normalizedUrl(baseUrl);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return new OtbrStatus(false, "OTBR base URL is invalid", "Unsupported protocol: " + url.getProtocol());
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                return new OtbrStatus(true, "OTBR endpoint is reachable", "HTTP " + responseCode + " from " + url);
            }
            return new OtbrStatus(false, "OTBR endpoint is not reachable", "HTTP " + responseCode + " from " + url);
        } catch (MalformedURLException e) {
            return new OtbrStatus(false, "OTBR base URL is invalid", e.getMessage());
        } catch (IOException e) {
            return new OtbrStatus(false, "OTBR endpoint is not reachable", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static URL normalizedUrl(String baseUrl) throws MalformedURLException {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return new URL(normalizedBaseUrl);
    }
}
