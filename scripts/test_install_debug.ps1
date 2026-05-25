Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptUnderTest = Join-Path $PSScriptRoot "install_debug.ps1"
if (-not (Test-Path -LiteralPath $ScriptUnderTest)) {
    throw "Missing script under test: $ScriptUnderTest"
}

function New-TestProject {
    $root = Join-Path ([System.IO.Path]::GetTempPath()) ("openhab-install-test-" + [Guid]::NewGuid().ToString("N"))
    $apkDir = Join-Path $root "app\build\outputs\apk\debug"
    New-Item -ItemType Directory -Force -Path $apkDir | Out-Null
    $apk = Join-Path $apkDir "app-debug.apk"
    Set-Content -LiteralPath $apk -Value "fake-apk" -NoNewline
    return @{ Root = $root; Apk = $apk }
}

function New-FakeAdb {
    param(
        [Parameter(Mandatory = $true)] [string] $Root,
        [Parameter(Mandatory = $true)] [string] $Name,
        [Parameter(Mandatory = $true)] [AllowEmptyCollection()] [string[]] $DeviceLines
    )

    $path = Join-Path $Root ($Name + ".ps1")
    $log = Join-Path $Root ($Name + ".log")
    $lineLiteral = "@(" + (($DeviceLines | ForEach-Object { "'" + ($_ -replace "'", "''") + "'" }) -join ", ") + ")"
    $content = @"
param([Parameter(ValueFromRemainingArguments = `$true)] [string[]] `$Args)
Add-Content -LiteralPath '$log' -Value (`$Args -join ' ')
if (`$Args.Count -gt 0 -and `$Args[0] -eq 'devices') {
    Write-Output 'List of devices attached'
    foreach (`$line in $lineLiteral) {
        Write-Output `$line
    }
    exit 0
}
if (`$Args -contains 'install') {
    Write-Output 'Success'
    exit 0
}
Write-Error ('Unexpected adb args: ' + (`$Args -join ' '))
exit 9
"@
    Set-Content -LiteralPath $path -Value $content
    return @{ Path = $path; Log = $log }
}

function Invoke-InstallHelper {
    param(
        [Parameter(Mandatory = $true)] [string] $ProjectRoot,
        [Parameter(Mandatory = $true)] [string] $AdbPath,
        [Parameter(Mandatory = $true)] [string] $ApkPath,
        [string] $Serial = ""
    )

    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        $ScriptUnderTest,
        "-ProjectRoot",
        $ProjectRoot,
        "-SkipBuild",
        "-AdbPath",
        $AdbPath,
        "-ApkPath",
        $ApkPath
    )
    if ($Serial -ne "") {
        $arguments += @("-Serial", $Serial)
    }
    $output = & pwsh @arguments 2>&1
    return @{ ExitCode = $LASTEXITCODE; Output = ($output -join "`n") }
}

function Invoke-InstallHelperWithoutAdbOverride {
    param(
        [Parameter(Mandatory = $true)] [string] $ProjectRoot,
        [Parameter(Mandatory = $true)] [string] $ApkPath
    )

    $output = & pwsh -NoProfile -ExecutionPolicy Bypass -File $ScriptUnderTest `
        -ProjectRoot $ProjectRoot `
        -SkipBuild `
        -ApkPath $ApkPath 2>&1
    return @{ ExitCode = $LASTEXITCODE; Output = ($output -join "`n") }
}

function Assert-Equal {
    param(
        [Parameter(Mandatory = $true)] $Expected,
        [Parameter(Mandatory = $true)] $Actual,
        [Parameter(Mandatory = $true)] [string] $Message
    )
    if ($Expected -ne $Actual) {
        throw "$Message Expected '$Expected' but got '$Actual'."
    }
}

function Assert-Contains {
    param(
        [Parameter(Mandatory = $true)] [string] $Needle,
        [Parameter(Mandatory = $true)] [string] $Haystack,
        [Parameter(Mandatory = $true)] [string] $Message
    )
    if (-not $Haystack.Contains($Needle)) {
        throw "$Message Expected to find '$Needle' in '$Haystack'."
    }
}

$project = New-TestProject
try {
    $adbNoDevices = New-FakeAdb -Root $project.Root -Name "adb-no-devices" -DeviceLines @()
    $result = Invoke-InstallHelper -ProjectRoot $project.Root -AdbPath $adbNoDevices.Path -ApkPath $project.Apk
    Assert-Equal 2 $result.ExitCode "No-device helper exit code mismatch."
    Assert-Contains "No connected Android devices" $result.Output "No-device helper output mismatch."

    $adbUnauthorized = New-FakeAdb -Root $project.Root -Name "adb-unauthorized" -DeviceLines @(
        "emulator-5554`tunauthorized",
        "R58M0000000`toffline"
    )
    $result = Invoke-InstallHelper -ProjectRoot $project.Root -AdbPath $adbUnauthorized.Path -ApkPath $project.Apk
    Assert-Equal 2 $result.ExitCode "Unauthorized/offline helper exit code mismatch."
    Assert-Contains "No connected Android devices" $result.Output "Unauthorized/offline helper output mismatch."

    $adbOneDevice = New-FakeAdb -Root $project.Root -Name "adb-one-device" -DeviceLines @("emulator-5554`tdevice")
    $result = Invoke-InstallHelper -ProjectRoot $project.Root -AdbPath $adbOneDevice.Path -ApkPath $project.Apk
    Assert-Equal 0 $result.ExitCode "Single-device helper exit code mismatch."
    Assert-Contains "install -r" (Get-Content -LiteralPath $adbOneDevice.Log -Raw) "Single-device helper did not call adb install."

    $adbTwoDevices = New-FakeAdb -Root $project.Root -Name "adb-two-devices" -DeviceLines @(
        "emulator-5554`tdevice",
        "R58M0000000`tdevice"
    )
    $result = Invoke-InstallHelper -ProjectRoot $project.Root -AdbPath $adbTwoDevices.Path -ApkPath $project.Apk
    Assert-Equal 4 $result.ExitCode "Multi-device helper exit code mismatch."
    Assert-Contains "Multiple Android devices" $result.Output "Multi-device helper output mismatch."

    $result = Invoke-InstallHelper -ProjectRoot $project.Root -AdbPath $adbTwoDevices.Path -ApkPath $project.Apk -Serial "R58M0000000"
    Assert-Equal 0 $result.ExitCode "Serial install helper exit code mismatch."
    Assert-Contains "-s R58M0000000 install -r" (Get-Content -LiteralPath $adbTwoDevices.Log -Raw) "Serial helper did not call adb -s install."

    $missingApk = Join-Path $project.Root "missing.apk"
    $result = Invoke-InstallHelper -ProjectRoot $project.Root -AdbPath $adbOneDevice.Path -ApkPath $missingApk
    Assert-Equal 11 $result.ExitCode "Missing APK helper exit code mismatch."
    Assert-Contains "Debug APK not found" $result.Output "Missing APK helper output mismatch."

    $emptyApk = Join-Path $project.Root "empty.apk"
    New-Item -ItemType File -Path $emptyApk | Out-Null
    $result = Invoke-InstallHelper -ProjectRoot $project.Root -AdbPath $adbOneDevice.Path -ApkPath $emptyApk
    Assert-Equal 11 $result.ExitCode "Empty APK helper exit code mismatch."
    Assert-Contains "Debug APK is empty" $result.Output "Empty APK helper output mismatch."

    $sdkRoot = Join-Path $project.Root "sdk"
    $platformTools = Join-Path $sdkRoot "platform-tools"
    New-Item -ItemType Directory -Force -Path $platformTools | Out-Null
    $sdkAdb = New-FakeAdb -Root $platformTools -Name "adb" -DeviceLines @("emulator-5554`tdevice")
    $escapedSdkRoot = (($sdkRoot -replace "\\", "\\") -replace ":", "\:")
    Set-Content -LiteralPath (Join-Path $project.Root "local.properties") -Value ("sdk.dir=" + $escapedSdkRoot)
    $result = Invoke-InstallHelperWithoutAdbOverride -ProjectRoot $project.Root -ApkPath $project.Apk
    Assert-Equal 0 $result.ExitCode "local.properties adb resolution exit code mismatch."
    Assert-Contains "install -r" (Get-Content -LiteralPath $sdkAdb.Log -Raw) "local.properties adb resolution did not install."

    Write-Output "install_debug.ps1 self-test passed."
} finally {
    if (Test-Path -LiteralPath $project.Root) {
        Remove-Item -LiteralPath $project.Root -Recurse -Force
    }
}
