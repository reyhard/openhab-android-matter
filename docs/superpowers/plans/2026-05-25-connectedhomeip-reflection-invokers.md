# connectedhomeip Reflection Invokers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reflection invokers for connectedhomeip BLE Thread pairing and OpenCommissioningWindow callback bridging.

**Architecture:** Extend `ConnectedHomeIpReflectionCommandFactory` from method lookup/construction into safe command invocation. Keep Android BLE scanning and connected device pointer acquisition out of scope; this slice assumes the future gateway has already obtained `BluetoothGatt`, connection id, controller instance, and device pointer.

**Tech Stack:** Java 17, Android JVM unit tests, dynamic proxy for connectedhomeip `OpenCommissioningCallback`.

---

### Task 1: Pairing Invocation

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactory.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactoryTest.java`

- [ ] **Step 1: Write failing invocation test**

Add to `ConnectedHomeIpReflectionCommandFactoryTest`:

```java
@Test
public void invokesPairDeviceThroughBle() throws Exception {
    ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
    FakeChipDeviceController controller = new FakeChipDeviceController();
    Object params = new Object();

    factory.invokePairDeviceThroughBle(controller, null, 42, 987654321L, 20202021L, params);

    assertEquals(42, controller.connId);
    assertEquals(987654321L, controller.deviceId);
    assertEquals(20202021L, controller.setupPin);
    assertSame(params, controller.params);
}
```

- [ ] **Step 2: Run RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionCommandFactoryTest"
```

Expected: FAIL because `invokePairDeviceThroughBle(...)` does not exist.

- [ ] **Step 3: Implement pairing invoker**

Add:

```java
public void invokePairDeviceThroughBle(
        Object controller,
        BluetoothGatt bleServer,
        int connId,
        long deviceId,
        long setupPin,
        Object commissionParameters) throws ReflectiveOperationException
```

It calls `pairDeviceThroughBleMethod().invoke(...)`.

### Task 2: OCW Callback Bridge

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpOpenCommissioningWindowCallback.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactory.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactoryTest.java`
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write failing OCW success callback test**

In the fake callback interface, add:

```java
void onError(int status, long deviceId);
void onSuccess(long deviceId, String manualPairingCode, String qrCode);
```

Add test:

```java
@Test
public void invokesOpenPairingWindowAndReturnsManualCodeFromCallback() throws Exception {
    ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
    FakeChipDeviceController controller = new FakeChipDeviceController();
    ConnectedHomeIpOpenCommissioningWindowCallback callback =
            factory.newOpenCommissioningWindowCallback("controller-state");

    boolean started = factory.invokeOpenPairingWindowWithPinCallback(
            controller,
            1234L,
            new ConnectedHomeIpOpenCommissioningWindowRequest(987654321L, 300, 1000L, 3840, "controller-state"),
            null,
            callback.proxy());

    MatterOpenCommissioningWindowResult result = callback.awaitResult(1000);

    assertTrue(started);
    assertEquals(1234L, controller.devicePtr);
    assertEquals(300, controller.duration);
    assertEquals(1000L, controller.iteration);
    assertEquals(3840, controller.discriminator);
    assertEquals("3497-0112-332", result.temporaryCode());
    assertEquals("controller-state", result.controllerState());
}
```

- [ ] **Step 2: Write failing OCW error callback test**

Add test where fake controller calls `callback.onError(55, 987654321L)` and `awaitResult(1000)` throws `IllegalStateException` containing `55` and `987654321`.

- [ ] **Step 3: Run RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionCommandFactoryTest"
```

Expected: FAIL because callback bridge and invoker do not exist.

- [ ] **Step 4: Implement callback bridge**

Create `ConnectedHomeIpOpenCommissioningWindowCallback` with:

```java
public Object proxy()
public MatterOpenCommissioningWindowResult awaitResult(long timeoutMillis) throws InterruptedException
```

Use `Proxy.newProxyInstance(...)` for the callback interface. On `onSuccess`, prefer non-blank `manualPairingCode`; if blank, use `qrCode`. On `onError`, store an `IllegalStateException`. Use `CountDownLatch`.

- [ ] **Step 5: Implement OCW invoker**

Add to factory:

```java
public ConnectedHomeIpOpenCommissioningWindowCallback newOpenCommissioningWindowCallback(String controllerState)
public boolean invokeOpenPairingWindowWithPinCallback(
        Object controller,
        long devicePtr,
        ConnectedHomeIpOpenCommissioningWindowRequest request,
        Long setupPinCode,
        Object callbackProxy) throws ReflectiveOperationException
```

It calls `openPairingWindowWithPinCallbackMethod().invoke(...)` and returns the boolean result.

- [ ] **Step 6: Run GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionCommandFactoryTest"
```

Expected: PASS.

- [ ] **Step 7: Update docs**

Document that reflected invocation and OCW callback bridging exist. Keep BLE scanning/GATT connection id registration, pairing completion listener, attestation delegate, and connected device pointer acquisition as remaining work.

- [ ] **Step 8: Full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/controller docs/chip-jni-integration.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-connectedhomeip-reflection-invokers.md
git commit -m "feat: invoke connectedhomeip commands by reflection"
```

---

## Self-Review

- Spec coverage: This advances the real connectedhomeip gateway by making the reflected `pairDeviceThroughBLE(...)` and `openPairingWindowWithPINCallback(...)` APIs callable and testable.
- Placeholder scan: No placeholders remain; excluded BLE and pointer acquisition responsibilities are explicit.
- Type consistency: Method names and request types match `ConnectedHomeIpReflectionCommandFactory`, `ConnectedHomeIpOpenCommissioningWindowRequest`, and existing result classes.
