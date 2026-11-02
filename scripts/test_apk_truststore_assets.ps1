param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $ApkPath = "",
    [int] $MinimumPaaCertificates = 1,
    [switch] $RequireCdTrustKeys,
    [switch] $RequireChipControllerNativeLibraries,
    [string] $ExpectedChipSourceTag = "",
    [string] $ExpectedChipSourceCommit = ""
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

function Get-TrustStoreEntries {
    param(
        [Parameter(Mandatory = $true)] [System.IO.Compression.ZipArchive] $Zip,
        [Parameter(Mandatory = $true)] [string] $Prefix
    )

    return @($Zip.Entries | Where-Object {
        $_.FullName.StartsWith($Prefix, [System.StringComparison]::Ordinal) -and
        $_.Length -gt 0 -and
        ($_.FullName.EndsWith(".pem", [System.StringComparison]::OrdinalIgnoreCase) -or
            $_.FullName.EndsWith(".der", [System.StringComparison]::OrdinalIgnoreCase))
    })
}

function Get-ZipEntrySha256 {
    param([Parameter(Mandatory = $true)] [System.IO.Compression.ZipArchiveEntry] $Entry)

    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $stream = $Entry.Open()
        try {
            return ([System.BitConverter]::ToString($sha256.ComputeHash($stream)) -replace '-', '').ToLowerInvariant()
        } finally {
            $stream.Dispose()
        }
    } finally {
        $sha256.Dispose()
    }
}

if (-not (Test-Path -LiteralPath $ProjectRoot -PathType Container)) {
    throw "Project root does not exist: $ProjectRoot"
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $ProjectRoot "app\build\outputs\apk\debug\openhab-matter-helper.apk"
}

Assert-True (Test-Path -LiteralPath $ApkPath -PathType Leaf) "APK not found: $ApkPath"
Assert-True ($MinimumPaaCertificates -ge 1) "MinimumPaaCertificates must be at least 1."

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($ApkPath)
try {
    $paaEntries = @(Get-TrustStoreEntries -Zip $zip -Prefix "assets/matter/truststore/paa/")
    Assert-True ($paaEntries.Count -ge $MinimumPaaCertificates) (
        "APK is missing packaged PAA trust-store certificates under assets/matter/truststore/paa/. Found " +
        $paaEntries.Count + ", expected at least " + $MinimumPaaCertificates + ".")

    if ($RequireCdTrustKeys) {
        $cdEntries = @(Get-TrustStoreEntries -Zip $zip -Prefix "assets/matter/truststore/cd/")
        Assert-True ($cdEntries.Count -gt 0) "APK is missing packaged CD trust keys under assets/matter/truststore/cd/."
    }

    if ($RequireChipControllerNativeLibraries) {
        foreach ($library in @("libCHIPController.so", "libc++_shared.so")) {
            $entry = $zip.GetEntry("lib/arm64-v8a/$library")
            Assert-True ($null -ne $entry -and $entry.Length -gt 0) (
                "APK is missing packaged connectedhomeip native library lib/arm64-v8a/$library.")
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($ExpectedChipSourceTag) -or
        -not [string]::IsNullOrWhiteSpace($ExpectedChipSourceCommit)) {
        $manifestEntry = $zip.GetEntry("assets/matter/connectedhomeip/chip-artifact-manifest.json")
        Assert-True ($null -ne $manifestEntry) (
            "APK is missing connectedhomeip provenance manifest assets/matter/connectedhomeip/chip-artifact-manifest.json.")
        $reader = [System.IO.StreamReader]::new($manifestEntry.Open())
        try {
            $manifest = $reader.ReadToEnd() | ConvertFrom-Json
        } finally {
            $reader.Dispose()
        }
        if (-not [string]::IsNullOrWhiteSpace($ExpectedChipSourceTag)) {
            Assert-True ($manifest.connectedhomeip_tag -eq $ExpectedChipSourceTag) (
                "APK connectedhomeip tag mismatch: expected $ExpectedChipSourceTag but found $($manifest.connectedhomeip_tag).")
        }
        if (-not [string]::IsNullOrWhiteSpace($ExpectedChipSourceCommit)) {
            Assert-True ($manifest.connectedhomeip_commit -eq $ExpectedChipSourceCommit) (
                "APK connectedhomeip commit mismatch: expected $ExpectedChipSourceCommit but found $($manifest.connectedhomeip_commit).")
        }

        Assert-True ($null -ne $manifest.files) (
            "APK connectedhomeip manifest does not list artifact file checksums.")
        $manifestFileChecksums = @{}
        foreach ($property in $manifest.files.PSObject.Properties) {
            $manifestFileChecksums[$property.Name] = $property.Value.ToString().ToLowerInvariant()
        }
        foreach ($trustStore in @(
            @{ ApkPrefix = "assets/matter/truststore/paa/"; ManifestPrefix = "paa-root-certs/" },
            @{ ApkPrefix = "assets/matter/truststore/cd/"; ManifestPrefix = "cd-certs/" }
        )) {
            foreach ($entry in @(Get-TrustStoreEntries -Zip $zip -Prefix $trustStore.ApkPrefix)) {
                $relative = $entry.FullName.Substring($trustStore.ApkPrefix.Length)
                $manifestPath = $trustStore.ManifestPrefix + $relative
                Assert-True ($manifestFileChecksums.ContainsKey($manifestPath)) (
                    "APK trust-store asset is not covered by the embedded connectedhomeip manifest: " + $entry.FullName)
                $actualHash = Get-ZipEntrySha256 -Entry $entry
                Assert-True ($actualHash -eq $manifestFileChecksums[$manifestPath]) (
                    "APK trust-store asset checksum does not match the embedded connectedhomeip manifest: " + $entry.FullName)
            }
        }
    }
} finally {
    $zip.Dispose()
}

Write-Output "APK trust-store asset smoke-test passed."
