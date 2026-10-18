$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$env:JAVA_HOME = (Resolve-Path '.tools/jdk/jdk-17.0.20.1+1').Path
$javaBin = Join-Path $env:JAVA_HOME 'bin'
$sdk = '.tools/android-sdk'
$bt = Join-Path $sdk 'build-tools/35.0.0'
$androidJar = Join-Path $sdk 'platforms/android-35/android.jar'
$out = 'app/build/manual'
New-Item -ItemType Directory -Force "$out/classes", "$out/dex", 'app/build/outputs/apk/debug' | Out-Null
# Old anonymous callback classes must not leak into a newly packaged APK.
$classRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot "$out/classes"))
$buildRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'app/build/manual'))
if (-not $classRoot.StartsWith($buildRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe class cleanup path' }
Get-ChildItem -LiteralPath $classRoot -Filter '*.class' -File -Recurse | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
function Check-Exit { if ($LASTEXITCODE -ne 0) { throw "Build tool failed: $LASTEXITCODE" } }

[xml]$manifest = Get-Content -Raw -Encoding UTF8 'app/src/main/AndroidManifest.xml'
$ns = 'http://schemas.android.com/apk/res/android'
$manifest.manifest.SetAttribute('package', 'com.local.tapsumo')
$gradleConfig = Get-Content -Raw -Encoding UTF8 'app/build.gradle'
$versionCode = [regex]::Match($gradleConfig, 'versionCode\s+(\d+)').Groups[1].Value
$versionName = [regex]::Match($gradleConfig, "versionName\s+'([^']+)'").Groups[1].Value
if (-not $versionCode -or -not $versionName) { throw 'Cannot read version from app/build.gradle' }
$manifest.manifest.SetAttribute('versionCode', $ns, $versionCode) | Out-Null
$manifest.manifest.SetAttribute('versionName', $ns, $versionName) | Out-Null
$usesSdk = $manifest.CreateElement('uses-sdk')
$usesSdk.SetAttribute('minSdkVersion', $ns, '23') | Out-Null
$usesSdk.SetAttribute('targetSdkVersion', $ns, '35') | Out-Null
$manifest.manifest.PrependChild($usesSdk) | Out-Null
$manifest.Save((Join-Path $PSScriptRoot "$out/AndroidManifest.xml"))

& "$bt/aapt.exe" package -f -M "$out/AndroidManifest.xml" -I $androidJar -S 'app/src/main/res' -A 'app/src/main/assets' -0 wav -F "$out/unsigned.apk"
Check-Exit
$sources = @(Get-ChildItem 'app/src/main/java' -Filter '*.java' -Recurse | ForEach-Object FullName)
& "$javaBin/javac.exe" -encoding UTF-8 -source 8 -target 8 -bootclasspath $androidJar -d "$out/classes" @sources
Check-Exit
$classes = @(Get-ChildItem "$out/classes" -Filter '*.class' -Recurse | ForEach-Object FullName)
& "$bt/d8.bat" --lib $androidJar --min-api 23 --output "$out/dex" @classes
Check-Exit
& "$javaBin/jar.exe" uf "$out/unsigned.apk" -C "$out/dex" classes.dex
Check-Exit
& "$bt/zipalign.exe" -f 4 "$out/unsigned.apk" "$out/aligned.apk"
Check-Exit
$key = Join-Path $PSScriptRoot '.tools/debug.keystore'
if (-not (Test-Path $key)) {
    & "$javaBin/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
    Check-Exit
}
$apk = 'app/build/outputs/apk/debug/app-debug.apk'
& "$bt/apksigner.bat" sign --ks $key --ks-pass pass:android --key-pass pass:android --out $apk "$out/aligned.apk"
Check-Exit
& "$bt/apksigner.bat" verify --verbose $apk
Check-Exit
& "$bt/aapt.exe" dump badging $apk
Check-Exit
New-Item -ItemType Directory -Force '安装包/当前版本' | Out-Null
$delivery = "安装包/当前版本/神秘双人小游戏-v$versionName.apk"
Copy-Item -LiteralPath $apk -Destination $delivery -Force
Write-Output "APK ready: $delivery"
