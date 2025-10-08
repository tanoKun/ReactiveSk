<#
runEnv1.12.2-V2.6.3.ps1
PowerShell 版ランナー (Windows向け)
Usage:
  PowerShell -ExecutionPolicy Bypass -File .\runEnv.ps1 [-server <name.jar>] [-skript <Skript-vX.Y.Z.jar>] [-java <path_to_java>] [-NoStart] [-Detach] [-Stop]
#>
param(
    [string]$server = "1.12.2.jar",
    [string]$skript = "Skript-v2.6.3.jar",
    [string]$java = $null,
    [switch]$NoStart,
    [switch]$Detach,
    [switch]$Stop
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$serversDir = Join-Path $root 'test\servers'
$skriptPluginsDir = Join-Path $root 'test\skript-plugins'
$scriptsDir = Join-Path $root 'test\scripts'
$reactiveDir = Join-Path $root 'ReactiveSk-skript-v2_6_3\build\libs'

$serverBase = [IO.Path]::GetFileNameWithoutExtension($server)
$serverDir = Join-Path $serversDir $serverBase
$pluginsDir = Join-Path $serverDir 'plugins'
$skriptDest = Join-Path $pluginsDir 'Skript\scripts'
$pidFile = Join-Path $serverDir 'server.pid'
$logFile = Join-Path $serverDir 'server.log'

# initialize plugins dir: remove existing plugins to start clean
if (Test-Path $pluginsDir) {
    try {
        Remove-Item -Path $pluginsDir -Recurse -Force -ErrorAction SilentlyContinue
    } catch {
        Write-Host "Warning: failed to remove existing plugins dir: $_" -ForegroundColor Yellow
    }
}

# recreate server and plugins directories (skript dest will be created under plugins)
if (-not (Test-Path $serverDir)) { New-Item -ItemType Directory -Path $serverDir | Out-Null }
if (-not (Test-Path $pluginsDir)) { New-Item -ItemType Directory -Path $pluginsDir | Out-Null }
if (-not (Test-Path $skriptDest)) { New-Item -ItemType Directory -Path $skriptDest -Force | Out-Null }

# copy server jar (strict)
$serverSource = Join-Path $serversDir $server
if (Test-Path $serverSource -PathType Leaf) {
    Copy-Item -Path $serverSource -Destination (Join-Path $serverDir 'server.jar') -Force
}

# write eula as ASCII with CRLF
$eulaFile = Join-Path $serverDir 'eula.txt'
[System.IO.File]::WriteAllText($eulaFile, "eula=true`r`n", [System.Text.Encoding]::ASCII)

# copy ReactiveSk plugin if exists
if (Test-Path $reactiveDir) {
    $r = Get-ChildItem -Path $reactiveDir -Filter '*.jar' -File -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($r) { Copy-Item -Path $r.FullName -Destination $pluginsDir -Force }
}

# copy skript (strict)
$skriptSource = Join-Path $skriptPluginsDir $skript
if (Test-Path $skriptSource -PathType Leaf) {
    Copy-Item -Path $skriptSource -Destination $pluginsDir -Force
}

# copy .sk files
Get-ChildItem -Path $scriptsDir -Filter '*.sk' -File -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination (Join-Path $skriptDest $_.Name) -Force
}

if ($Stop) {
    if (Test-Path $pidFile) {
        try {
            $pid = Get-Content $pidFile | Select-Object -First 1
            if ($pid) {
                $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
                if ($proc) {
                    Write-Host "Stopping process id $pid"
                    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                } else {
                    Write-Host "No process with id $pid"
                }
            }
        } finally {
            Remove-Item $pidFile -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "PID file not found: $pidFile"
    }
    exit 0
}

if ($NoStart) {
    Write-Host "Files prepared under: $serverDir (no start)"
    exit 0
}

# determine java executable
if ([string]::IsNullOrEmpty($java)) {
    if ($env:JAVA_8_HOME) { $javaExe = Join-Path $env:JAVA_8_HOME 'bin\java.exe' } else { $javaExe = 'java' }
} else { $javaExe = $java }

# start server
$serverJar = Join-Path $serverDir 'server.jar'
if (-not (Test-Path $serverJar -PathType Leaf)) {
    Write-Host "server.jar not found in $serverDir" -ForegroundColor Red
    exit 1
}

if ($Detach) {
    Write-Host "Starting server (detached): $serverJar using java: $javaExe"
    $startArgs = @('-jar', 'server.jar', 'nogui')
    try {
        # PowerShell Core on Windows supports -RedirectStandardOutput; use if available
        $p = Start-Process -FilePath $javaExe -ArgumentList $startArgs -WorkingDirectory $serverDir -RedirectStandardOutput $logFile -RedirectStandardError $logFile -WindowStyle Hidden -PassThru
    } catch {
        # Fallback for Windows PowerShell: use cmd.exe to redirect output
        $cmd = "$javaExe -jar server.jar nogui > `"$logFile`" 2>&1"
        $p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $cmd -WorkingDirectory $serverDir -WindowStyle Hidden -PassThru
    }

    if ($p -and $p.Id) {
        try {
            Set-Content -Path $pidFile -Value $p.Id -Force
            Write-Host "Wrote PID $($p.Id) to $pidFile"
        } catch {
            Write-Host "Failed to write PID file: $_"
        }
    }
    exit 0
} else {
    Write-Host "Starting server: $serverJar using java: $javaExe"
    Push-Location $serverDir
    try {
        & $javaExe -jar $serverJar nogui
    } finally {
        Pop-Location
    }
}
