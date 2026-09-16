param(
    [int] $GamePid = 0,
    [Parameter(Mandatory = $true)] [string] $PendingJar,
    [Parameter(Mandatory = $true)] [string] $TargetJar,
    [Parameter(Mandatory = $true)] [string] $OldJar,
    [string[]] $AdditionalOldJars = @(),
    [Parameter(Mandatory = $true)] [string] $ArchiveDirectory,
    [Parameter(Mandatory = $true)] [string] $ExpectedSha256,
    [Parameter(Mandatory = $true)] [string] $LogPath
)

$ErrorActionPreference = 'Stop'
$backups = @()
$handles = @()

function Write-InstallLog([string] $Message) {
    Add-Content -LiteralPath $LogPath -Value ("{0:O} {1}" -f (Get-Date), $Message)
}

try {
    $PendingJar = [IO.Path]::GetFullPath($PendingJar)
    $TargetJar = [IO.Path]::GetFullPath($TargetJar)
    $OldJar = [IO.Path]::GetFullPath($OldJar)
    $AdditionalOldJars = @($AdditionalOldJars | ForEach-Object { [IO.Path]::GetFullPath($_) })
    $oldJars = @($OldJar) + $AdditionalOldJars | Select-Object -Unique
    $ArchiveDirectory = [IO.Path]::GetFullPath($ArchiveDirectory)
    $mods = Split-Path -Parent $TargetJar
    if ((Split-Path -Leaf $mods) -ine 'mods' -or
            $ArchiveDirectory -ieq $mods -or $ArchiveDirectory.StartsWith($mods + '\', [StringComparison]::OrdinalIgnoreCase) -or
            $PendingJar.StartsWith($mods + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe installation paths' }
    foreach ($old in $oldJars) {
        if ((Split-Path -Parent $old) -ine $mods) { throw 'Unsafe old JAR path' }
    }
    if ((Get-FileHash -LiteralPath $PendingJar -Algorithm SHA256).Hash -ine $ExpectedSha256) { throw 'Prepared JAR hash mismatch' }
    if ($oldJars -notcontains $TargetJar -and (Test-Path -LiteralPath $TargetJar)) { throw 'Target already exists' }
    Write-InstallLog 'Prepared and verified; waiting for Minecraft only if necessary'
    if ($GamePid -gt 0) { Wait-Process -Id $GamePid -ErrorAction SilentlyContinue }
    # A launcher running with -jar launcher.jar is not a Minecraft game process.
    $instance = (Split-Path -Parent $mods).TrimEnd('\','/')
    function GameRunning {
        foreach ($process in Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'") {
            if (!$process.CommandLine) { throw 'Cannot identify a Java process safely' }
            if ($process.CommandLine -notmatch '(?:net\.minecraft|KnotClient|--gameDir|--launchTarget)') { continue }
            if ($process.CommandLine -match '--gameDir(?:=|\s+)(?:"([^"]+)"|([^\s]+))') {
                $directory = if ($Matches[1]) { $Matches[1] } else { $Matches[2] }
                if ([IO.Path]::GetFullPath($directory).TrimEnd('\','/') -ine $instance) { continue }
            }
            return $true
        }
        return $false
    }
    while (GameRunning) { Start-Sleep -Seconds 2 }

    New-Item -ItemType Directory -Path $ArchiveDirectory -Force | Out-Null
    $staged = Join-Path $ArchiveDirectory ([guid]::NewGuid().ToString('N') + '.pending')
    Copy-Item -LiteralPath $PendingJar -Destination $staged
    if ((Get-FileHash -LiteralPath $staged -Algorithm SHA256).Hash -ine $ExpectedSha256) { throw 'Staging hash mismatch' }
    # Deny concurrent reads/writes while permitting our rename. Never force a locked JAR.
    foreach ($old in $oldJars) {
        $hash = (Get-FileHash -LiteralPath $old -Algorithm SHA256).Hash
        $backup = Join-Path $ArchiveDirectory ([IO.Path]::GetFileName($old) + '.' + [guid]::NewGuid().ToString('N') + '.backup')
        $handles += [IO.File]::Open($old, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::Delete)
        $backups += [pscustomobject]@{ Original = $old; Backup = $backup; Hash = $hash }
    }
    try {
        if (GameRunning) { throw 'Minecraft restarted; prepared JAR retained' }
        foreach ($entry in $backups) {
            Move-Item -LiteralPath $entry.Original -Destination $entry.Backup
        }
        Move-Item -LiteralPath $staged -Destination $TargetJar
    } finally {
        foreach ($handle in $handles) { $handle.Dispose() }
    }
    if ((Get-FileHash -LiteralPath $TargetJar -Algorithm SHA256).Hash -ine $ExpectedSha256) { throw 'Final integrity mismatch' }
    foreach ($entry in $backups) {
        if ((Get-FileHash -LiteralPath $entry.Backup -Algorithm SHA256).Hash -ine $entry.Hash) {
            throw 'Backup integrity mismatch'
        }
    }

    Write-InstallLog ('INSTALLED: destination and ' + $backups.Count + ' backup(s) verified')
} catch {
    foreach ($handle in $handles) {
        try { $handle.Dispose() } catch { }
    }
    if ($backups.Count -gt 0 -and (Test-Path -LiteralPath $TargetJar)) {
        Move-Item -LiteralPath $TargetJar -Destination (Join-Path $ArchiveDirectory ([guid]::NewGuid().ToString('N') + '.failed'))
    }
    foreach ($entry in $backups) {
        if ((Test-Path -LiteralPath $entry.Backup) -and !(Test-Path -LiteralPath $entry.Original)) {
            Copy-Item -LiteralPath $entry.Backup -Destination $entry.Original
        }
    }
    Write-InstallLog ('BLOCKED: prepared JAR retained; failure category: ' + $_.CategoryInfo.Category +
            '; exception: ' + $_.Exception.GetType().Name + '; script line: ' + $_.InvocationInfo.ScriptLineNumber)
    exit 1
}
