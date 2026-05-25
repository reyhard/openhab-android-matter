# ConnectedHomeIp Commissioning Monitor Installation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure the connectedhomeip reflection gateway registers the CHIP completion listener before starting BLE commissioning.

**Architecture:** Keep the completion listener as the callback/await primitive, and add a small reflection monitor wrapper that installs it on the `ChipDeviceController`. The gateway depends only on `ConnectedHomeIpCommissioningMonitor`, which now has an explicit preparation step before BLE pairing.

**Tech Stack:** Android Java, JUnit4/Robolectric unit tests, connectedhomeip Java controller invoked through reflection.

---

### Task 1: Gateway Preparation Hook

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpCommissioningMonitor.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionGateway.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionGatewayTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void commissionBleThreadPreparesMonitorBeforePairingCommand() throws Exception {
    FakeChipDeviceController controller = new FakeChipDeviceController();
    CapturingCommissioningMonitor monitor = new CapturingCommissioningMonitor();
    ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
            () -> controller,
            new CapturingBleConnectionProvider(),
            () -> 987654321L,
            monitor,
            unusedAttestationHandler(),
            unusedPointerProvider(),
            fakeCommandFactory(),
            1000L);

    gateway.commissionBleThread(new ConnectedHomeIpCommissioningRequest(
            "0E080000000000010000",
            20202021L,
            3840,
            false,
            "controller-state"));

    assertSame(controller, monitor.preparedController);
    assertTrue(controller.pairedAfterMonitorPrepared);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionGatewayTest"`

Expected: FAIL because `ConnectedHomeIpCommissioningMonitor` has no `prepare` hook and the gateway cannot mark the monitor prepared before pairing.

- [ ] **Step 3: Write minimal implementation**

```java
public interface ConnectedHomeIpCommissioningMonitor {
    void prepare(Object controller) throws Exception;

    MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) throws Exception;
}
```

Call `commissioningMonitor.prepare(controller)` in `ConnectedHomeIpReflectionGateway.commissionBleThread` after attestation setup and before opening the BLE connection/pairing command.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionGatewayTest"`

Expected: PASS.

### Task 2: Reflection Monitor Wrapper

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommissioningMonitor.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerCallbackReflectionTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void reflectionCommissioningMonitorRegistersListenerAndAwaitsCompletion() throws Exception {
    ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
    FakeChipDeviceController controller = new FakeChipDeviceController();
    ConnectedHomeIpReflectionCommissioningMonitor monitor =
            new ConnectedHomeIpReflectionCommissioningMonitor(factory, 1000L);

    monitor.prepare(controller);
    controller.completionListener.onCommissioningComplete(987654321L, 0L);

    MatterCommissioningResult result = monitor.awaitCommissioned(987654321L, "controller-state");

    assertEquals(987654321L, result.nodeId());
    assertEquals("controller-state", result.controllerState());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpControllerCallbackReflectionTest"`

Expected: FAIL because `ConnectedHomeIpReflectionCommissioningMonitor` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
public final class ConnectedHomeIpReflectionCommissioningMonitor implements ConnectedHomeIpCommissioningMonitor {
    private final ConnectedHomeIpReflectionCommandFactory commandFactory;
    private final long timeoutMillis;
    private ConnectedHomeIpCommissioningCompletionListener listener;

    public ConnectedHomeIpReflectionCommissioningMonitor(ConnectedHomeIpReflectionCommandFactory commandFactory) {
        this(commandFactory, ConnectedHomeIpCommissioningCompletionListener.DEFAULT_TIMEOUT_MILLIS);
    }

    public ConnectedHomeIpReflectionCommissioningMonitor(
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            long timeoutMillis) {
        this.commandFactory = require(commandFactory, "commandFactory");
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void prepare(Object controller) throws Exception {
        listener = commandFactory.newCommissioningCompletionListener(timeoutMillis);
        commandFactory.invokeSetCompletionListener(controller, listener.proxy());
    }

    @Override
    public MatterCommissioningResult awaitCommissioned(long nodeId, String controllerState) throws Exception {
        if (listener == null) {
            throw new IllegalStateException("Commissioning listener has not been prepared");
        }
        return listener.awaitCommissioned(nodeId, controllerState);
    }
}
```

Expose a package-visible timeout constructor on `ConnectedHomeIpCommissioningCompletionListener` if needed by the wrapper test.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpControllerCallbackReflectionTest"`

Expected: PASS.

### Task 3: Verification and Commit

**Files:**
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Update documentation**

Record that the reflection commissioning monitor now installs the connectedhomeip completion listener before BLE pairing.

- [ ] **Step 2: Run full offline verification**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/controller docs
git commit -m "feat: install connectedhomeip commissioning listener"
```
