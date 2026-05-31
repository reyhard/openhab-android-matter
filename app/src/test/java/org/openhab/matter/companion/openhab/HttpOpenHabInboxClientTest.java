package org.openhab.matter.companion.openhab;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpOpenHabInboxClientTest {
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
