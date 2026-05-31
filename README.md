# openhab-android-matter

Android companion app for adding Matter-over-Thread devices to openHAB.

The app is meant to guide a user through the practical handoff that openHAB
currently needs: commission the device to the phone first, open a temporary
Matter commissioning window from the phone, send that temporary setup code to
openHAB Matter discovery, and wait for the device to appear in the openHAB
Inbox.

## Current State

- Real connectedhomeip commissioning has been validated on one Android phone
  and one Thread Matter device, including the Compose automated setup path from
  QR scan through openHAB Inbox success.
- The current validated handoff uses the manual setup code returned by
  OpenCommissioningWindow and submits it to openHAB Matter Scan Input.
- The app fails closed when connectedhomeip artifacts or runtime readiness are
  missing. Real Thread commissioning and OpenCommissioningWindow are not
  silently routed through the fake controller.
- Broader hardware validation, long-run fabric persistence hardening, and full
  phone-side Matter fabric inventory are still future work.

For detailed implementation status, see
[docs/implementation-status.md](docs/implementation-status.md). For the
OpenCommissioningWindow internals, see
[docs/open-commissioning-window-workflow.md](docs/open-commissioning-window-workflow.md).

## What You Need

- An Android phone with Bluetooth, location services, and network access
  enabled.
- openHAB with the Matter binding installed and an online Matter controller.
- A Thread Border Router for the same Thread network the device should join.
- The Thread Active Operational Dataset. The app can store it encrypted after
  manual entry; Thread Border Router discovery only detects visible border
  routers and does not extract the dataset.
- A Matter-over-Thread device in factory-new state or pairing mode.
- The Matter QR code or manual setup code from the device or its packaging.
- Optional: an openHAB REST API token if your openHAB REST API requires
  authentication.

Keep the phone, Thread Border Router, and openHAB host on networks where IPv6
Matter traffic can work. IPv4 reachability to the Thread Border Router is not
enough for openHAB Matter pairing.

## Troubleshooting in the App

The app includes advanced troubleshooting screens for:

- openHAB Matter binding/controller readiness.
- Bluetooth, location service, runtime permission, Wi-Fi/mobile-data, and VPN
  checks on the phone.
- Phone-side Matter mDNS browsing for `_matterc._udp` and `_matter._tcp`.
- Best-effort phone-side IPv6 reachability to a user-entered device address.
- Retrying OpenCommissioningWindow for the currently staged phone-side device.
- Forgetting this app's stored bootstrap device state on the phone.

Phone-side mDNS and IPv6 checks are useful diagnostics, but they do not prove
what openHAB, Avahi, the router, or the Thread Border Router can see. If openHAB
pairing fails after the phone succeeds, compare phone logs with openHAB-side
mDNS/Avahi output and check for stale `_matterc._udp` records.

## Current Limitations

- Real-device validation has only covered a limited hardware set so far.
- The **Devices on this phone** screen shows this app's stored staging state,
  not a full connectedhomeip fabric inventory.
- The app cannot clear stale Avahi/router mDNS records from the openHAB host.
- connectedhomeip fabric persistence still needs broader reboot, upgrade, and
  reinstall validation.

## Build Configuration

This workspace uses:

- Android SDK: `D:\Tools\Android\SDK`
- Android Gradle Plugin: `8.11.1`
- Compile SDK: `36`
- Target SDK: `35`
- Min SDK: `26`
- Java: `17`

## Build

Build and test the debug APK:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

The APK is written to:

```text
app\build\outputs\apk\debug\openhab-matter-helper.apk
```

The default source build packages a non-production JNI stub and is useful for
offline tests and diagnostics. To pair real Matter devices, package official
connectedhomeip Android controller artifacts as described below.

## connectedhomeip Artifacts for Real Pairing

Real commissioning uses the reflection-backed connectedhomeip Java controller
path. To package the official connectedhomeip Android controller artifacts used
by that path, pass `-PopenhabMatterChipControllerArtifactsDir=<dir>` where
`<dir>` contains CHIPTool-style jars and native libraries:

