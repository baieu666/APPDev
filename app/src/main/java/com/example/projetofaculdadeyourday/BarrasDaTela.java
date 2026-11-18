package com.example.projetofaculdadeyourday;

import android.os.Build;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.content.res.Configuration;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

final class BarrasDaTela {
    static void ajustarBarras(AppCompatActivity tela, int idRaiz) {
        View raiz = tela.findViewById(idRaiz);

        int modo = tela.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean temaClaro = modo != Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(tela.getWindow(), raiz).setAppearanceLightStatusBars(temaClaro);
        WindowCompat.getInsetsController(tela.getWindow(), raiz).setAppearanceLightNavigationBars(temaClaro);
        if (Build.VERSION.SDK_INT < 26) {
            tela.getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        }

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
}
