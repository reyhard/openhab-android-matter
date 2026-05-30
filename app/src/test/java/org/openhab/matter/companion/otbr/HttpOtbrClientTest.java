package org.openhab.matter.companion.otbr;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HttpOtbrClientTest {
    @Test
    public void reportsReachableForSuccessfulHttpResponse() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "otbr");
        server.start();

        OtbrStatus status = new HttpOtbrClient().checkReadiness(server.baseUrl());

        assertTrue(status.reachable());
        assertEquals("OTBR endpoint is reachable", status.message());
        assertTrue(status.details().contains("HTTP 200"));
    }

    @Test
    public void reportsUnreachableForHttpErrorResponse() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(404, "not found");
        server.start();

        OtbrStatus status = new HttpOtbrClient().checkReadiness(server.baseUrl());

        assertFalse(status.reachable());
        assertEquals("OTBR endpoint is not reachable", status.message());
        assertTrue(status.details().contains("HTTP 404"));
    }

    @Test
    public void acceptsPlainIpAddressAsNetworkReachabilityTarget() {
        OtbrStatus status = new HttpOtbrClient().checkReadiness("127.0.0.1");

        assertTrue(status.reachable());
        assertEquals("OTBR address is accepted", status.message());
        assertTrue(status.details().contains("127.0.0.1"));
    }

    @Test
    public void acceptsPlainIpv4AddressWithPortAsNetworkReachabilityTarget() {
        OtbrStatus status = new HttpOtbrClient().checkReadiness("127.0.0.1:49154");

        assertTrue(status.reachable());
        assertEquals("OTBR address is accepted", status.message());
        assertTrue(status.details().contains("127.0.0.1:49154"));
    }

    @Test
    public void acceptsHostNameWithPortAsNetworkReachabilityTarget() {
        OtbrStatus status = new HttpOtbrClient().checkReadiness("localhost:49154");

        assertTrue(status.reachable());
        assertEquals("OTBR address is accepted", status.message());
        assertTrue(status.details().contains("localhost:49154"));
    }

    @Test
    public void acceptsScopedIpv6AddressAsNetworkAddress() {
        OtbrStatus status = new HttpOtbrClient().checkReadiness("fe80::abcd%wlan0");

        assertTrue(status.reachable());
        assertEquals("OTBR address is accepted", status.message());
        assertTrue(status.details().contains("fe80::abcd%wlan0"));
    }

    @Test
    public void reportsUnreachableForUnsupportedProtocol() {
        OtbrStatus status = new HttpOtbrClient().checkReadiness("file:/tmp/otbr");

        assertFalse(status.reachable());
        assertEquals("OTBR address is invalid", status.message());
        assertTrue(status.details().contains("Unsupported protocol"));
    }

    private static final class OneShotHttpServer {
        private final int statusCode;
        private final String body;
        private final ServerSocket serverSocket;
        private Thread thread;

        OneShotHttpServer(int statusCode, String body) throws IOException {
            this.statusCode = statusCode;
            this.body = body;
            this.serverSocket = new ServerSocket(0);
        }

        void start() {
            thread = new Thread(this::serveOnce, "test-otbr-http-server");
            thread.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort();
        }

        private void serveOnce() {
            try (ServerSocket ignored = serverSocket;
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8));
                    OutputStream output = socket.getOutputStream()) {
                String line;
                while ((line = reader.readLine()) != null) {
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
                // Test assertions cover whether the client received the response.
            }
        }
    }
}
