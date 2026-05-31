# ConnectedHomeIp Attestation Trust Store Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add production-grade Matter device attestation trust-store support by loading trusted PAA certificates (and optional CD trust keys) and wiring them into connectedhomeip before BLE Thread commissioning.

**Architecture:** Keep the existing fail-closed native readiness and commissioning flow, but add a new trust-store seam that is configured once at controller creation and applied before attestation continuation. The implementation uses reflection-friendly adapters to avoid compile-time CHIP dependencies, loads trust material from APK assets populated from a local external directory at build time, and keeps developer bypass behavior unchanged.

**Tech Stack:** Android Java, Gradle (Groovy), connectedhomeip Java reflection seams, JUnit4 unit tests, Android AssetManager.

---

## File Structure

- `app/build.gradle` - add optional PAA/CD trust-store packaging properties, validation, and generated assets wiring.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifacts.java` - extend required class checks for attestation trust-store APIs.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStore.java` - immutable in-memory trust material holder.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreLoader.java` - load DER/PEM assets into byte arrays.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreDelegate.java` - dynamic proxy for `chip.devicecontroller.AttestationTrustStoreDelegate`.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactory.java` - construct/invoke trust-store delegate and `setAttestationTrustStoreDelegate(...)` overloads via reflection.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationHandler.java` - expand seam to include trust-store preparation.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionAttestationHandler.java` - set trust-store delegate before device-attestation delegate.
- `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactory.java` - create default trust-store loader and inject into attestation handler.
- `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifactsTest.java` - verify new required classes.
- `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreLoaderTest.java` - loader coverage for PEM/DER and missing assets.
- `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreDelegateTest.java` - SKID-based PAA lookup behavior.
- `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactoryTest.java` - reflection method coverage for trust-store APIs.
- `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionAttestationHandlerTest.java` - verify setup ordering and bypass invariants.
- `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactoryTest.java` - ensure default gateway wiring still works with trust-store seam.
- `docs/chip-jni-integration.md` - document trust-store delegate and source of PAA/CD assets.
- `docs/implementation-status.md` - record implemented trust-store verification support and constraints.

---

### Task 1: Add Build-Time Trust-Store Packaging Contract

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifactsTest.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifacts.java`

- [ ] **Step 1: Write failing artifact-contract test for trust-store classes**

In `ConnectedHomeIpControllerArtifactsTest`, update expected class list in `checksExactlyTheRequiredConnectedHomeIpControllerClasses`:

