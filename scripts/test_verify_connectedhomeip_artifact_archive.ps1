param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $PowerShellPath = "pwsh"
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

$verifier = Join-Path $PSScriptRoot "verify_connectedhomeip_artifact_archive.ps1"
Assert-True (Test-Path -LiteralPath $verifier -PathType Leaf) "Verifier not found: $verifier"

$tmpRoot = Join-Path (Join-Path $ProjectRoot "build") "tmp/chip-archive-verifier-smoke"
Remove-Item -LiteralPath $tmpRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $tmpRoot | Out-Null

$requiredJars = @(
    "CHIPController.jar",
    "CHIPInteractionModel.jar",
    "CHIPClusterID.jar",
    "CHIPClusters.jar",
    "AndroidPlatform.jar",
    "OnboardingPayload.jar",
    "libMatterTlv.jar",
    "libMatterJson.jar"
)

function New-SyntheticArtifact {
    param(
        [Parameter(Mandatory = $true)] [string] $Name,
        [switch] $IncludeCd,
        [switch] $OmitPaa
    )

    $root = Join-Path $tmpRoot $Name
    New-Item -ItemType Directory -Force -Path (Join-Path $root "jniLibs/arm64-v8a") | Out-Null
    foreach ($jar in $requiredJars) {
        Set-Content -LiteralPath (Join-Path $root $jar) -Value "synthetic $jar" -NoNewline
    }
    Set-Content -LiteralPath (Join-Path $root "jniLibs/arm64-v8a/libCHIPController.so") -Value "synthetic libCHIPController" -NoNewline
    Set-Content -LiteralPath (Join-Path $root "jniLibs/arm64-v8a/libc++_shared.so") -Value "synthetic libc++" -NoNewline

    if (-not $OmitPaa) {
        New-Item -ItemType Directory -Force -Path (Join-Path $root "paa-root-certs") | Out-Null
        Set-Content -LiteralPath (Join-Path $root "paa-root-certs/IKEA_vid_0x117C.pem") -Value "synthetic PAA" -NoNewline
    }
    if ($IncludeCd) {
        New-Item -ItemType Directory -Force -Path (Join-Path $root "cd-certs") | Out-Null
        Set-Content -LiteralPath (Join-Path $root "cd-certs/CD_Signing_Key.pem") -Value "synthetic CD" -NoNewline
    }

    return $root
}

