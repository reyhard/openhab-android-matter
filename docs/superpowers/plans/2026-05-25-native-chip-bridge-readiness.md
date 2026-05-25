# Native CHIP Bridge Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an honest JNI bridge metadata gate so the APK can package a native `openhab_matter_chip` library while selecting the native Matter controller only when that library declares itself as a production connectedhomeip implementation.

**Architecture:** Split native loading/calls behind a `NativeChipBridge` interface so JVM tests can verify readiness without invoking real JNI. Add a small CMake-built JNI stub that exports metadata and throws for commissioning calls; the stub proves packaging but reports `production=false`, so `MatterControllerSelector` keeps the simulated controller. Real connectedhomeip work later replaces the stub metadata and native implementations.

**Tech Stack:** Java 17, Android Gradle Plugin 8.11.1, Android NDK/CMake via SDK CMake 3.22.1, JNI C++.

---

## File Structure

- Modify: `app/build.gradle` - enable CMake external native build for the debug APK.
- Create: `app/src/main/cpp/CMakeLists.txt` - build `libopenhab_matter_chip.so`.
- Create: `app/src/main/cpp/openhab_matter_chip_stub.cpp` - JNI stub metadata and explicit unsupported operations.
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeChipBridge.java` - testable native bridge interface.
- Create: `app/src/main/java/org/openhab/matter/companion/controller/SystemNativeChipBridge.java` - production JNI-backed bridge implementation.
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeChipBridgeMetadata.java` - parse and validate bridge metadata.
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java` - use bridge metadata for readiness and native calls.
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterControllerStatus.java` - include bridge kind and production flag.
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ChipMatterControllerReadinessTest.java` - TDD coverage for missing, stub, production, and missing entry points.
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java` - verify stub metadata does not select native.
- Modify: `README.md`, `docs/implementation-status.md`, `docs/chip-jni-integration.md` - document the metadata contract and that the bundled stub is not real Matter commissioning.

---

### Task 1: Java Bridge Metadata Contract

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeChipBridge.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/SystemNativeChipBridge.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeChipBridgeMetadata.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterControllerStatus.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ChipMatterControllerReadinessTest.java`

- [ ] **Step 1: Write failing metadata parser tests**

Add tests to `ChipMatterControllerReadinessTest`:

```java
@Test
public void reportsStubBridgeAsNotReadyEvenWhenLibraryLoads() {
    NativeChipBridge bridge = new NativeChipBridge() {
        @Override
        public void load(String libraryName) {
        }

        @Override
        public String metadata() {
            return "kind=stub;version=0.1.0;production=false;message=JNI stub only";
        }

        @Override
        public long commissionBleThread(String datasetHex, long pin, int discriminator) {
            throw new AssertionError("stub commissioning must not be called");
        }

        @Override
        public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator) {
            throw new AssertionError("stub OCW must not be called");
        }
    };

    ChipMatterController controller = new ChipMatterController(bridge, ChipMatterControllerConfig.defaultConfig());
    ChipMatterControllerStatus status = controller.readiness();

    assertFalse(status.ready());
    assertEquals("stub", status.bridgeKind());
    assertFalse(status.productionReady());
    assertTrue(status.message().contains("JNI stub only"));
}

@Test
public void reportsReadyOnlyForProductionConnectedhomeipBridge() {
    NativeChipBridge bridge = new NativeChipBridge() {
        @Override
        public void load(String libraryName) {
        }

        @Override
        public String metadata() {
            return "kind=connectedhomeip;version=2026.05;production=true;message=connectedhomeip controller ready";
        }

        @Override
        public long commissionBleThread(String datasetHex, long pin, int discriminator) {
            return 1234L;
        }

        @Override
        public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator) {
            return "MT:PRODUCTION";
        }
    };

    ChipMatterController controller = new ChipMatterController(bridge, new ChipMatterControllerConfig(
            "custom_chip",
            true));

    ChipMatterControllerStatus status = controller.readiness();

    assertTrue(status.ready());
    assertEquals("connectedhomeip", status.bridgeKind());
    assertTrue(status.productionReady());
    assertEquals("custom_chip", status.libraryName());
    assertTrue(status.attestationBypassEnabled());
}
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: compile fails because `NativeChipBridge`, `bridgeKind()`, and `productionReady()` do not exist yet.

