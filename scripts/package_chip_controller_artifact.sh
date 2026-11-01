#!/usr/bin/env bash
#
# Package connectedhomeip Android controller artifacts for openhab-android-matter.
#
# This script is intentionally independent of the build itself. It validates the
# expected controller layout, records provenance, and writes the archive and its
# SHA-256 sidecar. It never publishes anything.

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: package_chip_controller_artifact.sh \
  --build-lib-dir DIR \
  --paa-root DIR \
  --output-dir DIR \
  --archive PATH \
  --source-tag TAG \
  --source-commit SHA \
  --abi ABI \
  [--build-target TARGET] \
  [--build-profile PROFILE] \
  [--jdk VERSION] \
  [--ndk VERSION] \
  [--kotlin VERSION] \
  [--build-job-url URL] \
  [--cd-root DIR]
EOF
}

BUILD_LIB_DIR=""
PAA_ROOT=""
CD_ROOT=""
OUTPUT_DIR=""
ARCHIVE=""
SOURCE_TAG=""
SOURCE_COMMIT=""
ABI=""
BUILD_TARGET="android-arm64-chip-tool"
BUILD_PROFILE="release"
JDK_VERSION="17"
NDK_VERSION="28.2.13676358"
KOTLIN_VERSION="2.1.21"
BUILD_JOB_URL=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --build-lib-dir) BUILD_LIB_DIR="$2"; shift 2 ;;
        --paa-root) PAA_ROOT="$2"; shift 2 ;;
        --cd-root) CD_ROOT="$2"; shift 2 ;;
        --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
        --archive) ARCHIVE="$2"; shift 2 ;;
        --source-tag) SOURCE_TAG="$2"; shift 2 ;;
        --source-commit) SOURCE_COMMIT="$2"; shift 2 ;;
        --abi) ABI="$2"; shift 2 ;;
        --build-target) BUILD_TARGET="$2"; shift 2 ;;
        --build-profile) BUILD_PROFILE="$2"; shift 2 ;;
        --jdk) JDK_VERSION="$2"; shift 2 ;;
        --ndk) NDK_VERSION="$2"; shift 2 ;;
        --kotlin) KOTLIN_VERSION="$2"; shift 2 ;;
        --build-job-url) BUILD_JOB_URL="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
    esac
done

for required in BUILD_LIB_DIR PAA_ROOT OUTPUT_DIR ARCHIVE SOURCE_TAG SOURCE_COMMIT ABI; do
    if [[ -z "${!required}" ]]; then
        echo "Missing required argument for $required" >&2
        usage >&2
        exit 2
    fi
done

JARS=(
    CHIPController.jar
    CHIPInteractionModel.jar
    CHIPClusterID.jar
    CHIPClusters.jar
    AndroidPlatform.jar
    OnboardingPayload.jar
    libMatterTlv.jar
    libMatterJson.jar
)
NATIVE_LIBRARIES=(
    libCHIPController.so
    libc++_shared.so
)

fail() {
    echo "ERROR: $*" >&2
    exit 1
}

[[ -d "$BUILD_LIB_DIR" ]] || fail "build lib directory does not exist: $BUILD_LIB_DIR"
[[ -d "$PAA_ROOT" ]] || fail "PAA root does not exist: $PAA_ROOT"
[[ -n "$SOURCE_TAG" && -n "$SOURCE_COMMIT" ]] || fail "source tag and commit are required"

rm -rf "$OUTPUT_DIR" "$ARCHIVE" "${ARCHIVE}.sha256"
mkdir -p "$OUTPUT_DIR/jniLibs/$ABI"

for jar in "${JARS[@]}"; do
    if [[ "$jar" == "AndroidPlatform.jar" ]]; then
        src="$BUILD_LIB_DIR/src/platform/android/$jar"
    else
        src="$BUILD_LIB_DIR/src/controller/java/$jar"
    fi
    [[ -f "$src" ]] || fail "missing controller jar: $src"
    [[ -s "$src" ]] || fail "empty controller jar: $src"
    unzip -tq "$src" >/dev/null || fail "invalid controller jar: $src"
    cp "$src" "$OUTPUT_DIR/$jar"
done

for library in "${NATIVE_LIBRARIES[@]}"; do
    src="$BUILD_LIB_DIR/jni/$ABI/$library"
    [[ -f "$src" ]] || fail "missing native library: $src"
    [[ -s "$src" ]] || fail "empty native library: $src"
    cp "$src" "$OUTPUT_DIR/jniLibs/$ABI/$library"
done

