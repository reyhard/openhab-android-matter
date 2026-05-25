param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $GradlePath = "",
    [string] $AdbPath = "",
    [string] $ApkPath = "",
    [string] $Serial = "",
    [switch] $SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Exit-With {
    param(
        [Parameter(Mandatory = $true)] [int] $Code,
        [Parameter(Mandatory = $true)] [string] $Message
    )
    Write-Output $Message
    exit $Code
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)] [string] $Command,
        [Parameter(Mandatory = $true)] [string[]] $Arguments,
        [Parameter(Mandatory = $true)] [int] $FailureCode,
        [Parameter(Mandatory = $true)] [string] $FailureMessage
    )
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        Exit-With $FailureCode $FailureMessage
    }
}

function Resolve-AdbPath {
    param([Parameter(Mandatory = $true)] [string] $Root)

    $localProperties = Join-Path $Root "local.properties"
    if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
        foreach ($line in Get-Content -LiteralPath $localProperties) {
            if ($line -match "^sdk\.dir=(.+)$") {
                $sdkDir = $Matches[1].Trim() -replace "\\\\", "\" -replace "\\:", ":"
                foreach ($candidateName in @("adb.exe", "adb.ps1", "adb")) {
                    $candidate = Join-Path $sdkDir ("platform-tools\" + $candidateName)
                    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
                        return $candidate
                    }
                }
            }
        }
    }
    return "adb"
}

if (-not (Test-Path -LiteralPath $ProjectRoot -PathType Container)) {
    Exit-With 11 "Project root does not exist: $ProjectRoot"
}

$resolvedProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($GradlePath)) {
    $GradlePath = Join-Path $resolvedProjectRoot "gradlew.bat"
}
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $resolvedProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
}
if ([string]::IsNullOrWhiteSpace($AdbPath)) {
    $AdbPath = Resolve-AdbPath $resolvedProjectRoot
}

if (-not $SkipBuild) {
    if (-not (Test-Path -LiteralPath $GradlePath -PathType Leaf)) {
        Exit-With 10 "Gradle wrapper not found: $GradlePath"
    }
    Push-Location $resolvedProjectRoot
    try {
        Invoke-Checked `
            -Command $GradlePath `
            -Arguments @(":app:testDebugUnitTest", ":app:assembleDebug", "--offline") `
            -FailureCode 10 `
            -FailureMessage "Gradle build or tests failed."
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    Exit-With 11 "Debug APK not found: $ApkPath"
}

$apk = Get-Item -LiteralPath $ApkPath
if ($apk.Length -le 0) {
    Exit-With 11 "Debug APK is empty: $ApkPath"
}
Write-Output ("Debug APK ready: {0} ({1} bytes)" -f $apk.FullName, $apk.Length)

$deviceOutput = & $AdbPath devices 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Output ($deviceOutput -join "`n")
    Exit-With 12 "adb devices failed."
}

$devices = @()
foreach ($line in $deviceOutput) {
    $trimmed = $line.Trim()
    if ($trimmed.Length -eq 0 -or $trimmed -eq "List of devices attached") {
        continue
    }
    $parts = $trimmed -split "\s+"
    if ($parts.Length -ge 2 -and $parts[1] -eq "device") {
        $devices += $parts[0]
    }
}

if ($devices.Count -eq 0) {
    Exit-With 2 "No connected Android devices or emulators are ready for install."
}

Write-Output ("Ready Android devices: " + ($devices -join ", "))

$installArgs = @()
if (-not [string]::IsNullOrWhiteSpace($Serial)) {
    if (-not ($devices -contains $Serial)) {
        Exit-With 3 "Requested Android device is not ready: $Serial"
    }
    Write-Output "Selected Android device: $Serial"
    $installArgs += @("-s", $Serial)
} elseif ($devices.Count -gt 1) {
    Exit-With 4 ("Multiple Android devices are ready: " + ($devices -join ", ") + ". Re-run with -Serial <device-id>.")
}

$installArgs += @("install", "-r", $apk.FullName)
& $AdbPath @installArgs
if ($LASTEXITCODE -ne 0) {
    Exit-With 13 "adb install failed."
}

Write-Output "Debug APK installed successfully."
exit 0
