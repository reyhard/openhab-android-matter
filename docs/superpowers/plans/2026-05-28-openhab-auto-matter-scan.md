# openHAB Auto Matter Scan Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically submit the OpenCommissioningWindow 11-digit manual code to openHAB Matter discovery scan input and observe Inbox completion.

**Architecture:** Add a focused openHAB discovery-scan client beside the existing readiness and Inbox clients. Wire `MainActivity.runOpenCommissioningWindow()` so OCW success saves state, displays the returned code/QR, and then auto-submits the manual code to openHAB when a base URL is configured. Treat the scan POST as asynchronous and use existing Inbox SSE/polling support for completion reporting.

**Tech Stack:** Android Java, `HttpURLConnection`, JUnit 4, Gradle `:app:testDebugUnitTest`, existing openHAB REST token storage and Inbox SSE clients.

---

## Scope And File Map

Specification: `docs/superpowers/specs/2026-05-28-openhab-auto-matter-scan-design.md`

Create:

- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryClient.java` - interface for starting openHAB Matter scan input.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryScanStatus.java` - value object for scan-start result.
- `app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClient.java` - deterministic test/offline implementation.
- `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClient.java` - real REST implementation of `POST /rest/discovery/bindings/matter/scan?input=...`.
- `app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClientTest.java`
- `app/src/test/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClientTest.java`

Modify:

- `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java` - scan result presentation helpers.
- `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java` - presentation tests.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - auto-submit after OCW success and observe Inbox.
- `docs/implementation-status.md` - add implemented status after code and tests pass.
- `docs/open-commissioning-window-workflow.md` - update OCW flow with auto openHAB scan handoff after code return.

Do not modify the openHAB Matter binding for this v1.

Before implementation, check for pre-existing dirty files:

```powershell
git status --short
```

Work with existing user changes. Stage only files touched by each task.

---

### Task 1: Discovery Scan Interface, Status, And Fake

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryClient.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryScanStatus.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClient.java`
- Create: `app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClientTest.java`

- [ ] **Step 1: Write the failing fake-client tests**

Create `app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClientTest.java`:

```java
package org.openhab.matter.companion.openhab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FakeOpenHabMatterDiscoveryClientTest {
    @Test
    public void startedReturnsSuccessfulScanStatus() throws Exception {
        OpenHabMatterDiscoveryClient client = FakeOpenHabMatterDiscoveryClient.started(120);

        OpenHabMatterDiscoveryScanStatus status = client.startMatterScan("http://openhab.local:8080",
                "34970112332", "oh.test.token");

        assertTrue(status.reachable());
        assertTrue(status.started());
        assertEquals("openHAB Matter scan started", status.message());
        assertEquals(120, status.timeoutSeconds());
        assertEquals("Simulated Matter scan start for http://openhab.local:8080", status.details());
    }

    @Test
    public void failedReturnsFailureStatus() throws Exception {
        OpenHabMatterDiscoveryClient client = FakeOpenHabMatterDiscoveryClient.failed(
                "openHAB Matter scan could not be started", "Simulated failure");

        OpenHabMatterDiscoveryScanStatus status = client.startMatterScan("http://openhab.local:8080",
                "34970112332", "");

        assertFalse(status.reachable());
        assertFalse(status.started());
        assertEquals("openHAB Matter scan could not be started", status.message());
        assertEquals(0, status.timeoutSeconds());
        assertEquals("Simulated failure", status.details());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*FakeOpenHabMatterDiscoveryClientTest"
```

Expected: FAIL with errors that `OpenHabMatterDiscoveryClient`, `OpenHabMatterDiscoveryScanStatus`, and `FakeOpenHabMatterDiscoveryClient` do not exist.

- [ ] **Step 3: Add the interface**

Create `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryClient.java`:

```java
package org.openhab.matter.companion.openhab;

public interface OpenHabMatterDiscoveryClient {
    default OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode) throws Exception {
        return startMatterScan(baseUrl, pairingCode, "");
    }

    OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode, String apiToken) throws Exception;
}
```

- [ ] **Step 4: Add the status value object**

Create `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryScanStatus.java`:

```java
package org.openhab.matter.companion.openhab;

