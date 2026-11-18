package com.example.projetofaculdadeyourday;

import org.junit.Test;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class PrazoDaAgendaTest {
    private Calendar data(int ano, int mes, int dia, int hora, int minuto) {
        Calendar valor = new GregorianCalendar(TimeZone.getTimeZone("America/Sao_Paulo"));
        valor.clear();
        valor.set(ano, mes - 1, dia, hora, minuto);
        return valor;
    }

    @Test
    public void hojeSugereUmaHoraDepois() {
        Calendar agora = data(2026, 11, 18, 10, 15);
        assertEquals(data(2026, 11, 18, 11, 15), PrazoDaAgenda.sugerir(agora, agora));
    }

    @Test
    public void hojeAntesDaMeiaNoiteAvancaOMes() {
        Calendar agora = data(2026, 11, 30, 23, 30);
        assertEquals(data(2026, 12, 1, 0, 30), PrazoDaAgenda.sugerir(agora, agora));
    }

    @Test
    public void hojeNoReveillonAvancaOAno() {
        Calendar agora = data(2026, 12, 31, 23, 30);
        assertEquals(data(2027, 1, 1, 0, 30), PrazoDaAgenda.sugerir(agora, agora));
    }

    @Test
    public void outroDiaMantemADataEscolhidaMesmoTardeDaNoite() {
        Calendar agora = data(2026, 12, 31, 23, 30);
        Calendar escolhido = data(2027, 1, 10, 0, 0);
        assertEquals(data(2027, 1, 10, 0, 30), PrazoDaAgenda.sugerir(agora, escolhido));
    }
}
