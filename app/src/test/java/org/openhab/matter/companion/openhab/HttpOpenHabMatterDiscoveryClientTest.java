package org.openhab.matter.companion.openhab;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpOpenHabMatterDiscoveryClientTest {
    @Test
    public void postsMatterScanWithUrlEncodedInput() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "120");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "3497-0112-332", "");
        server.await();

        assertTrue(status.reachable());
        assertTrue(status.started());
        assertEquals(120, status.timeoutSeconds());
        assertEquals("POST", server.method());
        assertEquals("/rest/discovery/bindings/matter/scan?input=3497-0112-332", server.path());
        assertFalse(status.details().contains("3497-0112-332"));
    }

    @Test
    public void sendsBearerTokenWhenConfigured() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "120");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "34970112332", "oh.test.token");
        server.await();

        assertTrue(status.started());
        assertEquals("Bearer oh.test.token", server.authorizationHeader());
    }

    @Test
    public void urlEncodesQrPayloadFallbackWithoutLoggingIt() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "120");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "MT:Y.K9042C00KA0648G00", "");
        server.await();

        assertTrue(status.started());
        assertEquals("/rest/discovery/bindings/matter/scan?input=MT%3AY.K9042C00KA0648G00", server.path());
        assertFalse(status.details().contains("MT:"));
    }

    @Test
    public void usesDefaultTimeoutWhenResponseBodyIsNotNumeric() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "accepted");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "34970112332", "");
        server.await();

        assertTrue(status.started());
        assertEquals(120, status.timeoutSeconds());
    }

    @Test
    public void reportsAuthFailureWithoutEchoingTokenOrCode() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(403, "forbidden");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "34970112332", "oh.secret.token");
        server.await();

        assertTrue(status.reachable());
        assertFalse(status.started());
        assertEquals("openHAB Matter scan authentication failed", status.message());
        assertFalse(status.details().contains("34970112332"));
        assertFalse(status.details().contains("oh.secret.token"));
    }

    @Test
    public void reportsDiscoveryServiceMissingForNotFound() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(404, "not found");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "34970112332", "");
        server.await();

        assertTrue(status.reachable());
        assertFalse(status.started());
        assertEquals("openHAB Matter discovery service was not found", status.message());
    }

    @Test
    public void reportsInvalidUrlForUnsupportedProtocol() throws Exception {
        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan("file:/tmp/openhab", "34970112332", "");

        assertFalse(status.reachable());
        assertFalse(status.started());
        assertEquals("openHAB Matter scan URL is invalid", status.message());
    }

    @Test
    public void reportsMalformedUrlWithoutEchoingTokenOrCode() throws Exception {
        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan("openhab.local:8080", "34970112332", "oh.secret.token");

        assertFalse(status.reachable());
        assertFalse(status.started());
        assertEquals("openHAB Matter scan URL is invalid", status.message());
        assertEquals("Malformed openHAB Matter scan URL", status.details());
        assertFalse(status.details().contains("34970112332"));
        assertFalse(status.details().contains("oh.secret.token"));
    }

    @Test
    public void stripsConfiguredBaseUrlSecretsFromStatusDetails() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(500, "failed");
        server.start();
        String secretBaseUrl = "http://user:secret@127.0.0.1:" + server.port()
                + "?token=abc#fragment";

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(secretBaseUrl, "34970112332", "");
        server.await();

        assertTrue(status.reachable());
        assertFalse(status.started());
        assertEquals("openHAB Matter scan could not be started", status.message());
        assertTrue(status.details().contains("http://127.0.0.1:" + server.port()
                + "/rest/discovery/bindings/matter/scan"));
        assertFalse(status.details().contains("user"));
        assertFalse(status.details().contains("secret"));
        assertFalse(status.details().contains("token=abc"));
        assertFalse(status.details().contains("fragment"));
        assertFalse(status.details().contains("34970112332"));
    }

    private static final class OneShotHttpServer {
        private static final int ACCEPT_TIMEOUT_MILLIS = 3000;
        private static final int JOIN_TIMEOUT_MILLIS = 5000;

        private final int statusCode;
        private final String body;
        private final ServerSocket serverSocket;
        private Thread thread;
        private String method = "";
        private String path = "";
        private String authorizationHeader = "";

        OneShotHttpServer(int statusCode, String body) throws IOException {
            this.statusCode = statusCode;
            this.body = body;
            this.serverSocket = new ServerSocket(0);
            this.serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MILLIS);
        }

        void start() {
            thread = new Thread(this::serveOnce, "test-openhab-matter-scan-http-server");
            thread.setDaemon(true);
            thread.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void await() throws InterruptedException {
            thread.join(JOIN_TIMEOUT_MILLIS);
            assertFalse("HTTP test server did not finish within timeout", thread.isAlive());
        }

        String method() {
            return method;
        }

        String path() {
            return path;
        }

        String authorizationHeader() {
            return authorizationHeader;
        }

        private void serveOnce() {
            try (ServerSocket ignored = serverSocket;
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                            StandardCharsets.UTF_8));
                    OutputStream output = socket.getOutputStream()) {
                String requestLine = reader.readLine();
                if (requestLine != null) {
                    String[] parts = requestLine.split(" ");
                    method = parts[0];
                    path = parts[1];
                }
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
                        + "Content-Type: text/plain\r\n"
                        + "Content-Length: " + bodyBytes.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                output.write(response.getBytes(StandardCharsets.UTF_8));
                output.write(bodyBytes);
                output.flush();
            } catch (SocketTimeoutException ignored) {
                // Test assertions cover whether the client connected to the server.
            } catch (IOException ignored) {
                // Test assertions cover whether the client received the expected response.
            }
        }
    }
}
