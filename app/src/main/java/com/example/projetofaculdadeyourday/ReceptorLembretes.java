package com.example.projetofaculdadeyourday;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReceptorLembretes extends BroadcastReceiver {
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Lembretes.ativos(context)) return;
        Context app = context.getApplicationContext();
        PendingResult resultado = goAsync();
        EXECUTOR.execute(() -> {
            try (BancoLocal banco = new BancoLocal(app)) {
                Lembretes.sincronizar(app, banco, 0);
            } finally {
                resultado.finish();
            }
        });
    }
}
