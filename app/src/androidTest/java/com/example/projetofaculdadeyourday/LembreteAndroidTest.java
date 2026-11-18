package com.example.projetofaculdadeyourday;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class LembreteAndroidTest {
    @Test
    public void receptorPublicaSemTelaAbertaEDesligarRemoveAviso() throws Exception {
        // Este cenário usa os serviços reais e a instalação limpa do emulador de CI.
        assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("emuladorDescartavel")));
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NotificationManager gerente = app.getSystemService(NotificationManager.class);
        try (BancoLocal banco = new BancoLocal(app)) {
            assertTrue(banco.buscarTarefas(0, Long.MAX_VALUE).isEmpty());
            assertTrue(!Lembretes.ativos(app) && gerente.getActiveNotifications().length == 0);
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(app.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
            Lembretes.definirAtivos(app, true);
            long prazo = System.currentTimeMillis() + 2000;
            banco.salvarTarefa("Lembrete de teste", prazo, 1);
            BancoLocal.Tarefa tarefa = banco.buscarTarefas(prazo, prazo + 1).get(0);
            try {
                Lembretes.sincronizar(app, banco, 0);
                PendingIntent disparo = PendingIntent.getBroadcast(app, 0,
                        new Intent(app, ReceptorLembretes.class),
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                assertNotNull(disparo);
                // Dispara a mesma operação do AlarmManager ao vencer o prazo. Assim o teste
                // não depende do intervalo de agrupamento de alarmes do dispositivo.
                SystemClock.sleep(Math.max(0, prazo - System.currentTimeMillis() + 1));
                disparo.send();
                StatusBarNotification aviso = aguardarAviso(gerente, String.valueOf(tarefa.id));
                assertNotNull(aviso);
                assertEquals(tarefa.nome, aviso.getNotification().extras.getString(Notification.EXTRA_TITLE));
                assertNotNull(aviso.getNotification().contentIntent);
                Lembretes.definirAtivos(app, false);
                long limite = SystemClock.elapsedRealtime() + 5000;
                while (gerente.getActiveNotifications().length > 0 && SystemClock.elapsedRealtime() < limite) {
                    SystemClock.sleep(50);
                }
                assertEquals(0, gerente.getActiveNotifications().length);
            } finally {
                Lembretes.definirAtivos(app, false);
                banco.excluirTarefa(tarefa.id);
            }
        }
    }

    private StatusBarNotification aguardarAviso(NotificationManager gerente, String tag) {
        long limite = SystemClock.elapsedRealtime() + 10000;
        do {
            for (StatusBarNotification aviso : gerente.getActiveNotifications()) {
                if (tag.equals(aviso.getTag())) return aviso;
            }
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < limite);
        return null;
    }
}
