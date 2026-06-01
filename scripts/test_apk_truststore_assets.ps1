param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $ApkPath = "",
    [int] $MinimumPaaCertificates = 1,
    [switch] $RequireCdTrustKeys
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
} finally {
    $zip.Dispose()
}

Write-Output "APK trust-store asset smoke-test passed."
