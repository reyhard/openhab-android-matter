package org.openhab.matter.companion.openhab;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HttpOpenHabClientTest {
    @Test
    public void reportsReadyWhenRestAndMatterControllerAreOnline() throws Exception {
        RouteHttpServer server = new RouteHttpServer()
                .route("/rest/", 200, "{}")
                .route("/rest/things", 200, "[{\"UID\":\"matter:controller:home\","
                        + "\"thingTypeUID\":\"matter:controller\","
                        + "\"statusInfo\":{\"status\":\"ONLINE\"}}]");
        server.start(2);

        OpenHabStatus status = new HttpOpenHabClient().checkReadiness(server.baseUrl());

        assertTrue(status.online());
        assertTrue(status.restReachable());
        assertTrue(status.matterControllerReady());
        assertEquals("openHAB Matter controller is ready", status.message());
        assertTrue(status.details().contains("/rest/things"));
    }

    @Test
    public void reportsNotReadyWhenMatterControllerIsMissing() throws Exception {
        RouteHttpServer server = new RouteHttpServer()
                .route("/rest/", 200, "{}")
                .route("/rest/things", 200, "[{\"UID\":\"astro:sun:local\",\"thingTypeUID\":\"astro:sun\"}]");
        server.start(2);

        OpenHabStatus status = new HttpOpenHabClient().checkReadiness(server.baseUrl());

        assertFalse(status.online());
        assertTrue(status.restReachable());
        assertFalse(status.matterControllerReady());
        assertEquals("openHAB Matter controller was not found", status.message());
    }

    @Test
    public void reportsNotReadyWhenMatterControllerIsOffline() throws Exception {
        RouteHttpServer server = new RouteHttpServer()
                .route("/rest/", 200, "{}")
                .route("/rest/things", 200, "[{\"UID\":\"matter:controller:home\","
                        + "\"thingTypeUID\":\"matter:controller\","
                        + "\"statusInfo\":{\"status\":\"OFFLINE\"}}]");
        server.start(2);

        OpenHabStatus status = new HttpOpenHabClient().checkReadiness(server.baseUrl());

        assertFalse(status.online());
        assertTrue(status.restReachable());
        assertFalse(status.matterControllerReady());
        assertEquals("openHAB Matter controller is not online", status.message());
    }

    @Test
    public void ignoresNestedOnlineStatusOutsideMatterControllerStatusInfo() throws Exception {
        RouteHttpServer server = new RouteHttpServer()
                .route("/rest/", 200, "{}")
                .route("/rest/things", 200, "[{\"UID\":\"matter:controller:home\","
                        + "\"thingTypeUID\":\"matter:controller\","
                        + "\"statusInfo\":{\"status\":\"OFFLINE\"},"
                        + "\"channels\":[{\"status\":\"ONLINE\"}]}]");
        server.start(2);

        OpenHabStatus status = new HttpOpenHabClient().checkReadiness(server.baseUrl());

        assertFalse(status.online());
        assertTrue(status.restReachable());
        assertFalse(status.matterControllerReady());
        assertEquals("openHAB Matter controller is not online", status.message());
    }

    @Test
    public void reportsNotReadyWhenThingsEndpointReturnsHttpError() throws Exception {
        RouteHttpServer server = new RouteHttpServer()
                .route("/rest/", 200, "{}")
                .route("/rest/things", 500, "server error");
        server.start(2);

        OpenHabStatus status = new HttpOpenHabClient().checkReadiness(server.baseUrl());

        assertFalse(status.online());
        assertTrue(status.restReachable());
        assertFalse(status.matterControllerReady());
        assertEquals("openHAB Matter controller readiness could not be verified", status.message());
        assertTrue(status.details().contains("HTTP 500"));
    }

    @Test
    public void preservesRestFailureBeforeCheckingThings() throws Exception {
        RouteHttpServer server = new RouteHttpServer()
                .route("/rest/", 404, "not found")
                .route("/rest/things", 200, "[]");
        server.start(1);

        OpenHabStatus status = new HttpOpenHabClient().checkReadiness(server.baseUrl());

        assertFalse(status.online());
        assertFalse(status.restReachable());
        assertFalse(status.matterControllerReady());
        assertEquals("openHAB REST API is not reachable", status.message());
    }

    @Test
    public void preservesRestReachabilityWhenThingsEndpointCannotBeRead() throws Exception {
        RouteHttpServer server = new RouteHttpServer()
                .route("/rest/", 200, "{}");
        server.start(1);

        OpenHabStatus status = new HttpOpenHabClient().checkReadiness(server.baseUrl());

        assertFalse(status.online());
        assertTrue(status.restReachable());
        assertFalse(status.matterControllerReady());
        assertEquals("openHAB Matter controller readiness could not be verified", status.message());
    }

    private static final class RouteHttpServer {
        private final ServerSocket serverSocket;
        private final Map<String, TestResponse> responses = new HashMap<>();
        private Thread thread;

        RouteHttpServer() throws IOException {
            this.serverSocket = new ServerSocket(0);
            this.serverSocket.setSoTimeout(5000);
        }

        RouteHttpServer route(String path, int statusCode, String body) {
            responses.put(path, new TestResponse(statusCode, body));
            return this;
        }

        void start(int expectedRequests) {
            thread = new Thread(() -> serve(expectedRequests), "test-openhab-http-server");
            thread.setDaemon(true);
            thread.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort();
        }

        private void serve(int expectedRequests) {
            try (ServerSocket ignored = serverSocket) {
                for (int i = 0; i < expectedRequests; i++) {
                    try (Socket socket = serverSocket.accept();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream(), StandardCharsets.UTF_8));
                            OutputStream output = socket.getOutputStream()) {
                        String requestLine = reader.readLine();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) {
                                break;
                            }
                        }
                        String path = requestLine == null ? "" : requestLine.split(" ")[1];
                        TestResponse response = responses.getOrDefault(path, new TestResponse(404, "not found"));
                        byte[] bodyBytes = response.body.getBytes(StandardCharsets.UTF_8);
                        String headers = "HTTP/1.1 " + response.statusCode + " Test\r\n"
                                + "Content-Type: application/json\r\n"
                                + "Content-Length: " + bodyBytes.length + "\r\n"
                                + "Connection: close\r\n\r\n";
                        output.write(headers.getBytes(StandardCharsets.UTF_8));
                        output.write(bodyBytes);
                        output.flush();
                    }
                }
            } catch (IOException ignored) {
                // Test assertions cover whether the client received the expected response.
            }
        }
    }

    private static final class TestResponse {
        private final int statusCode;
        private final String body;

        TestResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
