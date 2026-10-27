# Your Day no VS Code

Abra a pasta raiz deste repositorio no VS Code para continuar o projeto.

## Usar

- Gerar o APK: **Ctrl+Shift+B**.
- Outras acoes: **Terminal > Run Task**, escolha uma tarefa **Your Day**.
- Para testar, abra o emulador e depois execute **instalar e abrir no celular ou emulador**.
- Um celular real precisa ter depuracao USB habilitada e autorizar o computador para essa tarefa.
- A instalacao automatica exige um unico dispositivo conectado. Atualiza o aplicativo preservando os dados quando a assinatura e o pacote sao os mesmos.
- O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Onde editar

- Logica das telas: `app/src/main/java/com/example/projetofaculdadeyourday/MainActivity.java`.
- Banco local: `app/src/main/java/com/example/projetofaculdadeyourday/BancoLocal.java`.
- Telas: `app/src/main/res/layout/`.
- Nome do aplicativo: `app/src/main/res/values/strings.xml`.

Configure `JAVA_HOME` para seu JDK e `ANDROID_HOME` para seu Android SDK.
O script tambem reconhece `sdk.dir` em `local.properties`. Os arquivos
`local.properties` e `.vscode/settings.json` sao locais e nao entram no Git.
O Android Studio pode ficar fechado; mantenha o Java, o SDK e o emulador instalados.

O suporte a importacao Android da extensao Java e experimental. O resultado da
compilacao Gradle e a referencia para saber se o aplicativo compila. O botao
Run Java de um arquivo isolado nao executa este aplicativo Android.

## Terminal PowerShell

Gerar o APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Se houver varios dispositivos, escolha explicitamente:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\android.ps1 -Action Devices
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\android.ps1 -Action Run -DeviceSerial SERIAL_DO_DISPOSITIVO
```

Se houver varios emuladores cadastrados, use `-Action Emulator -AvdName NOME_DO_EMULADOR`.
