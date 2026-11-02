# connectedhomeip 1.6.1 upgrade evidence

Status: **candidate implemented; publication and hardware validation pending**. This document records
baseline evidence and validation boundaries. It is not a claim that the upgrade has been released or
hardware-validated.

Target upstream: `project-chip/connectedhomeip` tag `v1.6.1.0`, commit
`3bcdd56ba54fb88b2afb4bfef575014671df7aa7` (confirmed with `git ls-remote` and a local checkout).
Previous configured baseline: `v1.5.1.0` (`abcc720b48c5e59c0edcfe65c516f76ca9448aa3`).

## T0 baseline

Repository baseline before edits:

- Branch: detached `HEAD` at `5fb5c7d77e55b4e04aaba81e3b694fb45980d05a` (`main`,
  `origin/docs/openhab-auto-discovery-spec-2026-07-21`).
- Working tree: clean.
- Workflow defaults: `chip-controller-artifacts.yml` used `v1.5.1.0` for manual dispatch and the
  weekly schedule, and published `openhab-chip-artifacts-arm64.zip` to the mutable
  `chip-controller-artifacts-latest` release with `gh release upload --clobber`.
- `android.yml` and `release.yml` downloaded that mutable `latest` asset and did not verify a whole
  archive digest or embedded source manifest.

Published artifact inspected on 2026-11-01:

| Field | Value |
| --- | --- |
| Release tag | `chip-controller-artifacts-latest` |
| Release id | `332162707` |
| Release published | `2026-05-31T16:16:00Z` |
| Asset id | `563103466` |
| Asset name | `openhab-chip-artifacts-arm64.zip` |
| Asset size | `30,684,491` bytes |
| Asset digest (GitHub) | `sha256:e36626405d44b5411f3948558695df6789314939dc74b1ef2de690cf3225e09a` |
| Downloaded SHA-256 | `e36626405d44b5411f3948558695df6789314939dc74b1ef2de690cf3225e09a` |
| Embedded manifest | none |
| PAA certificates | 150 (includes IKEA VID `0x117C`) |
| CD certificates | 10 |
| Provenance | **unknown**; no manifest or source metadata in the archive |

The asset was updated on 2026-09-14, before the 2026-09-25 `v1.6.1.0` release. The checked-in
workflow default was `v1.5.1.0`, but the archive does not prove which source it came from. Its
`chip.devicecontroller.ICDRegistrationInfo` has no `createForDeferredConfiguration()` method, which
is consistent with a pre-1.6.1 jar but is not proof of the exact upstream commit.

Baseline workflow status:

- Weekly `Build CHIP Controller Artifacts` runs from 2026-09-21 through 2026-10-26 failed at the
  `Set up Android SDK` step. Run `40303080142` logged
  `sdkmanager ... failed with exit code 1` and `Failed to find package 'tools'`. This is a
  workflow/action-level baseline failure, not a CHIP build failure.
- `Android CI` at `5fb5c7d` succeeded on 2026-11-01 (run `41351532217`), including the step that
  downloads the mutable latest artifact.
- `CodeQL` failed at the same `Set up Android SDK` step in run `41351197688` and succeeded in the
  immediately following run `41351532202`.

Compatibility checklist to verify against the 1.6.1 jars:

- `ChipDeviceController` construction, `loadJni()`, `ControllerParams`.
- `AndroidChipPlatform` eight-argument constructor and Android BLE/platform dependencies.
- `pairDeviceThroughBLE`, `getConnectedDevicePointer`, `releaseConnectedDevicePointer`.
- `openPairingWindowWithPINCallback` and the concrete `OpenCommissioningCallback` implementation.
- `DeviceAttestationDelegate`, `AttestationTrustStoreDelegate`, attestation continuation.
- `CommissionParameters` and deferred `ICDRegistrationInfo`.
- completion-listener stage/error callbacks and ICD registration callbacks.
- PAA/CD trust-store loading and APK packaging.

## Upstream source checks

- `OpenCommissioningCallback` still declares `onError(int, long)` and
  `onSuccess(long, String, String)`.
