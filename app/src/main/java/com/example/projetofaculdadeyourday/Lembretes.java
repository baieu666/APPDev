package com.example.projetofaculdadeyourday;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

final class Lembretes {
    static final String CANAL = "prazos";
    static final String EXTRA_DIA = "dia_do_lembrete";
    private static final String DESDE = "ativados_desde";

    private static SharedPreferences preferencias(Context contexto) {
        return contexto.getSharedPreferences("lembretes", Context.MODE_PRIVATE);
    }

    static boolean ativos(Context contexto) {
        return preferencias(contexto).contains(DESDE);
    }

    static synchronized void definirAtivos(Context contexto, boolean ativar) {
        if (ativar) {
            if (!ativos(contexto)) {
                preferencias(contexto).edit().putLong(DESDE, System.currentTimeMillis()).apply();
            }
        } else {
            preferencias(contexto).edit().remove(DESDE).apply();
            contexto.getSystemService(NotificationManager.class).cancelAll();
            contexto.getSystemService(AlarmManager.class).cancel(alarme(contexto));
        }
    }

    static boolean permitidos(Context contexto) {
        NotificationManager gerente = contexto.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 33 && contexto.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel canal = new NotificationChannel(CANAL,
                    contexto.getString(R.string.lembretes), NotificationManager.IMPORTANCE_DEFAULT);
            gerente.createNotificationChannel(canal);
            if (gerente.getNotificationChannel(CANAL).getImportance() == NotificationManager.IMPORTANCE_NONE) {
                return false;
            }
        }
        return gerente.areNotificationsEnabled();
    }

    private static PendingIntent alarme(Context contexto) {
        Intent intent = new Intent(contexto, ReceptorLembretes.class);
        return PendingIntent.getBroadcast(contexto, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    // Todas as origens consultam o banco novamente. Há apenas um alarme para o próximo prazo.
    static synchronized void sincronizar(Context contexto, BancoLocal banco, long tarefaAlterada) {
        try {
            NotificationManager gerente = contexto.getSystemService(NotificationManager.class);
            if (tarefaAlterada > 0) {
                gerente.cancel(String.valueOf(tarefaAlterada), 1);
            }
            if (!ativos(contexto)) return;
            AlarmManager alarmes = contexto.getSystemService(AlarmManager.class);
            PendingIntent alarme = alarme(contexto);
            if (!permitidos(contexto)) {
                alarmes.cancel(alarme);
                return;
            }
            long desde = preferencias(contexto).getLong(DESDE, Long.MAX_VALUE);
            for (BancoLocal.Tarefa tarefa : banco.lembretesVencidos(desde, System.currentTimeMillis())) {
                Intent abrir = new Intent(contexto, MainActivity.class)
                        .setData(Uri.parse("yourday://tarefa/" + tarefa.id))
                        .putExtra(EXTRA_DIA, tarefa.prazo)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent destino = PendingIntent.getActivity(contexto, 0, abrir,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                NotificationCompat.Builder aviso = new NotificationCompat.Builder(contexto, CANAL)
                        .setSmallIcon(R.drawable.ic_lembrete)
                        .setContentTitle(tarefa.nome)
                        .setContentText(contexto.getString(R.string.prazo_lembrete))
                        .setWhen(tarefa.prazo)
                        .setContentIntent(destino)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true);
                gerente.notify(String.valueOf(tarefa.id), 1, aviso.build());
                banco.marcarLembreteEnviado(tarefa);
            }
            long proximo = banco.proximoPrazo(desde);
            if (proximo == -1L) {
                alarmes.cancel(alarme);
            } else {
                alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximo, alarme);
            }
        } catch (RuntimeException erro) {
            // Uma falha do serviço de alertas não desfaz uma tarefa já salva.
            Log.e("YourDay", "Falha ao sincronizar lembretes de prazo", erro);
        }
    }

    private Lembretes() {}
}