PAA_CERT_COUNT="$(find "$PAA_ROOT" -type f \( -name '*.pem' -o -name '*.der' \) | wc -l | tr -d '[:space:]')"
[[ "$PAA_CERT_COUNT" -ge 1 ]] || fail "no PAA trust-store certificates found in $PAA_ROOT"
PAA_IKEA_MATCH="$(find "$PAA_ROOT" -type f \( -name '*.pem' -o -name '*.der' \) -print | grep -Ei 'IKEA|117C' || true)"
if [[ -z "$PAA_IKEA_MATCH" ]]; then
    fail "PAA trust store does not include IKEA VID 0x117C root required by validated device logs"
fi
mkdir -p "$OUTPUT_DIR/paa-root-certs"
cp -a "$PAA_ROOT"/. "$OUTPUT_DIR/paa-root-certs/"

PAA_DIGEST="$(cd "$PAA_ROOT" && find . -type f \( -name '*.pem' -o -name '*.der' \) -print0 | sort -z | xargs -0 sha256sum | sha256sum | awk '{print $1}')"

CD_CERT_COUNT=0
CD_DIGEST=""
if [[ -n "$CD_ROOT" && -d "$CD_ROOT" ]]; then
    CD_CERT_COUNT="$(find "$CD_ROOT" -type f \( -name '*.pem' -o -name '*.der' \) | wc -l | tr -d '[:space:]')"
    if [[ "$CD_CERT_COUNT" -gt 0 ]]; then
        mkdir -p "$OUTPUT_DIR/cd-certs"
        cp -a "$CD_ROOT"/. "$OUTPUT_DIR/cd-certs/"
        CD_DIGEST="$(cd "$CD_ROOT" && find . -type f \( -name '*.pem' -o -name '*.der' \) -print0 | sort -z | xargs -0 sha256sum | sha256sum | awk '{print $1}')"
    fi
fi

FILES_JSON="$(cd "$OUTPUT_DIR" && find . -type f ! -name chip-artifact-manifest.json -print0 \
    | sort -z \
    | xargs -0 sha256sum \
    | awk '{path=$2; sub(/^\.\//, "", path); printf "%s\t%s\n", path, $1}' \
    | jq -R -s 'split("\n") | map(select(length > 0) | split("\t") | {key: .[0], value: .[1]}) | from_entries')"

jq -n \
    --arg schema "1" \
    --arg tag "$SOURCE_TAG" \
    --arg commit "$SOURCE_COMMIT" \
    --arg abi "$ABI" \
    --arg target "$BUILD_TARGET" \
    --arg profile "$BUILD_PROFILE" \
    --arg jdk "$JDK_VERSION" \
    --arg ndk "$NDK_VERSION" \
    --arg kotlin "$KOTLIN_VERSION" \
    --arg build_job_url "$BUILD_JOB_URL" \
    --arg generated_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --arg paa_digest "$PAA_DIGEST" \
    --argjson paa_count "$PAA_CERT_COUNT" \
    --arg cd_digest "$CD_DIGEST" \
    --argjson cd_count "$CD_CERT_COUNT" \
    --argjson files "$FILES_JSON" \
    '{
        schema: ($schema | tonumber),
        connectedhomeip_tag: $tag,
        connectedhomeip_commit: $commit,
        abi: $abi,
        build_target: $target,
        build_profile: $profile,
        toolchain: {
            jdk: $jdk,
            ndk: $ndk,
            kotlin: $kotlin
        },
        build_job_url: (if $build_job_url == "" then null else $build_job_url end),
        generated_at: $generated_at,
        paa_trust_store: {
            cert_count: $paa_count,
            sha256: $paa_digest
        },
        cd_trust_store: (
            if $cd_count > 0 then {cert_count: $cd_count, sha256: $cd_digest} else null end
        ),
        files: $files
    }' > "$OUTPUT_DIR/chip-artifact-manifest.json"

(
    cd "$OUTPUT_DIR"
    zip -qr "$ARCHIVE" .
)

(
    cd "$(dirname "$ARCHIVE")"
    sha256sum "$(basename "$ARCHIVE")" > "$(basename "$ARCHIVE").sha256"
)

echo "Packaged connectedhomeip artifact:"
echo "  archive: $ARCHIVE"
echo "  manifest: $OUTPUT_DIR/chip-artifact-manifest.json"
echo "  source: $SOURCE_TAG ($SOURCE_COMMIT)"
echo "  abi: $ABI"
echo "  sha256: $(awk '{print $1}' "${ARCHIVE}.sha256")"
