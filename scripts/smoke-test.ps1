param(
    [string]$BaseUrl = 'http://localhost:8080/tempo',
    [string]$TomcatHome = (Join-Path $env:USERPROFILE 'tools\apache-tomcat-10.1.61')
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$playwrightRoot = Join-Path $env:USERPROFILE 'tools\tempo-playwright'
$nodeModules = Join-Path $playwrightRoot 'node_modules'
$failed = $false

function Write-Result([string]$Name, [bool]$Passed, [string]$Detail = '') {
    if ($Passed) {
        Write-Output "PASS $Name"
    } else {
        Write-Output "FAIL $Name$(if ($Detail) { ': ' + $Detail })"
        $script:failed = $true
    }
}

function Wait-ForApp([int]$TimeoutSeconds = 90) {
    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        $status = 0
        try {
            $request = [System.Net.WebRequest]::Create("$BaseUrl/login")
            $request.Timeout = 1500
            $response = $request.GetResponse()
            $status = [int]$response.StatusCode
            $response.Close()
        } catch [System.Net.WebException] {
            if ($_.Exception.Response) {
                $status = [int]$_.Exception.Response.StatusCode
                $_.Exception.Response.Close()
            }
        }
        if ($status -eq '200') { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Set-TomcatEnvironment {
    $jdk25 = Join-Path $env:USERPROFILE 'tools\jdk-25'
    if (Test-Path (Join-Path $jdk25 'bin\java.exe')) {
        $env:JAVA_HOME = $jdk25
        $env:JRE_HOME = $jdk25
        $env:PATH = "$jdk25\bin;$env:PATH"
    } elseif (-not $env:JAVA_HOME) {
        throw 'Set JAVA_HOME to a Java 25+ JDK before running the Tomcat persistence restart.'
    }
    $env:CATALINA_HOME = $TomcatHome
    $env:CATALINA_BASE = $TomcatHome
}

function Get-TomcatProcesses {
    $escapedHome = [regex]::Escape($TomcatHome)
    @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -match $escapedHome })
}

function Get-TomcatLogCounts {
    $paths = @(Get-ChildItem (Join-Path $TomcatHome 'logs') -File -Filter '*.log' |
        ForEach-Object { $_.FullName })
    if ($paths.Count -eq 0) {
        return [pscustomobject]@{ DatabaseReady = 0; StartupErrors = 0; Exceptions = 0 }
    }
    [pscustomobject]@{
        DatabaseReady = @(Select-String -Path $paths -SimpleMatch '[Tempo] Database ready.').Count
        StartupErrors = @(Select-String -Path $paths -Pattern '\bSEVERE\b|DATABASE SETUP FAILED|Reminder refresh failed').Count
        Exceptions = @(Select-String -Path $paths -Pattern '\bException\b').Count
    }
}

if (-not (Test-Path (Join-Path $nodeModules 'playwright\package.json'))) {
    npm install --prefix $playwrightRoot --no-save --no-package-lock playwright@1.64.1
    if ($LASTEXITCODE -ne 0) { throw 'Could not install the isolated Playwright runtime.' }
}
$env:NODE_PATH = $nodeModules
$env:TEMPO_BASE_URL = $BaseUrl
$env:TEMPO_SMOKE_ID = [Guid]::NewGuid().ToString('N').Substring(0, 10)
$env:TEMPO_SMOKE_PASSWORD = "TempoSmoke-$($env:TEMPO_SMOKE_ID)!9"

$browserCache = Join-Path $env:LOCALAPPDATA 'ms-playwright'
$chromium = Get-ChildItem $browserCache -Directory -Filter 'chromium-*' -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName 'chrome-win64\chrome.exe') } |
    Select-Object -First 1
if (-not $chromium) {
    npx --yes --package=playwright@1.64.1 playwright install chromium
    if ($LASTEXITCODE -ne 0) { throw 'Could not install Playwright Chromium.' }
}

Set-TomcatEnvironment
$startupScript = Join-Path $TomcatHome 'bin\startup.bat'
$shutdownScript = Join-Path $TomcatHome 'bin\shutdown.bat'
$warFile = Join-Path $projectRoot 'target\tempo.war'
$webapps = Join-Path $TomcatHome 'webapps'
if (-not (Test-Path $startupScript) -or -not (Test-Path $shutdownScript)) {
    Write-Result 'Tomcat 10.1 installation is available' $false $TomcatHome
    exit 1
}
if (-not (Test-Path $warFile)) {
    Write-Result 'Packaged Tempo WAR is available' $false $warFile
    exit 1
}

if ((Get-TomcatProcesses).Count -gt 0) {
    Push-Location $TomcatHome
    try {
        & '.\bin\shutdown.bat' 2>$null | Out-Null
    } finally {
        Pop-Location
    }
}
$stopped = $false
for ($i = 0; $i -lt 45; $i++) {
    if ((Get-TomcatProcesses).Count -eq 0) { $stopped = $true; break }
    Start-Sleep -Seconds 1
}
Write-Result 'Tomcat stopped before deployment' $stopped
if (-not $stopped) { exit 1 }

