param(
    [Parameter(Mandatory = $true)] [int] $GamePid,
    [Parameter(Mandatory = $true)] [string] $PendingJar,
    [Parameter(Mandatory = $true)] [string] $TargetJar,
    [Parameter(Mandatory = $true)] [string] $OldJar,
    [Parameter(Mandatory = $true)] [string] $ArchiveDirectory,
    [Parameter(Mandatory = $true)] [string] $ExpectedSha256,
    [Parameter(Mandatory = $true)] [string] $LogPath
)

$ErrorActionPreference = 'Stop'

function Write-InstallLog([string] $Message) {
    Add-Content -LiteralPath $LogPath -Value ("{0:O} {1}" -f (Get-Date), $Message)
}

try {
    Write-InstallLog "Waiting for Minecraft PID $GamePid"
    Wait-Process -Id $GamePid -ErrorAction SilentlyContinue

    New-Item -ItemType Directory -Path $ArchiveDirectory -Force | Out-Null
    $installed = $false
    for ($attempt = 1; $attempt -le 240 -and -not $installed; $attempt++) {
        try {
            if (Test-Path -LiteralPath $OldJar -PathType Leaf) {
                $archiveName = [IO.Path]::GetFileName($OldJar) + '.superseded.' + (Get-Date -Format 'yyyyMMddHHmmssfff')
                Move-Item -LiteralPath $OldJar -Destination (Join-Path $ArchiveDirectory $archiveName) -Force
            }
            Move-Item -LiteralPath $PendingJar -Destination $TargetJar -Force
            $actualSha256 = (Get-FileHash -LiteralPath $TargetJar -Algorithm SHA256).Hash
            if ($actualSha256 -ne $ExpectedSha256) {
                throw "Installed JAR hash mismatch: $actualSha256"
            }
            $installed = $true
        } catch {
            if ($attempt -eq 240) { throw }
            Start-Sleep -Milliseconds 250
        }
    }

    Write-InstallLog "Installed and verified: $TargetJar"
} catch {
    Write-InstallLog ("INSTALL FAILED: " + $_.Exception.Message)
    exit 1
}
