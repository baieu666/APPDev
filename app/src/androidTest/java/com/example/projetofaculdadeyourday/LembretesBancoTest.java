package com.example.projetofaculdadeyourday;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LembretesBancoTest {
    @Test
    public void lembretesAcompanhamAlteracoesSemRepetirPrazosEnviados() {
        long inicio = System.currentTimeMillis();
        long prazo = inicio + 3_600_000L;
        try (BancoLocal banco = new BancoLocal(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), null)) {
            banco.salvarTarefa("Antiga", inicio - 1, 1);
            banco.salvarTarefa("Revisar", prazo, 1);
            assertTrue(banco.lembretesVencidos(inicio, prazo - 1).isEmpty());
            BancoLocal.Tarefa tarefa = banco.lembretesVencidos(inicio, prazo).get(0);
            banco.atualizarConclusao(tarefa.id, true);
            assertEquals(-1L, banco.proximoPrazo(inicio));
            banco.atualizarConclusao(tarefa.id, false);
            assertEquals(prazo, banco.proximoPrazo(inicio));
            banco.marcarLembreteEnviado(tarefa);
            banco.editarTarefa(tarefa.id, "Revisar aula", prazo, 2);
            assertTrue(banco.lembretesVencidos(inicio, prazo).isEmpty());
            long novoPrazo = prazo + 60_000;
            banco.editarTarefa(tarefa.id, "Revisar aula", novoPrazo, 2);
            banco.marcarLembreteEnviado(tarefa); // resultado atrasado do prazo anterior
            assertEquals(novoPrazo, banco.proximoPrazo(inicio));
            banco.salvarTarefa("Cópia", novoPrazo, 2);
            assertEquals(2, banco.lembretesVencidos(inicio, novoPrazo).size());
            banco.excluirTarefa(tarefa.id);
            assertEquals("Cópia", banco.lembretesVencidos(inicio, novoPrazo).get(0).nome);
        }
    }
}