- `ICDRegistrationInfo.createForDeferredConfiguration()` exists and returns a registration info
  with an empty symmetric-key sentinel.
- `CommissionParameters` documents that `null` ICD registration means ignore registration, while
  the deferred factory requests `onICDRegistrationInfoRequired`.
- The app already passes `createForDeferredConfiguration()` when available and keeps a builder
  fallback for older artifacts. No new ICD implementation was added.
- `ChipDeviceController.CompletionListener.onICDRegistrationComplete(long, ICDDeviceInfo)` is
  present; its first argument is an error code, not a node id. The existing diagnostics label is
  inaccurate, but it does not change commissioning control flow. This remains a separate
  diagnostics cleanup unless it becomes required by the upgrade.
- The existing `-Werror` Perl patch still matches both `_kotlinc_flags` blocks in
  `v1.6.1.0/build/chip/java/rules.gni`.

## Local candidate build

Built in WSL Ubuntu 22.04 from a `v1.6.1.0` checkout at
`3bcdd56ba54fb88b2afb4bfef575014671df7aa7` (PAA trust store refreshed from DCL;
the repository's existing Kotlin `-Werror` patch applied before the build):

```bash
python3 scripts/build/build_examples.py --target android-arm64-chip-tool --ninja-jobs 6 build
```

The build completed in 6m49s with JDK 17, Android NDK `28.2.13676358`, Kotlin
compiler `2.1.21`, and Android SDK platform 34 (the upstream Android build
requirement). The repository's existing `-Werror` removal patch still matched
`build/chip/java/rules.gni` after checkout; `-Werror` was removed only from the
Kotlin flags.

The candidate was packaged with `scripts/package_chip_controller_artifact.sh`:

| Field | Value |
| --- | --- |
| Source tag | `v1.6.1.0` |
| Source commit | `3bcdd56ba54fb88b2afb4bfef575014671df7aa7` |
| ABI | `arm64-v8a` |
| Archive | `openhab-chip-artifacts-arm64-v1.6.1.0.zip` |
| Archive size | `33,740,622` bytes |
| Archive SHA-256 | `6a0ad753ba2f905fa9fc9ba8655495d7b1abf974e5882d391e7bbb22b007ec28` |
| PAA certificates | 296 (IKEA VID `0x117C` present) |
| PAA trust-store digest | `9996ec8673dd118f6b65d1015a32f203b878bbd36991eb62032ab92fb83b9619` |
| CD certificates | 10 |
| CD trust-store digest | `42b49e73a7c9229529d6f9b4ecf6af17a24577074b02776e81582dc84df8c039` |
| `jniLibs/arm64-v8a/libCHIPController.so` | `14df58335aee1cd8bdbf8d786c553b90ac53df4ef88bdd02977c068abc58436b` |
| `jniLibs/arm64-v8a/libc++_shared.so` | `ab4e6c71b96b851de45a8a9bd86369e7dbc2130a44b3b4520564be94847910f2` |
| `CHIPController.jar` | `16a9bbbc78fb7c8b5b86a9d7041b49f4a3f8ba20cf26bad67b32bf290b2de59b` |

The archive is a local candidate under `.artifacts/` and was **not** published.
The digest above is an observed candidate checksum, not a trusted or pinned
release value. The manifest contains per-file checksums and PAA/CD digests; the
`.sha256` sidecar is a convenience copy and is not an independent trust anchor.
Before consumer use, a maintainer must verify the archive against a separately
trusted digest and pin that value.

The producer workflow runs build/bootstrap with `contents: read`. Release write
permission is granted only to a separate publishing job gated on a manual
`workflow_dispatch` with `publish=true`; scheduled builds cannot publish.

## Validation results

All commands below were executed locally on 2026-11-01 and 2026-11-02.

- Provenance verifier, positive:
  `scripts/verify_connectedhomeip_artifact_archive.ps1` passed against the
  expected archive digest, source tag, source commit, sidecar, manifest,
  required files, exact packaged-file coverage, and per-file checksums. The
  expected digest was supplied locally for verification and is **not yet
  independently pinned**.
- Provenance verifier, negative:
  - Wrong expected archive SHA-256 failed with
    `artifact archive SHA-256 mismatch ... Refusing to extract.`
  - Wrong expected source tag failed with `Artifact source tag mismatch`.
  - Empty or incomplete manifest coverage, unlisted/tampered files,
    traversal/unsafe paths, duplicate entries, missing PAA certificates, and a
    non-empty caller extraction directory failed as expected.
- Gradle artifact verification with the real candidate passed, including the
  manifest tag/commit, exact packaged-file coverage, and embedded file
  checksums.
- Gradle negative checks rejected empty/incomplete manifest coverage, a missing
  controller jar, a missing `libCHIPController.so`, an extra unlisted file, a
  tampered manifest checksum, and unsafe manifest paths. The APK provenance
  preparation task also failed when artifact verification failed, proving the
  manifest asset cannot be copied from an incomplete artifact.
- `:app:testDebugUnitTest` with the real 1.6.1 jars: **499 tests, 0 failures,
  0 skipped**. `ConnectedHomeIpV161CompatibilityTest` ran against the packaged
  jars and confirmed the reflected controller signatures, deferred ICD factory,
  completion listener, attestation delegate, `AndroidChipPlatform` constructor,
  and BLE manager constructor.
- `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` succeeded
  with the real candidate jars. Lint reported only baseline-filtered warnings;
  release vital lint reported no errors or warnings.
- The debug APK contains `lib/arm64-v8a/libCHIPController.so`,
  `lib/arm64-v8a/libc++_shared.so`, packaged PAA assets, and
  `assets/matter/connectedhomeip/chip-artifact-manifest.json`.
- APK badging and trust-store/native-library/provenance smoke checks passed.
- `apkanalyzer` confirmed the APK DEX contains
  `chip.devicecontroller.ICDRegistrationInfo.createForDeferredConfiguration()`.

**Not tested:** installing on a phone, BLE Thread commissioning, operational
IPv6 discovery, OpenCommissioningWindow, openHAB Inbox, app restart/fabric
restore, sleepy/ICD device behavior, and failure recovery. A successful build
and unit test run is not hardware validation.

## Rollback

Do not roll back by changing only a version string while keeping new native bytes.

1. If a verified immutable `v1.5.1.0` artifact exists, point both Android CI and Release consumers
   at that tag, asset name, manifest tag/commit, and pinned SHA-256.
2. If no verified artifact exists, rebuild `v1.5.1.0` from
   `abcc720b48c5e59c0edcfe65c516f76ca9448aa3` with the same packaging script and publish it as a new
   immutable versioned artifact; do not mutate `chip-controller-artifacts-latest`.
3. Restore any app-side API adaptations together with their tests.
4. Re-run artifact verification, unit tests, lint, APK packaging checks, and a smoke commissioning
   flow. A green Gradle build alone is not a rollback proof.

## Known blockers

- No authorization was given to create/publish a GitHub release or tag, push, merge, or publish a
  signed app release. The immutable `v1.6.1.0` artifact release does not exist, so CI/release
  consumers remain on the mutable `chip-controller-artifacts-latest` source. Switching
  `android.yml`, `release.yml`, or `codeql.yml` before that release is published would make PR CI
  fail on a missing asset.
- The updated producer workflow has not been executed in GitHub Actions because no push/dispatch
  authorization was given. Its checkout, build, packaging, and verification commands were executed
  locally. The weekly scheduled producer path also still hits the pre-existing
  `Set up Android SDK` / `sdkmanager "tools"` baseline failure noted above.
- Publishing the immutable artifact requires a maintainer with `contents: write` access to dispatch
  `Build CHIP Controller Artifacts` with `publish=true` (or an approved equivalent), then pin the
  published archive SHA-256 and source tag/commit in both Android CI and Release consumers.
- No real Android phone plus Thread Matter device plus Thread Border Router/openHAB environment was
  available in this task. Hardware commissioning, OCW, openHAB Inbox, sleepy/ICD, restart, and
  negative network tests are **not tested**.
- Distribution licensing (`GPL-2.0` app with Apache-2.0 connectedhomeip binaries) still requires
  owner/legal review before distributing a CHIP-bundled APK.
