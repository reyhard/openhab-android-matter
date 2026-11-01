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
        [Parameter(Mandatory = $true)] [ValidateSet("Valid", "EmptyControllerJar", "InvalidControllerJar", "MissingRequiredClass")] [string] $JarMode
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
        if ($JarMode -eq "EmptyControllerJar" -and $jar -eq "CHIPController.jar") {
            New-Item -ItemType File -Force -Path $path | Out-Null
        } elseif ($JarMode -eq "InvalidControllerJar" -and $jar -eq "CHIPController.jar") {
            Set-Content -LiteralPath $path -Value "synthetic invalid $jar" -NoNewline
        } elseif ($jar -eq "CHIPController.jar") {
            New-SyntheticJar -Path $path -Name $jar -IncludeRequiredClasses ($JarMode -ne "MissingRequiredClass")
        } else {
            New-SyntheticJar -Path $path -Name $jar
        }
    }

    $jniLibsRoot = Join-Path $Root "jniLibs"
    foreach ($abi in @("arm64-v8a", "armeabi-v7a", "x86", "x86_64")) {
        $abiDir = Join-Path $jniLibsRoot $abi
        New-Item -ItemType Directory -Force -Path $abiDir | Out-Null
        foreach ($library in @("libCHIPController.so", "libc++_shared.so")) {
            Set-Content -LiteralPath (Join-Path $abiDir $library) -Value "synthetic $abi $library" -NoNewline
        }
    }
}

