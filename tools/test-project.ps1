$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$javaBin = Join-Path $PWD '.tools/jdk/jdk-17.0.20.1+1/bin'
New-Item -ItemType Directory -Force '.tools/test-classes' | Out-Null
$names = @('Match','PushMotion','DuelMotion','VisualPosition','SoundSettings','GameCatalog','GameNavigator','BoardMatch','BallDuel','RaceMatch','PoolMatch','SeriesScore','DriveMath','TankMatch','GameOptions')
$sources = @($names | ForEach-Object { "app/src/main/java/com/local/tapsumo/$_.java" })
$tests = @(Get-ChildItem 'tests' -Filter '*Test.java' | ForEach-Object FullName)
& "$javaBin/javac.exe" -encoding UTF-8 -d '.tools/test-classes' @sources @tests
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
foreach ($test in Get-ChildItem 'tests' -Filter '*Test.java') {
    & "$javaBin/java.exe" -cp '.tools/test-classes' "com.local.tapsumo.$($test.BaseName)"
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $($test.Name)" }
}
$nodeRuntime = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
if (Test-Path $nodeRuntime) {
    & $nodeRuntime --test 'tests/audio-assets.test.cjs'
    if ($LASTEXITCODE -ne 0) { throw 'Audio asset tests failed' }
} else { Write-Output 'Audio check skipped: Node runtime unavailable.' }