$explodedApp = Join-Path $webapps 'tempo'
if (Test-Path $explodedApp) {
    Remove-Item -LiteralPath $explodedApp -Recurse -Force
}
Copy-Item -LiteralPath $warFile -Destination (Join-Path $webapps 'tempo.war') -Force
Write-Result 'Fresh WAR deployed to Tomcat' (Test-Path (Join-Path $webapps 'tempo.war'))
$initialLogCounts = Get-TomcatLogCounts

Push-Location $TomcatHome
try {
    & '.\bin\startup.bat'
} finally {
    Pop-Location
}
$appReady = Wait-ForApp
Write-Result 'Application readiness at /tempo/login' $appReady
if (-not $appReady) {
    $appLogs = Get-ChildItem (Join-Path $TomcatHome 'logs') -File -Filter '*.log' |
        Sort-Object LastWriteTime -Descending | Select-Object -First 4
    foreach ($log in $appLogs) {
        $matchingLines = Select-String -Path $log.FullName -Pattern 'SEVERE|Exception|DATABASE SETUP FAILED|Database ready' |
            Select-Object -Last 20
        foreach ($line in $matchingLines) { Write-Output $line.Line }
    }
    Push-Location $TomcatHome
    try { & '.\bin\shutdown.bat' 2>$null | Out-Null } finally { Pop-Location }
    exit 1
}
$initialStartupLogCounts = Get-TomcatLogCounts
Write-Result 'Tomcat log confirms database initialization' ($initialStartupLogCounts.DatabaseReady -gt $initialLogCounts.DatabaseReady)
Write-Result 'No database, exception, or severe startup errors in Tomcat log' (
    $initialStartupLogCounts.StartupErrors -le $initialLogCounts.StartupErrors -and
    $initialStartupLogCounts.Exceptions -le $initialLogCounts.Exceptions
)

$testFiles = @(
    'scripts\e2e\auth-flow.cjs',
    'scripts\e2e\goals-flow.cjs',
    'scripts\e2e\admin-flow.cjs'
)
foreach ($testFile in $testFiles) {
    Push-Location $projectRoot
    try {
        node (Join-Path $projectRoot $testFile)
        if ($LASTEXITCODE -ne 0) { $script:failed = $true }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path (Join-Path $TomcatHome 'bin\shutdown.bat'))) {
    Write-Result 'Tomcat restart persistence check' $false 'Pass -TomcatHome with a Tomcat 10.1 installation to enable this check.'
    exit 1
}

Set-TomcatEnvironment
Push-Location $TomcatHome
try {
    & '.\bin\shutdown.bat' 2>$null | Out-Null
} finally {
    Pop-Location
}
$stopped = $false
for ($i = 0; $i -lt 45; $i++) {
    $status = 0
    try {
        $request = [System.Net.WebRequest]::Create("$BaseUrl/login")
        $request.Timeout = 1500
        $response = $request.GetResponse()
        $status = [int]$response.StatusCode
        $response.Close()
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $_.Exception.Response.Close()
        }
    }
    if ($status -ne '200') { $stopped = $true; break }
    Start-Sleep -Seconds 1
}
Write-Result 'Tomcat stops cleanly before persistence check' $stopped

$restartReady = $false
if ($stopped) {
    $restartLogCounts = Get-TomcatLogCounts
    Push-Location $TomcatHome
    try {
        & '.\bin\startup.bat'
    } finally {
        Pop-Location
    }
    $restartReady = Wait-ForApp
}
Write-Result 'Tomcat restarts and serves /tempo/login' $restartReady

if ($restartReady) {
    $restartLogCountsAfter = Get-TomcatLogCounts
    Write-Result 'Tomcat log confirms database initialization after restart' (
        $restartLogCountsAfter.DatabaseReady -gt $restartLogCounts.DatabaseReady
    )
    Write-Result 'No database, exception, or severe errors after restart' (
        $restartLogCountsAfter.StartupErrors -le $restartLogCounts.StartupErrors -and
        $restartLogCountsAfter.Exceptions -le $restartLogCounts.Exceptions
    )

    Push-Location $projectRoot
    try {
        node (Join-Path $projectRoot 'scripts\e2e\persistence-check.cjs')
        if ($LASTEXITCODE -ne 0) { $script:failed = $true }
    } finally {
        Pop-Location
    }
}

try {
    Push-Location $TomcatHome
    & '.\bin\shutdown.bat' 2>$null | Out-Null
    Pop-Location
    Write-Output 'PASS Tomcat stopped after validation'
} catch {
    if ((Get-Location).Path -eq $TomcatHome) { Pop-Location }
    Write-Result 'Tomcat stopped after validation' $false $_.Exception.Message
}

if ($script:failed) { exit 1 }
Write-Output 'PASS All smoke-test flows completed'