function Get-FileChecksums {
    param([Parameter(Mandatory = $true)] [string] $Root)

    $files = [ordered]@{}
    Get-ChildItem -LiteralPath $Root -Recurse -File | ForEach-Object {
        $relative = [System.IO.Path]::GetRelativePath($Root, $_.FullName).Replace('\', '/')
        if ($relative -ne "chip-artifact-manifest.json") {
            $files[$relative] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    }
    return $files
}

function Write-Manifest {
    param(
        [Parameter(Mandatory = $true)] [string] $Root,
        [Parameter(Mandatory = $true)] $Files,
        [string] $Tag = "v1.6.1.0",
        [string] $Commit = "3bcdd56ba54fb88b2afb4bfef575014671df7aa7"
    )

    $manifest = [ordered]@{
        schema = 1
        connectedhomeip_tag = $Tag
        connectedhomeip_commit = $Commit
        abi = "arm64-v8a"
        build_target = "android-arm64-chip-tool"
        files = $Files
    }
    Set-Content -LiteralPath (Join-Path $Root "chip-artifact-manifest.json") `
        -Value ($manifest | ConvertTo-Json -Depth 8) -Encoding utf8NoBOM
}

function New-Archive {
    param(
        [Parameter(Mandatory = $true)] [string] $Root,
        [Parameter(Mandatory = $true)] [string] $ArchivePath
    )

    Remove-Item -LiteralPath $ArchivePath -Force -ErrorAction SilentlyContinue
    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $Root,
        $ArchivePath,
        [System.IO.Compression.CompressionLevel]::Optimal,
        $false)
    return (Get-FileHash -LiteralPath $ArchivePath -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Add-ZipEntry {
    param(
        [Parameter(Mandatory = $true)] [string] $ArchivePath,
        [Parameter(Mandatory = $true)] [string] $EntryName,
        [Parameter(Mandatory = $true)] [string] $Content
    )

    $zip = [System.IO.Compression.ZipFile]::Open($ArchivePath, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $entry = $zip.CreateEntry($EntryName)
        $writer = [System.IO.StreamWriter]::new($entry.Open())
        try {
            $writer.Write($Content)
        } finally {
            $writer.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
    return (Get-FileHash -LiteralPath $ArchivePath -Algorithm SHA256).Hash.ToLowerInvariant()
}

function New-EmptyDirectory {
    param([Parameter(Mandatory = $true)] [string] $Path)
    Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
}

function Invoke-VerifierCase {
    param(
        [Parameter(Mandatory = $true)] [string] $ArchivePath,
        [Parameter(Mandatory = $true)] [string] $Hash,
        [string[]] $ExtraArgs = @()
    )

    $output = & $PowerShellPath -NoProfile -File $verifier `
        -ArchivePath $ArchivePath `
        -ExpectedSha256 $Hash `
        -ExpectedSourceTag "v1.6.1.0" `
        -ExpectedSourceCommit "3bcdd56ba54fb88b2afb4bfef575014671df7aa7" `
        @ExtraArgs 2>&1
    return @{ ExitCode = $LASTEXITCODE; Output = ($output -join "`n") }
}

# Valid archive without optional CD certificates.
$validRoot = New-SyntheticArtifact -Name "valid"
$validFiles = Get-FileChecksums -Root $validRoot
Write-Manifest -Root $validRoot -Files $validFiles
$validArchive = Join-Path $tmpRoot "valid.zip"
$validHash = New-Archive -Root $validRoot -ArchivePath $validArchive
$valid = Invoke-VerifierCase -ArchivePath $validArchive -Hash $validHash
Assert-True ($valid.ExitCode -eq 0) ("Expected valid archive to pass. Output:`n" + $valid.Output)

# A new or empty caller-supplied extraction directory is allowed and populated.
$emptyTarget = Join-Path $tmpRoot "empty-target"
New-Item -ItemType Directory -Force -Path $emptyTarget | Out-Null
$emptyTargetResult = Invoke-VerifierCase -ArchivePath $validArchive -Hash $validHash -ExtraArgs @("-ExtractTo", $emptyTarget)
Assert-True ($emptyTargetResult.ExitCode -eq 0) ("Expected empty ExtractTo destination to pass. Output:`n" + $emptyTargetResult.Output)
Assert-True (Test-Path -LiteralPath (Join-Path $emptyTarget "CHIPController.jar") -PathType Leaf) (
    "Expected verifier to populate an empty caller-supplied ExtractTo destination.")

# Valid archive with optional CD certificates.
$cdRoot = New-SyntheticArtifact -Name "valid-cd" -IncludeCd
$cdFiles = Get-FileChecksums -Root $cdRoot
Write-Manifest -Root $cdRoot -Files $cdFiles
$cdArchive = Join-Path $tmpRoot "valid-cd.zip"
$cdHash = New-Archive -Root $cdRoot -ArchivePath $cdArchive
$cd = Invoke-VerifierCase -ArchivePath $cdArchive -Hash $cdHash
Assert-True ($cd.ExitCode -eq 0) ("Expected valid archive with optional CD certificates to pass. Output:`n" + $cd.Output)

# Duplicate ZIP entry names must be rejected before extraction.
$duplicateZip = Join-Path $tmpRoot "duplicate-entry.zip"
Copy-Item -LiteralPath $validArchive -Destination $duplicateZip -Force
$duplicateZipHash = Add-ZipEntry -ArchivePath $duplicateZip -EntryName "CHIPController.jar" -Content "duplicate"
$duplicateZipTarget = Join-Path $tmpRoot "duplicate-entry-target"
New-EmptyDirectory -Path $duplicateZipTarget
$duplicateZipResult = Invoke-VerifierCase -ArchivePath $duplicateZip -Hash $duplicateZipHash -ExtraArgs @("-ExtractTo", $duplicateZipTarget)
Assert-True ($duplicateZipResult.ExitCode -ne 0) "Expected duplicate ZIP entry names to fail."
Assert-True ($duplicateZipResult.Output.Contains("duplicate entry")) ("Expected duplicate-entry error. Output:`n" + $duplicateZipResult.Output)
Assert-True (@(Get-ChildItem -LiteralPath $duplicateZipTarget -Force).Count -eq 0) (
    "Verifier extracted files before rejecting duplicate ZIP entries.")

# Windows case collisions must be rejected before extraction.
$collisionZip = Join-Path $tmpRoot "case-collision.zip"
Copy-Item -LiteralPath $validArchive -Destination $collisionZip -Force
$collisionHash = Add-ZipEntry -ArchivePath $collisionZip -EntryName "CHIPController.JAR" -Content "case collision"
$collisionTarget = Join-Path $tmpRoot "case-collision-target"
New-EmptyDirectory -Path $collisionTarget
$collisionResult = Invoke-VerifierCase -ArchivePath $collisionZip -Hash $collisionHash -ExtraArgs @("-ExtractTo", $collisionTarget)
Assert-True ($collisionResult.ExitCode -ne 0) "Expected case-colliding ZIP entry names to fail."
Assert-True ($collisionResult.Output.Contains("case-colliding entry")) ("Expected case-collision error. Output:`n" + $collisionResult.Output)
Assert-True (@(Get-ChildItem -LiteralPath $collisionTarget -Force).Count -eq 0) (
    "Verifier extracted files before rejecting case-colliding ZIP entries.")

# Differently-cased parent paths collide on Windows even when complete names differ.
$prefixCollisionZip = Join-Path $tmpRoot "parent-prefix-collision.zip"
Copy-Item -LiteralPath $validArchive -Destination $prefixCollisionZip -Force
$prefixCollisionHash = Add-ZipEntry -ArchivePath $prefixCollisionZip -EntryName "PAA-ROOT-CERTS/upper.pem" -Content "prefix collision"
$prefixCollisionTarget = Join-Path $tmpRoot "parent-prefix-collision-target"
New-EmptyDirectory -Path $prefixCollisionTarget
$prefixCollisionResult = Invoke-VerifierCase -ArchivePath $prefixCollisionZip -Hash $prefixCollisionHash -ExtraArgs @("-ExtractTo", $prefixCollisionTarget)
Assert-True ($prefixCollisionResult.ExitCode -ne 0) "Expected case-colliding parent path to fail."
Assert-True ($prefixCollisionResult.Output.Contains("case-colliding path prefix")) (
    "Expected parent-prefix collision error. Output:`n" + $prefixCollisionResult.Output)
Assert-True (@(Get-ChildItem -LiteralPath $prefixCollisionTarget -Force).Count -eq 0) (
    "Verifier extracted files before rejecting a case-colliding parent path.")

# ZIP traversal names must be rejected before extraction.
$traversalZip = Join-Path $tmpRoot "archive-traversal.zip"
Copy-Item -LiteralPath $validArchive -Destination $traversalZip -Force
$traversalZipHash = Add-ZipEntry -ArchivePath $traversalZip -EntryName "../escape.txt" -Content "escape"
$traversalZipTarget = Join-Path $tmpRoot "archive-traversal-target"
New-EmptyDirectory -Path $traversalZipTarget
$traversalZipResult = Invoke-VerifierCase -ArchivePath $traversalZip -Hash $traversalZipHash -ExtraArgs @("-ExtractTo", $traversalZipTarget)
Assert-True ($traversalZipResult.ExitCode -ne 0) "Expected traversal ZIP entry name to fail."
Assert-True ($traversalZipResult.Output.Contains("unsafe entry path")) ("Expected archive traversal error. Output:`n" + $traversalZipResult.Output)
Assert-True (@(Get-ChildItem -LiteralPath $traversalZipTarget -Force).Count -eq 0) (
    "Verifier extracted files before rejecting a traversal ZIP entry.")

# Win32 aliases (trailing dots/spaces) must be rejected before extraction.
foreach ($aliasSuffix in @('.', ' ')) {
    $aliasLabel = if ($aliasSuffix -eq '.') { 'trailing-dot' } else { 'trailing-space' }
    $aliasZip = Join-Path $tmpRoot "$aliasLabel-alias.zip"
    Copy-Item -LiteralPath $validArchive -Destination $aliasZip -Force
    $aliasHash = Add-ZipEntry -ArchivePath $aliasZip -EntryName "alias-check/duplicate.txt" -Content "first"
    $aliasHash = Add-ZipEntry -ArchivePath $aliasZip -EntryName "alias-check/duplicate.txt$aliasSuffix" -Content "second"
    $aliasTarget = Join-Path $tmpRoot "$aliasLabel-alias-target"
    New-EmptyDirectory -Path $aliasTarget
    $aliasResult = Invoke-VerifierCase -ArchivePath $aliasZip -Hash $aliasHash -ExtraArgs @("-ExtractTo", $aliasTarget)
    Assert-True ($aliasResult.ExitCode -ne 0) "Expected $aliasLabel ZIP aliases to fail."
    Assert-True ($aliasResult.Output.Contains("unsafe entry path")) (
        "Expected unsafe-path error for $aliasLabel aliases. Output:`n" + $aliasResult.Output)
    Assert-True (@(Get-ChildItem -LiteralPath $aliasTarget -Force).Count -eq 0) (
        "Verifier extracted files before rejecting $aliasLabel aliases.")
}

# Empty files map must not pass.
$emptyRoot = New-SyntheticArtifact -Name "empty-map"
Write-Manifest -Root $emptyRoot -Files ([ordered]@{})
$emptyArchive = Join-Path $tmpRoot "empty-map.zip"
$emptyHash = New-Archive -Root $emptyRoot -ArchivePath $emptyArchive
$empty = Invoke-VerifierCase -ArchivePath $emptyArchive -Hash $emptyHash
Assert-True ($empty.ExitCode -ne 0) "Expected empty manifest files map to fail."
Assert-True ($empty.Output.Contains("does not list required file")) ("Expected missing required coverage error. Output:`n" + $empty.Output)

# Omitting one required file from an otherwise complete map must fail.
$omittedRoot = New-SyntheticArtifact -Name "omitted-required"
$omittedFiles = Get-FileChecksums -Root $omittedRoot
$omittedFiles.Remove("CHIPController.jar")
Write-Manifest -Root $omittedRoot -Files $omittedFiles
$omittedArchive = Join-Path $tmpRoot "omitted-required.zip"
$omittedHash = New-Archive -Root $omittedRoot -ArchivePath $omittedArchive
$omitted = Invoke-VerifierCase -ArchivePath $omittedArchive -Hash $omittedHash
Assert-True ($omitted.ExitCode -ne 0) "Expected omitted required file to fail."
Assert-True ($omitted.Output.Contains("does not list required file: CHIPController.jar")) (
    "Expected omitted-required error. Output:`n" + $omitted.Output)

# An extra packaged file that is not in the manifest must fail.
$unlistedRoot = New-SyntheticArtifact -Name "unlisted-file"
$unlistedFiles = Get-FileChecksums -Root $unlistedRoot
Write-Manifest -Root $unlistedRoot -Files $unlistedFiles
Set-Content -LiteralPath (Join-Path $unlistedRoot "unlisted.bin") -Value "unlisted" -NoNewline
$unlistedArchive = Join-Path $tmpRoot "unlisted-file.zip"
$unlistedHash = New-Archive -Root $unlistedRoot -ArchivePath $unlistedArchive
$unlisted = Invoke-VerifierCase -ArchivePath $unlistedArchive -Hash $unlistedHash
Assert-True ($unlisted.ExitCode -ne 0) "Expected unlisted packaged file to fail."
Assert-True ($unlisted.Output.Contains("not listed in the manifest: unlisted.bin")) (
    "Expected unlisted-file error. Output:`n" + $unlisted.Output)

# A manifest path that escapes the extraction root must fail.
$traversalRoot = New-SyntheticArtifact -Name "traversal"
$traversalFiles = Get-FileChecksums -Root $traversalRoot
$traversalFiles["../escape.txt"] = ("0" * 64)
Write-Manifest -Root $traversalRoot -Files $traversalFiles
$traversalArchive = Join-Path $tmpRoot "traversal.zip"
$traversalHash = New-Archive -Root $traversalRoot -ArchivePath $traversalArchive
$traversal = Invoke-VerifierCase -ArchivePath $traversalArchive -Hash $traversalHash
Assert-True ($traversal.ExitCode -ne 0) "Expected traversal manifest path to fail."
Assert-True ($traversal.Output.Contains("unsafe file path")) ("Expected unsafe-path error. Output:`n" + $traversal.Output)

# Duplicate JSON properties must fail even though JSON parsers may collapse them.
$duplicateRoot = New-SyntheticArtifact -Name "duplicate-key"
$duplicateFiles = Get-FileChecksums -Root $duplicateRoot
Write-Manifest -Root $duplicateRoot -Files $duplicateFiles
$duplicateManifestPath = Join-Path $duplicateRoot "chip-artifact-manifest.json"
$duplicateText = Get-Content -LiteralPath $duplicateManifestPath -Raw
$duplicateText = $duplicateText -replace '("CHIPController\.jar"\s*:\s*"[0-9a-f]{64}")', '$1, $1'
Set-Content -LiteralPath $duplicateManifestPath -Value $duplicateText -NoNewline -Encoding utf8NoBOM
$duplicateArchive = Join-Path $tmpRoot "duplicate-key.zip"
$duplicateHash = New-Archive -Root $duplicateRoot -ArchivePath $duplicateArchive
$duplicate = Invoke-VerifierCase -ArchivePath $duplicateArchive -Hash $duplicateHash
Assert-True ($duplicate.ExitCode -ne 0) "Expected duplicate manifest key to fail."
Assert-True ($duplicate.Output.Contains("duplicate JSON property")) ("Expected duplicate-key error. Output:`n" + $duplicate.Output)

# Duplicate top-level manifest keys must fail.
$duplicateTopRoot = Join-Path $tmpRoot "duplicate-top-level"
Copy-Item -LiteralPath $validRoot -Destination $duplicateTopRoot -Recurse -Force
$duplicateTopManifest = Join-Path $duplicateTopRoot "chip-artifact-manifest.json"
$duplicateTopText = Get-Content -LiteralPath $duplicateTopManifest -Raw
$duplicateTopText = $duplicateTopText -replace '"connectedhomeip_tag"\s*:\s*"v1\.6\.1\.0"', '"connectedhomeip_tag": "v1.6.1.0", "connectedhomeip_tag": "v1.6.1.0"'
Set-Content -LiteralPath $duplicateTopManifest -Value $duplicateTopText -NoNewline -Encoding utf8NoBOM
$duplicateTopArchive = Join-Path $tmpRoot "duplicate-top-level.zip"
$duplicateTopHash = New-Archive -Root $duplicateTopRoot -ArchivePath $duplicateTopArchive
$duplicateTop = Invoke-VerifierCase -ArchivePath $duplicateTopArchive -Hash $duplicateTopHash
Assert-True ($duplicateTop.ExitCode -ne 0) "Expected duplicate top-level manifest key to fail."
Assert-True ($duplicateTop.Output.Contains("duplicate JSON property 'connectedhomeip_tag'")) (
    "Expected duplicate top-level JSON property error. Output:`n" + $duplicateTop.Output)

# Duplicate nested manifest keys must fail.
$duplicateNestedRoot = Join-Path $tmpRoot "duplicate-nested"
Copy-Item -LiteralPath $validRoot -Destination $duplicateNestedRoot -Recurse -Force
$duplicateNestedManifest = Join-Path $duplicateNestedRoot "chip-artifact-manifest.json"
$duplicateNestedText = (Get-Content -LiteralPath $duplicateNestedManifest -Raw).TrimEnd()
$duplicateNestedText = $duplicateNestedText.Substring(0, $duplicateNestedText.Length - 1) +
    ', "metadata": {"duplicate": "a", "duplicate": "b"}}'
Set-Content -LiteralPath $duplicateNestedManifest -Value $duplicateNestedText -NoNewline -Encoding utf8NoBOM
$duplicateNestedArchive = Join-Path $tmpRoot "duplicate-nested.zip"
$duplicateNestedHash = New-Archive -Root $duplicateNestedRoot -ArchivePath $duplicateNestedArchive
$duplicateNested = Invoke-VerifierCase -ArchivePath $duplicateNestedArchive -Hash $duplicateNestedHash
Assert-True ($duplicateNested.ExitCode -ne 0) "Expected duplicate nested manifest key to fail."
Assert-True ($duplicateNested.Output.Contains("duplicate JSON property 'duplicate'")) (
    "Expected duplicate nested JSON property error. Output:`n" + $duplicateNested.Output)

# Missing PAA certificates must fail even if the rest of the manifest is complete.
$missingPaaRoot = New-SyntheticArtifact -Name "missing-paa" -OmitPaa
$missingPaaFiles = Get-FileChecksums -Root $missingPaaRoot
Write-Manifest -Root $missingPaaRoot -Files $missingPaaFiles
$missingPaaArchive = Join-Path $tmpRoot "missing-paa.zip"
$missingPaaHash = New-Archive -Root $missingPaaRoot -ArchivePath $missingPaaArchive
$missingPaa = Invoke-VerifierCase -ArchivePath $missingPaaArchive -Hash $missingPaaHash
Assert-True ($missingPaa.ExitCode -ne 0) "Expected missing PAA certificates to fail."
Assert-True ($missingPaa.Output.Contains("missing packaged PAA")) ("Expected missing-PAA error. Output:`n" + $missingPaa.Output)

# A caller-supplied non-empty extraction directory must be rejected without deletion.
$callerRoot = Join-Path $tmpRoot "caller-dir"
New-Item -ItemType Directory -Force -Path $callerRoot | Out-Null
$sentinel = Join-Path $callerRoot "sentinel.txt"
Set-Content -LiteralPath $sentinel -Value "keep" -NoNewline
$caller = Invoke-VerifierCase -ArchivePath $validArchive -Hash $validHash -ExtraArgs @("-ExtractTo", $callerRoot)
Assert-True ($caller.ExitCode -ne 0) "Expected non-empty caller extraction directory to fail."
Assert-True ($caller.Output.Contains("refusing to remove caller files")) ("Expected extraction-destination error. Output:`n" + $caller.Output)
Assert-True (Test-Path -LiteralPath $sentinel -PathType Leaf) "Verifier deleted a caller file in ExtractTo."
Assert-True ((Get-Content -LiteralPath $sentinel -Raw) -eq "keep") "Verifier modified a caller file in ExtractTo."

Write-Output "connectedhomeip archive provenance verifier smoke-test passed."
