$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

# Prefer local .build, then ops-crypt .build, then user Android SDK + system Java
$BuildDir = Join-Path $ProjectRoot ".build"
$SharedBuilds = @(
    (Join-Path $ProjectRoot ".build"),
    "C:\Users\franc\ops-crypt-android\.build",
    "C:\Users\franc\Downloads\ksm-chat-android\.build"
)

$JdkDir = $null
$SdkDir = $null
foreach ($b in $SharedBuilds) {
    $j = Join-Path $b "jdk-17"
    $s = Join-Path $b "android-sdk"
    if (-not $JdkDir -and (Test-Path (Join-Path $j "bin\java.exe"))) { $JdkDir = $j }
    if (-not $SdkDir -and (Test-Path $s)) { $SdkDir = $s }
}

if (-not $SdkDir -and $env:LOCALAPPDATA) {
    $userSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $userSdk) { $SdkDir = $userSdk }
}

if ($JdkDir) {
    $env:JAVA_HOME = $JdkDir
    $env:Path = "$JdkDir\bin;" + $env:Path
}
if ($SdkDir) {
    $env:ANDROID_HOME = $SdkDir
    $env:ANDROID_SDK_ROOT = $SdkDir
    $env:Path = "$SdkDir\platform-tools;" + $env:Path
}

# local.properties for Gradle
$lp = Join-Path $ProjectRoot "local.properties"
$sdkEscaped = ($SdkDir -replace '\\', '\\')
if ($SdkDir) {
    Set-Content -Path $lp -Value "sdk.dir=$($SdkDir -replace '\\','\\')" -Encoding ASCII
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
Write-Host "Building JP Secure Lock..."

Set-Location $ProjectRoot
& .\gradlew.bat assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed: $LASTEXITCODE" }

$apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$outDir = Join-Path $env:USERPROFILE "Downloads\JP_Secure_Lock"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
$dest = Join-Path $outDir "JP_Secure_Lock_1.0.apk"
if (Test-Path $apk) {
    Copy-Item $apk $dest -Force
    Write-Host "APK: $apk" -ForegroundColor Green
    Write-Host "Copia: $dest" -ForegroundColor Green
} else {
    throw "APK nao gerado."
}
