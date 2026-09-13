$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $root "backend"
$frontendDir = Join-Path $root "frontend"
$backendLog = Join-Path $backendDir "backend-run.log"
$backendPort = 8081
$backendHost = "localhost"
$backendStartupTimeoutSeconds = 45

function Show-BackendLogTail {
    if (Test-Path $backendLog) {
        Write-Host ""
        Write-Host "Last backend log lines:" -ForegroundColor Yellow
        Get-Content -LiteralPath $backendLog -Tail 40
    } else {
        Write-Host "Backend log file not found: $backendLog" -ForegroundColor Yellow
    }
}

function Test-PortOpen {
    param(
        [string]$HostName,
        [int]$Port
    )

    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne(1000, $false)) {
            $client.Close()
            return $false
        }
        $client.EndConnect($iar)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

if (-not (Test-PortOpen -HostName $backendHost -Port $backendPort)) {
    Write-Host "Starting backend on port $backendPort..."
    if (Test-Path $backendLog) {
        Remove-Item -LiteralPath $backendLog -Force
    }

    $backendProcess = Start-Process -FilePath "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" `
        -ArgumentList "-NoProfile", "-Command", "Set-Location '$backendDir'; & 'C:\Users\Hp\maven\apache-maven-3.9.14\bin\mvn.cmd' spring-boot:run '-Dspring-boot.run.profiles=local' *>> '$backendLog'" `
        -WindowStyle Hidden `
        -PassThru

    $started = $false
    for ($i = 0; $i -lt $backendStartupTimeoutSeconds; $i++) {
        Start-Sleep -Seconds 1
        if (Test-PortOpen -HostName $backendHost -Port $backendPort) {
            $started = $true
            break
        }

        if ($backendProcess.HasExited) {
            Write-Host "Backend process exited before it became ready. Check $backendLog" -ForegroundColor Red
            Show-BackendLogTail
            exit 1
        }
    }

    if (-not $started) {
        Write-Host "Backend did not start within $backendStartupTimeoutSeconds seconds. Check $backendLog" -ForegroundColor Red
        Show-BackendLogTail
        exit 1
    }
} else {
    Write-Host "Backend is already running on http://$backendHost`:$backendPort"
}

Write-Host "Backend is ready on http://$backendHost`:$backendPort"
Write-Host "Starting frontend..."
Set-Location $frontendDir
npm start
