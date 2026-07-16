[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ApkPath,
    [string]$VersionName,
    [string]$OutputDirectory,
    [string]$SbomPath,
    [switch]$GenerateSbom,
    [switch]$RunVerification,
    [switch]$AllowDirtyWorkingTree
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$GradleWrapper = Join-Path $ProjectRoot "gradlew.bat"
$CertificateAllowlist = Join-Path $PSScriptRoot "release-certificate.txt"

function Invoke-CommandChecked {
    param([string]$FilePath, [string[]]$Arguments = @(), [switch]$AllowFailure)
    $output = @(& $FilePath @Arguments 2>&1 | ForEach-Object { "$_" })
    $code = $LASTEXITCODE
    if (-not $AllowFailure -and $code -ne 0) {
        throw "Command failed ($code): $FilePath $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }
    [pscustomobject]@{ Output = $output; ExitCode = $code }
}

function Find-ApkSigner {
    $sdkRoots = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
        $(if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA "Android\Sdk" })) |
        Where-Object { $_ } | Select-Object -Unique
    $candidates = foreach ($root in $sdkRoots) {
        $buildTools = Join-Path $root "build-tools"
        if (Test-Path -LiteralPath $buildTools -PathType Container) {
            foreach ($directory in Get-ChildItem -LiteralPath $buildTools -Directory) {
                $path = Join-Path $directory.FullName "apksigner.bat"
                $parsedVersion = $null
                if ((Test-Path -LiteralPath $path -PathType Leaf) -and
                    [version]::TryParse($directory.Name, [ref]$parsedVersion)) {
                    [pscustomobject]@{ Path = $path; Version = $parsedVersion }
                }
            }
        }
    }
    $selected = $candidates | Sort-Object Version -Descending | Select-Object -First 1
    if (-not $selected) {
        throw "apksigner.bat was not found in ANDROID_HOME, ANDROID_SDK_ROOT, or LOCALAPPDATA."
    }
    $selected.Path
}

function ConvertFrom-KeyValueLines {
    param([string[]]$Lines)
    $values = @{}
    foreach ($line in $Lines) {
        if ($line -match '^([A-Z_]+)=(.*)$') { $values[$Matches[1]] = $Matches[2].Trim() }
    }
    $values
}

