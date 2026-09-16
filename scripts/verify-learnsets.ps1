param(
    [Parameter(Mandatory=$true)][string]$ModDirectory,
    [Parameter(Mandatory=$true)][string]$OfficialCobblemonJar
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Audit only: reproduce the old union once and compare it with our autonomous resource.
# This script is NOT shipped or executed by the mod.
function AddDocument($index, [string]$id, $document) {
    $forms = @(@{ name=''; moves=$document.moves }) + @($document.forms)
    foreach ($form in $forms) {
        if (!$form -or !$form.moves) { continue }
        $key = $id.ToLowerInvariant() + '/' + ([string]$form.name).Trim().ToLowerInvariant()
        if (!$index.ContainsKey($key)) { $index[$key] = [Collections.Generic.HashSet[string]]::new() }
        foreach ($raw in $form.moves) {
            if ($raw -match '^(?:[0-9]+|tm|egg|tutor|evolution|form_change):([^ ]+)') {
                $move = ($Matches[1] -split ':')[-1].ToLowerInvariant()
                if ($move -match '^[a-z0-9_-]+$') { [void]$index[$key].Add($move) }
            }
        }
    }
}
function AddJar($index, [string]$path) {
    $zip = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -notmatch '^data/([^/]+)/(species|species_additions)/.+/.+\.json$') { continue }
            $namespace = $Matches[1]; $collection = $Matches[2]
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $document = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
            $id = if ($collection -eq 'species_additions') { $document.target } else { $namespace + ':' + [IO.Path]::GetFileNameWithoutExtension($entry.Name) }
            if ($id) { AddDocument $index $id $document }
        }
    } finally { $zip.Dispose() }
}
$oldIndex = @{}; $newIndex = @{}
foreach ($file in Get-ChildItem -LiteralPath $ModDirectory -Filter '*.jar') { AddJar $oldIndex $file.FullName }
AddJar $newIndex $OfficialCobblemonJar
$supplements = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../src/main/resources/assets/tropimon_team_saver/data/learnset_supplements.json') -Raw | ConvertFrom-Json
foreach ($property in $supplements.PSObject.Properties) { AddDocument $newIndex $property.Name $property.Value }
$keys = @($oldIndex.Keys) + @($newIndex.Keys) | Sort-Object -Unique
$different = @($keys | Where-Object {
    !$oldIndex.ContainsKey($_) -or !$newIndex.ContainsKey($_) -or !$oldIndex[$_].SetEquals($newIndex[$_])
})
if ($different.Count) { throw ('Learnset differences: ' + ($different -join ', ')) }
Write-Output "Equivalent learnsets: $($keys.Count) species/form keys; no differences."
