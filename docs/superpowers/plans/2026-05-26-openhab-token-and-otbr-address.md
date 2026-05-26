# openHAB Token And OTBR Address Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users configure an openHAB REST API token and make OTBR diagnostics accept a plain OTBR IP/host address instead of requiring an HTTP service URL.

**Architecture:** Store the openHAB token with the same encrypted app-private config path used for the Thread dataset. Apply `Authorization: Bearer <token>` to all openHAB REST and SSE clients when configured, matching the Windows app's local-token pattern. Keep optional HTTP URL diagnostics for OTBR endpoints, but treat plain host/IP input as an accepted OTBR network address because a pure OpenThread Border Router is not expected to host an HTTP service or answer echo probes.

**Tech Stack:** Java 17, Android SDK, `HttpURLConnection`, Android Keystore-backed `SecretCodec`, JUnit 4.

---

## Tasks

### Task 1: Persist openHAB REST API Token Securely

- [ ] Add `openHabApiToken` and unreadable-token state to `AppConfig`.
- [ ] Encrypt the token in `SecureAppConfigMapper`.
- [ ] Add a `SharedPreferencesAppConfigRepository` key for the encrypted token.
- [ ] Extend config tests for save/load, unreadable token fail-closed behavior, and legacy constructor compatibility.

### Task 2: Apply Bearer Token To openHAB REST Calls

- [ ] Add token-aware overloads to openHAB readiness, Inbox, and SSE clients.
- [ ] Set `Authorization: Bearer <token>` only when the configured token is non-blank.
- [ ] Add HTTP server tests proving auth headers are sent for `/rest/`, `/rest/things`, `/rest/inbox`, and `/rest/events`.
- [ ] Add tests proving token values are not repeated in failure details.

### Task 3: Add Token UI And Logging

- [ ] Add an openHAB token input field to `MainActivity`.
- [ ] Save and reload the token from encrypted app config without printing it or copying it to unencrypted activity instance state.
- [ ] Pass the token into openHAB readiness, Inbox, and SSE calls.
- [ ] Update presentation copy and tests so logs say whether a token is configured without exposing it.

### Task 4: Fix OTBR Address Diagnostics

- [ ] Keep HTTP/HTTPS URL behavior for users who run an OTBR diagnostic service.
- [ ] Accept plain IPv4, bracketed IPv6, unbracketed IPv6, and host names.
- [ ] Validate and resolve plain addresses without failing pure OTBRs that do not answer HTTP or echo probes.
- [ ] Update UI copy from "base URL" to "address or diagnostic URL".
- [ ] Add tests for plain IPv4 loopback, bracketed IPv6 normalization, and unsupported non-HTTP URL schemes.

### Task 5: Verify And Commit

- [ ] Run targeted config/openHAB/OTBR/presentation tests.
- [ ] Run `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline`.
- [ ] Run install helper preflight.
- [ ] Run `git diff --check`.
- [ ] Commit with message `feat: add openhab token auth and otbr address checks`.
