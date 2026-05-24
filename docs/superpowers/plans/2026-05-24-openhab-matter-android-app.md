# openHAB Matter Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an installable Android companion app that guides Matter-over-Thread and Wi-Fi/multi-admin pairing for openHAB, with a tested controller boundary ready for the real CHIP JNI implementation.

**Architecture:** Start with a dependency-light Java Android app so it can build from the local Android SDK and cached Gradle artifacts. Keep Matter commissioning behind `MatterController`, shipping a deterministic fake controller for the first installable APK and isolating the future connectedhomeip JNI bridge behind the same interface.

**Tech Stack:** Android Gradle Plugin 8.11.1, Gradle 8.14, Java 17 source compatibility, Android SDK `D:\Tools\Android\SDK`, compile SDK 36 with target SDK 35, JUnit 4.13.2 JVM tests, programmatic Android Views.

---

## Requirements From `docs/research.md`

- Thread path: store/paste Thread Active Operational Dataset hex and validate it.
- Thread path: parse a Matter QR/manual setup payload enough to surface PIN and discriminator for commissioning.
- Thread path: run a BLE-to-Thread commissioning flow through a controller boundary equivalent to `chip-tool pairing ble-thread`.
- Thread path: open a commissioning window and display a temporary pairing code for the openHAB Matter UI Scan Input.
- Wi-Fi/multi-admin path: capture an existing setup or multi-admin code and guide the user to paste it into openHAB.
- openHAB path: do not require openHAB binding changes; provide user instructions and optional diagnostics.
- Installability: produce a debug APK that can be installed with `adb install`.

## File Structure

- `settings.gradle` - Gradle project settings for the single Android app module.
- `build.gradle` - root plugin versions and repositories.
- `gradle.properties` - AndroidX/Jetifier-disabled project settings and JVM args.
- `local.properties` - local Android SDK path for this machine; keep out of source control later if this project becomes shared.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` - Gradle wrapper generated from local Gradle 8.14.
- `app/build.gradle` - Android application module, Java 17, JUnit tests.
- `app/src/main/AndroidManifest.xml` - app metadata and Bluetooth/location/network permissions.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - native Android screen flow.
- `app/src/main/java/org/openhab/matter/companion/domain/ThreadDataset.java` - Thread dataset normalization and validation.
- `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayload.java` - setup payload model.
- `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayloadParser.java` - QR/manual code parser with deterministic fallback semantics.
- `app/src/main/java/org/openhab/matter/companion/domain/CommissioningStep.java` - progress event value object.
- `app/src/main/java/org/openhab/matter/companion/domain/OpenHabInstructions.java` - help text and openHAB UI route.
- `app/src/main/java/org/openhab/matter/companion/controller/MatterController.java` - real/fake controller interface.
- `app/src/main/java/org/openhab/matter/companion/controller/FakeMatterController.java` - deterministic installable MVP controller.
- `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java` - JNI bridge placeholder that fails with actionable text until native library is added.
- `app/src/main/java/org/openhab/matter/companion/ui/AppState.java` - UI state holder.
- `app/src/test/java/org/openhab/matter/companion/domain/ThreadDatasetTest.java` - dataset validation tests.
- `app/src/test/java/org/openhab/matter/companion/domain/MatterSetupPayloadParserTest.java` - setup parser tests.
- `app/src/test/java/org/openhab/matter/companion/controller/FakeMatterControllerTest.java` - commissioning/OCW controller tests.
- `README.md` - build, install, and scope notes.

---

### Task 1: Android Build Scaffold

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `local.properties`
- Create: `.gitignore`
- Create: `app/build.gradle`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `README.md`

- [ ] **Step 1: Write the minimal Android project files**

Create `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "openhab-android-matter"
include ":app"
```

Create `build.gradle`:

```groovy
plugins {
    id "com.android.application" version "8.11.1" apply false
}
```

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=false
android.nonTransitiveRClass=true
```

Create `local.properties`:

```properties
sdk.dir=D:\\Tools\\Android\\SDK
```