- [ ] **Step 3: Add bridge and metadata implementation**

Create `NativeChipBridge.java`:

```java
package org.openhab.matter.companion.controller;

public interface NativeChipBridge {
    void load(String libraryName);

    String metadata();

    long commissionBleThread(String datasetHex, long pin, int discriminator);

    String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
}
```

Create `SystemNativeChipBridge.java`:

```java
package org.openhab.matter.companion.controller;

public final class SystemNativeChipBridge implements NativeChipBridge {
    @Override
    public void load(String libraryName) {
        System.loadLibrary(libraryName);
    }

    @Override
    public String metadata() {
        return nativeControllerMetadata();
    }

    @Override
    public long commissionBleThread(String datasetHex, long pin, int discriminator) {
        return nativeCommissionBleThread(datasetHex, pin, discriminator);
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator) {
        return nativeOpenCommissioningWindow(nodeId, timeoutSeconds, discriminator);
    }

    private static native String nativeControllerMetadata();

    private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);

    private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
}
```

Create `NativeChipBridgeMetadata.java`:

```java
package org.openhab.matter.companion.controller;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeChipBridgeMetadata {
    private final String kind;
    private final String version;
    private final boolean production;
    private final String message;

    private NativeChipBridgeMetadata(String kind, String version, boolean production, String message) {
        this.kind = kind == null ? "unknown" : kind;
        this.version = version == null ? "" : version;
        this.production = production;
        this.message = message == null ? "" : message;
    }

    public static NativeChipBridgeMetadata parse(String rawMetadata) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawMetadata != null) {
            String[] parts = rawMetadata.split(";");
            for (String part : parts) {
                int separator = part.indexOf('=');
                if (separator > 0) {
                    String key = part.substring(0, separator).trim();
                    String value = part.substring(separator + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        }
        return new NativeChipBridgeMetadata(
                values.get("kind"),
                values.get("version"),
                Boolean.parseBoolean(values.get("production")),
                values.get("message"));
    }

    public boolean productionReady() {
        return production && "connectedhomeip".equals(kind);
    }

    public String kind() {
        return kind;
    }

    public String version() {
        return version;
    }

    public String message() {
        return message;
    }
}
```

Modify `ChipMatterControllerStatus.java` constructor and accessors:

```java
private final String bridgeKind;
private final boolean productionReady;
```

Add the fields to the constructor after `attestationBypassEnabled`, default `bridgeKind` to `"unknown"`, and expose:

```java
public String bridgeKind() {
    return bridgeKind;
}

public boolean productionReady() {
    return productionReady;
}
```

Modify `ChipMatterController.java`:

```java
private final NativeChipBridge bridge;
```

Use `new SystemNativeChipBridge()` in the default constructor. Keep the old `NativeLibraryLoader` constructor as a compatibility overload by wrapping it in a `NativeChipBridge` that returns production metadata after load. Add a primary constructor accepting `NativeChipBridge`.

In `ensureReady()`, call `bridge.load(libraryName)`, then `NativeChipBridgeMetadata.parse(bridge.metadata())`. Set `ready` to `metadata.productionReady()`. If not ready, the message must include the metadata message and state that the bridge is not production connectedhomeip. In commissioning methods, call `bridge.commissionBleThread(...)` and `bridge.openCommissioningWindow(...)`.

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: all unit tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/controller/ChipMatterControllerReadinessTest.java
git commit -m "feat: gate native chip readiness on bridge metadata"
```

---

### Task 2: Selector Safety For Stub Bridges

**Files:**
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelector.java`

- [ ] **Step 1: Write failing selector test**

Add to `MatterControllerSelectorTest`:

```java
@Test
public void fallsBackWhenNativeBridgeIsOnlyStub() {
    MatterController fallback = new FakeMatterController();
    ChipMatterController nativeController = new ChipMatterController(new NativeChipBridge() {
        @Override
        public void load(String libraryName) {
        }

        @Override
        public String metadata() {
            return "kind=stub;version=0.1.0;production=false;message=packaging stub";
        }

        @Override
        public long commissionBleThread(String datasetHex, long pin, int discriminator) {
            throw new AssertionError("selector must not call commissioning");
        }

        @Override
        public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator) {
            throw new AssertionError("selector must not call OCW");
        }
    }, ChipMatterControllerConfig.defaultConfig());

    MatterControllerSelection selection = MatterControllerSelector.select(
            fallback,
            nativeController,
            true);

    assertSame(fallback, selection.controller());
    assertFalse(selection.nativeSelected());
    assertTrue(selection.message().contains("Native CHIP controller not ready"));
    assertTrue(selection.message().contains("stub"));
}
```

- [ ] **Step 2: Run test to verify RED or currently covered behavior**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected before Task 1 code: compile fail. After Task 1, this should pass if selector already delegates correctly to `status.ready()`. If it passes immediately after Task 1, keep the test because it guards against future stub selection regressions.

- [ ] **Step 3: Adjust selector message only if needed**

If the test fails only because the message omits the bridge kind, update the fallback message to include `status.bridgeKind()`:

```java
"Native CHIP controller not ready (" + status.bridgeKind() + "): " + status.message()
        + ". Continuing with simulated Matter controller."
```

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: all unit tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelector.java app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java
git commit -m "test: prevent selecting stub native chip bridge"
```

---

### Task 3: Package JNI Stub Library

**Files:**
- Modify: `app/build.gradle`
- Create: `app/src/main/cpp/CMakeLists.txt`
- Create: `app/src/main/cpp/openhab_matter_chip_stub.cpp`

- [ ] **Step 1: Add native packaging verification target**

No Java test can load Android JNI on the host JVM reliably. The required verification for this task is build output inspection after `assembleDebug`: the APK must contain at least one `lib/<abi>/libopenhab_matter_chip.so`, and Java readiness must still report stub/not production via unit tests.

- [ ] **Step 2: Enable CMake in Gradle**

Modify `app/build.gradle` under `android`:

```gradle
    externalNativeBuild {
        cmake {
            path file("src/main/cpp/CMakeLists.txt")
            version "3.22.1"
        }
    }
```

- [ ] **Step 3: Add CMake file**

Create `app/src/main/cpp/CMakeLists.txt`:

```cmake
cmake_minimum_required(VERSION 3.22.1)

project(openhab_matter_chip_stub LANGUAGES CXX)

add_library(openhab_matter_chip SHARED openhab_matter_chip_stub.cpp)

target_compile_features(openhab_matter_chip PRIVATE cxx_std_17)
```

- [ ] **Step 4: Add JNI stub implementation**

Create `app/src/main/cpp/openhab_matter_chip_stub.cpp`:

```cpp
#include <jni.h>