```java
assertEquals(Arrays.asList(
        "chip.devicecontroller.ChipDeviceController",
        "chip.devicecontroller.ControllerParams",
        "chip.devicecontroller.NetworkCredentials",
        "chip.devicecontroller.NetworkCredentials$ThreadCredentials",
        "chip.devicecontroller.CommissionParameters",
        "chip.devicecontroller.CommissionParameters$Builder",
        "chip.devicecontroller.ChipDeviceController$CompletionListener",
        "chip.devicecontroller.DeviceAttestationDelegate",
        "chip.devicecontroller.AttestationTrustStoreDelegate",
        "chip.devicecontroller.DeviceAttestation",
        "chip.devicecontroller.OpenCommissioningCallback",
        "chip.devicecontroller.GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback",
        "chip.platform.AndroidChipPlatform",
        "chip.platform.AndroidBleManager",
        "chip.platform.AndroidNfcCommissioningManager",
        "chip.platform.PreferencesKeyValueStoreManager",
        "chip.platform.PreferencesConfigurationManager",
        "chip.platform.NsdManagerServiceResolver",
        "chip.platform.NsdManagerServiceBrowser",
        "chip.platform.ChipMdnsCallbackImpl",
        "chip.platform.DiagnosticDataProviderImpl",
        "chip.platform.BleCallback"), checkedClassNames);
```

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpControllerArtifactsTest"
```

Expected: FAIL because `ConnectedHomeIpControllerArtifacts.REQUIRED_CLASS_NAMES` does not yet include trust-store related classes.

- [ ] **Step 3: Implement artifact readiness and Gradle packaging checks**

In `ConnectedHomeIpControllerArtifacts.java`, add required classes:

```java
private static final String[] REQUIRED_CLASS_NAMES = {
        "chip.devicecontroller.ChipDeviceController",
        "chip.devicecontroller.ControllerParams",
        "chip.devicecontroller.NetworkCredentials",
        "chip.devicecontroller.NetworkCredentials$ThreadCredentials",
        "chip.devicecontroller.CommissionParameters",
        "chip.devicecontroller.CommissionParameters$Builder",
        "chip.devicecontroller.ChipDeviceController$CompletionListener",
        "chip.devicecontroller.DeviceAttestationDelegate",
        "chip.devicecontroller.AttestationTrustStoreDelegate",
        "chip.devicecontroller.DeviceAttestation",
        "chip.devicecontroller.OpenCommissioningCallback",
        "chip.devicecontroller.GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback",
        "chip.platform.AndroidChipPlatform",
        "chip.platform.AndroidBleManager",
        "chip.platform.AndroidNfcCommissioningManager",
        "chip.platform.PreferencesKeyValueStoreManager",
        "chip.platform.PreferencesConfigurationManager",
        "chip.platform.NsdManagerServiceResolver",
        "chip.platform.NsdManagerServiceBrowser",
        "chip.platform.ChipMdnsCallbackImpl",
        "chip.platform.DiagnosticDataProviderImpl",
        "chip.platform.BleCallback"
};
```

In `app/build.gradle`, extend jar-class validation and add optional trust-store asset inputs:

```groovy
def chipPaaTrustStoreDir = (findProperty("openhabMatterChipPaaTrustStoreDir") ?: "").toString().trim()
def chipCdTrustStoreDir = (findProperty("openhabMatterChipCdTrustStoreDir") ?: "").toString().trim()

def chipControllerRequiredClassEntries = [
        "chip/devicecontroller/ChipDeviceController.class",
        "chip/devicecontroller/ControllerParams.class",
        "chip/devicecontroller/NetworkCredentials.class",
        'chip/devicecontroller/NetworkCredentials$ThreadCredentials.class',
        "chip/devicecontroller/CommissionParameters.class",
        'chip/devicecontroller/CommissionParameters$Builder.class',
        'chip/devicecontroller/ChipDeviceController$CompletionListener.class',
        "chip/devicecontroller/DeviceAttestationDelegate.class",
        "chip/devicecontroller/AttestationTrustStoreDelegate.class",
        "chip/devicecontroller/DeviceAttestation.class",
        "chip/devicecontroller/OpenCommissioningCallback.class",
        'chip/devicecontroller/GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback.class',
        "chip/platform/AndroidChipPlatform.class",
        "chip/platform/AndroidBleManager.class",
        "chip/platform/AndroidNfcCommissioningManager.class",
        "chip/platform/PreferencesKeyValueStoreManager.class",
        "chip/platform/PreferencesConfigurationManager.class",
        "chip/platform/NsdManagerServiceResolver.class",
        "chip/platform/NsdManagerServiceBrowser.class",
        "chip/platform/ChipMdnsCallbackImpl.class",
        "chip/platform/DiagnosticDataProviderImpl.class",
        "chip/platform/BleCallback.class"
]

def generatedTrustStoreAssetsDir = layout.buildDirectory.dir("generated/openhabMatter/truststoreAssets")

android {
    if (!chipControllerArtifactsDir.isEmpty()) {
        sourceSets {
            main {
                assets.srcDir generatedTrustStoreAssetsDir
            }
        }
    }
}

