param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [Parameter(Mandatory = $true)] [bool] $Condition,
        [Parameter(Mandatory = $true)] [string] $Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$releaseWorkflowPath = Join-Path $ProjectRoot ".github\workflows\release.yml"
$androidWorkflowPath = Join-Path $ProjectRoot ".github\workflows\android.yml"
$buildGradlePath = Join-Path $ProjectRoot "app\build.gradle"

Assert-True (Test-Path -LiteralPath $releaseWorkflowPath -PathType Leaf) "Release workflow not found: $releaseWorkflowPath"
Assert-True (Test-Path -LiteralPath $androidWorkflowPath -PathType Leaf) "Android workflow not found: $androidWorkflowPath"
Assert-True (Test-Path -LiteralPath $buildGradlePath -PathType Leaf) "App Gradle file not found: $buildGradlePath"

$releaseWorkflow = Get-Content -LiteralPath $releaseWorkflowPath -Raw
$androidWorkflow = Get-Content -LiteralPath $androidWorkflowPath -Raw
$buildGradle = Get-Content -LiteralPath $buildGradlePath -Raw

Assert-True ($releaseWorkflow.Contains("APK_PATH: app/build/outputs/apk/release/openhab-matter-helper.apk")) `
    "Release workflow must publish the release APK, not the debug APK."
Assert-True ($releaseWorkflow.Contains('ANDROID_RELEASE_KEYSTORE_BASE64: ${{ secrets.ANDROID_RELEASE_KEYSTORE_BASE64 }}')) `
    "Release workflow must read the base64 release keystore from GitHub secrets."
Assert-True ($releaseWorkflow.Contains("openhabMatterReleaseStoreFile=")) `
    "Release workflow must pass the decoded keystore path to Gradle."
Assert-True ($releaseWorkflow.Contains("openhabMatterReleaseStorePassword=")) `
    "Release workflow must pass the keystore password to Gradle."
Assert-True ($releaseWorkflow.Contains("openhabMatterReleaseKeyAlias=")) `
    "Release workflow must pass the key alias to Gradle."
Assert-True ($releaseWorkflow.Contains("openhabMatterReleaseKeyPassword=")) `
    "Release workflow must pass the key password to Gradle."
Assert-True ($releaseWorkflow.Contains("apksigner") -and $releaseWorkflow.Contains("verify --print-certs")) `
    "Release workflow must verify the signed APK certificate before upload."

Assert-True ($androidWorkflow.Contains("APK_PATH: app/build/outputs/apk/debug/openhab-matter-helper.apk")) `
    "Regular Android CI should keep publishing the debug APK without requiring release secrets."

Assert-True ($buildGradle.Contains("openhabMatterReleaseStoreFile")) `
    "Gradle config must define the release signing store file property."
Assert-True ($buildGradle.Contains("signingConfigs")) `
    "Gradle config must define release signing configuration."
Assert-True ($buildGradle.Contains("signingConfig signingConfigs.release")) `
    "Release build type must use the configured release signing key when present."

Write-Output "Release signing pipeline smoke-test passed."