```text
CHIPController.jar
CHIPInteractionModel.jar
CHIPClusterID.jar
CHIPClusters.jar
AndroidPlatform.jar
OnboardingPayload.jar
libMatterTlv.jar
libMatterJson.jar
jniLibs\arm64-v8a\libCHIPController.so
jniLibs\arm64-v8a\libc++_shared.so
jniLibs\armeabi-v7a\libCHIPController.so
jniLibs\armeabi-v7a\libc++_shared.so
jniLibs\x86\libCHIPController.so
jniLibs\x86\libc++_shared.so
jniLibs\x86_64\libCHIPController.so
jniLibs\x86_64\libc++_shared.so
```

Example build for an arm64 device:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug -PopenhabMatterChipControllerArtifactsDir=<artifact-dir> -PopenhabMatterChipControllerAbis=arm64-v8a
```

### GitHub Actions Packaging

The `Android CI` workflow now requires connectedhomeip artifacts and builds APKs
with `-PopenhabMatterChipControllerArtifactsDir` so uploaded CI APKs include
the CHIP controller.

The artifact source path is managed automatically:

- Workflow `Build CHIP Controller Artifacts` builds arm64 CHIPTool-style
  artifacts from connectedhomeip and publishes release tag
  `chip-controller-artifacts-latest` with asset
  `openhab-chip-artifacts-arm64.zip`.
- `Android CI` downloads that release asset by tag on every run. No manual
  URL/secret path updates are required after the release asset is published.
- Workflow `Release APK` is manually dispatched. It downloads the same CHIP
  controller artifact, runs tests/lint/artifact checks, builds the APK, generates
  release notes from commit history, and creates or updates a GitHub release.
  See `CONTRIBUTING.md` for commit message rules used by the generated changelog.

For production attestation verification, also package local trust-store
certificates with `-PopenhabMatterChipPaaTrustStoreDir=<dir>` and optionally
`-PopenhabMatterChipCdTrustStoreDir=<dir>`, where `<dir>` paths point to local
connectedhomeip DCL mirror outputs such as
`credentials\production\paa-root-certs` and `credentials\production\cd-certs`.

The Gradle verifier rejects missing native libraries, empty files, corrupt
controller jars, and controller jar sets that do not contain the class entries
required by runtime readiness. Placeholder files cannot pass the packaging gate.

Run the synthetic artifact validation smoke test with:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_connectedhomeip_artifacts.ps1
```

The legacy native bridge can also package a prebuilt
`libopenhab_matter_chip.so` by passing
`-PopenhabMatterChipNativeMode=prebuilt "-PopenhabMatterChipPrebuiltDir=<dir>"`
where `<dir>` contains ABI subdirectories with `libopenhab_matter_chip.so`.
Current real commissioning uses the Java/JNI connectedhomeip controller
artifacts above, not the bundled JNI stub.

## Install

Connect an Android device with USB debugging enabled, then run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1
```

The helper runs the offline unit tests and debug APK build, verifies
`app\build\outputs\apk\debug\openhab-matter-helper.apk`, lists ready ADB devices, and
installs only when exactly one device or emulator is attached. If multiple
devices are attached, pass `-Serial <device-id>`. If the APK is already built
and you only want the ADB check/install step, pass `-SkipBuild`.

To verify package readiness without an attached device, stop before ADB with:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly
```

The same helper can build artifact-specific variants by forwarding Gradle
properties:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly -ChipControllerArtifactsDir <artifact-dir>
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly -ChipNativeMode prebuilt -ChipPrebuiltDir <prebuilt-dir>
```

Run the APK badging smoke test with:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_apk_badging.ps1
```

## Developer References

- [docs/implementation-status.md](docs/implementation-status.md): current
  capability and validation status.
- [docs/open-commissioning-window-workflow.md](docs/open-commissioning-window-workflow.md):
  OCW control flow, parameters, source map, and failure paths.
- [docs/research.md](docs/research.md): architecture and research background.
- [docs/chip-jni-integration.md](docs/chip-jni-integration.md): native bridge
  notes.

## Trademark Disclaimer

Product names, logos, brands and other trademarks referred to within the openHAB website are the property of their respective  trademark holders. These trademark holders are not affiliated with  openHAB or our website. They do not sponsor or endorse our materials.

Matter logo is trademark of Connectivity Standards Alliance
