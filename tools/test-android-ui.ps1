param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not $SkipBuild) { & './build-apk.ps1' }
$maven = '.tools/maven/apache-maven-3.9.9/bin/mvn.cmd'
$required = @($maven, '.tools/ui-test-libs/monitor/classes.jar', '.tools/ui-test-libs/espresso/classes.jar', '.tools/robolectric-sdk/android-all-instrumented-9-robolectric-4913185-2-i7.jar', '.tools/robolectric-sdk/android-all-instrumented-15-robolectric-12650502-i7.jar')
foreach ($tool in $required) { if (-not (Test-Path -LiteralPath $tool)) { throw "Missing UI test tool: $tool. Ask Codex to prepare the test environment." } }
$configDirectory = Join-Path $PWD '.tools/ui-test-resources/com/android/tools'
New-Item -ItemType Directory -Force $configDirectory | Out-Null
$properties = @{}
$files = @{
    android_merged_manifest = 'app/build/manual/AndroidManifest.xml'
    android_merged_resources = 'app/src/main/res'
    android_merged_assets = 'app/src/main/assets'
    android_resource_apk = 'app/build/outputs/apk/debug/app-debug.apk'
}
foreach ($key in $files.Keys) { $properties[$key] = (New-Object Uri((Resolve-Path -LiteralPath $files[$key]).Path)).AbsoluteUri }
$properties['android_custom_package'] = 'com.local.tapsumo'
$lines = @($properties.Keys | Sort-Object | ForEach-Object { "$_=$($properties[$_])" })
[IO.File]::WriteAllLines((Join-Path $configDirectory 'test_config.properties'),$lines,[Text.Encoding]::ASCII)
$env:JAVA_HOME = (Resolve-Path '.tools/jdk/jdk-17.0.20.1+1').Path
& $maven -o -f 'tools/android-ui-check/pom.xml' "-Dmaven.repo.local=$PWD/.tools/maven-repo" -B -ntp test
if ($LASTEXITCODE -ne 0) { throw 'Android view tests failed' }
Write-Output 'Android 9 and 15 native view checks passed. Preview images: docs/界面预览'
