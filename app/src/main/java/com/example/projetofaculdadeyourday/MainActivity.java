package com.example.projetofaculdadeyourday;

import android.os.Bundle;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.IOException;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Locale PORTUGUES_BRASIL = Locale.forLanguageTag("pt-BR");

    private String formatar(long instante, String formato) {
        return new SimpleDateFormat(formato, PORTUGUES_BRASIL).format(new Date(instante));
    }

    private FotoPerfil fotos;
    private final ActivityResultLauncher<String> escolherFoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) atualizarFoto(uri, false);
            });

    private BancoLocal banco;
    private EditText editNome;
    private Button btnComecar;

    private final Handler atualizadorStatus =
            new Handler(Looper.getMainLooper());

    // O valor sentinela acompanha o dia atual, inclusive na virada da meia-noite.
    private long dataEscolhida = Long.MIN_VALUE;
    private long diaDaLista = -1L;
    private int consultaAtual;

    private Calendar diaSelecionado() {
        Calendar dia = Calendar.getInstance();
        if (dataEscolhida != Long.MIN_VALUE) dia.setTimeInMillis(dataEscolhida);
        dia.set(Calendar.HOUR_OF_DAY, 0);
        dia.set(Calendar.MINUTE, 0);
        dia.set(Calendar.SECOND, 0);
        dia.set(Calendar.MILLISECOND, 0);
        return dia;
    }

    private void selecionarDia(Calendar dia) {
        dataEscolhida = android.text.format.DateUtils.isToday(dia.getTimeInMillis())
                ? Long.MIN_VALUE : dia.getTimeInMillis();
        carregarTarefasDoDia();
    }

    private void moverDia(int deslocamento) {
        Calendar dia = diaSelecionado();
        dia.add(Calendar.DAY_OF_MONTH, deslocamento);
        selecionarDia(dia);
    }

    private void abrirCalendario() {
        Calendar dia = diaSelecionado();
        new DatePickerDialog(this, (view, ano, mes, numero) -> {
            dia.set(ano, mes, numero);
            selecionarDia(dia);
        }, dia.get(Calendar.YEAR), dia.get(Calendar.MONTH),
                dia.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putLong("dataEscolhida", dataEscolhida);
    }

    private final Runnable atualizarRelogio = new Runnable() {
        @Override
        public void run() {
            LinearLayout lista = findViewById(R.id.listaTarefas);

            if (lista != null) {
                if (diaDaLista != diaSelecionado().getTimeInMillis()) {
                    carregarTarefasDoDia();
                } else {
                    // Atualiza as cores sem reconstruir a lista.
                    for (int i = 0; i < lista.getChildCount(); i++) {
                        atualizarStatusTarefa(lista.getChildAt(i));
                    }
                }
            }

            atualizadorStatus.postDelayed(this, 15_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) dataEscolhida = savedInstanceState.getLong("dataEscolhida", Long.MIN_VALUE);

        EdgeToEdge.enable(this);
        banco = new BancoLocal(this);
        fotos = new FotoPerfil(this);

        mostrarEntrada();
        habilitarEntrada(false);

        // Consulta o banco sem bloquear a tela.
        executor.execute(() -> {
            try {
                String nomeSalvo = banco.buscarNome();

                atualizarTela(() -> {
                    if (nomeSalvo.isEmpty()) {
                        habilitarEntrada(true);
                    } else {
                        mostrarInicio(nomeSalvo);
                    }
                });

            } catch (Exception erro) {
                Log.e("YourDay", "Erro ao carregar perfil", erro);

                atualizarTela(() -> {
                    habilitarEntrada(true);
                    avisar("Não foi possível carregar o nome salvo.");
                });
            }
        });
    }

    private void mostrarEntrada() {
        setContentView(R.layout.activity_main);
        ajustarBarras();

        editNome = findViewById(R.id.editNome);
        btnComecar = findViewById(R.id.btnComecar);

        btnComecar.setOnClickListener(view -> salvarPerfil());
    }

    private void salvarPerfil() {
        String nome = editNome.getText().toString().trim();

        if (nome.isEmpty()) {
            editNome.setError("Digite como você quer ser chamado.");
            editNome.requestFocus();
            return;
        }

        // Fecha o teclado antes de trocar a tela.
        InputMethodManager teclado =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (teclado != null) {
            teclado.hideSoftInputFromWindow(editNome.getWindowToken(), 0);
        }

        habilitarEntrada(false);

        executor.execute(() -> {
            try {
                banco.salvarNome(nome);
                atualizarTela(() -> mostrarInicio(nome));

            } catch (Exception erro) {
                Log.e("YourDay", "Erro ao salvar perfil", erro);

                atualizarTela(() -> {
                    habilitarEntrada(true);
                    avisar("Não foi possível salvar. Tente novamente.");
                });
            }
        });
    }

    private void mostrarInicio(String nome) {
        setContentView(R.layout.activity_inicio);
        ajustarBarras();

        TextView saudacao = findViewById(R.id.textSaudacao);
        saudacao.setText("Olá, " + nome + "!");

        configurarFotoPerfil(nome);
        findViewById(R.id.diaAnterior).setOnClickListener(v -> moverDia(-1));
        findViewById(R.id.proximoDia).setOnClickListener(v -> moverDia(1));
        findViewById(R.id.escolherData).setOnClickListener(v -> abrirCalendario());

        Button adicionar = findViewById(R.id.btnAdicionarTarefa);
        adicionar.setOnClickListener(view -> abrirFormulario(null));

        carregarTarefasDoDia();
    }

    private void configurarFotoPerfil(String nome) {
        TextView inicial = findViewById(R.id.inicialPerfil);
        inicial.setText(nome.isEmpty() ? "?" :
                nome.substring(0, nome.offsetByCodePoints(0, 1)).toUpperCase(PORTUGUES_BRASIL));
        View avatar = findViewById(R.id.editarFoto);
        avatar.setClipToOutline(true);
        avatar.setOnClickListener(v -> {
            if (findViewById(R.id.fotoPerfil).getVisibility() != View.VISIBLE) {
                escolherFoto.launch("image/*");
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.foto_perfil)
                    .setItems(new String[]{getString(R.string.trocar_foto), getString(R.string.remover_foto)},
                            (dialog, opcao) -> {
                                if (opcao == 0) escolherFoto.launch("image/*");
                                else atualizarFoto(null, true);
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
        atualizarFoto(null, false);
    }

    private void atualizarFoto(Uri origem, boolean remover) {
        FotoPerfil.FILA.execute(() -> {
            try {
                if (remover) fotos.remover();
                Bitmap bitmap = origem == null ? fotos.carregar() : fotos.salvar(origem);
                atualizarTela(() -> {
                    ImageView foto = findViewById(R.id.fotoPerfil);
                    if (foto == null) return;
                    foto.setImageBitmap(bitmap);
                    foto.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
                    findViewById(R.id.editarFoto).setContentDescription(getString(
                            bitmap == null ? R.string.escolher_foto : R.string.alterar_foto));
                });
            } catch (IOException | RuntimeException erro) {
                Log.w("YourDay", "Falha ao acessar foto de perfil", erro);
                atualizarTela(() -> avisar(getString(R.string.erro_foto)));
            }
        });
    }

    private void habilitarEntrada(boolean habilitada) {
        editNome.setEnabled(habilitada);
        btnComecar.setEnabled(habilitada);
    }

    private void ajustarBarras() {
        View raiz = findViewById(R.id.main);

        // Deixa os ícones das barras claros sobre o fundo preto.
        WindowCompat.getInsetsController(getWindow(), raiz)
                .setAppearanceLightStatusBars(false);

        WindowCompat.getInsetsController(getWindow(), raiz)
                .setAppearanceLightNavigationBars(false);

        // Evita que o conteúdo fique atrás das barras ou do teclado.
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (view, insets) -> {
            Insets espacos = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.ime()
            );

            view.setPadding(
                    espacos.left,
                    espacos.top,
                    espacos.right,
                    espacos.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(raiz);
    }

    private void atualizarTela(Runnable acao) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                acao.run();
            }
        });
    }

    private void avisar(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }

    private void carregarTarefasDoDia() {
        LinearLayout lista = findViewById(R.id.listaTarefas);
        TextView mensagem = findViewById(R.id.textSemTarefas);
        TextView textoProgresso = findViewById(R.id.textProgresso);
        ProgressBar barra = findViewById(R.id.barraProgresso);

        Calendar dia = diaSelecionado();
        long inicio = dia.getTimeInMillis();
        dia.add(Calendar.DAY_OF_MONTH, 1);
        long fim = dia.getTimeInMillis();
        int consulta = ++consultaAtual;
        Button data = findViewById(R.id.escolherData);
        String rotulo = formatar(inicio, "d 'de' MMMM 'de' yyyy");
        data.setText(rotulo);
        data.setContentDescription("Escolher data no calendário: " + rotulo);
        TextView resumo = findViewById(R.id.textResumo);
        if (inicio != diaDaLista) {
            lista.removeAllViews();
            mensagem.setVisibility(View.GONE);
            textoProgresso.setText("—");
            resumo.setText("");
            barra.setProgress(0);
            barra.setContentDescription("Carregando progresso");
        }
        diaDaLista = inicio;

        executor.execute(() -> {
            try {
                List<BancoLocal.Tarefa> tarefas =
                        banco.buscarTarefas(inicio, fim);

                atualizarTela(() -> {
                    if (consulta != consultaAtual) return;
                    lista.removeAllViews();

                    mensagem.setText("Nenhuma tarefa neste dia.");
                    mensagem.setVisibility(
                            tarefas.isEmpty() ? View.VISIBLE : View.GONE
                    );

                    String[] niveis = getResources().getStringArray(R.array.niveis_prioridade);
                    int concluidas = 0;

                    for (BancoLocal.Tarefa tarefa : tarefas) {
                        if (tarefa.concluida) {
                            concluidas++;
                        }

                        View item = getLayoutInflater().inflate(
                                R.layout.item_tarefa,
                                lista,
                                false
                        );

                        CheckBox checkbox = item.findViewById(R.id.checkTarefa);
                        TextView titulo = item.findViewById(R.id.textNomeTarefa);
                        TextView horario = item.findViewById(R.id.textHorarioTarefa);
                        TextView status = item.findViewById(R.id.textStatusTarefa);

                        titulo.setText(tarefa.nome);
                        TextView prioridade = item.findViewById(R.id.textPrioridade);
                        prioridade.setText(getString(R.string.rotulo_prioridade, niveis[tarefa.prioridade]));
                        prioridade.setTextColor(getColor(tarefa.prioridade == 2
                                ? R.color.prioridade_alta : R.color.texto_secundario));

                        String hora = formatar(tarefa.prazo, "HH:mm");

                        horario.setText("Horário limite: " + hora);

                        item.setTag(tarefa);
                        atualizarStatusTarefa(item);

                        checkbox.setChecked(tarefa.concluida);

                        checkbox.setContentDescription(
                                tarefa.concluida
                                        ? "Marcar " + tarefa.nome + " como pendente"
                                        : "Concluir " + tarefa.nome
                        );

                        checkbox.setOnCheckedChangeListener((botao, marcada) -> {
                            // Evita salvar novamente ao restaurar o estado após erro.
                            if (marcada == tarefa.concluida) {
                                return;
                            }

                            checkbox.setEnabled(false);

                            executor.execute(() -> {
                                try {
                                    banco.atualizarConclusao(tarefa.id, marcada);

                                    atualizarTela(() -> carregarTarefasDoDia());

                                } catch (Exception erro) {
                                    Log.e("YourDay", "Erro ao concluir tarefa", erro);

                                    atualizarTela(() -> {
                                        checkbox.setChecked(tarefa.concluida);
                                        checkbox.setEnabled(true);
                                        avisar("Não foi possível atualizar a tarefa.");
                                    });
                                }
                            });
                        });

                        View.OnLongClickListener acaoExcluir = view -> {
                            confirmarExclusao(tarefa);
                            return true;
                        };

                        View.OnClickListener editar = view -> abrirFormulario(tarefa);
                        View botaoEditar = item.findViewById(R.id.btnEditarTarefa);
                        botaoEditar.setContentDescription("Editar " + tarefa.nome);
                        botaoEditar.setOnClickListener(editar);
                        item.setOnClickListener(editar);
                        titulo.setOnClickListener(editar);
                        horario.setOnClickListener(editar);
                        status.setOnClickListener(editar);
                        item.setOnLongClickListener(acaoExcluir);
                        titulo.setOnLongClickListener(acaoExcluir);
                        horario.setOnLongClickListener(acaoExcluir);
                        status.setOnLongClickListener(acaoExcluir);

                        lista.addView(item);
                    }

                    int total = tarefas.size();

                    int porcentagem = total == 0 ? 0 : Math.round(concluidas * 100f / total);
                    int restantes = total - concluidas;
                    barra.setProgress(porcentagem);
                    textoProgresso.setText(porcentagem + "%");
                    resumo.setText(concluidas + (concluidas == 1 ? " concluída" : " concluídas")
                            + "    ·    " + restantes + (restantes == 1 ? " restante" : " restantes"));
                    barra.setContentDescription(porcentagem + "% das tarefas concluídas");
                });

            } catch (Exception erro) {
                Log.e("YourDay", "Erro ao carregar tarefas", erro);

                atualizarTela(() -> {
                    if (consulta != consultaAtual) return;
                    mensagem.setText("Não foi possível carregar as tarefas.");
                    mensagem.setVisibility(View.VISIBLE);
                    avisar("Não foi possível atualizar a lista.");
                });
            }
        });
    }

    private void abrirFormulario(BancoLocal.Tarefa existente) {
        View formulario = getLayoutInflater().inflate(
                R.layout.dialog_nova_tarefa,
                null
        );

        EditText nomeTarefa = formulario.findViewById(R.id.editNomeTarefa);
        Button botaoData = formulario.findViewById(R.id.btnDataTarefa);
        Button botaoHora = formulario.findViewById(R.id.btnHoraTarefa);

        Spinner prioridade = formulario.findViewById(R.id.prioridade);
        prioridade.setSelection(existente == null ? 1 : existente.prioridade);
        Button adicionarOutra = formulario.findViewById(R.id.adicionarOutra);
        adicionarOutra.setVisibility(existente == null ? View.VISIBLE : View.GONE);

        Calendar prazo = Calendar.getInstance();
        if (existente == null) {
            prazo.add(Calendar.HOUR_OF_DAY, 1);
            Calendar dataBase = diaSelecionado();
            prazo.set(dataBase.get(Calendar.YEAR), dataBase.get(Calendar.MONTH),
                    dataBase.get(Calendar.DAY_OF_MONTH));
            prazo.set(Calendar.SECOND, 0);
            prazo.set(Calendar.MILLISECOND, 0);
        } else {
            prazo.setTimeInMillis(existente.prazo);
            nomeTarefa.setText(existente.nome);
            nomeTarefa.setSelection(nomeTarefa.length());
        }

        Runnable atualizarBotoes = () -> {
            String data = formatar(prazo.getTimeInMillis(), "dd/MM/yyyy");

            String hora = formatar(prazo.getTimeInMillis(), "HH:mm");

            botaoData.setText("Data: " + data);
            botaoHora.setText("Horário limite: " + hora);
        };

        atualizarBotoes.run();

        botaoData.setOnClickListener(view -> {
            DatePickerDialog calendario = new DatePickerDialog(
                    this,
                    (seletor, ano, mes, dia) -> {
                        prazo.set(Calendar.YEAR, ano);
                        prazo.set(Calendar.MONTH, mes);
                        prazo.set(Calendar.DAY_OF_MONTH, dia);
                        atualizarBotoes.run();
                    },
                    prazo.get(Calendar.YEAR),
                    prazo.get(Calendar.MONTH),
                    prazo.get(Calendar.DAY_OF_MONTH)
            );

            calendario.show();
        });

        botaoHora.setOnClickListener(view -> {
            TimePickerDialog relogio = new TimePickerDialog(
                    this,
                    (seletor, hora, minuto) -> {
                        prazo.set(Calendar.HOUR_OF_DAY, hora);
                        prazo.set(Calendar.MINUTE, minuto);
                        atualizarBotoes.run();
                    },
                    prazo.get(Calendar.HOUR_OF_DAY),
                    prazo.get(Calendar.MINUTE),
                    true
            );

            relogio.show();
        });

        AlertDialog janela = new AlertDialog.Builder(this)
                .setTitle(existente == null ? "Nova tarefa" : "Editar tarefa")
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        janela.setOnShowListener(dialog -> {
            Button salvar = janela.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancelar = janela.getButton(AlertDialog.BUTTON_NEGATIVE);

            java.util.function.Consumer<Boolean> habilitar = ativo -> {
                salvar.setEnabled(ativo);
                cancelar.setEnabled(ativo);
                nomeTarefa.setEnabled(ativo);
                botaoData.setEnabled(ativo);
                botaoHora.setEnabled(ativo);
                adicionarOutra.setEnabled(ativo);
                prioridade.setEnabled(ativo);
                janela.setCancelable(ativo);
            };
            View.OnClickListener gravar = view -> {
                String nome = nomeTarefa.getText().toString().trim();

                if (nome.isEmpty()) {
                    nomeTarefa.setError("Digite o nome da tarefa.");
                    nomeTarefa.requestFocus();
                    return;
                }

                long horarioEscolhido = prazo.getTimeInMillis();

                int nivel = prioridade.getSelectedItemPosition();
                boolean continuar = view == adicionarOutra;
                habilitar.accept(false);

                executor.execute(() -> {
                    try {
                        if (existente == null) {
                            banco.salvarTarefa(nome, horarioEscolhido, nivel);
                        } else {
                            banco.editarTarefa(existente.id, nome, horarioEscolhido, nivel);
                        }

                        atualizarTela(() -> {
                            if (continuar) {
                                nomeTarefa.setText("");
                                prioridade.setSelection(1);
                                habilitar.accept(true);
                                nomeTarefa.requestFocus();
                            } else {
                                janela.dismiss();
                            }
                            carregarTarefasDoDia();

                            String data = formatar(horarioEscolhido, "dd/MM/yyyy");

                            avisar("Tarefa salva para " + data + ".");
                        });

                    } catch (Exception erro) {
                        Log.e("YourDay", "Erro ao salvar tarefa", erro);

                        atualizarTela(() -> {
                            habilitar.accept(true);
                            avisar("Não foi possível salvar. Tente novamente.");
                        });
                    }
                });
            };
            salvar.setOnClickListener(gravar);
            adicionarOutra.setOnClickListener(gravar);
        });

        janela.show();
    }

    private void atualizarStatusTarefa(View item) {
        Object dados = item.getTag();

        if (!(dados instanceof BancoLocal.Tarefa)) {
            return;
        }

        BancoLocal.Tarefa tarefa = (BancoLocal.Tarefa) dados;

        TextView status = item.findViewById(R.id.textStatusTarefa);

        long agora = System.currentTimeMillis();
        long tempoRestante = tarefa.prazo - agora;
        long quinzeMinutos = 15L * 60 * 1000;

        int cor;
        String descricao;

        if (tarefa.concluida) {
            cor = R.color.agenda_verde;
            descricao = "Concluída";

        } else if (tempoRestante <= 0) {
            cor = R.color.agenda_atrasada;
            descricao = "Atrasada";

        } else if (tempoRestante <= quinzeMinutos) {
            cor = R.color.prioridade_alta;
            descricao = "Perto do prazo";

        } else {
            cor = R.color.texto_secundario;
            descricao = "Pendente";
        }

        status.setText(descricao);
        status.setTextColor(getColor(cor));
    }

    private void confirmarExclusao(BancoLocal.Tarefa tarefa) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir tarefa?")
                .setMessage(
                        "Deseja excluir \"" + tarefa.nome + "\"?\n\n" +
                                "Essa ação não pode ser desfeita."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    executor.execute(() -> {
                        try {
                            banco.excluirTarefa(tarefa.id);

                            atualizarTela(() -> {
                                carregarTarefasDoDia();
                                avisar("Tarefa excluída.");
                            });

                        } catch (Exception erro) {
                            Log.e("YourDay", "Erro ao excluir tarefa", erro);

                            atualizarTela(() ->
                                    avisar("Não foi possível excluir a tarefa.")
                            );
                        }
                    });
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        atualizadorStatus.removeCallbacks(atualizarRelogio);
        atualizadorStatus.post(atualizarRelogio);
    }

    @Override
    protected void onPause() {
        atualizadorStatus.removeCallbacks(atualizarRelogio);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Aguarda as operações pendentes antes de fechar o banco.
        executor.execute(() -> banco.close());
        executor.shutdown();

        super.onDestroy();
    }
}