namespace {

void ThrowUnsupportedOperation(JNIEnv *env, const char *message) {
    jclass exceptionClass = env->FindClass("java/lang/UnsupportedOperationException");
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message);
    }
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_org_openhab_matter_companion_controller_SystemNativeChipBridge_nativeControllerMetadata(
        JNIEnv *env,
        jclass) {
    return env->NewStringUTF(
            "kind=stub;version=0.1.0;production=false;message=JNI bridge stub packaged without connectedhomeip controller");
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_openhab_matter_companion_controller_SystemNativeChipBridge_nativeCommissionBleThread(
        JNIEnv *env,
        jclass,
        jstring,
        jlong,
        jint) {
    ThrowUnsupportedOperation(env, "connectedhomeip commissioning is not implemented in the packaged JNI stub");
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_openhab_matter_companion_controller_SystemNativeChipBridge_nativeOpenCommissioningWindow(
        JNIEnv *env,
        jclass,
        jlong,
        jint,
        jint) {
    ThrowUnsupportedOperation(env, "connectedhomeip OpenCommissioningWindow is not implemented in the packaged JNI stub");
    return nullptr;
}
```

- [ ] **Step 5: Build and inspect APK**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: build succeeds and `app/build/outputs/apk/debug/app-debug.apk` exists.

Inspect:

```powershell
jar tf app\build\outputs\apk\debug\app-debug.apk | Select-String "libopenhab_matter_chip.so"
```

Expected: at least one ABI path, for example `lib/arm64-v8a/libopenhab_matter_chip.so`.

- [ ] **Step 6: Commit**

```powershell
git add app/build.gradle app/src/main/cpp
git commit -m "feat: package native chip bridge stub"
```

---

### Task 4: Documentation And Status

**Files:**
- Modify: `README.md`
- Modify: `docs/implementation-status.md`
- Modify: `docs/chip-jni-integration.md`

- [ ] **Step 1: Update README native controller wording**

In `README.md`, replace the native readiness bullet with:

```markdown
- Native CHIP controller readiness diagnostics load `libopenhab_matter_chip.so` and require bridge metadata that declares a production connectedhomeip implementation.
- A packaged JNI stub verifies Android native-library packaging, but it reports `production=false` and is never selected for real commissioning.
```

In `Real CHIP Controller Work`, add:

```markdown
The bundled `libopenhab_matter_chip.so` is currently a JNI packaging stub. A production replacement must return metadata like `kind=connectedhomeip;production=true;...` and implement real BLE Thread commissioning plus OpenCommissioningWindow before the selector will use it.
```

- [ ] **Step 2: Update implementation status**

In `docs/implementation-status.md`, add implemented bullets:

```markdown
- Native CHIP bridge metadata distinguishes missing, stub, and production connectedhomeip libraries.
- Debug APK packages a JNI stub `libopenhab_matter_chip.so` for native loading/metadata verification.
```

Keep these not-implemented bullets:

```markdown
- Real BLE scanning, PASE, attestation, Thread dataset provisioning, and OpenCommissioningWindow.
- Real Matter/Thread commissioning through the connectedhomeip Android JNI stack.
- Real connectedhomeip Matter fabric key persistence and restore; the encrypted bootstrap state repository is present, but the native layer does not yet emit or consume fabric material.
```

- [ ] **Step 3: Update JNI integration contract**

Replace `docs/chip-jni-integration.md` native functions section with:

```markdown
`SystemNativeChipBridge` loads `libopenhab_matter_chip.so` and requires these JNI entry points:

```java
private static native String nativeControllerMetadata();
private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);
private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
```

`nativeControllerMetadata()` must return semicolon-separated key/value metadata:

```text
kind=connectedhomeip;version=<build-or-sdk-version>;production=true;message=<operator-readable-status>
```

The Android app treats any bridge that is missing metadata, has `production=false`, or has `kind` other than `connectedhomeip` as not ready and continues with the simulated controller.
```

- [ ] **Step 4: Run docs/status search**

Run:

```powershell
rg -n "stub|production=false|connectedhomeip|libopenhab_matter_chip|Not Implemented" README.md docs
```

Expected: docs clearly state that the stub is not real commissioning and real connectedhomeip remains not implemented.

- [ ] **Step 5: Commit**

```powershell
git add README.md docs/implementation-status.md docs/chip-jni-integration.md
git commit -m "docs: document native chip bridge metadata gate"
```

---

### Task 5: Final Verification

**Files:**
- No source edits unless verification exposes a defect.

- [ ] **Step 1: Run full local verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Inspect native library packaging**

Run:

```powershell
jar tf app\build\outputs\apk\debug\app-debug.apk | Select-String "libopenhab_matter_chip.so"
```

Expected: at least one packaged native library path.

- [ ] **Step 3: Check device availability**

Run:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe devices
```

Expected in the current environment: no attached devices. If a device appears, install with:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

- [ ] **Step 4: Final status**

Summarize:

- Unit tests and debug APK build result.
- Native stub library packaging result.
- Device install result or lack of attached device.
- Remaining blockers: real connectedhomeip JNI implementation, BLE/PASE/CASE/Thread provisioning, attestation, OCW, and real fabric persistence.

---

## Self-Review

- Spec coverage: This plan advances the native CHIP integration seam by making native library presence observable, packageable, and safe. It does not claim real connectedhomeip commissioning.
- Placeholder scan: No `TBD`, generic TODOs, or missing command details remain.
- Type consistency: `NativeChipBridge`, `SystemNativeChipBridge`, `NativeChipBridgeMetadata`, `ChipMatterControllerStatus.bridgeKind()`, and `productionReady()` are used consistently across tasks.
