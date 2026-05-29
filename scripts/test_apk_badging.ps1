param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $AaptPath = "",
    [string] $ApkPath = ""
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

function Convert-LocalPropertiesPath {
    param([Parameter(Mandatory = $true)] [string] $Value)

    return $Value.Trim() -replace "\\\\", "\" -replace "\\:", ":"
}

function Find-Aapt {
    param([Parameter(Mandatory = $true)] [string] $Root)

    $candidateSdkRoots = @()
    $localProperties = Join-Path $Root "local.properties"
    if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
        foreach ($line in Get-Content -LiteralPath $localProperties) {
            if ($line -match "^sdk\.dir=(.+)$") {
                $candidateSdkRoots += Convert-LocalPropertiesPath $Matches[1]
            }
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $candidateSdkRoots += $env:ANDROID_HOME
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
        $candidateSdkRoots += $env:ANDROID_SDK_ROOT
    }

    foreach ($sdkRoot in $candidateSdkRoots) {
        $buildTools = Join-Path $sdkRoot "build-tools"
        if (-not (Test-Path -LiteralPath $buildTools -PathType Container)) {
            continue
        }
        $aapt = Get-ChildItem -LiteralPath $buildTools -Recurse -File |
            Where-Object { $_.Name -eq "aapt.exe" -or $_.Name -eq "aapt" } |
            Sort-Object FullName -Descending |
            Select-Object -First 1
        if ($null -ne $aapt) {
            return $aapt.FullName
        }
    }

    return "aapt"
}

if (-not (Test-Path -LiteralPath $ProjectRoot -PathType Container)) {
    throw "Project root does not exist: $ProjectRoot"
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $ProjectRoot "app\build\outputs\apk\debug\openhab-matter-helper.apk"
}
if ([string]::IsNullOrWhiteSpace($AaptPath)) {
    $AaptPath = Find-Aapt $ProjectRoot
}

Assert-True (Test-Path -LiteralPath $ApkPath -PathType Leaf) "Debug APK not found: $ApkPath"

$badgingOutput = & $AaptPath "dump" "badging" $ApkPath 2>&1
if ($LASTEXITCODE -ne 0) {
    throw ("aapt dump badging failed:`n" + ($badgingOutput -join "`n"))
}

$badging = $badgingOutput -join "`n"
Assert-True (-not $badging.Contains("uses-implied-feature: name='android.hardware.camera'")) "APK still implies required camera hardware."
Assert-True (-not $badging.Contains("uses-implied-feature: name='android.hardware.location'")) "APK still implies required location hardware."
Assert-True ($badging.Contains("uses-feature-not-required: name='android.hardware.camera'")) "APK does not mark camera hardware optional."
Assert-True ($badging.Contains("uses-feature-not-required: name='android.hardware.location'")) "APK does not mark location hardware optional."

Write-Output "APK badging smoke-test passed."
