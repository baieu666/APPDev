package com.example.projetofaculdadeyourday;

import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class AgendaPerfilTest {
    @Test
    public void salvarPerfilMantemODiaEONomePermaneceAoReabrir() {
        assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("emuladorDescartavel")));
        try (ActivityScenario<MainActivity> tela = ActivityScenario.launch(MainActivity.class)) {
            aguardar(tela, atividade -> {
                Button comecar = atividade.findViewById(R.id.btnComecar);
                return comecar != null && comecar.isEnabled();
            });
            onView(withId(R.id.editNome)).perform(replaceText("Primeiro nome"), closeSoftKeyboard());
            onView(withId(R.id.btnComecar)).perform(click());
            aguardar(tela, atividade -> atividade.findViewById(R.id.proximoDia) != null);
            onView(withId(R.id.proximoDia)).perform(click());
            String[] dia = new String[1];
            tela.onActivity(atividade -> dia[0] = texto(atividade, R.id.escolherData));
            onView(withId(R.id.abrirPerfil)).perform(click());
            onView(withId(R.id.nomeDoPerfil)).perform(replaceText("Nome atualizado"), closeSoftKeyboard());
            onView(withId(R.id.salvarNomePerfil)).perform(click());
            aguardar(tela, atividade -> "Olá, Nome atualizado!".equals(texto(atividade, R.id.textSaudacao)));
            tela.onActivity(atividade -> assertEquals(dia[0], texto(atividade, R.id.escolherData)));
        }
        try (ActivityScenario<MainActivity> nova = ActivityScenario.launch(MainActivity.class)) {
            aguardar(nova, atividade -> "Olá, Nome atualizado!".equals(texto(atividade, R.id.textSaudacao)));
        }
    }

    private String texto(MainActivity tela, int id) {
        TextView campo = tela.findViewById(id);
        return campo == null ? null : campo.getText().toString();
    }

    private void aguardar(ActivityScenario<MainActivity> tela, Predicate<MainActivity> pronta) {
        long limite = SystemClock.elapsedRealtime() + 10000;
        AtomicBoolean ok = new AtomicBoolean();
        do {
            tela.onActivity(atividade -> ok.set(pronta.test(atividade)));
            if (ok.get()) return;
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < limite);
        fail("A tela não chegou ao estado esperado.");
    }
}
