package com.example.projetofaculdadeyourday;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class FotoPerfil {
    // A fila também ordena operações de duas instâncias durante a rotação da tela.
    static final ExecutorService FILA = Executors.newSingleThreadExecutor();
    private static final int LADO = 256;
    private final Context contexto;
    private final AtomicFile arquivo;

    FotoPerfil(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        arquivo = new AtomicFile(new File(contexto.getFilesDir(), "foto-perfil.jpg"));
    }

    Bitmap carregar() throws IOException {
        try (InputStream entrada = arquivo.openRead()) {
            return BitmapFactory.decodeStream(entrada);
        } catch (FileNotFoundException semFoto) {
            return null;
        }
    }

    void remover() {
        arquivo.delete();
    }

    Bitmap salvar(Uri origem) throws IOException {
        Bitmap imagem = Build.VERSION.SDK_INT >= 28 ? decodificar(origem) : decodificarLegado(origem);
        Bitmap miniatura = ThumbnailUtils.extractThumbnail(imagem, LADO, LADO);
        FileOutputStream saida = arquivo.startWrite();
        try {
            if (!miniatura.compress(Bitmap.CompressFormat.JPEG, 90, saida)) {
                throw new IOException("Falha ao gravar a foto.");
            }
            arquivo.finishWrite(saida);
        } catch (IOException | RuntimeException erro) {
            arquivo.failWrite(saida);
            throw erro;
        }
        return miniatura;
    }

    @android.annotation.TargetApi(28)
    private Bitmap decodificar(Uri origem) throws IOException {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(contexto.getContentResolver(), origem),
                (decoder, info, source) -> {
                    int maior = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                    decoder.setTargetSampleSize(Math.max(1, maior / (LADO * 2)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
    }

    private Bitmap decodificarLegado(Uri origem) throws IOException {
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inJustDecodeBounds = true;
        try (InputStream entrada = abrir(origem)) {
            BitmapFactory.decodeStream(entrada, null, opcoes);
        }
        if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) throw new IOException("Imagem inválida.");
        opcoes.inSampleSize = 1;
        while (Math.max(opcoes.outWidth, opcoes.outHeight) / opcoes.inSampleSize > LADO * 4) {
            opcoes.inSampleSize *= 2;
        }
        opcoes.inJustDecodeBounds = false;
        Bitmap imagem;
        try (InputStream entrada = abrir(origem)) {
            imagem = BitmapFactory.decodeStream(entrada, null, opcoes);
        }
        if (imagem == null) throw new IOException("Imagem inválida.");

        int orientacao;
        try (InputStream entrada = abrir(origem)) {
            orientacao = new ExifInterface(entrada).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        } catch (IOException semMetadados) {
            return imagem;
        }
        Matrix matriz = new Matrix();
        switch (orientacao) {
            case 2: matriz.setScale(-1, 1); break;
            case 3: matriz.setRotate(180); break;
            case 4: matriz.setScale(1, -1); break;
            case 5: matriz.setRotate(90); matriz.postScale(-1, 1); break;
            case 6: matriz.setRotate(90); break;
            case 7: matriz.setRotate(-90); matriz.postScale(-1, 1); break;
            case 8: matriz.setRotate(-90); break;
            default: return imagem;
        }
        return Bitmap.createBitmap(imagem, 0, 0, imagem.getWidth(), imagem.getHeight(), matriz, true);
    }

    private InputStream abrir(Uri origem) throws IOException {
        InputStream entrada = contexto.getContentResolver().openInputStream(origem);
        if (entrada == null) throw new IOException("Foto indisponível.");
        return entrada;
    }
}
