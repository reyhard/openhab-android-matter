package org.openhab.matter.companion.openhab;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class HttpOpenHabClient implements OpenHabClient {
    private static final int TIMEOUT_MILLIS = 3000;
    private static final Pattern MATTER_CONTROLLER_PATTERN = Pattern.compile(
            "\"(?:UID|thingTypeUID)\"\\s*:\\s*\"matter:controller(?::[^\"]*)?\"");
    private static final Pattern ONLINE_STATUS_PATTERN = Pattern.compile(
            "\"status\"\\s*:\\s*\"ONLINE\"");
    private static final Pattern STATUS_INFO_PATTERN = Pattern.compile(
            "\"statusInfo\"\\s*:\\s*\\{([^{}]*)\\}");

    @Override
    public OpenHabStatus checkReadiness(String baseUrl) {
        try {
            URL restUrl = restUrl(baseUrl);
            if (!isHttpUrl(restUrl)) {
                return new OpenHabStatus(false, false, false, "openHAB base URL is invalid",
                        "Unsupported protocol: " + restUrl.getProtocol());
            }

            HttpResponse restResponse = get(restUrl);
            if (!restResponse.success()) {
                return new OpenHabStatus(false, false, false, "openHAB REST API is not reachable",
                        "HTTP " + restResponse.statusCode + " from " + restUrl);
            }

            URL thingsUrl = thingsUrl(baseUrl);
            HttpResponse thingsResponse;
            try {
                thingsResponse = get(thingsUrl);
            } catch (IOException e) {
                return new OpenHabStatus(false, true, false,
                        "openHAB Matter controller readiness could not be verified",
                        "HTTP " + restResponse.statusCode + " from " + restUrl
                                + "; failed to read " + thingsUrl + ": " + e.getMessage());
            }
            String details = "HTTP " + restResponse.statusCode + " from " + restUrl
                    + "; HTTP " + thingsResponse.statusCode + " from " + thingsUrl;
            if (!thingsResponse.success()) {
                return new OpenHabStatus(false, true, false,
                        "openHAB Matter controller readiness could not be verified", details);
            }

            MatterControllerState controllerState = matterControllerState(thingsResponse.body);
            if (controllerState == MatterControllerState.ONLINE) {
                return new OpenHabStatus(true, true, true, "openHAB Matter controller is ready",
                        details + "; Matter controller ONLINE");
            }
            if (controllerState == MatterControllerState.OFFLINE) {
                return new OpenHabStatus(false, true, false, "openHAB Matter controller is not online",
                        details + "; Matter controller is not ONLINE");
            }
            return new OpenHabStatus(false, true, false, "openHAB Matter controller was not found",
                    details + "; Matter controller thing was not found");
        } catch (MalformedURLException e) {
            return new OpenHabStatus(false, false, false, "openHAB base URL is invalid", e.getMessage());
        } catch (IOException e) {
            return new OpenHabStatus(false, false, false, "openHAB REST API is not reachable", e.getMessage());
        }
    }

    private static URL restUrl(String baseUrl) throws MalformedURLException {
        return endpointUrl(baseUrl, "/rest/");
    }

    private static URL thingsUrl(String baseUrl) throws MalformedURLException {
        return endpointUrl(baseUrl, "/rest/things");
    }

    private static URL endpointUrl(String baseUrl, String path) throws MalformedURLException {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return new URL(normalizedBaseUrl + path);
    }

    private static boolean isHttpUrl(URL url) {
        return "http".equals(url.getProtocol()) || "https".equals(url.getProtocol());
    }

    private static HttpResponse get(URL url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            String body = responseCode >= 200 && responseCode < 400 ? readBody(connection.getInputStream()) : "";
            return new HttpResponse(responseCode, body);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        try (InputStream bodyStream = inputStream) {
            byte[] bytes = new byte[8192];
            StringBuilder body = new StringBuilder();
            int count;
            while ((count = bodyStream.read(bytes)) != -1) {
                body.append(new String(bytes, 0, count, StandardCharsets.UTF_8));
            }
            return body.toString();
        }
    }

    private static MatterControllerState matterControllerState(String thingsJson) {
        boolean controllerFound = false;
        for (String object : objectFragments(thingsJson)) {
            if (MATTER_CONTROLLER_PATTERN.matcher(object).find()) {
                controllerFound = true;
                if (matterControllerOnline(object)) {
                    return MatterControllerState.ONLINE;
                }
            }
        }
        return controllerFound ? MatterControllerState.OFFLINE : MatterControllerState.NOT_FOUND;
    }

    private static boolean matterControllerOnline(String thingObject) {
        java.util.regex.Matcher matcher = STATUS_INFO_PATTERN.matcher(thingObject);
        return matcher.find() && ONLINE_STATUS_PATTERN.matcher(matcher.group(1)).find();
    }

    private static String[] objectFragments(String json) {
        String body = json == null ? "" : json;
        java.util.ArrayList<String> fragments = new java.util.ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char value = body.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (value == '\\') {
                escaped = inString;
                continue;
            }
            if (value == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (value == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (value == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    fragments.add(body.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return fragments.toArray(new String[0]);
    }

    private enum MatterControllerState {
        ONLINE,
        OFFLINE,
        NOT_FOUND
    }

    private static final class HttpResponse {
        private final int statusCode;
        private final String body;

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        boolean success() {
            return statusCode >= 200 && statusCode < 400;
        }
    }
}
