param([switch]$WithTropimonMods, [switch]$IndependentPeersOnly)
$ErrorActionPreference = 'Stop'
# Uses cached official dependencies and an OFFLINE test account. Never launches the installed profile.
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gradleCache = Join-Path $env:USERPROFILE '.gradle/caches'
$modules = Join-Path $gradleCache 'modules-2/files-2.1'
$profileName = if ($IndependentPeersOnly) { 'independent-peers-production' } elseif ($WithTropimonMods) { 'coexistence-production' } else { 'standalone-production' }
$qa = Join-Path $repo "build/qa/$profileName"
New-Item -ItemType Directory -Path (Join-Path $qa 'mods') -Force | Out-Null
function CachedJar([string]$coordinate) {
    $parts = $coordinate.Split(':')
    $dir = Join-Path $modules "$($parts[0])/$($parts[1])/$($parts[2])"
    $suffix = if ($parts.Length -gt 3) { '-' + $parts[3] } else { '' }
    $name = "$($parts[1])-$($parts[2])$suffix.jar"
    $found = @(Get-ChildItem -LiteralPath $dir -Recurse -Filter $name -ErrorAction SilentlyContinue)
    if ($found.Count -eq 0 -and $coordinate -eq 'net.fabricmc:intermediary:1.21.1') {
        $download = Join-Path $qa 'intermediary-1.21.1.jar'
        if (!(Test-Path -LiteralPath $download)) {
            Invoke-WebRequest 'https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.1/intermediary-1.21.1.jar' -OutFile $download -TimeoutSec 30
        }
        return $download
    }
    if ($found.Count -ne 1) { throw "Missing/ambiguous cached official dependency: $coordinate" }
    return $found[0].FullName
}
$libraries = [Collections.Generic.List[string]]::new()
$info = Get-Content (Join-Path $gradleCache 'fabric-loom/1.21.1/mojang_minecraft_info.json') -Raw | ConvertFrom-Json
foreach ($lib in $info.libraries) {
    if ($lib.name -match 'natives-' -and $lib.name -notmatch ':natives-windows$') { continue }
    if ($lib.rules) {
        $allowed = $false
        foreach ($rule in $lib.rules) {
            if (!$rule.os -or $rule.os.name -eq 'windows') { $allowed = $rule.action -eq 'allow' }
        }
        if (!$allowed) { continue }
    }
    $libraries.Add((CachedJar $lib.name))
}
foreach ($coord in @('org.ow2.asm:asm:9.8','org.ow2.asm:asm-analysis:9.8','org.ow2.asm:asm-commons:9.8',
        'org.ow2.asm:asm-tree:9.8','org.ow2.asm:asm-util:9.8','net.fabricmc:sponge-mixin:0.16.3+mixin.0.8.7',
        'net.fabricmc:intermediary:1.21.1','net.fabricmc:fabric-loader:0.17.2')) {
    $libraries.Add((CachedJar $coord))
}
$libraries.Add((Join-Path $gradleCache 'fabric-loom/1.21.1/minecraft-client.jar'))
foreach ($coord in @('maven.modrinth:MdwFAVRL:FcsopG0e','net.fabricmc.fabric-api:fabric-api:0.116.6+1.21.1',
        'net.fabricmc:fabric-language-kotlin:1.13.7+kotlin.2.2.21')) {
    Copy-Item -LiteralPath (CachedJar $coord) -Destination (Join-Path $qa 'mods') -Force
}
$version = ((Get-Content (Join-Path $repo 'gradle.properties') | Where-Object { $_ -match '^mod_version=' }) -split '=',2)[1]
Copy-Item -LiteralPath (Join-Path $repo "build/libs/TropimonTeamBuilder-$version.jar") -Destination (Join-Path $qa 'mods') -Force
if ($WithTropimonMods -or $IndependentPeersOnly) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($file in Get-ChildItem (Join-Path $env:APPDATA '.Tropimon/mods') -Filter '*.jar') {
        $zip = [IO.Compression.ZipFile]::OpenRead($file.FullName)
        try {
            $entry = $zip.GetEntry('fabric.mod.json')
            if (!$entry) { continue }
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $manifest = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
            # These extra dependencies are needed by the OTHER mods, never by Team Builder.
            $include = if ($IndependentPeersOnly) {
                $manifest.id -in @('tropimon_catch_preview', 'tropimon_chat_filter', 'tropimon_damage_calc')
            } else {
                ($manifest.id -like 'tropi*' -or $manifest.id -in @('xaeroworldmap', 'geckolib')) -and $manifest.id -ne 'tropimon_team_saver'
            }
            if ($include) {
                Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $qa 'mods') -Force
                Write-Output "Coexistence: $($manifest.id) $($manifest.version)"
            }
        } finally { $zip.Dispose() }
    }
}
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
Push-Location $qa
try {
    & $java '-Xmx3G' '-Dfabric.development=false' '-cp' ($libraries -join ';') `
        'net.fabricmc.loader.impl.launch.knot.KnotClient' '--username' 'TeamBuilderQA' `
        '--uuid' '00000000-0000-0000-0000-000000000001' '--accessToken' '0' '--version' '1.21.1' `
        '--userType' 'legacy' '--gameDir' $qa '--assetsDir' (Join-Path $gradleCache 'fabric-loom/assets') `
        '--assetIndex' '1.21.1-17' '--width' '1100' '--height' '760'
    exit $LASTEXITCODE
} finally { Pop-Location }
