package com.example.projetofaculdadeyourday;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerfilActivity extends AppCompatActivity {
    static final String EXTRA_NOME = "nome_perfil";
    static final String EXTRA_TEMA = "tema_perfil";

    @Override
    protected void onCreate(Bundle estado) {
        getDelegate().setLocalNightMode(getIntent().getIntExtra(
                EXTRA_TEMA, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        super.onCreate(estado);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);
        BarrasDaTela.ajustarBarras(this);

        String nome = getIntent().getStringExtra(EXTRA_NOME);
        EditText campo = findViewById(R.id.nomeDoPerfil);
        campo.setText(nome);
        AvatarPerfil avatar = new AvatarPerfil(this);
        avatar.vincular(nome == null ? "" : nome);
        findViewById(R.id.acaoFotoPerfil).setOnClickListener(v -> findViewById(R.id.editarFoto).performClick());
        findViewById(R.id.voltarAgenda).setOnClickListener(v -> finish());

        Edicao edicao = new ViewModelProvider(this).get(Edicao.class);
        OnBackPressedCallback aguardarGravacao = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                // A gravação termina devolvendo o nome persistido à agenda.
            }
        };
        getOnBackPressedDispatcher().addCallback(this, aguardarGravacao);
        edicao.estado.observe(this, situacao -> {
            boolean gravando = situacao == Estado.GRAVANDO;
            aguardarGravacao.setEnabled(gravando);
            campo.setEnabled(!gravando);
            findViewById(R.id.salvarNomePerfil).setEnabled(!gravando);
            findViewById(R.id.voltarAgenda).setEnabled(!gravando);
            if (situacao == Estado.SALVO) {
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_NOME, edicao.nomeSalvo));
                finish();
            } else if (situacao == Estado.ERRO) {
                campo.setError(getString(R.string.erro_salvar_nome));
            }
        });

        findViewById(R.id.salvarNomePerfil).setOnClickListener(v -> {
            String novo = campo.getText().toString().trim();
            if (novo.isEmpty()) {
                campo.setError(getString(R.string.nome_obrigatorio));
                campo.requestFocus();
                return;
            }
            edicao.salvar(novo);
        });
        campo.setOnEditorActionListener((v, acao, evento) ->
                acao == EditorInfo.IME_ACTION_DONE && findViewById(R.id.salvarNomePerfil).performClick());
    }

    private enum Estado { PRONTO, GRAVANDO, SALVO, ERRO }

    // A gravação sobrevive à recriação da tela e nunca retém uma Activity.
    public static class Edicao extends AndroidViewModel {
        private final MutableLiveData<Estado> estado = new MutableLiveData<>(Estado.PRONTO);
        private final ExecutorService fila = Executors.newSingleThreadExecutor();
        private String nomeSalvo;

        public Edicao(Application app) {
            super(app);
        }

        void salvar(String nome) {
            if (estado.getValue() == Estado.GRAVANDO) return;
            nomeSalvo = nome;
            estado.setValue(Estado.GRAVANDO);
            fila.execute(() -> {
                try (BancoLocal banco = new BancoLocal(getApplication())) {
                    banco.salvarNome(nome);
                    estado.postValue(Estado.SALVO);
                } catch (RuntimeException erro) {
                    Log.e("YourDay", "Erro ao salvar o nome do perfil", erro);
                    estado.postValue(Estado.ERRO);
                }
            });
        }

        @Override
        protected void onCleared() {
            fila.shutdown();
        }
    }
}
