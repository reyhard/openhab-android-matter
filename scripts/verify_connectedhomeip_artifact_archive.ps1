param(
    [Parameter(Mandatory = $true)] [string] $ArchivePath,
    [Parameter(Mandatory = $true)] [string] $ExpectedSha256,
    [Parameter(Mandatory = $true)] [string] $ExpectedSourceTag,
    [Parameter(Mandatory = $true)] [string] $ExpectedSourceCommit,
    [string] $SidecarPath = "",
    [string] $ExtractTo = "",
    [string] $ExpectedAbi = "arm64-v8a"
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

function Get-Sha256 {
    param([Parameter(Mandatory = $true)] [string] $Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

$requiredFiles = @(
    "CHIPController.jar",
    "CHIPInteractionModel.jar",
    "CHIPClusterID.jar",
    "CHIPClusters.jar",
    "AndroidPlatform.jar",
    "OnboardingPayload.jar",
    "libMatterTlv.jar",
    "libMatterJson.jar",
    "jniLibs/$ExpectedAbi/libCHIPController.so",
    "jniLibs/$ExpectedAbi/libc++_shared.so"
)

Assert-True (Test-Path -LiteralPath $ArchivePath -PathType Leaf) "Artifact archive not found: $ArchivePath"
$expected = $ExpectedSha256.Trim().ToLowerInvariant()
Assert-True ($expected -match '^[0-9a-f]{64}$') "ExpectedSha256 must be a 64-character hex SHA-256."
Assert-True (-not [string]::IsNullOrWhiteSpace($ExpectedSourceTag)) "ExpectedSourceTag is required."
Assert-True ($ExpectedSourceCommit -match '^[0-9a-f]{40}$') "ExpectedSourceCommit must be a 40-character hex commit."

$actual = Get-Sha256 -Path $ArchivePath
Assert-True ($actual -eq $expected) (
    "connectedhomeip artifact archive SHA-256 mismatch: expected $expected but found $actual. Refusing to extract.")

if (-not [string]::IsNullOrWhiteSpace($SidecarPath)) {
    Assert-True (Test-Path -LiteralPath $SidecarPath -PathType Leaf) "Artifact SHA-256 sidecar not found: $SidecarPath"
    $sidecar = (Get-Content -LiteralPath $SidecarPath -Raw).Trim()
    Assert-True ($sidecar -match '^([0-9a-fA-F]{64})\s+') "Artifact SHA-256 sidecar has an unexpected format."
    $sidecarHash = $Matches[1].ToLowerInvariant()
    Assert-True ($sidecarHash -eq $expected) (
        "Artifact SHA-256 sidecar does not match the pinned digest: expected $expected but sidecar has $sidecarHash.")
}

$tempRoot = $null
if ([string]::IsNullOrWhiteSpace($ExtractTo)) {
    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("openhab-chip-verify-" + [System.Guid]::NewGuid().ToString("N"))
    $ExtractTo = $tempRoot
} else {
    Remove-Item -LiteralPath $ExtractTo -Recurse -Force -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Force -Path $ExtractTo | Out-Null

try {
    Expand-Archive -LiteralPath $ArchivePath -DestinationPath $ExtractTo -Force

    $manifestPath = Join-Path $ExtractTo "chip-artifact-manifest.json"
    Assert-True (Test-Path -LiteralPath $manifestPath -PathType Leaf) (
        "Artifact is missing chip-artifact-manifest.json.")

    try {
        $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    } catch {
        throw "Artifact manifest is not valid JSON: $manifestPath"
    }

    Assert-True ($manifest.connectedhomeip_tag -eq $ExpectedSourceTag) (
        "Artifact source tag mismatch: expected $ExpectedSourceTag but manifest has $($manifest.connectedhomeip_tag).")
    Assert-True ($manifest.connectedhomeip_commit -eq $ExpectedSourceCommit) (
        "Artifact source commit mismatch: expected $ExpectedSourceCommit but manifest has $($manifest.connectedhomeip_commit).")
    Assert-True ($manifest.abi -eq $ExpectedAbi) (
        "Artifact ABI mismatch: expected $ExpectedAbi but manifest has $($manifest.abi).")

    $manifestFiles = $manifest.files
    Assert-True ($null -ne $manifestFiles) "Artifact manifest does not list file checksums."

    foreach ($relative in $requiredFiles) {
        $absolute = Join-Path $ExtractTo $relative
        Assert-True (Test-Path -LiteralPath $absolute -PathType Leaf) "Artifact is missing required file: $relative"
        Assert-True ((Get-Item -LiteralPath $absolute).Length -gt 0) "Artifact contains an empty required file: $relative"
    }

    foreach ($property in $manifestFiles.PSObject.Properties) {
        $relative = $property.Name
        $absolute = Join-Path $ExtractTo $relative
        Assert-True (Test-Path -LiteralPath $absolute -PathType Leaf) (
            "Artifact manifest lists a missing file: $relative")
        $actualFileHash = Get-Sha256 -Path $absolute
        $expectedFileHash = $property.Value.ToString().ToLowerInvariant()
        Assert-True ($actualFileHash -eq $expectedFileHash) (
            "Artifact file checksum mismatch for $relative`: expected $expectedFileHash but found $actualFileHash.")
    }

    Write-Output "connectedhomeip artifact provenance verification passed."
    Write-Output "  tag: $ExpectedSourceTag"
    Write-Output "  commit: $ExpectedSourceCommit"
    Write-Output "  sha256: $expected"
} finally {
    if ($null -ne $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
