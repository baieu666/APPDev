param(
    [ValidateSet('Build', 'Run', 'Devices', 'Emulator')]
    [string]$Action = 'Build',
    [string]$DeviceSerial,
    [string]$AvdName
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
# Respeita a configuracao de cada computador; nao grava caminhos pessoais no Git.
$localProperties = Join-Path $projectRoot 'local.properties'
$sdkPath = $env:ANDROID_HOME
if (Test-Path -LiteralPath $localProperties) {
    $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir\s*=' } | Select-Object -First 1
    if ($sdkLine) {
        $sdkPath = ($sdkLine -replace '^sdk\.dir\s*=\s*', '').Replace('\:', ':').Replace('\\', '\').Replace('\ ', ' ')
    }
}
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath) { $sdkPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$env:ANDROID_HOME = $sdkPath
$adbPath = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
$apkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'

function Invoke-CheckedAdb {
    param([string[]]$Arguments)
    & $adbPath @Arguments
    if ($LASTEXITCODE -ne 0) { throw "O adb falhou (codigo $LASTEXITCODE)." }
}

try {
    if ($Action -eq 'Devices') {
        Invoke-CheckedAdb -Arguments @('devices', '-l')
        exit 0
    }

    if ($Action -eq 'Emulator') {
        $emulatorPath = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
        if (-not (Test-Path -LiteralPath $emulatorPath)) { throw 'Emulador Android nao encontrado.' }
        $availableAvds = @(& $emulatorPath -list-avds | Where-Object { $_.Trim() })
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel listar os emuladores.' }
        if (-not $AvdName) {
            if ($availableAvds.Count -ne 1) { throw 'Escolha um emulador com -AvdName NOME_DO_EMULADOR.' }
            $AvdName = $availableAvds[0].Trim()
        }
        if ($AvdName -notin $availableAvds) { throw 'O emulador escolhido nao foi encontrado.' }
        # Esta acao abre uma janela visivel porque foi escolhida pelo usuario.
        Start-Process -FilePath $emulatorPath -ArgumentList @('-avd', $AvdName)
        exit 0
    }

    if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        throw 'Configure JAVA_HOME para a pasta do JDK antes de compilar.'
    }

    if ($Action -eq 'Run') {
        $deviceOutput = & $adbPath devices
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel consultar os dispositivos.' }
        $readyDevices = @($deviceOutput | ForEach-Object {
            if ($_ -match '^(\S+)\s+device$') { $Matches[1] }
        })
        if ($DeviceSerial) {
            if ($DeviceSerial -notin $readyDevices) {
                throw 'O dispositivo escolhido nao esta conectado e autorizado.'
            }
        } elseif ($readyDevices.Count -eq 1) {
            $DeviceSerial = $readyDevices[0]
        } elseif ($readyDevices.Count -eq 0) {
            throw 'Abra o emulador ou conecte um celular com depuracao USB e autorize a conexao.'
        } else {
            throw 'Ha mais de um dispositivo. Deixe apenas um conectado ou use -DeviceSerial no terminal.'
        }
    }

    Push-Location $projectRoot
    try {
        & (Join-Path $projectRoot 'gradlew.bat') ':app:assembleDebug' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "A compilacao falhou (codigo $LASTEXITCODE)." }
    } finally {
        Pop-Location
    }

    if (-not (Test-Path -LiteralPath $apkPath)) { throw 'A compilacao terminou, mas o APK esperado nao foi encontrado.' }
    Write-Host "APK pronto: $apkPath"

    if ($Action -eq 'Run') {
        Invoke-CheckedAdb -Arguments @('-s', $DeviceSerial, 'install', '-r', $apkPath)
        Invoke-CheckedAdb -Arguments @('-s', $DeviceSerial, 'shell', 'am', 'start', '-W', '-n', 'com.example.projetofaculdadeyourday/.MainActivity')
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
