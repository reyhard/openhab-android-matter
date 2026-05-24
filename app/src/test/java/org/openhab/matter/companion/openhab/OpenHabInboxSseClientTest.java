package org.openhab.matter.companion.openhab;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OpenHabInboxSseClientTest {
    @Test
    public void requestsInboxEventsAndStopsWhenListenerReturnsFalse() throws Exception {
        OneShotSseServer server = new OneShotSseServer(
                "data: {\"topic\":\"openhab/inbox/matter:node:abc/added\",\"payload\":{\"thingUID\":\"matter:node:abc\"}}\n\n"
                        + "data: {\"topic\":\"openhab/inbox/other/added\"}\n\n");
        server.start();
        List<OpenHabInboxEvent> received = new ArrayList<>();
        OpenHabInboxEventListener listener = event -> {
            received.add(event);
            return false;
        };

        new OpenHabInboxSseClient().observe(server.baseUrl() + "/", listener);
        server.join();

        assertEquals("GET /rest/events?topics=openhab/inbox/* HTTP/1.1", server.requestLine());
        assertTrue(server.acceptsEventStream());
        assertEquals(1, received.size());
        assertTrue(received.get(0).matterEntryDetected());
        assertEquals("openhab/inbox/matter:node:abc/added", received.get(0).topic());
    }

    @Test
    public void ignoresKeepAliveEventsUntilInboxMessageArrives() throws Exception {
        OneShotSseServer server = new OneShotSseServer(
                "event: alive\n"
                        + "data: {}\n\n"
                        + "event: message\n"
                        + "data: {\"topic\":\"openhab/inbox/matter:node:abc/added\",\"payload\":{\"thingUID\":\"matter:node:abc\"}}\n\n");
        server.start();
        List<OpenHabInboxEvent> received = new ArrayList<>();

        new OpenHabInboxSseClient().observe(server.baseUrl(), event -> {
            received.add(event);
            return false;
        });
        server.join();

        assertEquals(1, received.size());
        assertEquals("openhab/inbox/matter:node:abc/added", received.get(0).topic());
        assertTrue(received.get(0).matterEntryDetected());
    }

    @Test
    public void rejectsNonHttpUrlsAsIoException() throws Exception {
        try {
            new OpenHabInboxSseClient().observe("file:/tmp/openhab-events", event -> false);
            fail("Expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unsupported protocol"));
        }
    }

    @Test
    public void stopsBeforeDispatchWhenCancellationSupplierIsFalse() throws Exception {
        OneShotSseServer server = new OneShotSseServer(
                "event: message\n"
                        + "data: {\"topic\":\"openhab/inbox/matter:node:abc/added\",\"payload\":{\"thingUID\":\"matter:node:abc\"}}\n\n");
        server.start();
        List<OpenHabInboxEvent> received = new ArrayList<>();

        new OpenHabInboxSseClient().observe(server.baseUrl(), event -> {
            received.add(event);
            return true;
        }, new AtomicBoolean(false)::get);
        server.join();

        assertEquals(0, received.size());
    }

    @Test
    public void stopsBeforeDispatchWhenCancelledAfterReadingEventBlock() throws Exception {
        OneShotSseServer server = new OneShotSseServer(
                "event: message\n"
                        + "data: {\"topic\":\"openhab/inbox/matter:node:abc/added\",\"payload\":{\"thingUID\":\"matter:node:abc\"}}\n\n");
        server.start();
        List<OpenHabInboxEvent> received = new ArrayList<>();
        AtomicBoolean firstSixChecks = new AtomicBoolean(true);
        int[] checks = new int[] {0};

        new OpenHabInboxSseClient().observe(server.baseUrl(), event -> {
            received.add(event);
            return true;
        }, () -> {
            checks[0]++;
            if (checks[0] > 6) {
                firstSixChecks.set(false);
            }
            return firstSixChecks.get();
        });
        server.join();

        assertEquals(0, received.size());
    }

    private static final class OneShotSseServer {
        private final String body;
        private final ServerSocket serverSocket;
        private Thread thread;
        private String requestLine = "";
        private boolean acceptsEventStream;

        OneShotSseServer(String body) throws IOException {
            this.body = body;
            this.serverSocket = new ServerSocket(0);
        }

        void start() {
            thread = new Thread(this::serveOnce, "test-sse-server");
            thread.start();
        }

        void join() throws InterruptedException {
            thread.join(3000);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort();
        }

        String requestLine() {
            return requestLine;
        }

        boolean acceptsEventStream() {
            return acceptsEventStream;
        }

        private void serveOnce() {
            try (ServerSocket ignored = serverSocket;
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    OutputStream output = socket.getOutputStream()) {
                requestLine = reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().startsWith("accept:") && line.contains("text/event-stream")) {
                        acceptsEventStream = true;
                    }
                    if (line.isEmpty()) {
                        break;
                    }
                }

                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                String response = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/event-stream\r\n"
                        + "Content-Length: " + bodyBytes.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                output.write(response.getBytes(StandardCharsets.UTF_8));
                output.write(bodyBytes);
                output.flush();
            } catch (IOException ignored) {
                // Test assertions cover whether the client received the event.
            }
        }
    }
}
