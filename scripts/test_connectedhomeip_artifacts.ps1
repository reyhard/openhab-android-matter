param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $GradlePath = ""
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

function New-ArtifactLayout {
    param(
        [Parameter(Mandatory = $true)] [string] $Root,
        [Parameter(Mandatory = $true)] [bool] $EmptyControllerJar
    )

    Remove-Item -LiteralPath $Root -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $Root | Out-Null

    $jars = @(
        "CHIPController.jar",
        "CHIPInteractionModel.jar",
        "CHIPClusterID.jar",
        "CHIPClusters.jar",
        "AndroidPlatform.jar",
        "OnboardingPayload.jar",
        "libMatterTlv.jar",
        "libMatterJson.jar"
    )
    foreach ($jar in $jars) {
        $path = Join-Path $Root $jar
        if ($EmptyControllerJar -and $jar -eq "CHIPController.jar") {
            New-Item -ItemType File -Force -Path $path | Out-Null
        } else {
            Set-Content -LiteralPath $path -Value "synthetic $jar" -NoNewline
        }
    }

    foreach ($abi in @("arm64-v8a", "armeabi-v7a", "x86", "x86_64")) {
        $abiDir = Join-Path $Root ("jniLibs\" + $abi)
        New-Item -ItemType Directory -Force -Path $abiDir | Out-Null
        foreach ($library in @("libCHIPController.so", "libc++_shared.so")) {
            Set-Content -LiteralPath (Join-Path $abiDir $library) -Value "synthetic $abi $library" -NoNewline
        }
    }
}

function Invoke-GradleArtifactVerification {
    param([Parameter(Mandatory = $true)] [string] $ArtifactRoot)

    Push-Location $ProjectRoot
    try {
        $output = & $GradlePath ":app:verifyConnectedHomeIpControllerArtifacts" "--offline" "-PopenhabMatterChipControllerArtifactsDir=$ArtifactRoot" 2>&1
        return @{ ExitCode = $LASTEXITCODE; Output = ($output -join "`n") }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $ProjectRoot -PathType Container)) {
    throw "Project root does not exist: $ProjectRoot"
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($GradlePath)) {
    $GradlePath = Join-Path $ProjectRoot "gradlew.bat"
}
if (-not (Test-Path -LiteralPath $GradlePath -PathType Leaf)) {
    throw "Gradle wrapper not found: $GradlePath"
}

$tmpRoot = Join-Path $ProjectRoot "build\tmp"
$positiveRoot = Join-Path $tmpRoot "chip-controller-artifacts-smoke"
$emptyRoot = Join-Path $tmpRoot "chip-controller-artifacts-empty"

New-ArtifactLayout -Root $positiveRoot -EmptyControllerJar $false
$positive = Invoke-GradleArtifactVerification -ArtifactRoot $positiveRoot
Assert-True ($positive.ExitCode -eq 0) ("Expected positive synthetic artifacts to pass, got exit " + $positive.ExitCode + ". Output:`n" + $positive.Output)

New-ArtifactLayout -Root $emptyRoot -EmptyControllerJar $true
$negative = Invoke-GradleArtifactVerification -ArtifactRoot $emptyRoot
Assert-True ($negative.ExitCode -ne 0) "Expected empty synthetic artifacts to fail validation."
Assert-True ($negative.Output.Contains("Empty connectedhomeip controller jar")) ("Expected empty-jar failure message. Output:`n" + $negative.Output)

Write-Output "connectedhomeip artifact validation smoke-test passed."
