param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
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

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)] [string] $Repository,
        [Parameter(Mandatory = $true)] [string[]] $Arguments
    )

    $output = & git -C $Repository @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ("git " + ($Arguments -join " ") + " failed:`n" + ($output -join "`n"))
    }
    return $output
}

function Add-Commit {
    param(
        [Parameter(Mandatory = $true)] [string] $Repository,
        [Parameter(Mandatory = $true)] [string] $Message,
        [Parameter(Mandatory = $true)] [string] $Content
    )

    Set-Content -LiteralPath (Join-Path $Repository "changes.txt") -Value $Content -NoNewline
    Invoke-Git $Repository @("add", "changes.txt") | Out-Null
    Invoke-Git $Repository @("commit", "-m", $Message) | Out-Null
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$scriptPath = Join-Path $ProjectRoot "scripts\generate_changelog.mjs"
Assert-True (Test-Path -LiteralPath $scriptPath -PathType Leaf) "Changelog generator not found: $scriptPath"

$workRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("openhab-matter-changelog-test-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $workRoot | Out-Null

try {
    Invoke-Git $workRoot @("init") | Out-Null
    Invoke-Git $workRoot @("config", "user.email", "test@example.invalid") | Out-Null
    Invoke-Git $workRoot @("config", "user.name", "Changelog Test") | Out-Null

    Add-Commit $workRoot "feat: initial release" "initial"
    Invoke-Git $workRoot @("tag", "v0.1.0") | Out-Null

    Add-Commit $workRoot "fix: first patch" "first"
    Invoke-Git $workRoot @("tag", "v0.1.1") | Out-Null

    Add-Commit $workRoot "fix: later patch" "later"

    Push-Location $workRoot
    try {
        $changelog = & node $scriptPath --tag v0.1.1 --repo example/project 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw ("generate_changelog.mjs failed:`n" + ($changelog -join "`n"))
        }
    } finally {
        Pop-Location
    }

    $text = $changelog -join "`n"
    Assert-True ($text.Contains("First patch")) "Expected existing tag release notes to include the tagged commit."
    Assert-True (-not $text.Contains("Later patch")) "Existing tag release notes must not include commits after the tag."
    Assert-True ($text.Contains("/compare/v0.1.0...v0.1.1")) "Expected full changelog link to compare previous tag to the release tag."
    Assert-True (-not $text.Contains("/compare/v0.1.1...v0.1.1")) "Full changelog link must not compare a tag with itself."
} finally {
    Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue
}
