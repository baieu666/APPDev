# Your Day!

O Your Day! é uma agenda pessoal para Android. Organize suas tarefas por data, horário e prioridade, receba lembretes de prazo e acompanhe o progresso do dia.

## Instalar no celular

Você precisa de um celular com **Android 7.0 ou superior**. Se você já tem o arquivo APK, comece pelo passo 2.

1. **Obtenha o APK.** Quem tem acesso ao repositório pode entrar no GitHub, abrir a última execução bem-sucedida da `main` em [Validar Your Day](https://github.com/baieu666/APPDev/actions/workflows/android.yml) e baixar **Your-Day-debug** na seção **Artifacts**. Extraia o ZIP para encontrar `app-debug.apk`. O download de cada execução fica disponível por sete dias.
2. **Abra o arquivo no celular.** Toque no arquivo com extensão `.apk` e depois em **Instalar**. Se o Android pedir autorização, permita que o aplicativo usado para abrir o arquivo instale essa cópia. Você pode desativar essa permissão depois.
3. **Abra a agenda.** Toque em **Abrir**, digite como quer ser chamado e selecione **Comece a organizar seu dia**.

## Experimente com duas tarefas

1. Escolha uma data sem tarefas e toque em **+ Adicionar tarefa**. Crie “Ler um capítulo” e “Fazer compras”, cada uma com seu horário limite e prioridade.
2. Marque “Ler um capítulo” como concluída. Com essas duas tarefas no dia, o progresso passa para **50%**.
3. Em **Tarefas do dia**, toque em **Mostrar** e escolha **Pendentes**. Só “Fazer compras” aparece. O progresso continua em 50% porque considera todas as tarefas daquele dia.
4. Abra o menu de ações da tarefa e toque em **Copiar para outro dia**. Escolha o dia seguinte e use a seta da agenda para conferir a cópia.

## O que mais dá para fazer

- **Organizar a agenda.** Navegue pelas setas ou pelo calendário. Toque em uma tarefa para editá-la. Para excluir, use o menu de ações ou mantenha a tarefa pressionada e confirme. As concluídas vão para o fim da lista.
- **Acompanhar prazos.** A agenda atualiza o status enquanto está aberta e sinaliza quando faltam até 15 minutos. Para receber uma notificação no horário limite, toque em **Ativar lembretes de prazo** e permita as notificações se o Android solicitar. Toque no mesmo controle para desligar. O aviso abre a data da tarefa. O app recupera os lembretes pendentes ao reiniciar o aparelho. A economia de bateria pode atrasar a entrega.
- **Personalizar o app.** Em **Meu perfil**, altere o nome da saudação e escolha, troque ou remova sua foto. Em **Tema**, escolha claro, escuro ou o tema do sistema.

O nome e as tarefas ficam em um banco SQLite no próprio aparelho.

## Rodar pelo código

Desenvolvido em Java e XML como projeto de faculdade, o app pode ser aberto no Android Studio ou no VS Code. O projeto usa SDK 37 para compilação e destino, Gradle Wrapper 9.6.0 e JDK 25 para executar o Gradle. O código Java do app usa compatibilidade com Java 11. Configure `JAVA_HOME` para o JDK e indique o SDK em `local.properties` ou `ANDROID_HOME`.

Com o emulador aberto, execute no PowerShell da pasta do projeto para compilar, instalar e abrir o app:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\android.ps1 -Action Run
```

Para gerar apenas o APK, execute `.\gradlew.bat :app:assembleDebug`. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`. Os atalhos e instruções para selecionar um dispositivo estão no [guia do VS Code](README-VSCODE.md).