public final class OpenHabMatterDiscoveryScanStatus {
    private final boolean reachable;
    private final boolean started;
    private final String message;
    private final String details;
    private final int timeoutSeconds;

    public OpenHabMatterDiscoveryScanStatus(boolean reachable, boolean started, String message, String details,
            int timeoutSeconds) {
        this.reachable = reachable;
        this.started = started;
        this.message = message;
        this.details = details;
        this.timeoutSeconds = Math.max(0, timeoutSeconds);
    }

    public boolean reachable() {
        return reachable;
    }

    public boolean started() {
        return started;
    }

    public String message() {
        return message;
    }

    public String details() {
        return details;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }
}
```

- [ ] **Step 5: Add the fake client**

Create `app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClient.java`:

```java
package org.openhab.matter.companion.openhab;

public final class FakeOpenHabMatterDiscoveryClient implements OpenHabMatterDiscoveryClient {
    private final boolean reachable;
    private final boolean started;
    private final String message;
    private final String details;
    private final int timeoutSeconds;

    private FakeOpenHabMatterDiscoveryClient(boolean reachable, boolean started, String message, String details,
            int timeoutSeconds) {
        this.reachable = reachable;
        this.started = started;
        this.message = message;
        this.details = details;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static FakeOpenHabMatterDiscoveryClient started(int timeoutSeconds) {
        return new FakeOpenHabMatterDiscoveryClient(true, true, "openHAB Matter scan started", null, timeoutSeconds);
    }

    public static FakeOpenHabMatterDiscoveryClient failed(String message, String details) {
        return new FakeOpenHabMatterDiscoveryClient(false, false, message, details, 0);
    }

    @Override
    public OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode, String apiToken) {
        String resolvedDetails = details != null ? details : "Simulated Matter scan start for " + baseUrl;
        return new OpenHabMatterDiscoveryScanStatus(reachable, started, message, resolvedDetails, timeoutSeconds);
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*FakeOpenHabMatterDiscoveryClientTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryClient.java `
        app/src/main/java/org/openhab/matter/companion/openhab/OpenHabMatterDiscoveryScanStatus.java `
        app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClient.java `
        app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabMatterDiscoveryClientTest.java
git commit -m "Add openHAB Matter discovery scan contract"
```

---

### Task 2: HTTP Discovery Scan Client

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClient.java`
- Create: `app/src/test/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClientTest.java`

- [ ] **Step 1: Write the failing HTTP client tests**

Create `app/src/test/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClientTest.java`:

```java
package org.openhab.matter.companion.openhab;

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

public class HttpOpenHabMatterDiscoveryClientTest {
    @Test
    public void postsMatterScanWithUrlEncodedInput() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "120");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "3497-0112-332", "");

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

