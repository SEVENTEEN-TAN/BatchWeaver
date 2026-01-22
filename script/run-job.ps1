[CmdletBinding()]
param(
    [string]$LogFile = "out.log",
    [string[]]$Jobs = @(
        "demoJob",
        "conditionalFlowJob",
        "chunkProcessingJob",
        "format1ImportJob",
        "format2ImportJob",
        "format3ImportJob",
        "masterImportJob",
        "format1ExportJob",
        "format2ExportJob",
        "complexWorkflowJob"
    ),
    [switch]$NoPause
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = $OutputEncoding
[Console]::InputEncoding = $OutputEncoding

$scriptDir = Split-Path -Parent $PSCommandPath
Set-Location -Path $scriptDir
$LogFile = Join-Path -Path $scriptDir -ChildPath $LogFile

# Reset log file (overwrite)
"" | Out-File -FilePath $LogFile -Encoding UTF8

function Write-Log {
    param($Message, $Color = "White")
    Write-Host $Message -ForegroundColor $Color
    $Message | Out-File -FilePath $LogFile -Append -Encoding UTF8
}

Write-Log "Running jobs via Maven..." "Cyan"
Write-Log "Start time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "Cyan"
Write-Log ""

$success = 0
$failed = 0

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    Write-Log "ERROR: mvn not found. Please install Maven and ensure PATH is set." "Red"
    if (-not $NoPause -and $Host.Name -eq "ConsoleHost") {
        Write-Host "`nPress any key to exit..." -ForegroundColor Yellow
        $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
    }
    exit 1
}

foreach ($job in $Jobs) {
    Write-Log "Running: $job" "Yellow"

    try {
        & $mvn.Source "spring-boot:run" "-DskipTests" "-Dspring-boot.run.arguments=--job.name=$job" 2>&1 | ForEach-Object {
            $_ | Out-File -FilePath $LogFile -Append -Encoding UTF8
            Write-Host $_
        }
        $exitCode = $LASTEXITCODE
        if ($exitCode -eq 0) {
            Write-Log "[OK] $job succeeded`n" "Green"
            $success++
        } else {
            Write-Log "[FAIL] $job failed (ExitCode=$exitCode)`n" "Red"
            $failed++
        }
    } catch {
        Write-Log "[ERROR] $job threw exception: $($_.Exception.Message)`n" "Red"
        $failed++
    }
}

Write-Log "==========================================" "Cyan"
Write-Log "Total: $($Jobs.Count) | Success: $success | Failed: $failed" "Cyan"
Write-Log "End time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "Cyan"
Write-Log "==========================================" "Cyan"

Write-Host "`nLog saved to: $LogFile" -ForegroundColor Green

if ($failed -gt 0) {
    if (-not $NoPause -and $Host.Name -eq "ConsoleHost") {
        Write-Host "`nPress any key to exit..." -ForegroundColor Yellow
        $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
    }
    exit 1
}

if (-not $NoPause -and $Host.Name -eq "ConsoleHost") {
    Write-Host "`nPress any key to exit..." -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
}
