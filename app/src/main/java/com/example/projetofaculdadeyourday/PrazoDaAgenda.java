package com.example.projetofaculdadeyourday;

import java.util.Calendar;

final class PrazoDaAgenda {
    private PrazoDaAgenda() {}

    static Calendar sugerir(Calendar agora, Calendar diaSelecionado) {
        Calendar prazo = (Calendar) agora.clone();
        prazo.set(Calendar.SECOND, 0);
        prazo.set(Calendar.MILLISECOND, 0);
        prazo.add(Calendar.HOUR_OF_DAY, 1);

        // Em Hoje, a sugestão pode cair amanhã. Ao navegar, respeite a data escolhida.
        if (diaSelecionado.get(Calendar.YEAR) != agora.get(Calendar.YEAR)
                || diaSelecionado.get(Calendar.DAY_OF_YEAR) != agora.get(Calendar.DAY_OF_YEAR)) {
            prazo.set(diaSelecionado.get(Calendar.YEAR), diaSelecionado.get(Calendar.MONTH),
                    diaSelecionado.get(Calendar.DAY_OF_MONTH));
        }
        return prazo;
    }
}
