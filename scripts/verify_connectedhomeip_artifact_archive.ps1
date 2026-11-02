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

function Test-SafeRelativePath {
    param([Parameter(Mandatory = $true)] [string] $Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $false
    }
    if ($Path.Contains([char]0) -or $Path.Contains('\') -or $Path.Contains(':')) {
        return $false
    }
    if ($Path.StartsWith("/", [System.StringComparison]::Ordinal)) {
        return $false
    }
    if ($Path -match '^[A-Za-z]:') {
        return $false
    }

    $segments = $Path.Split('/')
    foreach ($segment in $segments) {
        if ([string]::IsNullOrEmpty($segment) -or $segment -eq "." -or $segment -eq "..") {
            return $false
        }
    }
    return $true
}

function Get-ExtractedFileMap {
    param(
        [Parameter(Mandatory = $true)] [string] $Root,
        [Parameter(Mandatory = $true)] [string] $ManifestRelativePath
    )

    $rootFull = [System.IO.Path]::GetFullPath($Root).TrimEnd([System.IO.Path]::DirectorySeparatorChar)
    $files = [System.Collections.Generic.Dictionary[string, string]]::new([System.StringComparer]::Ordinal)
    $caseInsensitive = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($file in @(Get-ChildItem -LiteralPath $rootFull -Recurse -File -Force)) {
        if (($file.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Artifact contains an unsupported reparse-point file: $($file.FullName)"
        }
        $relative = [System.IO.Path]::GetRelativePath($rootFull, $file.FullName).Replace('\', '/')
        if ($relative -eq $ManifestRelativePath) {
            continue
        }
        Assert-True (Test-SafeRelativePath $relative) "Artifact contains an unsafe file path: $relative"
        Assert-True (-not $files.ContainsKey($relative)) "Artifact contains duplicate file path: $relative"
        Assert-True ($caseInsensitive.Add($relative)) "Artifact contains case-colliding file path: $relative"
        $files[$relative] = $file.FullName
    }

    return $files
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
$manifestRelativePath = "chip-artifact-manifest.json"

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
    $ExtractTo = [System.IO.Path]::GetFullPath($ExtractTo)
    if (Test-Path -LiteralPath $ExtractTo) {
        Assert-True (Test-Path -LiteralPath $ExtractTo -PathType Container) (
            "ExtractTo exists but is not a directory: $ExtractTo")
        Assert-True (@(Get-ChildItem -LiteralPath $ExtractTo -Force).Count -eq 0) (
            "ExtractTo must be a new or empty directory; refusing to remove caller files: $ExtractTo")
    }
}
New-Item -ItemType Directory -Force -Path $ExtractTo | Out-Null

try {
    Expand-Archive -LiteralPath $ArchivePath -DestinationPath $ExtractTo -Force

    $manifestPath = Join-Path $ExtractTo $manifestRelativePath
    Assert-True (Test-Path -LiteralPath $manifestPath -PathType Leaf) (
        "Artifact is missing chip-artifact-manifest.json.")

    $manifestText = Get-Content -LiteralPath $manifestPath -Raw
    $manifestDocument = $null
    try {
        $manifestDocument = [System.Text.Json.JsonDocument]::Parse($manifestText)
    } catch {
        throw "Artifact manifest is not valid JSON: $manifestPath"
    }

    try {
        $manifestRoot = $manifestDocument.RootElement
        Assert-True ($manifestRoot.ValueKind -eq [System.Text.Json.JsonValueKind]::Object) (
            "Artifact manifest must be a JSON object.")

        $manifestPropertyNames = @($manifestRoot.EnumerateObject() | ForEach-Object { $_.Name })
        foreach ($requiredProperty in @("connectedhomeip_tag", "connectedhomeip_commit", "abi", "files")) {
            Assert-True ($manifestPropertyNames -contains $requiredProperty) (
                "Artifact manifest is missing $requiredProperty.")
        }

        $manifestTag = $manifestRoot.GetProperty("connectedhomeip_tag").GetString()
        $manifestCommit = $manifestRoot.GetProperty("connectedhomeip_commit").GetString()
        $manifestAbi = $manifestRoot.GetProperty("abi").GetString()
        Assert-True ($manifestTag -eq $ExpectedSourceTag) (
            "Artifact source tag mismatch: expected $ExpectedSourceTag but manifest has $manifestTag.")
        Assert-True ($manifestCommit -eq $ExpectedSourceCommit) (
            "Artifact source commit mismatch: expected $ExpectedSourceCommit but manifest has $manifestCommit.")
        Assert-True ($manifestAbi -eq $ExpectedAbi) (
            "Artifact ABI mismatch: expected $ExpectedAbi but manifest has $manifestAbi.")

        $filesElement = $manifestRoot.GetProperty("files")
        Assert-True ($filesElement.ValueKind -eq [System.Text.Json.JsonValueKind]::Object) (
            "Artifact manifest files must be a JSON object.")

        $manifestHashes = [System.Collections.Generic.Dictionary[string, string]]::new([System.StringComparer]::Ordinal)
        $manifestCaseInsensitive = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        foreach ($property in $filesElement.EnumerateObject()) {
            $relative = $property.Name
            Assert-True (Test-SafeRelativePath $relative) "Artifact manifest contains an unsafe file path: $relative"
            Assert-True ($relative -ne $manifestRelativePath) (
                "Artifact manifest must not list itself as a checksummed file: $relative")
            Assert-True (-not $manifestHashes.ContainsKey($relative)) (
                "Artifact manifest contains a duplicate file entry: $relative")
            Assert-True ($manifestCaseInsensitive.Add($relative)) (
                "Artifact manifest contains case-colliding file entries: $relative")
            Assert-True ($property.Value.ValueKind -eq [System.Text.Json.JsonValueKind]::String) (
                "Artifact manifest checksum for $relative must be a string.")
            $manifestHashes[$relative] = $property.Value.GetString().ToLowerInvariant()
        }

        $extractedFiles = Get-ExtractedFileMap -Root $ExtractTo -ManifestRelativePath $manifestRelativePath

        foreach ($relative in $requiredFiles) {
            Assert-True ($manifestHashes.ContainsKey($relative)) (
                "Artifact manifest does not list required file: $relative")
            Assert-True ($extractedFiles.ContainsKey($relative)) (
                "Artifact is missing required file: $relative")
            Assert-True ((Get-Item -LiteralPath $extractedFiles[$relative]).Length -gt 0) (
                "Artifact contains an empty required file: $relative")
        }

        $paaCertificates = @(Get-ChildItem -LiteralPath (Join-Path $ExtractTo "paa-root-certs") -Recurse -File -Force -ErrorAction SilentlyContinue |
            Where-Object { $_.Extension -in @(".pem", ".der") })
        Assert-True ($paaCertificates.Count -gt 0) "Artifact is missing packaged PAA trust-store certificates."

        foreach ($relative in $manifestHashes.Keys) {
            Assert-True ($extractedFiles.ContainsKey($relative)) (
                "Artifact manifest lists a missing file: $relative")
            $actualFileHash = Get-Sha256 -Path $extractedFiles[$relative]
            $expectedFileHash = $manifestHashes[$relative]
            Assert-True ($actualFileHash -eq $expectedFileHash) (
                "Artifact file checksum mismatch for $relative`: expected $expectedFileHash but found $actualFileHash.")
        }

        foreach ($relative in $extractedFiles.Keys) {
            Assert-True ($manifestHashes.ContainsKey($relative)) (
                "Artifact contains a file that is not listed in the manifest: $relative")
        }

        Write-Output "connectedhomeip artifact provenance verification passed."
        Write-Output "  tag: $ExpectedSourceTag"
        Write-Output "  commit: $ExpectedSourceCommit"
        Write-Output "  sha256: $expected"
    } finally {
        $manifestDocument.Dispose()
    }
} finally {
    if ($null -ne $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