tasks.register("prepareConnectedHomeIpTrustStoreAssets") {
    outputs.dir(generatedTrustStoreAssetsDir)
    doLast {
        def outRoot = generatedTrustStoreAssetsDir.get().asFile
        delete(outRoot)
        if (chipPaaTrustStoreDir.isEmpty() && chipCdTrustStoreDir.isEmpty()) {
            return
        }
        outRoot.mkdirs()

        if (!chipPaaTrustStoreDir.isEmpty()) {
            File paaRoot = file(chipPaaTrustStoreDir)
            if (!paaRoot.isDirectory()) {
                throw new GradleException("PAA trust store directory does not exist: " + paaRoot)
            }
            copy {
                from paaRoot
                include "**/*.pem", "**/*.der"
                into new File(outRoot, "matter/truststore/paa")
            }
        }

        if (!chipCdTrustStoreDir.isEmpty()) {
            File cdRoot = file(chipCdTrustStoreDir)
            if (!cdRoot.isDirectory()) {
                throw new GradleException("CD trust store directory does not exist: " + cdRoot)
            }
            copy {
                from cdRoot
                include "**/*.pem", "**/*.der"
                into new File(outRoot, "matter/truststore/cd")
            }
        }
    }
}

preBuild.dependsOn("prepareConnectedHomeIpTrustStoreAssets")
```

- [ ] **Step 4: Run tests and Gradle validation to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpControllerArtifactsTest"
.\gradlew.bat :app:verifyConnectedHomeIpControllerArtifacts --offline "-PopenhabMatterChipControllerArtifactsDir=<artifact-dir>"
```

Expected: controller-artifact tests pass; artifact verification task passes with a valid artifact directory.

- [ ] **Step 5: Commit**

```powershell
git add app/build.gradle app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifacts.java app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifactsTest.java
git commit -m "build: validate trust-store APIs and package trust-store assets"
```

---

### Task 2: Implement Local Trust-Store Model and Asset Loader

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStore.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreLoader.java`
- Create: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreLoaderTest.java`

- [ ] **Step 1: Write failing loader tests (PEM, DER, empty directory)**

Create `ConnectedHomeIpAttestationTrustStoreLoaderTest.java`:

```java
package org.openhab.matter.companion.controller;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpAttestationTrustStoreLoaderTest {
    @Test
    public void loadsPemAndDerCertificatesFromAssets() throws Exception {
        FakeAssets assets = new FakeAssets();
        assets.add("matter/truststore/paa", "vendorA.pem", pem("AQID"));
        assets.add("matter/truststore/paa", "vendorB.der", new byte[] {4, 5, 6});

        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStoreLoader(assets::list, assets::open)
                        .load("matter/truststore/paa", "matter/truststore/cd");

        assertEquals(2, store.paaCertificates().size());
        assertEquals(0, store.cdTrustKeys().size());
    }

    @Test
    public void loadsCdTrustKeysWhenPresent() throws Exception {
        FakeAssets assets = new FakeAssets();
        assets.add("matter/truststore/cd", "cd1.der", new byte[] {9, 8, 7});

        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStoreLoader(assets::list, assets::open)
                        .load("matter/truststore/paa", "matter/truststore/cd");

        assertEquals(0, store.paaCertificates().size());
        assertEquals(1, store.cdTrustKeys().size());
    }

    @Test
    public void missingAssetDirectoriesReturnEmptyStore() throws Exception {
        FakeAssets assets = new FakeAssets();

        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStoreLoader(assets::list, assets::open)
                        .load("matter/truststore/paa", "matter/truststore/cd");

        assertTrue(store.paaCertificates().isEmpty());
        assertTrue(store.cdTrustKeys().isEmpty());
    }

    private static byte[] pem(String base64Body) {
        String text = "-----BEGIN CERTIFICATE-----\n" + base64Body + "\n-----END CERTIFICATE-----\n";
        return text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static final class FakeAssets {
        private final Map<String, Map<String, byte[]>> entries = new HashMap<>();

        void add(String dir, String name, byte[] value) {
            entries.computeIfAbsent(dir, ignored -> new HashMap<>()).put(name, value);
        }

        String[] list(String dir) {
            Map<String, byte[]> files = entries.get(dir);
            return files == null ? new String[0] : files.keySet().toArray(new String[0]);
        }

        InputStream open(String path) throws IOException {
            int slash = path.lastIndexOf('/');
            String dir = slash < 0 ? "" : path.substring(0, slash);
            String name = slash < 0 ? path : path.substring(slash + 1);
            Map<String, byte[]> files = entries.get(dir);
            if (files == null || !files.containsKey(name)) {
                throw new IOException("missing " + path);
            }
            return new ByteArrayInputStream(files.get(name));
        }
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpAttestationTrustStoreLoaderTest"
```