function Get-CapturedValue {
    param([string]$Text, [string]$Pattern, [string]$Description)
    $match = [regex]::Match($Text, $Pattern, [Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) { throw "Could not read $Description." }
    $match.Groups[1].Value.Trim()
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw "Git was not found in PATH." }
Push-Location $ProjectRoot
try {
    $repoCheck = Invoke-CommandChecked git @("rev-parse", "--is-inside-work-tree") -AllowFailure
    if ($repoCheck.ExitCode -ne 0 -or $repoCheck.Output[-1] -ne "true") {
        throw "The project is not inside a Git repository."
    }
    $resolvedApk = Resolve-Path -LiteralPath $ApkPath -ErrorAction SilentlyContinue
    if (-not $resolvedApk -or -not (Test-Path -LiteralPath $resolvedApk.Path -PathType Leaf)) {
        throw "APK was not found: $ApkPath"
    }
    if ([IO.Path]::GetExtension($resolvedApk.Path) -ine ".apk") {
        throw "The artifact must have the .apk extension."
    }

    $dirty = @((Invoke-CommandChecked git @("status", "--porcelain")).Output)
    $isClean = $dirty.Count -eq 0
    if (-not $isClean -and -not $AllowDirtyWorkingTree) {
        throw "The working tree is dirty. Use -AllowDirtyWorkingTree only after review.`n$($dirty -join [Environment]::NewLine)"
    }
    if (-not $isClean) { Write-Warning "DIRTY WORKING TREE; this is not a clean release checkout." }

    if (-not (Test-Path -LiteralPath $GradleWrapper)) { throw "Gradle Wrapper was not found." }
    $info = ConvertFrom-KeyValueLines (Invoke-CommandChecked $GradleWrapper @("-q", ":app:printReleaseInfo")).Output
    $required = @("VERSION_NAME", "VERSION_CODE", "APPLICATION_ID", "COMPILE_SDK", "MIN_SDK", "TARGET_SDK", "AGP_VERSION", "KOTLIN_VERSION")
    foreach ($key in $required) { if (-not $info.ContainsKey($key)) { throw "Gradle did not return $key." } }
    if (-not $VersionName) { $VersionName = $info.VERSION_NAME }
    if ([string]::IsNullOrWhiteSpace($VersionName)) { throw "VersionName is empty; pass -VersionName." }
    if ($GenerateSbom -and $SbomPath) { throw "Use either -GenerateSbom or -SbomPath, not both." }
    if (-not $GenerateSbom -and -not $SbomPath) {
        throw "A release SBOM is required. Use -GenerateSbom or pass -SbomPath."
    }

    if ($GenerateSbom) {
        Invoke-CommandChecked $GradleWrapper @(":app:generateReleaseSbom", "--rerun-tasks") | Out-Null
        $SbomPath = Join-Path $ProjectRoot "releases\v$VersionName\dex-workspace-manager-v$VersionName.cdx.json"
    } elseif (-not [IO.Path]::IsPathRooted($SbomPath)) {
        $SbomPath = Join-Path $ProjectRoot $SbomPath
    }
    $resolvedSbom = Resolve-Path -LiteralPath $SbomPath -ErrorAction SilentlyContinue
    if (-not $resolvedSbom -or -not (Test-Path -LiteralPath $resolvedSbom.Path -PathType Leaf)) {
        throw "SBOM was not found: $SbomPath"
    }
    try { $sbom = Get-Content -Raw -LiteralPath $resolvedSbom.Path | ConvertFrom-Json }
    catch { throw "SBOM is not valid JSON: $($_.Exception.Message)" }
    if ($sbom.bomFormat -ne "CycloneDX") { throw "SBOM bomFormat must be CycloneDX." }
    if ([string]::IsNullOrWhiteSpace($sbom.specVersion)) { throw "SBOM specVersion is missing." }
    if (@($sbom.components).Count -eq 0 -and @($sbom.dependencies).Count -eq 0) {
        throw "SBOM must contain components or dependency relationships."
    }
    if (-not $sbom.metadata -or -not $sbom.metadata.component) {
        throw "SBOM metadata.component is missing."
    }
    $sbomTool = @($sbom.metadata.tools.components) | Select-Object -First 1
    if (-not $sbomTool -or [string]::IsNullOrWhiteSpace($sbomTool.name)) {
        throw "SBOM generation tool metadata is missing."
    }

    $apksigner = Find-ApkSigner
    $verify = Invoke-CommandChecked $apksigner @("verify", "--verbose", "--print-certs", $resolvedApk.Path) -AllowFailure
    if ($verify.ExitCode -ne 0) {
        throw "APK verification failed; no manifest was created.`n$($verify.Output -join [Environment]::NewLine)"
    }
    $verifyText = $verify.Output -join "`n"
    $cert256 = (Get-CapturedValue $verifyText 'certificate SHA-256 digest:\s*([0-9a-fA-F]+)' 'certificate SHA-256').ToLowerInvariant()
    $expected = (Get-Content -Raw -LiteralPath $CertificateAllowlist).Trim().ToLowerInvariant()
    if ($expected -notmatch '^[0-9a-f]{64}$') { throw "release-certificate.txt is invalid." }
    if ($cert256 -ne $expected) {
        throw [regex]::Unescape('APK kh\u00f4ng \u0111\u01b0\u1ee3c k\u00fd b\u1eb1ng certificate release \u0111\u00e3 \u0111\u0103ng k\u00fd.')
    }

    $certDn = Get-CapturedValue $verifyText 'certificate DN:\s*(.+)$' 'certificate DN'
    $certSha1 = (Get-CapturedValue $verifyText 'certificate SHA-1 digest:\s*([0-9a-fA-F]+)' 'certificate SHA-1').ToLowerInvariant()
    $keyAlgorithm = Get-CapturedValue $verifyText 'key algorithm:\s*(.+)$' 'key algorithm'
    $keySize = Get-CapturedValue $verifyText 'key size \(bits\):\s*(\d+)' 'key size'
    $signers = Get-CapturedValue $verifyText 'Number of signers:\s*(\d+)' 'number of signers'
    $v1 = Get-CapturedValue $verifyText 'Verified using v1 scheme \(JAR signing\):\s*(true|false)' 'v1 scheme'
    $v2 = Get-CapturedValue $verifyText 'Verified using v2 scheme \(APK Signature Scheme v2\):\s*(true|false)' 'v2 scheme'
    $v3Match = [regex]::Match($verifyText, 'Verified using v3 scheme \(APK Signature Scheme v3\):\s*(true|false)')
    $v3 = if ($v3Match.Success) { $v3Match.Groups[1].Value } else { "not reported" }

    $apk = Get-Item -LiteralPath $resolvedApk.Path
    $apkHash = (Get-FileHash -LiteralPath $apk.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $branch = (Invoke-CommandChecked git @("branch", "--show-current")).Output[-1]
    if (-not $branch) { $branch = "detached HEAD" }
    $commit = (Invoke-CommandChecked git @("rev-parse", "HEAD")).Output[-1]
    $tagResult = Invoke-CommandChecked git @("describe", "--tags", "--abbrev=0") -AllowFailure
    $tag = if ($tagResult.ExitCode -eq 0) { $tagResult.Output[-1] } else { "none" }

    $gradleText = (Invoke-CommandChecked $GradleWrapper @("--version")).Output -join "`n"
    $gradleVersion = Get-CapturedValue $gradleText '^Gradle\s+(.+)$' 'Gradle version'
    $javaVersion = Get-CapturedValue $gradleText '^Launcher JVM:\s*(.+)$' 'Java version'
    $os = Get-CapturedValue $gradleText '^OS:\s*(.+)$' 'operating system'
    $wrapperText = Get-Content -Raw (Join-Path $ProjectRoot "gradle\wrapper\gradle-wrapper.properties")
    $wrapperHash = if ($wrapperText -match '(?m)^distributionSha256Sum=[0-9a-fA-F]{64}\s*$') { "yes" } else { "no" }
    $metadata = if (Test-Path (Join-Path $ProjectRoot "gradle\verification-metadata.xml")) { "present" } else { "missing" }
    $locks = @(Get-ChildItem $ProjectRoot -Recurse -File -Filter "gradle.lockfile" | Where-Object FullName -NotMatch '[\\/]build[\\/]')
    $lockState = if ($locks.Count) { "present" } else { "missing" }
    $mode = if ($metadata -eq "present") { "strict" } else { "not confirmed" }

    $tasks = [ordered]@{ testDebugUnitTest="NOT RUN"; lintDebug="NOT RUN"; assembleDebug="NOT RUN"; assembleRelease="NOT RUN" }
    $verificationFailed = $false
    if ($RunVerification) {
        foreach ($task in @($tasks.Keys)) {
            Write-Host "Running Gradle task: $task"
            $result = Invoke-CommandChecked $GradleWrapper @($task) -AllowFailure
            if ($result.ExitCode -eq 0) { $tasks[$task] = "PASS" }
            else { $tasks[$task] = "FAIL"; $verificationFailed = $true; Write-Warning "$task failed." }
        }
    }

    if (-not $OutputDirectory) { $OutputDirectory = Join-Path $ProjectRoot "releases\v$VersionName" }
    elseif (-not [IO.Path]::IsPathRooted($OutputDirectory)) { $OutputDirectory = Join-Path $ProjectRoot $OutputDirectory }
    $null = New-Item -ItemType Directory -Path $OutputDirectory -Force
    $sbomFileName = "dex-workspace-manager-v$VersionName.cdx.json"
    $publishedSbomPath = Join-Path $OutputDirectory $sbomFileName
    if ($resolvedSbom.Path -ne $publishedSbomPath) {
        Copy-Item -LiteralPath $resolvedSbom.Path -Destination $publishedSbomPath -Force
    }
    $sbomHash = (Get-FileHash -LiteralPath $publishedSbomPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksumPath = Join-Path $OutputDirectory "CHECKSUMS.sha256"
    Set-Content $checksumPath @("$apkHash  $($apk.Name)", "$sbomHash  $sbomFileName") -Encoding UTF8

    $clean = if ($isClean) { "yes" } else { "no" }
    $manifestPath = Join-Path $OutputDirectory "RELEASE_MANIFEST.md"
    $manifest = @"
# DeX Workspace Manager Release Manifest

## Release

* Version name: $VersionName
* Version code: $($info.VERSION_CODE)
* Application ID: $($info.APPLICATION_ID)
* Build date UTC: $([DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ"))
* Git branch: $branch
* Git commit: $commit
* Git tag: $tag
* Working tree clean: $clean

## Build Environment

* Operating system: $os
* Java version: $javaVersion
* Gradle version: $gradleVersion
* Android Gradle Plugin version: $($info.AGP_VERSION)
* Kotlin version: $($info.KOTLIN_VERSION)
* compileSdk: $($info.COMPILE_SDK)
* minSdk: $($info.MIN_SDK)
* targetSdk: $($info.TARGET_SDK)

## Artifact

* APK file name: $($apk.Name)
* File size: $($apk.Length) bytes
* APK SHA-256: $apkHash
* Signature schemes: v1=$v1, v2=$v2, v3=$v3
* Number of signers: $signers

## Signing Certificate

* Certificate DN: $certDn
* Certificate SHA-256: $cert256
* Certificate SHA-1: $certSha1
* Key algorithm: $keyAlgorithm
* Key size: $keySize bits

## Dependency Integrity

* Gradle Wrapper checksum configured: $wrapperHash
* Dependency verification metadata: $metadata
* Dependency lock state: $lockState
* Verification mode: $mode

## Build Verification

* testDebugUnitTest: $($tasks.testDebugUnitTest)
* lintDebug: $($tasks.lintDebug)
* assembleDebug: $($tasks.assembleDebug)
* assembleRelease: $($tasks.assembleRelease)

## Software Bill of Materials

* Format: CycloneDX JSON
* File name: $sbomFileName
* SBOM specification version: $($sbom.specVersion)
* SBOM SHA-256: $sbomHash
* Generation tool: $($sbomTool.name)
* Generation tool version: $($sbomTool.version)
* Generated from Git commit: $commit
* Dependency verification state: $metadata ($mode)
* Dependency lock state: $lockState

## Device Compatibility

* Note8 Hades ROM v3: NOT TESTED
* S23 Ultra: NOT TESTED

## Notes

* Release APK was signed manually.
* Keystore is not stored in the repository.
* APK must use the registered release certificate.
* This records environment, dependency state, and checksum; it does not claim byte-for-byte reproducibility.
"@
    Set-Content $manifestPath $manifest -Encoding UTF8

    $reportPath = Join-Path $OutputDirectory "TEST_REPORT.md"
    if (-not (Test-Path $reportPath)) {
        $checks = @("DeX-only Activity", "Home", "Layout Editor", "App Picker", "Save/restore workspace", "Launch individual app", "Launch whole workspace", "Bounds", "Order", "Delay", "App unavailable handling", "Reconnect DeX", "Restart device")
        $rows = ($checks | ForEach-Object { "* $_`: NOT TESTED" }) -join "`r`n"
        $report = @"
# Device Test Report

## Release

* Version: $VersionName
* Git commit: $commit
* APK SHA-256: $apkHash

## Samsung Note8 Hades ROM v3

$rows

## Samsung S23 Ultra

$rows

## Issues

## Tester

## Test date
"@
        Set-Content $reportPath $report -Encoding UTF8
    } else { Write-Warning "Preserving existing TEST_REPORT.md: $reportPath" }

    Write-Host "Certificate match: $cert256"
    Write-Host "Checksum: $checksumPath"
    Write-Host "Manifest: $manifestPath"
    Write-Host "SBOM: $publishedSbomPath"
    Write-Host "Device test report: $reportPath"
    if ($verificationFailed) { Write-Error "Verification failed; a diagnostic manifest was created." -ErrorAction Continue; exit 1 }
} finally { Pop-Location }