        assertTrue(status.started());
        assertEquals("Bearer oh.test.token", server.authorizationHeader());
    }

    @Test
    public void urlEncodesQrPayloadFallbackWithoutLoggingIt() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(200, "120");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "MT:Y.K9042C00KA0648G00", "");

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

        assertTrue(status.started());
        assertEquals(120, status.timeoutSeconds());
    }

    @Test
    public void reportsAuthFailureWithoutEchoingTokenOrCode() throws Exception {
        OneShotHttpServer server = new OneShotHttpServer(403, "forbidden");
        server.start();

        OpenHabMatterDiscoveryScanStatus status = new HttpOpenHabMatterDiscoveryClient()
                .startMatterScan(server.baseUrl(), "34970112332", "oh.secret.token");

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

    private static final class OneShotHttpServer {
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
        }

        void start() {
            thread = new Thread(this::serveOnce, "test-openhab-matter-scan-http-server");
            thread.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort();
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
            } catch (IOException ignored) {
                // Test assertions cover whether the client received the expected response.
            }
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*HttpOpenHabMatterDiscoveryClientTest"
```

Expected: FAIL with `cannot find symbol` for `HttpOpenHabMatterDiscoveryClient`.

- [ ] **Step 3: Implement the HTTP client**

Create `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClient.java`:

```java
package org.openhab.matter.companion.openhab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class HttpOpenHabMatterDiscoveryClient implements OpenHabMatterDiscoveryClient {
    private static final int TIMEOUT_MILLIS = 3000;
    private static final int MAX_RESPONSE_BYTES = 1024;
    private static final int DEFAULT_SCAN_TIMEOUT_SECONDS = 120;

    @Override
    public OpenHabMatterDiscoveryScanStatus startMatterScan(String baseUrl, String pairingCode, String apiToken) {
        HttpURLConnection connection = null;
        try {
            String safePairingCode = pairingCode == null ? "" : pairingCode.trim();
            if (safePairingCode.isEmpty()) {
                return failed(false, "openHAB Matter scan code is missing", "No pairing code was available.");
            }

            URL url = scanUrl(baseUrl, safePairingCode);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return failed(false, "openHAB Matter scan URL is invalid",
                        "Unsupported protocol: " + url.getProtocol());
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod("POST");
            applyBearerToken(connection, apiToken);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                int timeoutSeconds = parseTimeoutSeconds(readBoundedResponse(connection.getInputStream()));
                return new OpenHabMatterDiscoveryScanStatus(true, true, "openHAB Matter scan started",
                        "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl), timeoutSeconds);
            }
            if (responseCode == 401 || responseCode == 403) {
                return failed(true, "openHAB Matter scan authentication failed",
                        "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl));
            }
            if (responseCode == 404) {
                return failed(true, "openHAB Matter discovery service was not found",
                        "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl));
            }
            return failed(true, "openHAB Matter scan could not be started",
                    "HTTP " + responseCode + " from " + safeEndpointForDetails(baseUrl));
        } catch (MalformedURLException e) {
            return failed(false, "openHAB Matter scan URL is invalid", sanitize(e.getMessage()));
        } catch (IOException e) {
            return failed(false, "openHAB Matter scan is not reachable", sanitize(e.getMessage()));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static URL scanUrl(String baseUrl, String pairingCode) throws IOException {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        String encodedCode = URLEncoder.encode(pairingCode, StandardCharsets.UTF_8.name());
        return new URL(normalizedBaseUrl + "/rest/discovery/bindings/matter/scan?input=" + encodedCode);
    }

    private static void applyBearerToken(HttpURLConnection connection, String apiToken) {
        if (apiToken != null && !apiToken.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiToken.trim());
        }
    }

    private static int parseTimeoutSeconds(String responseText) {
        try {
            return Math.max(1, Integer.parseInt(responseText.trim()));
        } catch (RuntimeException ignored) {
            return DEFAULT_SCAN_TIMEOUT_SECONDS;
        }
    }

    private static String readBoundedResponse(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int totalBytes = 0;
            int readBytes;
            while (totalBytes < MAX_RESPONSE_BYTES
                    && (readBytes = stream.read(buffer, 0, Math.min(buffer.length, MAX_RESPONSE_BYTES - totalBytes))) != -1) {
                output.write(buffer, 0, readBytes);
                totalBytes += readBytes;
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } finally {
            stream.close();
        }
    }

    private static OpenHabMatterDiscoveryScanStatus failed(boolean reachable, String message, String details) {
        return new OpenHabMatterDiscoveryScanStatus(reachable, false, message, sanitize(details), 0);
    }

    private static String safeEndpointForDetails(String baseUrl) {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + "/rest/discovery/bindings/matter/scan";
    }

    private static String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "No details available";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*HttpOpenHabMatterDiscoveryClientTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClient.java `
        app/src/test/java/org/openhab/matter/companion/openhab/HttpOpenHabMatterDiscoveryClientTest.java
git commit -m "Add openHAB Matter discovery scan HTTP client"
```

---

### Task 3: Presentation Text For Scan Results

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`

- [ ] **Step 1: Add failing presentation tests**

Add these imports to `MainActivityPresentationTest.java`:

```java
import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryScanStatus;
```

Add these test methods to `MainActivityPresentationTest.java`:

```java
    @Test
    public void describesStartedOpenHabMatterScan() {
        OpenHabMatterDiscoveryScanStatus status = new OpenHabMatterDiscoveryScanStatus(
                true,
                true,
                "openHAB Matter scan started",
                "HTTP 200 from http://openhab.local:8080/rest/discovery/bindings/matter/scan",
                120);

        assertEquals(
                "openHAB Matter scan: started. Waiting up to 120 seconds for an Inbox entry.",
                MainActivityPresentation.openHabMatterScanResult(status));
    }

    @Test
    public void describesFailedOpenHabMatterScan() {
        OpenHabMatterDiscoveryScanStatus status = new OpenHabMatterDiscoveryScanStatus(
                true,
                false,
                "openHAB Matter discovery service was not found",
                "HTTP 404 from http://openhab.local:8080/rest/discovery/bindings/matter/scan",
                0);

        assertEquals(
                "openHAB Matter scan failed: openHAB Matter discovery service was not found.",
                MainActivityPresentation.openHabMatterScanResult(status));
    }

    @Test
    public void scanResultDetailsAreSanitized() {
        OpenHabMatterDiscoveryScanStatus status = new OpenHabMatterDiscoveryScanStatus(
                true,
                false,
                "openHAB Matter scan could not be started",
                "HTTP 500 from http://user:secret@openhab.local:8080/rest/discovery/bindings/matter/scan?input=34970112332 token=abc",
                0);

        assertEquals(
                "HTTP 500 from http://openhab.local:8080/rest/discovery/bindings/matter/scan token=<redacted>",
                MainActivityPresentation.openHabMatterScanDetails(status));
    }

    @Test
    public void describesOpenHabMatterScanNoInboxEntry() {
        assertEquals(
                "openHAB Matter scan started, but no Matter Inbox entry was detected yet.",
                MainActivityPresentation.openHabMatterScanNoInboxEntry());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MainActivityPresentationTest"
```

Expected: FAIL with `cannot find symbol` for `openHabMatterScanResult`, `openHabMatterScanDetails`, and `openHabMatterScanNoInboxEntry`.

- [ ] **Step 3: Add presentation helpers**

Add this import to `MainActivityPresentation.java`:

```java
import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryScanStatus;
```

Add these methods near `openHabInboxResult(...)`:

```java
    static String openHabMatterScanResult(OpenHabMatterDiscoveryScanStatus status) {
        if (status.started()) {
            return "openHAB Matter scan: started. Waiting up to " + status.timeoutSeconds()
                    + " seconds for an Inbox entry.";
        }
        return "openHAB Matter scan failed: " + safeTextForLog(status.message()) + ".";
    }

    static String openHabMatterScanDetails(OpenHabMatterDiscoveryScanStatus status) {
        return safeTextForLog(status.details());
    }

    static String openHabMatterScanNoInboxEntry() {
        return "openHAB Matter scan started, but no Matter Inbox entry was detected yet.";
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MainActivityPresentationTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java `
        app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java
git commit -m "Add openHAB Matter scan presentation"
```

---

### Task 4: Auto-Submit After OpenCommissioningWindow

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`

- [ ] **Step 1: Add the discovery client field and imports**

Add imports:

```java
import org.openhab.matter.companion.openhab.HttpOpenHabMatterDiscoveryClient;
import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryClient;
import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryScanStatus;
```

Add a field beside the existing openHAB clients:

```java
    private final OpenHabMatterDiscoveryClient openHabMatterDiscoveryClient = new HttpOpenHabMatterDiscoveryClient();
```

- [ ] **Step 2: Add the auto-submit helper methods**

Add these methods near `checkOpenHabInbox()`:

```java
    private void autoSubmitOpenHabMatterScan(String pairingCode) {
        state.openHabBaseUrl = openHabInput.getText().toString();
        state.openHabApiToken = openHabTokenInput.getText().toString();
        String baseUrl = state.openHabBaseUrl == null ? "" : state.openHabBaseUrl.trim();
        String safePairingCode = pairingCode == null ? "" : pairingCode.trim();
        if (safePairingCode.isEmpty()) {
            append("No temporary manual code was returned for automatic openHAB scan input.");
            append(OpenHabInstructions.scanInputInstructionsWithoutEcho());
            return;
        }
        if (baseUrl.isEmpty()) {
            append("openHAB base URL is not configured; use openHAB Scan Input manually.");
            append(OpenHabInstructions.scanInputInstructions(safePairingCode));
            return;
        }

        append("Starting openHAB Matter scan at " + MainActivityPresentation.safeUrlForLog(baseUrl) + " ...");
        new Thread(() -> startOpenHabMatterScanAndObserveInbox(baseUrl, safePairingCode, state.openHabApiToken),
                "openhab-matter-scan").start();
    }

    private void startOpenHabMatterScanAndObserveInbox(String baseUrl, String pairingCode, String apiToken) {
        OpenHabMatterDiscoveryScanStatus scanStatus;
        try {
            scanStatus = openHabMatterDiscoveryClient.startMatterScan(baseUrl, pairingCode, apiToken);
        } catch (Exception ex) {
            scanStatus = new OpenHabMatterDiscoveryScanStatus(false, false,
                    "openHAB Matter scan could not be started", ex.getMessage(), 0);
        }
        OpenHabMatterDiscoveryScanStatus finalScanStatus = scanStatus;
        runOnUiThread(() -> {
            append(MainActivityPresentation.openHabMatterScanResult(finalScanStatus));
            append(MainActivityPresentation.openHabMatterScanDetails(finalScanStatus));
        });
        if (!scanStatus.started()) {
            return;
        }

        boolean detected = observeOpenHabInboxAfterMatterScan(baseUrl, apiToken, scanStatus.timeoutSeconds());
        if (!detected) {
            runOnUiThread(() -> append(MainActivityPresentation.openHabMatterScanNoInboxEntry()));
        }
    }

    private boolean observeOpenHabInboxAfterMatterScan(String baseUrl, String apiToken, int timeoutSeconds) {
        long deadlineMillis = System.currentTimeMillis() + ((long) Math.max(1, timeoutSeconds) + 10L) * 1000L;
        final boolean[] detected = new boolean[] { false };
        try {
            openHabInboxSseClient.observe(baseUrl, event -> {
                boolean matterEntryDetected = event.matterEntryDetected();
                runOnUiThread(() -> append(MainActivityPresentation.openHabInboxSseEvent(matterEntryDetected)));
                detected[0] = matterEntryDetected;
                return !matterEntryDetected;
            }, () -> System.currentTimeMillis() < deadlineMillis && !Thread.currentThread().isInterrupted(), apiToken);
        } catch (Exception ex) {
            runOnUiThread(() -> append("openHAB Inbox SSE observation failed: "
                    + MainActivityPresentation.safeTextForLog(ex.getMessage())));
        }
        if (detected[0]) {
            return true;
        }
        try {
            OpenHabInboxStatus status = openHabInboxClient.checkInbox(baseUrl, apiToken);
            runOnUiThread(() -> append(MainActivityPresentation.openHabInboxResult(status)));
            return status.matterEntryDetected();
        } catch (Exception ex) {
            runOnUiThread(() -> append("openHAB Inbox check failed: "
                    + MainActivityPresentation.safeTextForLog(ex.getMessage())));
            return false;
        }
    }
```

- [ ] **Step 3: Call auto-submit after OCW success**

In `runOpenCommissioningWindow()`, inside the existing `runOnUiThread(() -> { ... })` success block, replace:

```java
                    append(OpenHabInstructions.scanInputInstructions(state.temporaryCode));
                    append(OpenHabInstructions.troubleshooting());
```

with:

```java
                    autoSubmitOpenHabMatterScan(state.temporaryCode);
                    append(OpenHabInstructions.troubleshooting());
```

This preserves manual instructions when no openHAB base URL is configured because `autoSubmitOpenHabMatterScan(...)` appends them in that case.

- [ ] **Step 4: Run targeted unit tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MainActivityPresentationTest" --tests "*HttpOpenHabMatterDiscoveryClientTest" --tests "*FakeOpenHabMatterDiscoveryClientTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java
git commit -m "Auto-submit OCW code to openHAB Matter scan"
```

---

### Task 5: Status Docs And Full Verification

**Files:**
- Modify: `docs/implementation-status.md`
- Modify: `docs/open-commissioning-window-workflow.md`

- [ ] **Step 1: Update implementation status**

In `docs/implementation-status.md`, add this bullet under `## Implemented` after the existing openHAB Inbox/SSE bullets:

```markdown
- OpenCommissioningWindow success can automatically submit the returned 11-digit manual setup code to openHAB Matter discovery scan input through `/rest/discovery/bindings/matter/scan?input=...`, using the configured bearer token and then observing Inbox status.
```

- [ ] **Step 2: Update OCW workflow summary**

In `docs/open-commissioning-window-workflow.md`, update the summary paragraph so it includes:

```markdown
When an openHAB base URL is configured, the app automatically posts the returned manual setup code to openHAB Matter discovery scan input and then observes the openHAB Inbox for a Matter entry. If no openHAB URL is configured, the app keeps the manual Scan Input instructions.
```

In the high-level Mermaid flow, replace the current openHAB instruction-only ending with nodes equivalent to:

```mermaid
P --> S{openHAB URL configured?}
S -- No --> Q[Show openHAB Scan Input instructions]
S -- Yes --> T[POST Matter discovery scan input with manual code]
T --> U[Observe openHAB Inbox SSE or poll Inbox]
U --> V{Matter Inbox entry detected?}
V -- Yes --> W[Report openHAB Matter Inbox entry detected]
V -- No --> X[Report scan started but no Inbox entry yet]
Q --> R
W --> R
X --> R
```

- [ ] **Step 3: Run full unit tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 4: Run debug APK build**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: PASS and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 5: Commit docs and any verification fixes**

```powershell
git add docs/implementation-status.md docs/open-commissioning-window-workflow.md
git commit -m "Document openHAB Matter auto-scan handoff"
```

If verification required small code/test fixes, include only the files changed for those fixes in this commit and mention them in the final summary.

---

## Hardware Validation Checklist

Run after the implementation is merged into a build with connectedhomeip controller artifacts:

- [ ] Install a debug APK built with connectedhomeip artifacts.
- [ ] Confirm openHAB base URL and REST API token are configured in the app.
- [ ] Run Thread commissioning on a fresh Thread Matter device.
- [ ] OpenCommissioningWindow from the app.
- [ ] Confirm the app displays the manual code and QR but does not log the full code or token.
- [ ] Confirm the app logs openHAB Matter scan start.
- [ ] Confirm openHAB receives a Matter Inbox entry.
- [ ] Confirm failure remains closed when connectedhomeip readiness fails.

Do not write setup codes, REST tokens, Thread datasets, QR payloads, or fabric material into permanent docs.

---

## Plan Self-Review

Spec coverage:

- App-side scan client is covered by Tasks 1 and 2.
- Auto-submit after OCW success is covered by Task 4.
- Configured bearer token reuse is covered by Task 2 tests and Task 4 wiring.
- Inbox observation after scan is covered by Task 4.
- Sanitized status and no secret logging are covered by Tasks 2 and 3.
- Status/workflow docs are covered by Task 5.

Placeholder scan: no unresolved placeholders are intentionally present.

Type consistency:

- The interface method is `startMatterScan(String baseUrl, String pairingCode, String apiToken)`.
- The status type is `OpenHabMatterDiscoveryScanStatus`.
- Presentation helper names are `openHabMatterScanResult`, `openHabMatterScanDetails`, and `openHabMatterScanNoInboxEntry`.
