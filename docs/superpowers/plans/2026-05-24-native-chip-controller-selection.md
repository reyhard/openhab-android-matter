# Native CHIP Controller Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow the app to switch from the simulated controller to `ChipMatterController` when the native CHIP JNI library is bundled and ready.

**Architecture:** Keep `FakeMatterController` as the safe default so the APK remains installable without native libraries. Add a small selector object that checks `ChipMatterController.readiness()` and returns either the native controller or the fallback controller with a user-visible reason. Wire the selector into `MainActivity` through an explicit "Use native CHIP controller if ready" button.

**Tech Stack:** Java 17, Android Views, JUnit 4.13.2, no new external dependencies.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelection.java` - immutable value describing the selected controller, whether native mode is active, and the message to show.
- `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelector.java` - selects `ChipMatterController` only when readiness succeeds; otherwise returns the supplied fallback controller.
- `app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java` - proves fake-default, native-ready, and native-missing behavior.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - replaces the hard-coded final fake controller with selectable controller state and adds the native-selection button.
- `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java` - formats controller-selection messages for the log.
- `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java` - proves the selection message is presented exactly.
- `README.md` and `docs/implementation-status.md` - document that native controller activation is possible only when `libopenhab_matter_chip.so` is bundled.

---

### Task 21: Controller Selection Domain

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelection.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelector.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java`

- [ ] **Step 1: Write the failing selector tests**

Create `app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java`:

```java
package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class MatterControllerSelectorTest {
    @Test
    public void keepsFallbackControllerWhenNativeIsNotRequested() {
        MatterController fallback = new FakeMatterController();
        ChipMatterController nativeController = new ChipMatterController(name -> { },
                ChipMatterControllerConfig.defaultConfig());

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                false);

        assertSame(fallback, selection.controller());
        assertFalse(selection.nativeSelected());
        assertTrue(selection.message().contains("Using simulated Matter controller"));
    }

    @Test
    public void selectsNativeControllerWhenReadyAndRequested() {
        MatterController fallback = new FakeMatterController();
        ChipMatterController nativeController = new ChipMatterController(name -> { },
                new ChipMatterControllerConfig("custom_chip", true));

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                true);

        assertSame(nativeController, selection.controller());
        assertTrue(selection.nativeSelected());
        assertTrue(selection.message().contains("Using native CHIP controller: custom_chip"));
    }

    @Test
    public void fallsBackWhenRequestedNativeControllerIsNotReady() {
        MatterController fallback = new FakeMatterController();
        ChipMatterController nativeController = new ChipMatterController(name -> {
            throw new UnsatisfiedLinkError("missing " + name);
        }, ChipMatterControllerConfig.defaultConfig());

        MatterControllerSelection selection = MatterControllerSelector.select(
                fallback,
                nativeController,
                true);

        assertSame(fallback, selection.controller());
        assertFalse(selection.nativeSelected());
        assertTrue(selection.message().contains("Native CHIP controller not ready"));
        assertTrue(selection.message().contains("Continuing with simulated Matter controller"));
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.controller.MatterControllerSelectorTest
```

Expected: compile fails because `MatterControllerSelection` and `MatterControllerSelector` do not exist.

- [ ] **Step 3: Implement `MatterControllerSelection`**

Create `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelection.java`:

```java
package org.openhab.matter.companion.controller;

public final class MatterControllerSelection {
    private final MatterController controller;
    private final boolean nativeSelected;
    private final String message;

    public MatterControllerSelection(MatterController controller, boolean nativeSelected, String message) {
        if (controller == null) {
            throw new IllegalArgumentException("Matter controller is required.");
        }
        this.controller = controller;
        this.nativeSelected = nativeSelected;
        this.message = message == null ? "" : message;
    }

    public MatterController controller() {
        return controller;
    }

    public boolean nativeSelected() {
        return nativeSelected;
    }

    public String message() {
        return message;
    }
}
```

- [ ] **Step 4: Implement `MatterControllerSelector`**

Create `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelector.java`:

```java
package org.openhab.matter.companion.controller;

public final class MatterControllerSelector {
    private MatterControllerSelector() {
    }

    public static MatterControllerSelection select(
            MatterController fallbackController,
            ChipMatterController nativeController,
            boolean preferNative) {
        if (fallbackController == null) {
            throw new IllegalArgumentException("Fallback Matter controller is required.");
        }
        if (!preferNative) {
            return new MatterControllerSelection(
                    fallbackController,
                    false,
                    "Using simulated Matter controller. Native CHIP controller was not requested.");
        }
        if (nativeController == null) {
            return new MatterControllerSelection(
                    fallbackController,
                    false,
                    "Native CHIP controller not configured. Continuing with simulated Matter controller.");
        }
        ChipMatterControllerStatus status = nativeController.readiness();
        if (status.ready()) {
            return new MatterControllerSelection(
                    nativeController,
                    true,
                    "Using native CHIP controller: " + status.libraryName());
        }
        return new MatterControllerSelection(
                fallbackController,
                false,
                "Native CHIP controller not ready: " + status.message()
                        + ". Continuing with simulated Matter controller.");
    }
}
```