Expected: FAIL because loader and trust-store classes do not exist.

- [ ] **Step 3: Implement immutable trust-store model and loader**

Create `ConnectedHomeIpAttestationTrustStore.java`:

```java
package org.openhab.matter.companion.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConnectedHomeIpAttestationTrustStore {
    private final List<byte[]> paaCertificates;
    private final List<byte[]> cdTrustKeys;

    public ConnectedHomeIpAttestationTrustStore(List<byte[]> paaCertificates, List<byte[]> cdTrustKeys) {
        this.paaCertificates = copy(paaCertificates);
        this.cdTrustKeys = copy(cdTrustKeys);
    }

    public List<byte[]> paaCertificates() {
        return paaCertificates;
    }

    public List<byte[]> cdTrustKeys() {
        return cdTrustKeys;
    }

    private static List<byte[]> copy(List<byte[]> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<byte[]> output = new ArrayList<>(values.size());
        for (byte[] value : values) {
            if (value == null || value.length == 0) {
                continue;
            }
            output.add(value.clone());
        }
        return Collections.unmodifiableList(output);
    }
}
```

Create `ConnectedHomeIpAttestationTrustStoreLoader.java`:

```java
package org.openhab.matter.companion.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ConnectedHomeIpAttestationTrustStoreLoader {
    public interface AssetLister {
        String[] list(String path) throws IOException;
    }

    public interface AssetOpener {
        InputStream open(String path) throws IOException;
    }

    private final AssetLister listAssets;
    private final AssetOpener openAsset;

    public ConnectedHomeIpAttestationTrustStoreLoader(AssetLister listAssets, AssetOpener openAsset) {
        this.listAssets = listAssets;
        this.openAsset = openAsset;
    }

    public ConnectedHomeIpAttestationTrustStore load(String paaPath, String cdPath) {
        return new ConnectedHomeIpAttestationTrustStore(
                readCertificates(paaPath),
                readCertificates(cdPath));
    }

    private List<byte[]> readCertificates(String rootPath) {
        List<byte[]> certificates = new ArrayList<>();
        if (rootPath == null || rootPath.isEmpty()) {
            return certificates;
        }
        try {
            String[] names = listAssets.list(rootPath);
            if (names == null) {
                return certificates;
            }
            for (String name : names) {
                if (name == null || name.isEmpty()) {
                    continue;
                }
                String fullPath = rootPath + "/" + name;
                try (InputStream input = openAsset.open(fullPath)) {
                    byte[] raw = readFully(input);
                    byte[] parsed = parseCertificate(raw);
                    if (parsed.length > 0) {
                        certificates.add(parsed);
                    }
                }
            }
        } catch (IOException exception) {
            ConnectedHomeIpDiagnostics.emit("Attestation trust-store asset path unavailable: " + rootPath);
        }
        return certificates;
    }

    private static byte[] parseCertificate(byte[] value) {
        String text = new String(value, StandardCharsets.US_ASCII);
        if (text.contains("-----BEGIN CERTIFICATE-----")) {
            String stripped = text
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");
            return Base64.getDecoder().decode(stripped);
        }
        return value;
    }

    private static byte[] readFully(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (true) {
            int read = input.read(buffer);
            if (read < 0) {
                return out.toByteArray();
            }
            out.write(buffer, 0, read);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpAttestationTrustStoreLoaderTest"
```

