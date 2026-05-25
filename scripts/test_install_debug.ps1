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

function New-FakeGradle {
    param([Parameter(Mandatory = $true)] [hashtable] $Project)

    $path = Join-Path $Project.Root "gradlew.ps1"
    $log = Join-Path $Project.Root "gradle.log"
    $apk = $Project.Apk
    $apkDir = Split-Path -Parent $apk
    $content = @"
param([Parameter(ValueFromRemainingArguments = `$true)] [string[]] `$Args)
Add-Content -LiteralPath '$log' -Value (`$Args -join ' ')
New-Item -ItemType Directory -Force -Path '$apkDir' | Out-Null
Set-Content -LiteralPath '$apk' -Value 'fake-apk-from-gradle' -NoNewline
exit 0
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

function Invoke-InstallHelperWithGradle {
    param(
        [Parameter(Mandatory = $true)] [string] $ProjectRoot,
        [Parameter(Mandatory = $true)] [string] $GradlePath,
        [Parameter(Mandatory = $true)] [string] $AdbPath,
        [Parameter(Mandatory = $true)] [string] $ApkPath,
        [switch] $PreflightOnly,
        [string] $ChipControllerArtifactsDir = "",
        [string] $ChipControllerAbis = "",
        [string] $ChipNativeMode = "",
        [string] $ChipPrebuiltDir = ""
    )

    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        $ScriptUnderTest,
        "-ProjectRoot",
        $ProjectRoot,
        "-GradlePath",
        $GradlePath,
        "-AdbPath",
        $AdbPath,
        "-ApkPath",
        $ApkPath
    )
    if ($PreflightOnly) {
        $arguments += "-PreflightOnly"
    }
    if ($ChipControllerArtifactsDir -ne "") {
        $arguments += @("-ChipControllerArtifactsDir", $ChipControllerArtifactsDir)
    }
    if ($ChipControllerAbis -ne "") {
        $arguments += @("-ChipControllerAbis", $ChipControllerAbis)
    }
    if ($ChipNativeMode -ne "") {
        $arguments += @("-ChipNativeMode", $ChipNativeMode)
    }
    if ($ChipPrebuiltDir -ne "") {
        $arguments += @("-ChipPrebuiltDir", $ChipPrebuiltDir)
    }

    $output = & pwsh @arguments 2>&1
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

    $gradle = New-FakeGradle -Project $project
    $preflightAdb = New-FakeAdb -Root $project.Root -Name "adb-preflight" -DeviceLines @("emulator-5554`tdevice")
    Remove-Item -LiteralPath $project.Apk -Force
    $result = Invoke-InstallHelperWithGradle `
        -ProjectRoot $project.Root `
        -GradlePath $gradle.Path `
        -AdbPath $preflightAdb.Path `
        -ApkPath $project.Apk `
        -PreflightOnly
    Assert-Equal 0 $result.ExitCode "Preflight helper exit code mismatch."
    Assert-Contains "Preflight complete" $result.Output "Preflight helper output mismatch."
    Assert-Contains ":app:testDebugUnitTest :app:assembleDebug --offline" (Get-Content -LiteralPath $gradle.Log -Raw) "Preflight did not run Gradle build."
    Assert-Equal $false (Test-Path -LiteralPath $preflightAdb.Log) "Preflight should not call adb."

    Remove-Item -LiteralPath $gradle.Log -Force
    $result = Invoke-InstallHelperWithGradle `
        -ProjectRoot $project.Root `
        -GradlePath $gradle.Path `
        -AdbPath $preflightAdb.Path `
        -ApkPath $project.Apk `
        -PreflightOnly `
        -ChipControllerArtifactsDir "C:\tmp\chip-artifacts" `
        -ChipControllerAbis "arm64-v8a"
    Assert-Equal 0 $result.ExitCode "Preflight helper with controller artifacts exit code mismatch."
    $gradleLog = Get-Content -LiteralPath $gradle.Log -Raw
    Assert-Contains "-PopenhabMatterChipControllerArtifactsDir=C:\tmp\chip-artifacts" $gradleLog "Missing controller artifacts Gradle arg."
    Assert-Contains "-PopenhabMatterChipControllerAbis=arm64-v8a" $gradleLog "Missing controller ABI Gradle arg."

    Remove-Item -LiteralPath $gradle.Log -Force
    $result = Invoke-InstallHelperWithGradle `
        -ProjectRoot $project.Root `
        -GradlePath $gradle.Path `
        -AdbPath $preflightAdb.Path `
        -ApkPath $project.Apk `
        -PreflightOnly `
        -ChipNativeMode "prebuilt" `
        -ChipPrebuiltDir "C:\tmp\native"
    Assert-Equal 0 $result.ExitCode "Preflight helper with native prebuilt exit code mismatch."
    $gradleLog = Get-Content -LiteralPath $gradle.Log -Raw
    Assert-Contains "-PopenhabMatterChipNativeMode=prebuilt" $gradleLog "Missing native mode Gradle arg."
    Assert-Contains "-PopenhabMatterChipPrebuiltDir=C:\tmp\native" $gradleLog "Missing native prebuilt Gradle arg."

    Write-Output "install_debug.ps1 self-test passed."
} finally {
    if (Test-Path -LiteralPath $project.Root) {
        Remove-Item -LiteralPath $project.Root -Recurse -Force
    }
}
