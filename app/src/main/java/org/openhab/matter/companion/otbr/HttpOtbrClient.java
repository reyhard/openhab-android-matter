package org.openhab.matter.companion.otbr;

import java.io.IOException;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public final class HttpOtbrClient implements OtbrClient {
    private static final int TIMEOUT_MILLIS = 3000;

    @Override
    public OtbrStatus checkReadiness(String baseUrl) {
        HttpURLConnection connection = null;
        try {
            String target = baseUrl == null ? "" : baseUrl.trim();
            if (!isHttpUrlText(target)) {
                return checkAddressReachability(target);
            }

            URL url = normalizedUrl(target);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return new OtbrStatus(false, "OTBR address is invalid", "Unsupported protocol: " + url.getProtocol());
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
            return new OtbrStatus(false, "OTBR address is invalid", e.getMessage());
        } catch (IOException e) {
            return new OtbrStatus(false, "OTBR endpoint is not reachable", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static OtbrStatus checkAddressReachability(String target) {
        String address = normalizeAddress(target);
        if (address.isEmpty()) {
            return new OtbrStatus(false, "OTBR address is invalid", "OTBR address is required");
        }
        if (hasUnsupportedScheme(address)) {
            try {
                URL url = new URL(address);
                return new OtbrStatus(false, "OTBR address is invalid", "Unsupported protocol: " + url.getProtocol());
            } catch (MalformedURLException e) {
                return new OtbrStatus(false, "OTBR address is invalid", e.getMessage());
            }
        }
        try {
            String resolvableAddress = stripEndpointPort(stripIpv6Zone(stripIpv6Brackets(address)));
            InetAddress inetAddress = InetAddress.getByName(resolvableAddress);
            return new OtbrStatus(true, "OTBR address is accepted",
                    "Address " + address + " resolves to " + inetAddress.getHostAddress()
                            + ". No HTTP service is required for a pure OpenThread Border Router.");
        } catch (IOException e) {
            return new OtbrStatus(false, "OTBR address is invalid", e.getMessage());
        } catch (IllegalArgumentException e) {
            return new OtbrStatus(false, "OTBR address is invalid", e.getMessage());
        }
    }

    private static boolean isHttpUrlText(String target) {
        String lower = target.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static boolean hasUnsupportedScheme(String target) {
        if (looksLikeHostPortEndpoint(target)) {
            return false;
        }
        return target.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*") && !looksLikeIpv6Address(target);
    }

    private static boolean looksLikeHostPortEndpoint(String target) {
        if (target == null || looksLikeIpv6Address(target)) {
            return false;
        }
        int colonIndex = target.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex != target.indexOf(':') || colonIndex == target.length() - 1) {
            return false;
        }
        return target.substring(colonIndex + 1).matches("[0-9]+");
    }

    private static boolean looksLikeIpv6Address(String target) {
        String value = stripIpv6Brackets(target);
        if (value.indexOf(':') < 0 || value.indexOf(':') == value.lastIndexOf(':')) {
            return false;
        }
        int zoneIndex = value.indexOf('%');
        if (zoneIndex >= 0) {
            String zone = value.substring(zoneIndex + 1);
            if (zone.isEmpty() || !zone.matches("[0-9A-Za-z_.-]+")) {
                return false;
            }
            value = value.substring(0, zoneIndex);
        }
        return value.matches("[0-9A-Fa-f:.]+");
    }

    private static String normalizeAddress(String target) {
        String normalized = target == null ? "" : target.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String stripIpv6Brackets(String target) {
        if (target != null && target.startsWith("[") && target.endsWith("]")) {
            return target.substring(1, target.length() - 1);
        }
        if (target != null && target.startsWith("[") && target.contains("]:")) {
            return target.substring(1, target.indexOf("]:"));
        }
        return target;
    }

    private static String stripIpv6Zone(String target) {
        if (target == null || !looksLikeIpv6Address(target)) {
            return target;
        }
        int zoneIndex = target.indexOf('%');
        if (zoneIndex < 0) {
            return target;
        }
        return target.substring(0, zoneIndex);
    }

    private static String stripEndpointPort(String target) {
        if (target == null || looksLikeIpv6Address(target)) {
            return target;
        }
        int colonIndex = target.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex == target.length() - 1) {
            return target;
        }
        String port = target.substring(colonIndex + 1);
        if (!port.matches("[0-9]+")) {
            return target;
        }
        return target.substring(0, colonIndex);
    }

    private static URL normalizedUrl(String baseUrl) throws MalformedURLException {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return new URL(normalizedBaseUrl);
    }
}