Expected: test class passes with PEM/DER parsing and missing-path behavior.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStore.java app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreLoader.java app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreLoaderTest.java
git commit -m "feat: add local attestation trust-store loader"
```

---

### Task 3: Add Reflection Trust-Store Delegate and Command Invokers

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreDelegate.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactory.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactoryTest.java`
- Create: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreDelegateTest.java`

- [ ] **Step 1: Write failing tests for trust-store delegate lookup and reflection invoker**

Create `ConnectedHomeIpAttestationTrustStoreDelegateTest.java`:

```java
package org.openhab.matter.companion.controller;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public final class ConnectedHomeIpAttestationTrustStoreDelegateTest {
    @Test
    public void returnsPaaCertificateMatchingSkid() {
        byte[] certA = new byte[] {1, 2, 3};
        byte[] certB = new byte[] {9, 8, 7};
        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStore(Arrays.asList(certA, certB), Arrays.asList());

        Object proxy = new ConnectedHomeIpAttestationTrustStoreDelegate(
                FakeAttestationTrustStoreDelegate.class,
                FakeDeviceAttestation.class,
                store)
                .proxy();

        byte[] result = ((FakeAttestationTrustStoreDelegate) proxy)
                .getProductAttestationAuthorityCert(new byte[] {7});

        assertArrayEquals(certB, result);
    }

    @Test
    public void returnsNullWhenSkidIsUnknown() {
        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStore(Arrays.asList(new byte[] {1, 2, 3}), Arrays.asList());

        Object proxy = new ConnectedHomeIpAttestationTrustStoreDelegate(
                FakeAttestationTrustStoreDelegate.class,
                FakeDeviceAttestation.class,
                store)
                .proxy();

        byte[] result = ((FakeAttestationTrustStoreDelegate) proxy)
                .getProductAttestationAuthorityCert(new byte[] {99});

        assertNull(result);
    }

    public interface FakeAttestationTrustStoreDelegate {
        byte[] getProductAttestationAuthorityCert(byte[] skid);
    }

    public static final class FakeDeviceAttestation {
        public static byte[] extractSkidFromPaaCert(byte[] cert) {
            return new byte[] {cert[cert.length - 1]};
        }
    }
}
```

In `ConnectedHomeIpReflectionCommandFactoryTest`, add:

```java
@Test
public void invokesSetAttestationTrustStoreDelegateWithCdTrustKeys() throws Exception {
    ConnectedHomeIpReflectionCommandFactory factory = fakeFactory();
    FakeChipDeviceController controller = new FakeChipDeviceController();

    factory.invokeSetAttestationTrustStoreDelegate(
            controller,
            new FakeAttestationTrustStoreDelegate() { },
            java.util.Arrays.asList(new byte[] {1, 2, 3}));

    assertTrue(controller.attestationTrustStoreDelegateSet);
    assertEquals(1, controller.cdTrustKeys.size());
}
```

and extend `FakeChipDeviceController` with:

```java
private boolean attestationTrustStoreDelegateSet;
private java.util.List<byte[]> cdTrustKeys;

public void setAttestationTrustStoreDelegate(
        FakeAttestationTrustStoreDelegate delegate,
        java.util.List<byte[]> cdTrustKeys) {
    this.attestationTrustStoreDelegateSet = delegate != null;
    this.cdTrustKeys = cdTrustKeys;
}
```

plus fake interface:

```java
public interface FakeAttestationTrustStoreDelegate {
}
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpAttestationTrustStoreDelegateTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionCommandFactoryTest"
```

Expected: FAIL because trust-store delegate class and reflection invoker methods do not exist.

- [ ] **Step 3: Implement delegate and reflection invokers**

Create `ConnectedHomeIpAttestationTrustStoreDelegate.java`:

```java
package org.openhab.matter.companion.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public final class ConnectedHomeIpAttestationTrustStoreDelegate {
    private final Object proxy;

    public ConnectedHomeIpAttestationTrustStoreDelegate(
            Class<?> delegateClass,
            Class<?> deviceAttestationClass,
            ConnectedHomeIpAttestationTrustStore store) {
        this.proxy = Proxy.newProxyInstance(
                delegateClass.getClassLoader(),
                new Class<?>[] {delegateClass},
                new Handler(deviceAttestationClass, store));
    }

    public Object proxy() {
        return proxy;
    }

    private static final class Handler implements InvocationHandler {
        private final Method extractSkid;
        private final ConnectedHomeIpAttestationTrustStore store;

        private Handler(Class<?> deviceAttestationClass, ConnectedHomeIpAttestationTrustStore store) {
            try {
                this.extractSkid = deviceAttestationClass.getMethod("extractSkidFromPaaCert", byte[].class);
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("DeviceAttestation.extractSkidFromPaaCert not found", exception);
            }
            this.store = store;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getProductAttestationAuthorityCert".equals(method.getName())) {
                byte[] requestedSkid = args != null && args.length > 0 ? (byte[]) args[0] : null;
                if (requestedSkid == null) {
                    return null;
                }
                for (byte[] cert : store.paaCertificates()) {
                    byte[] certSkid = (byte[]) extractSkid.invoke(null, cert);
                    if (Arrays.equals(certSkid, requestedSkid)) {
                        return cert;
                    }
                }
                return null;
            }
            return null;
        }
    }
}
```

In `ConnectedHomeIpReflectionCommandFactory.java`, add class constants and fields:

```java
private static final String ATTESTATION_TRUST_STORE_DELEGATE_CLASS =
        "chip.devicecontroller.AttestationTrustStoreDelegate";