Create `app/build.gradle`:

```groovy
plugins {
    id "com.android.application"
}

android {
    namespace "org.openhab.matter.companion"
    compileSdk 36

    defaultConfig {
        applicationId "org.openhab.matter.companion"
        minSdk 26
        targetSdk 35
        versionCode 1
        versionName "0.1.0"
        testInstrumentationRunner "android.test.InstrumentationTestRunner"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation "junit:junit:4.13.2"
}
```

Create `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="false"
        android:label="openHAB Matter Helper"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 2: Add a minimal theme**

Create `app/src/main/res/values/styles.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="@android:style/Theme.Material.Light.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:statusBarColor">#F7F3EA</item>
        <item name="android:navigationBarColor">#243230</item>
        <item name="android:colorAccent">#2F6F62</item>
    </style>
</resources>
```

- [ ] **Step 3: Add a minimal launcher activity**

Create `app/src/main/java/org/openhab/matter/companion/MainActivity.java`:

```java
package org.openhab.matter.companion;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("openHAB Matter Helper");
        setContentView(textView);
    }
}
```

- [ ] **Step 4: Generate the Gradle wrapper**

Run:

```powershell
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14-all\c2qonpi39x1mddn7hk5gh9iqj\gradle-8.14\bin\gradle.bat" wrapper --gradle-version 8.14 --distribution-type all
```

Expected: `BUILD SUCCESSFUL`, plus `gradlew`, `gradlew.bat`, and `gradle/wrapper/*`.

- [ ] **Step 5: Verify scaffold builds a debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit scaffold**

Run:

```powershell
git add settings.gradle build.gradle gradle.properties local.properties gradlew gradlew.bat gradle app app/src/main/AndroidManifest.xml README.md
git commit -m "build: scaffold Android application"
```

Expected: commit created on `feature/openhab-matter-android-app`.

---

### Task 2: Domain Validation and Setup Payload Parsing

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/domain/ThreadDataset.java`
- Create: `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayload.java`
- Create: `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayloadParser.java`
- Test: `app/src/test/java/org/openhab/matter/companion/domain/ThreadDatasetTest.java`
- Test: `app/src/test/java/org/openhab/matter/companion/domain/MatterSetupPayloadParserTest.java`

- [ ] **Step 1: Write failing dataset tests**

Create `app/src/test/java/org/openhab/matter/companion/domain/ThreadDatasetTest.java`:

```java
package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ThreadDatasetTest {
    @Test
    public void normalizesHexPrefixAndWhitespace() {
        ThreadDataset dataset = ThreadDataset.parse(" hex: 0E080000000000010000 ");
        assertEquals("0E080000000000010000", dataset.hex());
        assertEquals("hex:0E080000000000010000", dataset.chipToolValue());
    }

    @Test
    public void rejectsOddLengthHex() {
        assertThrows(IllegalArgumentException.class, () -> ThreadDataset.parse("ABC"));
    }

    @Test
    public void rejectsNonHexText() {
        assertThrows(IllegalArgumentException.class, () -> ThreadDataset.parse("hex:not-a-dataset"));
    }
}
```

- [ ] **Step 2: Write failing setup payload tests**

Create `app/src/test/java/org/openhab/matter/companion/domain/MatterSetupPayloadParserTest.java`:

```java
package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class MatterSetupPayloadParserTest {
    @Test
    public void parsesExplicitFieldsForThreadCommissioning() {
        MatterSetupPayload payload = MatterSetupPayloadParser.parse("pin=20202021;disc=3840;vendor=Aqara;product=U200");
        assertEquals(20202021L, payload.pin());
        assertEquals(3840, payload.discriminator());
        assertEquals("Aqara", payload.vendorName());
        assertEquals("U200", payload.productName());
    }

    @Test
    public void preservesQrPayloadWhenChipParserIsNeeded() {
        MatterSetupPayload payload = MatterSetupPayloadParser.parse("MT:ABCDEF0123456789");
        assertEquals("MT:ABCDEF0123456789", payload.rawPayload());
        assertTrue(payload.requiresChipParser());
    }

    @Test
    public void rejectsManualCodeWithoutLongDiscriminatorForThread() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("34970112332"));
    }
}
```

- [ ] **Step 3: Run tests and verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: compile fails because `ThreadDataset`, `MatterSetupPayload`, and `MatterSetupPayloadParser` do not exist.

- [ ] **Step 4: Implement `ThreadDataset`**

Create `app/src/main/java/org/openhab/matter/companion/domain/ThreadDataset.java`:

```java
package org.openhab.matter.companion.domain;

import java.util.Locale;

public final class ThreadDataset {
    private final String hex;

    private ThreadDataset(String hex) {
        this.hex = hex;
    }

    public static ThreadDataset parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Thread dataset is required.");
        }
        String normalized = input.trim().replace(" ", "");
        if (normalized.toLowerCase(Locale.US).startsWith("hex:")) {
            normalized = normalized.substring(4);
        }
        normalized = normalized.toUpperCase(Locale.US);
        if (normalized.length() < 16) {
            throw new IllegalArgumentException("Thread dataset is too short.");
        }
        if ((normalized.length() % 2) != 0) {
            throw new IllegalArgumentException("Thread dataset hex must contain complete bytes.");
        }
        if (!normalized.matches("[0-9A-F]+")) {
            throw new IllegalArgumentException("Thread dataset must be hexadecimal.");
        }
        return new ThreadDataset(normalized);
    }

    public String hex() {
        return hex;
    }

    public String chipToolValue() {
        return "hex:" + hex;
    }
}
```

- [ ] **Step 5: Implement setup payload model and parser**

Create `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayload.java`:

```java
package org.openhab.matter.companion.domain;

public final class MatterSetupPayload {
    private final String rawPayload;
    private final long pin;
    private final int discriminator;
    private final String vendorName;
    private final String productName;
    private final boolean requiresChipParser;

    public MatterSetupPayload(String rawPayload, long pin, int discriminator, String vendorName, String productName, boolean requiresChipParser) {
        this.rawPayload = rawPayload;
        this.pin = pin;
        this.discriminator = discriminator;
        this.vendorName = vendorName;
        this.productName = productName;
        this.requiresChipParser = requiresChipParser;
    }

    public String rawPayload() {
        return rawPayload;
    }

    public long pin() {
        return pin;
    }

    public int discriminator() {
        return discriminator;
    }

    public String vendorName() {
        return vendorName;
    }

    public String productName() {
        return productName;
    }

    public boolean requiresChipParser() {
        return requiresChipParser;
    }
}
```

Create `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayloadParser.java`:

```java
package org.openhab.matter.companion.domain;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MatterSetupPayloadParser {
    private MatterSetupPayloadParser() {
    }

    public static MatterSetupPayload parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Matter setup payload is required.");
        }
        String raw = input.trim();
        if (raw.startsWith("MT:")) {
            return new MatterSetupPayload(raw, 0L, -1, "Unknown vendor", "Unknown product", true);
        }
        if (raw.matches("\\d{11}")) {
            throw new IllegalArgumentException("Thread commissioning needs the long discriminator from QR or explicit disc= value.");
        }
        Map<String, String> fields = parseFields(raw);
        long pin = parseLongField(fields, "pin", "PIN");
        int discriminator = (int) parseLongField(fields, "disc", "long discriminator");
        String vendor = fields.getOrDefault("vendor", "Unknown vendor");
        String product = fields.getOrDefault("product", "Unknown product");
        if (pin < 1 || pin > 99999999L) {
            throw new IllegalArgumentException("PIN must be an 8-digit Matter setup PIN.");
        }
        if (discriminator < 0 || discriminator > 4095) {
            throw new IllegalArgumentException("Long discriminator must be between 0 and 4095.");
        }
        return new MatterSetupPayload(raw, pin, discriminator, vendor, product, false);
    }

    private static Map<String, String> parseFields(String raw) {
        Map<String, String> fields = new HashMap<>();
        for (String part : raw.split(";")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                fields.put(pair[0].trim().toLowerCase(Locale.US), pair[1].trim());
            }
        }
        return fields;
    }

    private static long parseLongField(Map<String, String> fields, String key, String label) {
        String value = fields.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing " + label + ".");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + ".", ex);
        }
    }
}
```

- [ ] **Step 6: Run tests and verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: all tests in `ThreadDatasetTest` and `MatterSetupPayloadParserTest` pass.

- [ ] **Step 7: Commit domain parser**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/domain app/src/test/java/org/openhab/matter/companion/domain
git commit -m "feat: add Matter setup validation"
```

Expected: commit created.

---

### Task 3: Matter Controller Boundary

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/domain/CommissioningStep.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterController.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/FakeMatterController.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/FakeMatterControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `app/src/test/java/org/openhab/matter/companion/controller/FakeMatterControllerTest.java`:

```java
package org.openhab.matter.companion.controller;

import org.junit.Test;
import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FakeMatterControllerTest {
    @Test
    public void commissionsThreadDeviceAndReturnsNodeId() throws Exception {
        MatterController controller = new FakeMatterController();
        List<CommissioningStep> steps = new ArrayList<>();
        long nodeId = controller.commissionBleThread(
                ThreadDataset.parse("0E080000000000010000"),
                new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                steps::add);

        assertEquals(1L, nodeId);
        assertEquals("Joined Thread network successfully", steps.get(steps.size() - 1).message());
    }

    @Test
    public void opensCommissioningWindowWithTemporaryCode() throws Exception {
        MatterController controller = new FakeMatterController();
        String code = controller.openCommissioningWindow(1L, 300, 3840, ignored -> { });
        assertTrue(code.matches("\\d{4}-\\d{4}-\\d{3}"));
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: compile fails because controller classes do not exist.

- [ ] **Step 3: Implement controller API and fake controller**

Create `app/src/main/java/org/openhab/matter/companion/domain/CommissioningStep.java`:

```java
package org.openhab.matter.companion.domain;

public final class CommissioningStep {
    private final String message;
    private final boolean complete;

    public CommissioningStep(String message, boolean complete) {
        this.message = message;
        this.complete = complete;
    }

    public String message() {
        return message;
    }

    public boolean complete() {
        return complete;
    }
}
```

Create `app/src/main/java/org/openhab/matter/companion/controller/MatterController.java`:

```java
package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public interface MatterController {
    interface ProgressListener {
        void onProgress(CommissioningStep step);
    }

    long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) throws Exception;

    String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) throws Exception;
}
```

Create `app/src/main/java/org/openhab/matter/companion/controller/FakeMatterController.java`:

```java
package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class FakeMatterController implements MatterController {
    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        emit(listener, "Scanning via BLE for discriminator " + payload.discriminator(), false);
        emit(listener, "PASE session established with PIN " + payload.pin(), false);
        emit(listener, "Attestation bypass is enabled for this developer MVP", false);
        emit(listener, "Provisioning Thread dataset " + dataset.hex(), false);
        emit(listener, "Joined Thread network successfully", true);
        return 1L;
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        emit(listener, "Opening commissioning window on node " + nodeId + " for " + timeoutSeconds + " seconds", false);
        emit(listener, "Temporary setup code generated for discriminator " + discriminator, true);
        return "3497-0112-332";
    }

    private static void emit(ProgressListener listener, String message, boolean complete) {
        if (listener != null) {
            listener.onProgress(new CommissioningStep(message, complete));
        }
    }
}
```

- [ ] **Step 4: Add real CHIP placeholder behind the same interface**

Create `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`:

```java
package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ChipMatterController implements MatterController {
    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        throw new UnsupportedOperationException("CHIP JNI library is not bundled yet. Use FakeMatterController for the installable MVP.");
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        throw new UnsupportedOperationException("CHIP JNI library is not bundled yet. Use FakeMatterController for the installable MVP.");
    }
}
```

- [ ] **Step 5: Run tests and verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: all domain and controller tests pass.

- [ ] **Step 6: Commit controller boundary**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/main/java/org/openhab/matter/companion/domain/CommissioningStep.java app/src/test/java/org/openhab/matter/companion/controller
git commit -m "feat: add Matter controller boundary"
```

Expected: commit created.

---

### Task 4: Native Android UI Flow

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/ui/AppState.java`
- Create: `app/src/main/java/org/openhab/matter/companion/domain/OpenHabInstructions.java`
- Create: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`

- [ ] **Step 1: Implement UI state and instructions**

Create `app/src/main/java/org/openhab/matter/companion/ui/AppState.java`:

```java
package org.openhab.matter.companion.ui;

public final class AppState {
    public String dataset = "";
    public String setupPayload = "";
    public String logs = "";
    public String temporaryCode = "";
    public long commissionedNodeId = -1L;
}
```

Create `app/src/main/java/org/openhab/matter/companion/domain/OpenHabInstructions.java`:

```java
package org.openhab.matter.companion.domain;

public final class OpenHabInstructions {
    private OpenHabInstructions() {
    }

    public static String scanInputInstructions(String temporaryCode) {
        return "Open openHAB Main UI > Settings > Things > + > Matter > Scan Input, then enter: " + temporaryCode;
    }

    public static String troubleshooting() {
        return "If the device does not appear in openHAB Inbox, verify the Matter binding controller is online, IPv6 routing works through the OTBR, and mDNS/Avahi is working between openHAB and the Thread network.";
    }
}
```

- [ ] **Step 2: Implement `MainActivity`**

Create `app/src/main/java/org/openhab/matter/companion/MainActivity.java`:

```java
package org.openhab.matter.companion;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.openhab.matter.companion.controller.FakeMatterController;
import org.openhab.matter.companion.controller.MatterController;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.MatterSetupPayloadParser;
import org.openhab.matter.companion.domain.OpenHabInstructions;
import org.openhab.matter.companion.domain.ThreadDataset;
import org.openhab.matter.companion.ui.AppState;

public final class MainActivity extends Activity {
    private final AppState state = new AppState();
    private final MatterController controller = new FakeMatterController();
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 36, 36, 36);
        root.setBackgroundColor(Color.rgb(247, 243, 234));
        scrollView.addView(root);

        TextView title = label("openHAB Matter Helper", 28, "#243230");
        TextView subtitle = label("Phone performs the commissioning-assistant steps; openHAB remains the long-term Matter controller.", 16, "#3E5551");
        EditText datasetInput = input("Thread Active Operational Dataset hex, for example hex:0E080000000000010000", true);
        EditText payloadInput = input("Matter payload: MT:... or pin=20202021;disc=3840;vendor=Aqara;product=U200", false);
        Button commission = button("Commission Thread device with phone BLE");
        Button openWindow = button("Open commissioning window for openHAB");
        Button wifi = button("Wi-Fi / multi-admin: show openHAB handoff");
        output = label("", 15, "#243230");

        commission.setOnClickListener(view -> runCommissioning(datasetInput.getText().toString(), payloadInput.getText().toString()));
        openWindow.setOnClickListener(view -> runOpenCommissioningWindow());
        wifi.setOnClickListener(view -> showWifiInstructions(payloadInput.getText().toString()));

        root.addView(title);
        root.addView(subtitle);
        root.addView(datasetInput);
        root.addView(payloadInput);
        root.addView(commission);
        root.addView(openWindow);
        root.addView(wifi);
        root.addView(output);

        setContentView(scrollView);
        append("Paste your OTBR dataset and scan or type a Matter setup payload.");
    }

    private void runCommissioning(String datasetText, String payloadText) {
        try {
            ThreadDataset dataset = ThreadDataset.parse(datasetText);
            MatterSetupPayload payload = MatterSetupPayloadParser.parse(payloadText);
            if (payload.requiresChipParser()) {
                append("QR payload requires CHIP parser. For this installable MVP, use explicit pin=...;disc=... fields.");
                return;
            }
            state.commissionedNodeId = controller.commissionBleThread(dataset, payload, step -> append(step.message()));
            append("Commissioned bootstrap node id: " + state.commissionedNodeId);
        } catch (Exception ex) {
            append("Error: " + ex.getMessage());
        }
    }

    private void runOpenCommissioningWindow() {
        try {
            if (state.commissionedNodeId < 0) {
                append("Commission a Thread device first.");
                return;
            }
            state.temporaryCode = controller.openCommissioningWindow(state.commissionedNodeId, 300, 3840, step -> append(step.message()));
            append(OpenHabInstructions.scanInputInstructions(state.temporaryCode));
            append(OpenHabInstructions.troubleshooting());
        } catch (Exception ex) {
            append("Error: " + ex.getMessage());
        }
    }

    private void showWifiInstructions(String payloadText) {
        if (payloadText == null || payloadText.trim().isEmpty()) {
            append("Scan or paste a Matter setup code from the device or another ecosystem first.");
            return;
        }
        append("For Wi-Fi or existing multi-admin flows, open openHAB > Matter > Scan Input and enter:");
        append(payloadText.trim());
    }

    private EditText input(String hint, boolean multiLine) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(Color.rgb(36, 50, 48));
        editText.setHintTextColor(Color.rgb(94, 111, 107));
        editText.setSingleLine(!multiLine);
        editText.setInputType(multiLine ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE : InputType.TYPE_CLASS_TEXT);
        editText.setPadding(0, 24, 0, 24);
        return editText;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        return button;
    }

    private TextView label(String text, int sizeSp, String color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sizeSp);
        textView.setTextColor(Color.parseColor(color));
        textView.setPadding(0, 12, 0, 12);
        return textView;
    }

    private void append(String message) {
        state.logs = state.logs + message + "\n";
        output.setText(state.logs);
    }
}
```

- [ ] **Step 3: Compile the debug app**

Run:

```powershell
.\gradlew.bat :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 4: Commit UI flow**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/main/java/org/openhab/matter/companion/ui app/src/main/java/org/openhab/matter/companion/domain/OpenHabInstructions.java
git commit -m "feat: add Android commissioning guide UI"
```

Expected: commit created.

---

### Task 5: Build, Install, and Scope Documentation

**Files:**
- Modify: `README.md`
- Create: `docs/implementation-status.md`

- [ ] **Step 1: Update README**

Replace `README.md` with:

```markdown
# openhab-android-matter

Android companion app for openHAB Matter pairing.

## Current MVP

This branch builds an installable Android APK with:

- Thread dataset validation.
- Matter setup payload validation for explicit `pin=...;disc=...` input.
- A deterministic fake Matter controller that simulates BLE Thread commissioning and OpenCommissioningWindow.
- A native Android UI that displays the temporary code and openHAB Matter Scan Input instructions.

The real connectedhomeip/CHIP JNI controller is intentionally isolated behind `MatterController`. `ChipMatterController` is present as the replacement point for the native implementation.

## Build

This workspace expects the Android SDK at:

```text
D:\Tools\Android\SDK
```

Build the debug APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

The APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Install

Connect an Android device with USB debugging enabled, then run:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

## Real CHIP Controller Work

The remaining production work is to replace `FakeMatterController` with a connectedhomeip-backed `ChipMatterController` that implements:

- `commissionBleThread(datasetHex, pin, discriminator)`
- `openCommissioningWindow(nodeId, timeout, discriminator)`

Until that native library is bundled, the app is installable and validates the openHAB user flow, but it does not actually provision Matter devices.
```

- [ ] **Step 2: Add implementation status**

Create `docs/implementation-status.md`:

```markdown
# Implementation Status

## Implemented

- Android Gradle project builds a debug APK.
- Native Android UI covers Thread, Wi-Fi, and multi-admin handoff flows.
- Thread dataset validation accepts `hex:` and raw hex input.
- Explicit setup payload parser accepts `pin=...;disc=...;vendor=...;product=...`.
- Fake Matter controller simulates BLE Thread commissioning and OCW.

## Not Implemented Yet

- Camera QR scanning.
- CHIP QR payload decoding for `MT:` payloads.
- Real BLE scanning, PASE, attestation, Thread dataset provisioning, and OpenCommissioningWindow.
- openHAB REST/SSE health checks.

## Production Replacement Seam

Replace `FakeMatterController` construction in `MainActivity` with `ChipMatterController` after the connectedhomeip Android JNI library is available.
```

- [ ] **Step 3: Run final build verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL`, with tests passing and APK generated.

- [ ] **Step 4: Verify APK exists**

Run:

```powershell
Test-Path app\build\outputs\apk\debug\app-debug.apk
```

Expected: `True`.

- [ ] **Step 5: Commit documentation**

Run:

```powershell
git add README.md docs/implementation-status.md
git commit -m "docs: document installable MVP status"
```

Expected: commit created.

---

### Task 6: Real CHIP JNI Integration Plan After Installable MVP

**Files:**
- Create: `docs/chip-jni-integration.md`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`

- [ ] **Step 1: Document exact native integration boundary**

Create `docs/chip-jni-integration.md`:

```markdown
# CHIP JNI Integration

## Native Functions Required

`ChipMatterController` needs a native library exposing:

```java
private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);
private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
```

## connectedhomeip Behavior To Mirror

- `chip-tool pairing ble-thread <NODE_ID> hex:<DATASET_HEX> <PIN> <DISCRIMINATOR>`
- `chip-tool pairing open-commissioning-window <NODE_ID> 1 300 1000 <DISCRIMINATOR>`

## Acceptance

- Given a known Thread device in pairing mode and an OTBR Active Operational Dataset, Android commissions the device over BLE.
- OTBR CLI shows the device joined the Thread network.
- Android opens a commissioning window and displays a temporary setup code.
- Entering the temporary code in openHAB Matter Scan Input makes the device appear in the Inbox.
```

- [ ] **Step 2: Replace placeholder with native method signatures**

Modify `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`:

```java
package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ChipMatterController implements MatterController {
    static {
        System.loadLibrary("openhab_matter_chip");
    }

    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        emit(listener, "Starting CHIP BLE Thread commissioning", false);
        long nodeId = nativeCommissionBleThread(dataset.hex(), payload.pin(), payload.discriminator());
        emit(listener, "CHIP BLE Thread commissioning complete for node " + nodeId, true);
        return nodeId;
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        emit(listener, "Opening CHIP commissioning window", false);
        String code = nativeOpenCommissioningWindow(nodeId, timeoutSeconds, discriminator);
        emit(listener, "CHIP commissioning window opened", true);
        return code;
    }

    private static void emit(ProgressListener listener, String message, boolean complete) {
        if (listener != null) {
            listener.onProgress(new CommissioningStep(message, complete));
        }
    }

    private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);

    private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
}
```

- [ ] **Step 3: Do not enable `ChipMatterController` in `MainActivity` until native library exists**

Keep this line unchanged in `MainActivity` until `libopenhab_matter_chip.so` is available:

```java
private final MatterController controller = new FakeMatterController();
```

- [ ] **Step 4: Commit JNI integration notes**

Run:

```powershell
git add docs/chip-jni-integration.md app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java
git commit -m "docs: define CHIP JNI integration boundary"
```

Expected: commit created.

---

## Self-Review

- Spec coverage: The plan covers dataset setup, setup payload parsing, BLE Thread commissioning boundary, OCW code display, openHAB Scan Input instructions, Wi-Fi/multi-admin handoff, installable APK generation, and real CHIP integration seam. It does not claim real BLE/Matter commissioning is complete in the first installable MVP.
- Placeholder scan: No `TBD`, `TODO`, "implement later", or unspecified test instructions remain. The only deferred production item is explicitly scoped as a separate native integration task with concrete method signatures and acceptance criteria.
- Type consistency: `ThreadDataset`, `MatterSetupPayload`, `CommissioningStep`, `MatterController`, `FakeMatterController`, and `ChipMatterController` names and method signatures are consistent across tasks.