function New-SyntheticJar {
    param(
        [Parameter(Mandatory = $true)] [string] $Path,
        [Parameter(Mandatory = $true)] [string] $Name,
        [bool] $IncludeRequiredClasses = $false
    )

    $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::ReadWrite)
    try {
        $zip = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create, $true)
        try {
            $entry = $zip.CreateEntry("META-INF/MANIFEST.MF")
            $writer = [System.IO.StreamWriter]::new($entry.Open())
            try {
                $writer.Write("Manifest-Version: 1.0`nCreated-By: openHAB Matter synthetic artifact smoke test`nName: $Name`n")
            } finally {
                $writer.Dispose()
            }
            if ($IncludeRequiredClasses) {
                foreach ($classEntry in $RequiredClassEntries) {
                    $zip.CreateEntry($classEntry) | Out-Null
                }
            }
        } finally {
            $zip.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

$RequiredClassEntries = @(
    'chip/devicecontroller/ChipDeviceController.class',
    'chip/devicecontroller/ControllerParams.class',
    'chip/devicecontroller/NetworkCredentials.class',
    'chip/devicecontroller/NetworkCredentials$ThreadCredentials.class',
    'chip/devicecontroller/CommissionParameters.class',
    'chip/devicecontroller/CommissionParameters$Builder.class',
    'chip/devicecontroller/ICDRegistrationInfo.class',
    'chip/devicecontroller/ICDRegistrationInfo$Builder.class',
    'chip/devicecontroller/ICDDeviceInfo.class',
    'chip/devicecontroller/ChipDeviceController$CompletionListener.class',
    'chip/devicecontroller/DeviceAttestationDelegate.class',
    'chip/devicecontroller/AttestationTrustStoreDelegate.class',
    'chip/devicecontroller/DeviceAttestation.class',
    'chip/devicecontroller/OpenCommissioningCallback.class',
    'chip/devicecontroller/GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback.class',
    'chip/platform/AndroidChipPlatform.class',
    'chip/platform/AndroidBleManager.class',
    'chip/platform/AndroidNfcCommissioningManager.class',
    'chip/platform/PreferencesKeyValueStoreManager.class',
    'chip/platform/PreferencesConfigurationManager.class',
    'chip/platform/NsdManagerServiceResolver.class',
    'chip/platform/NsdManagerServiceBrowser.class',
    'chip/platform/ChipMdnsCallbackImpl.class',
    'chip/platform/DiagnosticDataProviderImpl.class',
    'chip/platform/BleCallback.class'
)

function Invoke-GradleArtifactVerification {
    param(
        [Parameter(Mandatory = $true)] [string] $ArtifactRoot,
        [string[]] $ExtraGradleArgs = @()
    )

    Push-Location $ProjectRoot
    try {
        $output = & $GradlePath ":app:verifyConnectedHomeIpControllerArtifacts" "--offline" "-PopenhabMatterChipControllerArtifactsDir=$ArtifactRoot" @ExtraGradleArgs 2>&1
        return @{ ExitCode = $LASTEXITCODE; Output = ($output -join "`n") }
    } finally {
        Pop-Location
    }
}

function New-ArtifactManifest {
    param(
        [Parameter(Mandatory = $true)] [string] $Root,
        [string] $SourceTag = "v1.6.1.0",
        [string] $SourceCommit = "3bcdd56ba54fb88b2afb4bfef575014671df7aa7",
        [string] $TamperPath = ""
    )

    $files = [ordered]@{}
    Get-ChildItem -LiteralPath $Root -Recurse -File | ForEach-Object {
        if ($_.Name -eq "chip-artifact-manifest.json") {
            return
        }
        $relative = [System.IO.Path]::GetRelativePath($Root, $_.FullName).Replace('\', '/')
        $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($relative -eq $TamperPath) {
            $hash = "0" * 64
        }
        $files[$relative] = $hash
    }

    $manifest = [ordered]@{
        schema = 1
        connectedhomeip_tag = $SourceTag
        connectedhomeip_commit = $SourceCommit
        abi = "arm64-v8a"
        build_target = "android-arm64-chip-tool"
        build_profile = "release"
        files = $files
    }
    $path = Join-Path $Root "chip-artifact-manifest.json"
    Set-Content -LiteralPath $path -Value ($manifest | ConvertTo-Json -Depth 6) -Encoding UTF8
}

if (-not (Test-Path -LiteralPath $ProjectRoot -PathType Container)) {
    throw "Project root does not exist: $ProjectRoot"
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($GradlePath)) {
    if ($IsWindows) {
        $GradlePath = Join-Path $ProjectRoot "gradlew.bat"
    } else {
        $GradlePath = Join-Path $ProjectRoot "gradlew"
    }
}
if (-not (Test-Path -LiteralPath $GradlePath -PathType Leaf)) {
    throw "Gradle wrapper not found: $GradlePath"
}

$tmpRoot = Join-Path (Join-Path $ProjectRoot "build") "tmp"
$positiveRoot = Join-Path $tmpRoot "chip-controller-artifacts-smoke"
$emptyRoot = Join-Path $tmpRoot "chip-controller-artifacts-empty"
$invalidRoot = Join-Path $tmpRoot "chip-controller-artifacts-invalid-jar"
$missingClassRoot = Join-Path $tmpRoot "chip-controller-artifacts-missing-class"

New-ArtifactLayout -Root $positiveRoot -JarMode "Valid"
$positive = Invoke-GradleArtifactVerification -ArtifactRoot $positiveRoot
Assert-True ($positive.ExitCode -eq 0) ("Expected positive synthetic artifacts to pass, got exit " + $positive.ExitCode + ". Output:`n" + $positive.Output)

$manifestRoot = Join-Path $tmpRoot "chip-controller-artifacts-manifest"
New-ArtifactLayout -Root $manifestRoot -JarMode "Valid"
New-ArtifactManifest -Root $manifestRoot
$manifestPositive = Invoke-GradleArtifactVerification -ArtifactRoot $manifestRoot -ExtraGradleArgs @(
    "-PopenhabMatterChipArtifactExpectedSourceTag=v1.6.1.0",
    "-PopenhabMatterChipArtifactExpectedSourceCommit=3bcdd56ba54fb88b2afb4bfef575014671df7aa7"
)
Assert-True ($manifestPositive.ExitCode -eq 0) ("Expected matching provenance manifest to pass, got exit " + $manifestPositive.ExitCode + ". Output:`n" + $manifestPositive.Output)

$wrongTagRoot = Join-Path $tmpRoot "chip-controller-artifacts-wrong-tag"
New-ArtifactLayout -Root $wrongTagRoot -JarMode "Valid"
New-ArtifactManifest -Root $wrongTagRoot
$wrongTag = Invoke-GradleArtifactVerification -ArtifactRoot $wrongTagRoot -ExtraGradleArgs @(
    "-PopenhabMatterChipArtifactExpectedSourceTag=v1.6.0.0"
)
Assert-True ($wrongTag.ExitCode -ne 0) "Expected mismatched provenance tag to fail validation."
Assert-True ($wrongTag.Output.Contains("connectedhomeip artifact tag mismatch")) ("Expected tag mismatch failure message. Output:`n" + $wrongTag.Output)

$tamperedRoot = Join-Path $tmpRoot "chip-controller-artifacts-tampered-manifest"
New-ArtifactLayout -Root $tamperedRoot -JarMode "Valid"
New-ArtifactManifest -Root $tamperedRoot -TamperPath "CHIPController.jar"
$tampered = Invoke-GradleArtifactVerification -ArtifactRoot $tamperedRoot
Assert-True ($tampered.ExitCode -ne 0) "Expected tampered provenance checksum to fail validation."
Assert-True ($tampered.Output.Contains("connectedhomeip artifact checksum mismatch")) ("Expected checksum mismatch failure message. Output:`n" + $tampered.Output)

$missingManifestRoot = Join-Path $tmpRoot "chip-controller-artifacts-missing-manifest"
New-ArtifactLayout -Root $missingManifestRoot -JarMode "Valid"
$missingManifest = Invoke-GradleArtifactVerification -ArtifactRoot $missingManifestRoot -ExtraGradleArgs @(
    "-PopenhabMatterChipArtifactExpectedSourceTag=v1.6.1.0"
)
Assert-True ($missingManifest.ExitCode -ne 0) "Expected missing provenance manifest to fail when a source tag is required."
Assert-True ($missingManifest.Output.Contains("Missing connectedhomeip artifact provenance manifest")) ("Expected missing-manifest failure message. Output:`n" + $missingManifest.Output)

New-ArtifactLayout -Root $emptyRoot -JarMode "EmptyControllerJar"
$negative = Invoke-GradleArtifactVerification -ArtifactRoot $emptyRoot
Assert-True ($negative.ExitCode -ne 0) "Expected empty synthetic artifacts to fail validation."
Assert-True ($negative.Output.Contains("Empty connectedhomeip controller jar")) ("Expected empty-jar failure message. Output:`n" + $negative.Output)

New-ArtifactLayout -Root $invalidRoot -JarMode "InvalidControllerJar"
$invalid = Invoke-GradleArtifactVerification -ArtifactRoot $invalidRoot
Assert-True ($invalid.ExitCode -ne 0) "Expected invalid synthetic artifacts to fail validation."
Assert-True ($invalid.Output.Contains("Invalid connectedhomeip controller jar")) ("Expected invalid-jar failure message. Output:`n" + $invalid.Output)

New-ArtifactLayout -Root $missingClassRoot -JarMode "MissingRequiredClass"
$missingClass = Invoke-GradleArtifactVerification -ArtifactRoot $missingClassRoot
Assert-True ($missingClass.ExitCode -ne 0) "Expected artifacts missing required controller classes to fail validation."
Assert-True ($missingClass.Output.Contains("Missing connectedhomeip controller class entry")) ("Expected missing-class failure message. Output:`n" + $missingClass.Output)

Write-Output "connectedhomeip artifact validation smoke-test passed."
