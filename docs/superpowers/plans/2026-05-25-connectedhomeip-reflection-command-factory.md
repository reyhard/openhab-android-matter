# connectedhomeip Reflection Command Factory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the reflection-based command factory needed for a future Android gateway to construct connectedhomeip Thread credentials and commissioning parameters without compile-time CHIP dependencies.

**Architecture:** Extend `ThreadDataset` with byte conversion so connectedhomeip receives the operational dataset as `byte[]`. Add `ConnectedHomeIpReflectionCommandFactory`, a small reflection utility that creates `NetworkCredentials.ThreadCredentials`, calls `NetworkCredentials.forThread(...)`, builds `CommissionParameters`, and exposes method handles for `pairDeviceThroughBLE(...)` and `openPairingWindowWithPINCallback(...)`.

**Tech Stack:** Java 17, Android JVM unit tests, reflection over connectedhomeip Java artifacts.

---

### Task 1: Thread Dataset Bytes

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/domain/ThreadDataset.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/domain/ThreadDatasetTest.java`

- [ ] **Step 1: Write failing byte conversion test**

Add to `ThreadDatasetTest`:

```java
@Test
public void convertsNormalizedHexToBytes() {
    ThreadDataset dataset = ThreadDataset.parse("hex:0E08ff10");
    assertArrayEquals(new byte[] {
            0x0E,
            0x08,
            (byte) 0xFF,
            0x10
    }, dataset.bytes());
}
```

- [ ] **Step 2: Run RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ThreadDatasetTest"
```

Expected: FAIL because `ThreadDataset.bytes()` does not exist.

- [ ] **Step 3: Implement `bytes()`**

Add:

```java
public byte[] bytes() {
    byte[] bytes = new byte[hex.length() / 2];
    for (int index = 0; index < bytes.length; index++) {
        int start = index * 2;
        bytes[index] = (byte) Integer.parseInt(hex.substring(start, start + 2), 16);
    }
    return bytes;
}
```

- [ ] **Step 4: Run GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ThreadDatasetTest"
```

Expected: PASS.

### Task 2: Reflection Command Factory

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactory.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactoryTest.java`
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write failing reflection tests**

Create in test package fake nested connectedhomeip-compatible classes inside `ConnectedHomeIpReflectionCommandFactoryTest`:

```java
public static final class FakeNetworkCredentials {
    static ThreadCredentials lastThreadCredentials;

    public static Object forThread(ThreadCredentials threadCredentials) {
        lastThreadCredentials = threadCredentials;
        return "network:" + threadCredentials.operationalDataset.length;
    }

    public static final class ThreadCredentials {
        private final byte[] operationalDataset;

        public ThreadCredentials(byte[] operationalDataset) {
            this.operationalDataset = operationalDataset;
        }
    }
}
```

Add fake `CommissionParameters.Builder` that records calls and returns a built object.

Test:

```java
ConnectedHomeIpReflectionCommandFactory factory = new ConnectedHomeIpReflectionCommandFactory(
        FakeNetworkCredentials.class,
        FakeNetworkCredentials.ThreadCredentials.class,
        FakeCommissionParameters.Builder.class,
        FakeChipDeviceController.class);

Object params = factory.newThreadCommissionParameters(ThreadDataset.parse("hex:0E08FF10"));

assertArrayEquals(new byte[] {0x0E, 0x08, (byte) 0xFF, 0x10},
        FakeNetworkCredentials.lastThreadCredentials.operationalDataset);
assertSame(FakeCommissionParameters.BUILT, params);
assertNull(FakeCommissionParameters.lastBuilder.csrNonce);
assertEquals("network:4", FakeCommissionParameters.lastBuilder.networkCredentials);
assertNull(FakeCommissionParameters.lastBuilder.icdRegistrationInfo);
```

Add a second test verifying method lookup:

```java
assertEquals("pairDeviceThroughBLE", factory.pairDeviceThroughBleMethod().getName());
assertEquals("openPairingWindowWithPINCallback", factory.openPairingWindowWithPinCallbackMethod().getName());
```

- [ ] **Step 2: Run RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionCommandFactoryTest"
```

Expected: FAIL because `ConnectedHomeIpReflectionCommandFactory` does not exist.

- [ ] **Step 3: Implement command factory**

Create constructor:

```java
public ConnectedHomeIpReflectionCommandFactory(
        Class<?> networkCredentialsClass,
        Class<?> threadCredentialsClass,
        Class<?> commissionParametersBuilderClass,
        Class<?> chipDeviceControllerClass)
```

Create default factory:

```java
public static ConnectedHomeIpReflectionCommandFactory fromDefaultClassLoader()
```

It must use:

```text
chip.devicecontroller.NetworkCredentials
chip.devicecontroller.NetworkCredentials$ThreadCredentials
chip.devicecontroller.CommissionParameters$Builder
chip.devicecontroller.ChipDeviceController
```

Implement `newThreadCommissionParameters(ThreadDataset dataset)` through reflection.

Implement method lookups for:

```text
pairDeviceThroughBLE(android.bluetooth.BluetoothGatt, int, long, long, CommissionParameters)
openPairingWindowWithPINCallback(long, int, long, int, Long, OpenCommissioningCallback)
```

The OCW callback class should be loaded by name from the controller class loader.

- [ ] **Step 4: Run GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionCommandFactoryTest" --tests "*ThreadDatasetTest"
```

Expected: PASS.

- [ ] **Step 5: Update docs**

Document that Thread dataset bytes and reflection construction for `NetworkCredentials.forThread(...)` plus `CommissionParameters.Builder` exist, but BLE scanning/GATT connection and async callback bridging are still not implemented.

- [ ] **Step 6: Full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/domain/ThreadDataset.java app/src/test/java/org/openhab/matter/companion/domain/ThreadDatasetTest.java app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactory.java app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactoryTest.java docs/chip-jni-integration.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-connectedhomeip-reflection-command-factory.md
git commit -m "feat: build connectedhomeip commands by reflection"
```

---

## Self-Review

- Spec coverage: This advances the concrete Java gateway by implementing the data conversion and reflection construction needed before invoking connectedhomeip commissioning APIs.
- Placeholder scan: No placeholder language remains; BLE scanning and callback bridging are explicitly outside this slice and documented as remaining work.
- Type consistency: `ThreadDataset.bytes()`, `ConnectedHomeIpReflectionCommandFactory`, and connectedhomeip class names align with the local connectedhomeip API evidence.