private static final String DEVICE_ATTESTATION_CLASS = "chip.devicecontroller.DeviceAttestation";

private final Class<?> attestationTrustStoreDelegateClass;
private final Class<?> deviceAttestationClass;
```

Load them in `fromDefaultClassLoader()` and constructors, then add methods:

```java
public Object newAttestationTrustStoreDelegate(ConnectedHomeIpAttestationTrustStore store) {
    return new ConnectedHomeIpAttestationTrustStoreDelegate(
            requireAvailable(attestationTrustStoreDelegateClass, "attestationTrustStoreDelegateClass"),
            requireAvailable(deviceAttestationClass, "deviceAttestationClass"),
            store)
            .proxy();
}

public void invokeSetAttestationTrustStoreDelegate(
        Object controller,
        Object delegateProxy,
        java.util.List<byte[]> cdTrustKeys) throws ReflectiveOperationException {
    try {
        chipDeviceControllerClass
                .getMethod(
                        "setAttestationTrustStoreDelegate",
                        requireAvailable(attestationTrustStoreDelegateClass, "attestationTrustStoreDelegateClass"),
                        java.util.List.class)
                .invoke(controller, delegateProxy, cdTrustKeys);
    } catch (NoSuchMethodException noTwoArgMethod) {
        chipDeviceControllerClass
                .getMethod(
                        "setAttestationTrustStoreDelegate",
                        requireAvailable(attestationTrustStoreDelegateClass, "attestationTrustStoreDelegateClass"))
                .invoke(controller, delegateProxy);
    }
}
```

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpAttestationTrustStoreDelegateTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionCommandFactoryTest"
```

