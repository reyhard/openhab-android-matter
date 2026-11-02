package org.openhab.matter.companion.openhab;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpOpenHabInboxClientTest {
    private static final long OPENHAB_NODE_ID = Long.parseUnsignedLong("10051033818082827676");
    private static final String TARGET_ENTRY = "{"
            + "\"bridgeUID\":\"matter:controller:OTBR_01\","
            + "\"flag\":\"NEW\","
            + "\"label\":\"Matter Device: IKEA of Sweden ALPSTUGA air quality monitor\","
            + "\"properties\":{\"nodeId\":\"10051033818082827676\"},"
            + "\"representationProperty\":\"nodeId\","
            + "\"thingUID\":\"matter:node:OTBR_01:10051033818082827676\","
            + "\"thingTypeUID\":\"matter:node\""
            + "}";

    @Test
    public void detectsMatterInboxEntryFromSuccessfulResponse() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "[{\"thingUID\":\"matter:node:controller:device\"}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertTrue(status.reachable());
        assertTrue(status.matterEntryDetected());
    }

    @Test
    public void extractsMatterInboxEntryIdsFromSuccessfulResponse() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200,
                "[{\"thingUID\":\"matter:node:controller:old\"},{\"thingUID\":\"zwave:device:1\"},"
                        + "{\"bindingId\":\"matter\",\"thingUID\":\"matter:node:controller:new\"}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertTrue(status.matterEntryIds().contains("matter:node:controller:old"));
        assertTrue(status.matterEntryIds().contains("matter:node:controller:new"));
        assertFalse(status.matterEntryIds().contains("zwave:device:1"));
    }

    @Test
    public void parsesRealSizedInboxEntryBeyondSixteenKilobytes() throws Exception {
        // The target entry is byte-identical to the captured payload. Neighbouring entries are
        // sanitised reconstructions that preserve the real response's size, count, and offset.
        String response = readResource("/openhab-inbox-67-entries.json");
        assertEquals(20_077, response.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(18_683, response.indexOf(TARGET_ENTRY));
        assertEquals(1, countOccurrences(response, TARGET_ENTRY));
        OneShotHttpServer server = new OneShotHttpServer(200, response);
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertTrue(status.reachable());
        assertTrue(status.matterEntryDetected());
        assertFalse(status.responseIncomplete());
        assertEquals(67, status.entries().size());
        OpenHabInboxEntry target = entry(status, "matter:node:OTBR_01:10051033818082827676");
        assertNotNull(target);
        assertEquals("Matter Device: IKEA of Sweden ALPSTUGA air quality monitor", target.label());
        assertEquals("NEW", target.flag());
        assertEquals("10051033818082827676", target.nodeId());
        assertTrue(target.eligibleDiscovery());
        assertTrue(status.containsNodeId(OPENHAB_NODE_ID));
        assertFalse(status.containsNodeId(1_234L));
    }

    @Test
    public void keepsCompleteEntriesBeforeTruncatedTrailingObject() throws Exception {
        String response = "[{\"thingUID\":\"matter:node:controller:complete\","
                + "\"label\":\"Complete\",\"flag\":\"NEW\",\"properties\":{\"nodeId\":\"42\"}},"
                + "{\"thingUID\":\"matter:node:controller:partial\",\"label\":\"partial";
        OneShotHttpServer server = new OneShotHttpServer(200, response);
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertTrue(status.reachable());
        assertTrue(status.matterEntryDetected());
        assertEquals(1, status.entries().size());
        assertNotNull(entry(status, "matter:node:controller:complete"));
        assertTrue(status.containsNodeId(42L));
        assertTrue(status.responseIncomplete());
        assertTrue(status.details().contains("truncated or incomplete"));
    }

    @Test
    public void matchesMissingPropertiesNodeIdUsingWholeThingUidSuffix() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200,
                "[{\"thingUID\":\"matter:node:OTBR_01:10051033818082827676\",\"flag\":\"NEW\","
                        + "\"label\":\"IKEA ALPSTUGA\"}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertTrue(status.containsNodeId(OPENHAB_NODE_ID));
        assertFalse(status.containsNodeId(99_999L));
    }

    @Test
    public void parsesEscapedStringsAndReorderedFieldsWithoutReadingNestedValues() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200,
                "[{\"label\":\"IKEA \\\"ALPSTUGA\\\" \\\\ monitor\","
                        + "\"thingUID\":\"matter:node:OTBR_01:10051033818082827676\","
                        + "\"properties\":{"
                        + "\"other\":{\"thingUID\":\"matter:node:fake\",\"properties\":{\"nodeId\":\"1\"},"
                        + "\"flag\":\"NEW\"},"
                        + "\"nodeId\":\"10051033818082827676\"},"
                        + "\"flag\":\"NEW\"}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertEquals(1, status.entries().size());
        OpenHabInboxEntry target = entry(status, "matter:node:OTBR_01:10051033818082827676");
        assertNotNull(target);
        assertEquals("IKEA \"ALPSTUGA\" \\ monitor", target.label());
        assertEquals("10051033818082827676", target.nodeId());
        assertEquals(null, entry(status, "matter:node:fake"));
    }

    @Test
    public void parsesUnicodeEscapesInThingUidAndLabel() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200,
                "[{\"thingUID\":\"\\u006datter:node:OTBR_01:10051033818082827676\","
                        + "\"label\":\"Matter Device: IKEA \\u0026 ALPSTUGA caf\\u00e9\","
                        + "\"flag\":\"NEW\","
                        + "\"properties\":{\"nodeId\":\"10051033818082827676\"}}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        OpenHabInboxEntry target = entry(status, "matter:node:OTBR_01:10051033818082827676");
        assertNotNull(target);
        assertEquals("Matter Device: IKEA & ALPSTUGA café", target.label());
        assertTrue(status.containsNodeId(OPENHAB_NODE_ID));
    }

    @Test
    public void doesNotReadThingUidTextFromInsideLabelValue() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200,
                "[{\"thingUID\":\"zwave:device:1\","
                        + "\"label\":\"literal \\\"thingUID\\\":\\\"matter:node:fake\\\" in a label\","
                        + "\"flag\":\"NEW\"}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertFalse(status.matterEntryDetected());
        assertTrue(status.matterEntryIds().isEmpty());
        assertEquals(1, status.entries().size());
        assertTrue(status.entries().get(0).label().contains("\"thingUID\":\"matter:node:fake\""));
        assertEquals(null, entry(status, "matter:node:fake"));
    }

    @Test
    public void matchesHexadecimalNodeIdsCaseInsensitively() throws Exception {
        OneShotHttpServer upper = new OneShotHttpServer(200,
                "[{\"thingUID\":\"matter:node:OTBR_01:10051033818082827676\",\"flag\":\"NEW\","
                        + "\"properties\":{\"nodeId\":\"0x8B7C72014043DD9C\"}}]");
        OneShotHttpServer lower = new OneShotHttpServer(200,
                "[{\"thingUID\":\"matter:node:OTBR_01:10051033818082827676\",\"flag\":\"NEW\","
                        + "\"properties\":{\"nodeId\":\"0x8b7c72014043dd9c\"}}]");
        upper.start();
        lower.start();

        OpenHabInboxStatus upperStatus = new HttpOpenHabInboxClient().checkInbox(upper.baseUrl());
        OpenHabInboxStatus lowerStatus = new HttpOpenHabInboxClient().checkInbox(lower.baseUrl());

        assertTrue(upperStatus.containsNodeId(OPENHAB_NODE_ID));
        assertTrue(lowerStatus.containsNodeId(OPENHAB_NODE_ID));
    }

    @Test
    public void doesNotMatchNodeIdsBySubstring() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200,
                "[{\"thingUID\":\"matter:node:OTBR_01:99910051033818082827676\",\"flag\":\"NEW\","
                        + "\"properties\":{\"nodeId\":\"99910051033818082827676\"}}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertFalse(status.containsNodeId(OPENHAB_NODE_ID));
    }

    @Test
    public void doesNotTreatBindingOnlyPayloadAsMatterEntry() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200,
                "[{\"bindingId\":\"matter\",\"thingUID\":\"zwave:device:1\",\"flag\":\"NEW\"}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertTrue(status.reachable());
        assertFalse(status.matterEntryDetected());
        assertTrue(status.matterEntryIds().isEmpty());
        assertEquals(1, status.entries().size());
    }

    @Test
    public void oversizedResponseIsReachableButNeverDetected() throws Exception {
        int limit = 8 * 1024 * 1024;
        StringBuilder response = new StringBuilder(limit + 128);
        response.append("[{\"thingUID\":\"matter:node:controller:device\",\"flag\":\"NEW\"},");
        response.append(" ".repeat(limit));
        response.append(']');
        OneShotHttpServer server = new OneShotHttpServer(200, response.toString());
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertTrue(status.reachable());
        assertFalse(status.matterEntryDetected());
        assertTrue(status.entries().isEmpty());
        assertTrue(status.matterEntryIds().isEmpty());
        assertTrue(status.responseIncomplete());
        assertTrue(status.message().contains(String.valueOf(limit)));
        assertTrue(status.details().contains(String.valueOf(limit)));
    }

    @Test
    public void treatsRedirectAsUnreachable() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(302, "");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl());

        assertFalse(status.reachable());
        assertFalse(status.matterEntryDetected());
    }

    @Test
    public void sendsBearerTokenWhenConfigured() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "[{\"thingUID\":\"matter:node:controller:device\"}]");
        server.start();

        OpenHabInboxStatus status = new HttpOpenHabInboxClient().checkInbox(server.baseUrl(), "oh.test.token");

        assertTrue(status.reachable());
        assertEquals("Bearer oh.test.token", server.authorizationHeader());
    }

    private static OpenHabInboxEntry entry(OpenHabInboxStatus status, String thingUid) {
        for (OpenHabInboxEntry entry : status.entries()) {
            if (thingUid.equals(entry.thingUid())) {
                return entry;
            }
        }
        return null;
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = HttpOpenHabInboxClientTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + path);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static final class OneShotHttpServer {
        private final int statusCode;
        private final String body;
        private final ServerSocket serverSocket;
        private Thread thread;
        private String authorizationHeader = "";

        OneShotHttpServer(int statusCode, String body) throws IOException {
            this.statusCode = statusCode;
            this.body = body;
            this.serverSocket = new ServerSocket(0);
        }

        void start() {
            thread = new Thread(this::serveOnce, "test-http-server");
            thread.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort();
        }

        String authorizationHeader() {
            return authorizationHeader;
        }

        private void serveOnce() {
            try (ServerSocket ignored = serverSocket;
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    OutputStream output = socket.getOutputStream()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().startsWith("authorization:")) {
                        authorizationHeader = line.substring("authorization:".length()).trim();
                    }
                    if (line.isEmpty()) {
                        break;
                    }
                }
                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                String response = "HTTP/1.1 " + statusCode + " Test\r\n"
                        + "Content-Length: " + bodyBytes.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                output.write(response.getBytes(StandardCharsets.UTF_8));
                output.write(bodyBytes);
                output.flush();
            } catch (IOException ignored) {
                // Test will fail through the client result if the server cannot respond.
            }
        }
    }
}
