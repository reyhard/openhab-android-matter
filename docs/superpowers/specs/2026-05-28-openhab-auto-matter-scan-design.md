# openHAB Auto Matter Scan Design

## Goal

Automate the currently manual openHAB handoff after Android successfully opens an OpenCommissioningWindow for a Thread Matter device.

The app should submit the temporary 11-digit manual pairing code returned by connectedhomeip to openHAB Matter discovery, then observe openHAB Inbox status so the user can see whether openHAB found the device.

## Source Verification

The local openHAB Matter binding source supports this flow.

- `MatterDiscoveryService.startScan(String input)` forwards non-empty discovery scan input to the controller handler.
- `ControllerHandler.startScan(@Nullable String code)` calls `client.pairNode(code)` when a code is present.
- `MatterControllerClient.pairNode(String code)` accepts manual pairing codes and `MT:` QR payloads. Manual codes have hyphens stripped before sending `nodes/pairNode`.
- `matter-server/src/client/namespaces/Nodes.ts` decodes the manual code and commissions the node through matter.js. Its current discovery capabilities set `ble = false`, so this is IP commissioning using the already-open commissioning window, not direct BLE Thread onboarding.
- openHAB core `DiscoveryResource.scan()` exposes `POST /rest/discovery/bindings/{bindingId}/scan` and passes the optional `input` query parameter into the discovery registry.

Therefore the Android app can call:

```http
POST /rest/discovery/bindings/matter/scan?input=<url-encoded-11-digit-code>
Authorization: Bearer <token>
```

## Scope

In scope:

- Add app-side support for triggering openHAB Matter discovery scan input.
- Auto-submit the OCW manual code after OCW succeeds when an openHAB base URL is configured.
- Reuse the configured openHAB REST API token.
- Observe openHAB Inbox after starting the scan.
- Report clear, sanitized status in the app log.

Out of scope:

- Adding or changing openHAB binding channels.
- Auto-approving Inbox entries into Things.
- Deriving a manual setup code from QR.
- Replacing the Android connectedhomeip commissioning path.
- Implementing phone-as-BLE-proxy direct openHAB commissioning.

## User Flow

1. User configures openHAB base URL and optional REST API token.
2. User commissions the fresh Thread device through the existing Android connectedhomeip flow.
3. User opens an OpenCommissioningWindow from the app.
4. On OCW success, the app stores the returned controller state, displays the returned manual code and QR as it does today, then checks whether openHAB base URL is configured.
5. If openHAB base URL is configured, the app automatically submits the returned manual code to openHAB Matter discovery scan input.
6. The app starts observing openHAB Inbox using the existing SSE path, with polling as a fallback if needed.
7. The app reports one of:
   - openHAB Matter scan started and a Matter Inbox entry was detected.
   - openHAB Matter scan started, but no Matter Inbox entry was detected before the scan timeout plus grace period.
   - openHAB Matter scan could not be started, with sanitized HTTP/error details.
8. If no openHAB base URL is configured, the app keeps the current manual Scan Input instructions.

## Code Selection

Use the temporary manual code returned by OpenCommissioningWindow as the primary scan input.

The current working path uses the shorter 11-digit code, and the binding accepts that code directly. The app should submit that code exactly as returned by connectedhomeip, except for URL encoding in the REST query parameter.

The app may display the QR payload when returned, but this feature must not derive a manual code from QR. If a future connectedhomeip result has no manual code but has a QR payload, that can be handled as a separate design decision.

## App Architecture

Add a small openHAB client parallel to the existing readiness and Inbox clients:

- `OpenHabMatterDiscoveryClient`
- `HttpOpenHabMatterDiscoveryClient`
- `FakeOpenHabMatterDiscoveryClient` for deterministic tests
- `OpenHabMatterDiscoveryScanStatus`

The HTTP client responsibilities:

- Build `/rest/discovery/bindings/matter/scan?input=<encoded-code>` from the configured base URL.
- Use `POST`.
- Apply `Authorization: Bearer <token>` when configured.
- Accept only `http` and `https`.
- Parse the plain-text timeout response as an optional integer.
- Return a status object rather than throwing for expected HTTP failures.
- Never include the pairing code in returned details, logs, or exceptions.

`MainActivity.runOpenCommissioningWindow()` remains the orchestration point. After OCW success, it should call a small helper such as `autoSubmitOpenHabMatterScan(result.temporaryCode())`. That helper should no-op with manual instructions when the openHAB base URL is blank.

## Completion Detection

The discovery scan REST call is asynchronous. A 2xx response only means openHAB accepted the scan request.

The app should treat final success as a Matter Inbox entry being detected through the existing Inbox SSE parser or Inbox polling client. If SSE observation fails, the app can fall back to a single or repeated `/rest/inbox` check. If no Matter Inbox entry appears before the returned scan timeout plus a small grace period, report a pending/not-detected result rather than claiming pairing success.

Recommended timeout handling:

- Use the integer returned by the scan endpoint when available.
- If the body cannot be parsed, use a conservative default such as 120 seconds.
- Add a small grace period for Inbox event delivery.

## Error Handling

Fail closed and report clearly:

- Blank or missing manual code: do not call openHAB scan; keep manual instructions.
- Missing openHAB base URL: do not call openHAB scan; keep manual instructions.
- Invalid URL or unsupported protocol: report invalid openHAB URL.
- HTTP 401/403: report authentication/authorization failure without echoing the token or code.
- HTTP 404: report Matter discovery service not found, likely Matter binding/controller not available.
- Other non-2xx responses: report scan start failure with sanitized HTTP status.
- Network failure: report openHAB unreachable.
- Scan accepted but no Inbox entry: report scan started but no Matter Inbox entry detected yet.

## Security And Privacy

- Do not log or persist the temporary pairing code beyond existing UI state.
- Do not include the code in HTTP details because it appears in the query string.
- Do not include REST tokens in logs.
- Do not add the code to openHAB Items or channels in v1.
- Continue to store the openHAB REST API token through the existing encrypted app-private configuration path.

## Testing

Add focused unit tests around the new client and presentation output:

- Builds the scan URL with a URL-encoded `input` query parameter.
- Sends `POST`.
- Sends bearer token when configured.
- Parses a successful timeout response.
- Handles successful but unparsable timeout response.
- Converts 401/403, 404, other non-2xx, malformed URL, unsupported protocol, and IOException into status results.
- Sanitizes status details so they do not contain the manual code or token.
- MainActivity presentation text describes scan-started, scan-failed, Inbox-detected, and no-Inbox-entry outcomes without echoing secrets.

Integration verification should run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Hardware validation should confirm:

- OCW still fails closed when connectedhomeip is unavailable.
- OCW returns the manual code.
- The app auto-submits to openHAB only when openHAB URL is configured.
- openHAB Matter Inbox entry appears after scan.
- Logs do not expose the pairing code, REST token, Thread dataset, or QR payload.