Expected: both test classes pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreDelegate.java app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactory.java app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationTrustStoreDelegateTest.java app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionCommandFactoryTest.java
git commit -m "feat: wire reflection trust-store delegate for attestation"
```

---

### Task 4: Wire Trust-Store Setup Into Commissioning Path

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationHandler.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionAttestationHandler.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactory.java`
- Create: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionAttestationHandlerTest.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactoryTest.java`

- [ ] **Step 1: Write failing attestation-handler tests for ordering and bypass invariants**

Create `ConnectedHomeIpReflectionAttestationHandlerTest.java`:

```java
package org.openhab.matter.companion.controller;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpReflectionAttestationHandlerTest {
    @Test
    public void preparesTrustStoreBeforeDeviceAttestationDelegate() throws Exception {
        RecordingCommandFactory commandFactory = new RecordingCommandFactory();
        ConnectedHomeIpAttestationTrustStore store =
                new ConnectedHomeIpAttestationTrustStore(Arrays.asList(new byte[] {1, 2, 3}), Arrays.asList());
        ConnectedHomeIpReflectionAttestationHandler handler =
                new ConnectedHomeIpReflectionAttestationHandler(commandFactory, 120, store);

        handler.prepareForCommissioning(new Object(), 987654321L, false);

        assertEquals("setTrustStore,setDeviceDelegate", commandFactory.sequence.toString());
        assertTrue(commandFactory.deviceDelegateBypassDisabled);
    }

    private static final class RecordingCommandFactory extends ConnectedHomeIpReflectionCommandFactory {
        private final StringBuilder sequence = new StringBuilder();
        private boolean deviceDelegateBypassDisabled;

        RecordingCommandFactory() {
            super(
                    Object.class,
                    Object.class,
                    Object.class,
                    FakeChipDeviceController.class,
                    Object.class,
                    Object.class,
                    Object.class,
                    Object.class,
                    Object.class,
                    Object.class,
                    Object.class);
        }

        @Override
        public Object newAttestationTrustStoreDelegate(ConnectedHomeIpAttestationTrustStore store) {
            return new Object();
        }

        @Override
        public void invokeSetAttestationTrustStoreDelegate(Object controller, Object delegateProxy, java.util.List<byte[]> cdTrustKeys) {
            append("setTrustStore");
        }

        @Override
        public Object newDeviceAttestationDelegate(Object controller, boolean attestationBypassEnabled) {
            deviceDelegateBypassDisabled = !attestationBypassEnabled;
            return new Object();
        }

        @Override
        public void invokeSetDeviceAttestationDelegate(Object controller, int failSafeExpiryTimeoutSeconds, Object delegateProxy) {
            append("setDeviceDelegate");
        }

        private void append(String item) {
            if (sequence.length() > 0) {
                sequence.append(',');
            }
            sequence.append(item);
        }
    }

    public static final class FakeChipDeviceController {
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionAttestationHandlerTest"
```

Expected: FAIL because handler constructor and seam do not include trust-store data.

- [ ] **Step 3: Implement seam expansion and factory wiring**

In `ConnectedHomeIpAttestationHandler.java`, keep method signature but document trust-store prep in interface comment.

In `ConnectedHomeIpReflectionAttestationHandler.java`, add store field and constructor:

```java
private final ConnectedHomeIpAttestationTrustStore trustStore;

public ConnectedHomeIpReflectionAttestationHandler(
        ConnectedHomeIpReflectionCommandFactory commandFactory,
        int failSafeExpiryTimeoutSeconds,
        ConnectedHomeIpAttestationTrustStore trustStore) {
    if (trustStore == null) {
        throw new IllegalArgumentException("trustStore is required");
    }
    this.commandFactory = commandFactory;
    this.failSafeExpiryTimeoutSeconds = failSafeExpiryTimeoutSeconds;
    this.trustStore = trustStore;
}
```

In `prepareForCommissioning(...)`, set trust store before device delegate:

```java
Object trustStoreDelegate = commandFactory.newAttestationTrustStoreDelegate(trustStore);
commandFactory.invokeSetAttestationTrustStoreDelegate(
        controller,
        trustStoreDelegate,
        trustStore.cdTrustKeys());
Object delegate = commandFactory.newDeviceAttestationDelegate(controller, attestationBypassEnabled);
commandFactory.invokeSetDeviceAttestationDelegate(controller, failSafeExpiryTimeoutSeconds, delegate);
```

In `ConnectedHomeIpMatterControllerFactory.newDefaultGateway(...)`, load trust-store assets and pass store to handler:

```java
ConnectedHomeIpAttestationTrustStore trustStore =
        new ConnectedHomeIpAttestationTrustStoreLoader(
                path -> context.getAssets().list(path),
                path -> context.getAssets().open(path))
                .load("matter/truststore/paa", "matter/truststore/cd");

new ConnectedHomeIpReflectionAttestationHandler(
        commandFactory,
        ATTESTATION_FAIL_SAFE_EXPIRY_SECONDS,
        trustStore)
```

In `ConnectedHomeIpMatterControllerFactoryTest`, assert gateway creation still succeeds with ready artifacts and cached gateway tests remain green.

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionAttestationHandlerTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpMatterControllerFactoryTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionGatewayTest"
```

Expected: all selected tests pass; commissioning orchestration tests still pass with attestation-bypass behavior unchanged.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAttestationHandler.java app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionAttestationHandler.java app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactory.java app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionAttestationHandlerTest.java app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactoryTest.java
git commit -m "feat: configure connectedhomeip trust-store before attestation"
```

---

### Task 5: Document Trust-Store Workflow and Verify End-to-End Build/Test

**Files:**
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`
- Modify: `README.md`

- [ ] **Step 1: Write doc updates with exact build flags and DCL workflow**

In `docs/chip-jni-integration.md`, add a section under controller artifacts:

````markdown
### Attestation Trust Store Assets

When commissioning with attestation verification enabled, package trusted PAA certificates (and optional CD trust keys) into APK assets:

```powershell
.\gradlew.bat :app:assembleDebug --offline "-PopenhabMatterChipControllerArtifactsDir=<artifact-dir>" "-PopenhabMatterChipPaaTrustStoreDir=<connectedhomeip>\credentials\production\paa-root-certs" "-PopenhabMatterChipCdTrustStoreDir=<connectedhomeip>\credentials\production\cd-certs"
```

The app loads assets from `matter/truststore/paa` and `matter/truststore/cd`, then installs `ChipDeviceController.setAttestationTrustStoreDelegate(...)` before `setDeviceAttestationDelegate(...)`.

For missing vendor PAAs, refresh local certs from DCL using `credentials/fetch_paa_certs_from_dcl.py` in a connectedhomeip checkout and rebuild.
````

In `docs/implementation-status.md`, append an implemented bullet:

```markdown
- connectedhomeip attestation trust-store delegate now loads packaged PAA/CD assets and applies `setAttestationTrustStoreDelegate(...)` before commissioning, while preserving developer bypass semantics and fail-closed native gating.
```

In `README.md`, add a concise note in build section:

```markdown
- For production attestation verification, include local trust-store directories with `-PopenhabMatterChipPaaTrustStoreDir=...` (and optional `-PopenhabMatterChipCdTrustStoreDir=...`) when assembling the APK.
```

- [ ] **Step 2: Run full targeted verification suite**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.controller.ConnectedHomeIpControllerArtifactsTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpAttestationTrustStoreLoaderTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpAttestationTrustStoreDelegateTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionCommandFactoryTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionAttestationHandlerTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionGatewayTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpMatterControllerFactoryTest"
.\gradlew.bat :app:assembleDebug --offline "-PopenhabMatterChipControllerArtifactsDir=<artifact-dir>" "-PopenhabMatterChipPaaTrustStoreDir=<paa-dir>"
```

Expected: all targeted unit tests pass; assembleDebug succeeds and includes generated trust-store assets.

- [ ] **Step 3: Capture operator verification commands for real device**

Run on local connectedhomeip checkout (outside repo), then install app:

```powershell
python D:\Source\connectedhomeip\credentials\fetch_paa_certs_from_dcl.py --use-main-net-http --paa-trust-store-path D:\Source\connectedhomeip\credentials\production\paa-root-certs
.\gradlew.bat :app:assembleDebug --offline "-PopenhabMatterChipControllerArtifactsDir=<artifact-dir>" "-PopenhabMatterChipPaaTrustStoreDir=D:\Source\connectedhomeip\credentials\production\paa-root-certs"
D:\Tools\Android\SDK\platform-tools\adb.exe -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: app installs; attestation step no longer fails due to missing vendor PAA when vendor root exists in packaged trust store.

- [ ] **Step 4: Commit**

```powershell
git add README.md docs/chip-jni-integration.md docs/implementation-status.md
git commit -m "docs: document DCL-backed attestation trust-store workflow"
```

---

## Self-Review

- Spec coverage: Plan covers runtime trust-store delegate wiring, DCL-derived certificate sourcing, fail-closed commissioning behavior, and preservation of developer bypass.
- Placeholder scan: Removed ambiguous wording; each code-changing step includes concrete snippets and explicit commands.
- Type consistency: Method names are consistent across tasks (`newAttestationTrustStoreDelegate`, `invokeSetAttestationTrustStoreDelegate`, `prepareForCommissioning`), and constructor signature changes are reflected in factory wiring and tests.

---

Plan complete and saved to `docs/superpowers/plans/2026-05-31-connectedhomeip-attestation-trust-store.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration

2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
