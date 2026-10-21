param(
    [string]$SdkPath = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$JdkPath = 'C:\Program Files\Android\Android Studio\jbr'
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$env:JAVA_HOME = $JdkPath
$env:PATH = "$JdkPath\bin;$env:PATH"
$toolsPath = Join-Path $SdkPath 'build-tools\36.1.0'
$androidJar = Join-Path $SdkPath 'platforms\android-36\android.jar'
$buildPath = Join-Path $root 'work\build'
$classesPath = Join-Path $buildPath 'classes'
$dexPath = Join-Path $buildPath 'dex'
$keyPath = Join-Path $root 'work\signing\countdown-local.jks'
$outputPath = Join-Path $root 'outputs'
$generatedPath = Join-Path $buildPath 'generated'

function Run-Checked([string]$Program, [string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed ($LASTEXITCODE): $Program" }
}

foreach ($required in @($androidJar, "$JdkPath\bin\javac.exe", "$toolsPath\aapt2.exe", "$toolsPath\d8.bat", "$toolsPath\apksigner.bat")) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Required tool missing: $required" }
}
New-Item -ItemType Directory -Force -Path $classesPath,$dexPath,$generatedPath,$outputPath,(Split-Path $keyPath) | Out-Null
$manifestXml = [xml](Get-Content -LiteralPath "$root\app\src\main\AndroidManifest.xml" -Raw -Encoding UTF8)
$androidNs = 'http://schemas.android.com/apk/res/android'
$manifestXml.manifest.SetAttribute('package', 'local.countdown.probe')
$manifestXml.manifest.SetAttribute('versionCode', $androidNs, '17') | Out-Null
$manifestXml.manifest.SetAttribute('versionName', $androidNs, '0.17.0') | Out-Null
$sdkNode = $manifestXml.CreateElement('uses-sdk')
$sdkNode.SetAttribute('minSdkVersion', $androidNs, '30') | Out-Null
$sdkNode.SetAttribute('targetSdkVersion', $androidNs, '36') | Out-Null
$manifestXml.manifest.PrependChild($sdkNode) | Out-Null
$manifestXml.Save("$buildPath\AndroidManifest.xml")
Run-Checked "$toolsPath\aapt2.exe" @('compile','--dir',"$root\app\src\main\res",'-o',"$buildPath\resources.zip")
Run-Checked "$toolsPath\aapt2.exe" @('link','-o',"$buildPath\unsigned.apk",'--manifest',"$buildPath\AndroidManifest.xml",'-I',$androidJar,'--java',$generatedPath,"$buildPath\resources.zip")
$sourceFiles = @(Get-ChildItem (Join-Path $root 'app\src\main\java'),$generatedPath -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
Run-Checked "$JdkPath\bin\javac.exe" (@('--release','8','-encoding','UTF-8','-classpath',$androidJar,'-d',$classesPath) + $sourceFiles)
Run-Checked "$JdkPath\bin\jar.exe" @('cf',"$buildPath\classes.jar",'-C',$classesPath,'.')
Run-Checked "$toolsPath\d8.bat" @('--release','--min-api','30','--lib',$androidJar,'--output',$dexPath,"$buildPath\classes.jar")
Run-Checked "$JdkPath\bin\jar.exe" @('uf',"$buildPath\unsigned.apk",'-C',$dexPath,'classes.dex')
Run-Checked "$toolsPath\zipalign.exe" @('-f','-P','16','4',"$buildPath\unsigned.apk","$buildPath\aligned.apk")
if (-not (Test-Path -LiteralPath $keyPath)) {
    # A local-only development certificate. Keep this file to allow in-place updates.
    Run-Checked "$JdkPath\bin\keytool.exe" @('-genkeypair','-keystore',$keyPath,'-storepass','localdevelopment','-keypass','localdevelopment','-alias','countdown-local','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Countdown Local Development','-noprompt')
}
$apkPath = Join-Path $outputPath 'XiaomiCountdown-0.17.0.apk'
Run-Checked "$toolsPath\apksigner.bat" @('sign','--v4-signing-enabled','false','--ks',$keyPath,'--ks-key-alias','countdown-local','--ks-pass','pass:localdevelopment','--key-pass','pass:localdevelopment','--out',$apkPath,"$buildPath\aligned.apk")
Run-Checked "$toolsPath\apksigner.bat" @('verify','--verbose',$apkPath)
Run-Checked "$toolsPath\zipalign.exe" @('-c','-P','16','4',$apkPath)
Get-FileHash -Algorithm SHA256 -LiteralPath $apkPath | Format-List
Write-Output "Built: $apkPath"