- [ ] **Step 5: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.controller.MatterControllerSelectorTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelection.java app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelector.java app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java
git commit -m "feat: add Matter controller selection"
```

---

### Task 22: UI And Documentation Integration

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Test: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write the failing presentation test**

Add this test to `MainActivityPresentationTest`:

```java
@Test
public void describesMatterControllerSelection() {
    MatterControllerSelection selection = new MatterControllerSelection(
            new FakeMatterController(),
            true,
            "Using native CHIP controller: custom_chip");

    assertEquals(
            "Matter controller selection: Using native CHIP controller: custom_chip",
            MainActivityPresentation.matterControllerSelection(selection));
}
```

Add imports:

```java
import org.openhab.matter.companion.controller.FakeMatterController;
import org.openhab.matter.companion.controller.MatterControllerSelection;
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.MainActivityPresentationTest
```

Expected: compile fails because `MainActivityPresentation.matterControllerSelection(...)` does not exist.

- [ ] **Step 3: Implement the presentation helper**

Add this import to `MainActivityPresentation.java`:

```java
import org.openhab.matter.companion.controller.MatterControllerSelection;
```

Add this method:

```java
static String matterControllerSelection(MatterControllerSelection selection) {
    return "Matter controller selection: " + selection.message();
}
```

- [ ] **Step 4: Wire controller selection into `MainActivity`**

Modify imports in `MainActivity.java`:

```java
import org.openhab.matter.companion.controller.MatterControllerSelection;
import org.openhab.matter.companion.controller.MatterControllerSelector;
```

Replace controller fields:

```java
private final MatterController fakeMatterController = new FakeMatterController();
private final ChipMatterController chipMatterController = new ChipMatterController();
private MatterController controller = fakeMatterController;
private boolean nativeMatterControllerSelected;
```

Add a button in `onCreate`:

```java
Button useNativeChip = button("Use native CHIP controller if ready");
useNativeChip.setOnClickListener(view -> useNativeChipControllerIfReady());
root.addView(useNativeChip);
```

Place it next to the existing `Check native CHIP controller` button.

Add this method:

```java
private void useNativeChipControllerIfReady() {
    MatterControllerSelection selection = MatterControllerSelector.select(
            fakeMatterController,
            chipMatterController,
            true);
    controller = selection.controller();
    nativeMatterControllerSelected = selection.nativeSelected();
    append(MainActivityPresentation.matterControllerSelection(selection));
}
```

Update the commissioning/open-window log text so it no longer always says simulated:

```java
append(nativeMatterControllerSelected
        ? "Starting native CHIP Thread commissioning."
        : "Starting simulated Thread commissioning. No real BLE, Thread, or Matter operation will be performed.");
```

```java
append(nativeMatterControllerSelected
        ? "Opening native CHIP commissioning window."
        : "Opening a simulated commissioning window. This does not call a real Matter controller.");
```

- [ ] **Step 5: Update documentation**

In `README.md`, add:

```markdown
- Runtime controller selection can switch from the simulated controller to `ChipMatterController` when the native JNI library is bundled and readiness passes.
```

In `docs/implementation-status.md`, add:

```markdown
- Runtime controller selection can switch from the simulated controller to `ChipMatterController` when the native JNI library is bundled and readiness passes.
```

Keep the not-implemented real commissioning bullets unchanged because this does not bundle `libopenhab_matter_chip.so`.

- [ ] **Step 6: Verify**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java README.md docs/implementation-status.md
git commit -m "feat: expose native controller selection"
```

---

## Self-Review

- Spec coverage: The plan moves the app closer to the `docs/research.md` target by allowing the real CHIP controller boundary to be activated from the UI when a native library is available. It does not claim to implement the connectedhomeip native library, BLE scanning, PASE, Thread provisioning, attestation, OCW internals, or CameraX scanning.
- Placeholder scan: No `TBD`, `TODO`, "implement later", or unspecified test instructions remain.
- Type consistency: `MatterControllerSelection`, `MatterControllerSelector`, `ChipMatterController`, and `MainActivityPresentation.matterControllerSelection(...)` names are consistent across tasks.
