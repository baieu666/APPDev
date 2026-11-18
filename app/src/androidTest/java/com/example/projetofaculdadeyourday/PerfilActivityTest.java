package com.example.projetofaculdadeyourday;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PerfilActivityTest {
    @Test
    public void perfilValidaNomeEMantemRascunhoAoRecriar() {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent abrir = new Intent(contexto, PerfilActivity.class)
                .putExtra(PerfilActivity.EXTRA_NOME, "Gabriel");
        try (ActivityScenario<PerfilActivity> tela = ActivityScenario.launch(abrir)) {
            tela.onActivity(perfil -> {
                EditText nome = perfil.findViewById(R.id.nomeDoPerfil);
                assertEquals("Gabriel", nome.getText().toString());
                nome.setText("   ");
                perfil.findViewById(R.id.salvarNomePerfil).performClick();
                assertEquals(perfil.getString(R.string.nome_obrigatorio), nome.getError().toString());
                nome.setText("Baieu");
            });
            tela.recreate();
            tela.onActivity(perfil -> {
                EditText nome = perfil.findViewById(R.id.nomeDoPerfil);
                assertEquals("Baieu", nome.getText().toString());
            });
        }
    }
}
