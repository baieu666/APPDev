package com.example.projetofaculdadeyourday;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.Locale;

final class AvatarPerfil {
    private final AppCompatActivity tela;
    private final FotoPerfil fotos;
    private final ActivityResultLauncher<String> escolherFoto;

    AvatarPerfil(AppCompatActivity tela) {
        this.tela = tela;
        fotos = new FotoPerfil(tela);
        escolherFoto = tela.registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) atualizarFoto(uri, false);
        });
    }

    void recarregar() {
        atualizarFoto(null, false);
    }

    void vincular(String nome) {
        TextView inicial = tela.findViewById(R.id.inicialPerfil);
        inicial.setText(nome.isEmpty() ? "?" :
                nome.substring(0, nome.offsetByCodePoints(0, 1)).toUpperCase(Locale.forLanguageTag("pt-BR")));
        View avatar = tela.findViewById(R.id.editarFoto);
        avatar.setClipToOutline(true);
        avatar.setOnClickListener(v -> {
            if (tela.findViewById(R.id.fotoPerfil).getVisibility() != View.VISIBLE) {
                escolherFoto.launch("image/*");
                return;
            }
            new AlertDialog.Builder(tela)
                    .setTitle(R.string.foto_perfil)
                    .setItems(new String[]{tela.getString(R.string.trocar_foto), tela.getString(R.string.remover_foto)},
                            (dialog, opcao) -> {
                                if (opcao == 0) escolherFoto.launch("image/*");
                                else atualizarFoto(null, true);
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
        recarregar();
    }

    private void atualizarFoto(Uri origem, boolean remover) {
        FotoPerfil.FILA.execute(() -> {
            try {
                if (remover) fotos.remover();
                Bitmap bitmap = origem == null ? fotos.carregar() : fotos.salvar(origem);
                tela.runOnUiThread(() -> {
                    if (tela.isDestroyed() || tela.isFinishing()) return;
                    ImageView foto = tela.findViewById(R.id.fotoPerfil);
                    if (foto == null) return;
                    foto.setImageBitmap(bitmap);
                    foto.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
                    tela.findViewById(R.id.editarFoto).setContentDescription(tela.getString(
                            bitmap == null ? R.string.escolher_foto : R.string.alterar_foto));
                });
            } catch (IOException | RuntimeException erro) {
                Log.w("YourDay", "Falha ao acessar foto de perfil", erro);
                tela.runOnUiThread(() -> {
                    if (!tela.isDestroyed() && !tela.isFinishing()) {
                        Toast.makeText(tela, R.string.erro_foto, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
