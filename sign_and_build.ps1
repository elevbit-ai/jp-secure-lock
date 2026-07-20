$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
Set-Location $ProjectRoot

# --- Resolve JDK / SDK ---
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
}

$sdkProp = ($SdkDir -replace '\\', '/')
Set-Content -Path (Join-Path $ProjectRoot "local.properties") -Value "sdk.dir=$sdkProp" -Encoding ASCII

# --- Keystore (release) ---
$ksDir = Join-Path $ProjectRoot "signing"
if (-not (Test-Path $ksDir)) { New-Item -ItemType Directory -Force -Path $ksDir | Out-Null }
$ksPath = Join-Path $ksDir "jp-secure-lock.jks"
$storePass = "JpSecure2026!"
$keyPass = "JpSecure2026!"
$alias = "jpsecure"

$keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
if (-not (Test-Path $keytool)) { $keytool = "keytool" }

if (-not (Test-Path $ksPath)) {
    Write-Host "Gerando keystore de assinatura (Joaquim Pedro)..." -ForegroundColor Cyan
    & $keytool -genkeypair -v `
        -keystore $ksPath `
        -storetype JKS `
        -alias $alias `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -storepass $storePass `
        -keypass $keyPass `
        -dname "CN=Joaquim Pedro de Morais Filho, OU=JP Secure Lock, O=Joaquim Pedro, L=Brasil, ST=BR, C=BR"
    if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar keystore" }
}

# keystore.properties (caminho relativo ao modulo app/ -> ../signing/...)
$ksProps = @"
storeFile=../signing/jp-secure-lock.jks
storePassword=$storePass
keyAlias=$alias
keyPassword=$keyPass
"@
Set-Content -Path (Join-Path $ProjectRoot "keystore.properties") -Value $ksProps -Encoding ASCII

# Nao commitar segredos: garantir gitignore
$gi = Join-Path $ProjectRoot ".gitignore"
if (-not (Test-Path $gi)) {
    Set-Content $gi @"
/.gradle/
/build/
/app/build/
/.build/
/local.properties
/keystore.properties
/signing/*.jks
/signing/*.keystore
*.apk
*.ap_
.idea/
"@
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
Write-Host "Assinando e compilando RELEASE..." -ForegroundColor Cyan

& .\gradlew.bat clean assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Build release falhou" }

$apk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
$outDir = Join-Path $env:USERPROFILE "Downloads\JP_Secure_Lock"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
$dest = Join-Path $outDir "JP-Secure-Lock-AntiTheft-v1.4.0.apk"

if (-not (Test-Path $apk)) { throw "APK release nao encontrado: $apk" }
Copy-Item $apk $dest -Force

# Verificar assinatura
$apksigner = $null
$bt = Join-Path $SdkDir "build-tools"
if (Test-Path $bt) {
    $latest = Get-ChildItem $bt -Directory | Sort-Object Name -Descending | Select-Object -First 1
    $cand = Join-Path $latest.FullName "apksigner.bat"
    if (Test-Path $cand) { $apksigner = $cand }
}
if ($apksigner) {
    Write-Host "Verificando assinatura..." -ForegroundColor Cyan
    & $apksigner verify --verbose $dest
}

Write-Host ""
Write-Host "APK ASSINADO OK" -ForegroundColor Green
Write-Host "  $apk"
Write-Host "  $dest"
Write-Host ""
Write-Host "Keystore: $ksPath" -ForegroundColor Yellow
Write-Host "Alias: $alias | Guarde a senha em local seguro." -ForegroundColor Yellow
Write-Host "Senha (esta maquina): $storePass" -ForegroundColor Yellow
