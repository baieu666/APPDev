# Your Day!

Aplicativo Android de organizacao de rotina, desenvolvido em Java e XML como projeto de faculdade.

## Funcionalidades atuais

- Cadastro do nome com armazenamento local em SQLite.
- Criacao de tarefas com nome, data e horario limite.
- Listagem das tarefas do dia e marcacao de conclusao.
- Progresso diario e tarefas concluidas no fim da lista.
- Status pendente, perto do prazo (15 minutos), atrasada e concluida.
- Atualizacao dos status enquanto o aplicativo esta aberto.
- Exclusao de tarefas com confirmacao por toque longo.
- Navegação pela agenda com calendário e botões de dia anterior e seguinte.
- Edição, prioridades e cópia de tarefas entre dias.
- Tela **Meu perfil**, acessível abaixo da saudação da agenda. Permite editar o nome e escolher, trocar ou remover a foto. Ao salvar o nome, a agenda atualiza a saudação.
- Temas claro, escuro ou do sistema.
- Lembretes locais de prazo, opcionais. Para ligar, toque em **Ativar lembretes de prazo** na agenda e permita as notificações se o Android solicitar. Toque no mesmo controle para desligar.

Os lembretes acompanham as alterações nas tarefas. Ao tocar em um aviso, o aplicativo abre a data da tarefa. O agendamento usa alarmes inexatos do Android, que podem chegar após o horário, especialmente em economia de bateria. Ao reiniciar o aparelho, o app recupera os prazos pendentes. Os dados ficam no próprio dispositivo.

## Desenvolvimento

O projeto pode ser aberto no Android Studio ou editado no VS Code.
Consulte [as instrucoes do VS Code](README-VSCODE.md) para os atalhos de compilacao e instalacao.

Configuracao atual: Android minimo API 24, compile/target SDK 37, Gradle Wrapper 9.6.0
e toolchain de execucao Gradle com Java 25. O codigo do aplicativo usa compatibilidade Java 11.
Instale o Android SDK correspondente e configure seu caminho em `local.properties`
ou na variavel `ANDROID_HOME`. Configure `JAVA_HOME` para o JDK.

Para gerar o APK de teste no PowerShell, na raiz do projeto:

```powershell
.\gradlew.bat :app:assembleDebug
```

O arquivo gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.
