package com.example.projetofaculdadeyourday;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BancoLocal extends SQLiteOpenHelper {

    public BancoLocal(Context context) {
        super(context.getApplicationContext(), "your_day.db", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE perfil (" +
                        "id INTEGER PRIMARY KEY, " +
                        "nome TEXT NOT NULL)"
        );

        criarTabelaTarefas(db);
    }

    private void criarTabelaTarefas(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE tarefas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nome TEXT NOT NULL, " +
                        "prazo INTEGER NOT NULL, " +
                        "concluida INTEGER NOT NULL DEFAULT 0, " +
                        "prioridade INTEGER NOT NULL DEFAULT 1)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            criarTabelaTarefas(db);
            return;
        }
        if (oldVersion < 3) {
            db.execSQL(
                    "ALTER TABLE tarefas " +
                            "ADD COLUMN concluida INTEGER NOT NULL DEFAULT 0"
            );
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE tarefas ADD COLUMN prioridade INTEGER NOT NULL DEFAULT 1");
        }
    }

    public void salvarNome(String nome) {
        ContentValues dados = new ContentValues();
        dados.put("id", 1);
        dados.put("nome", nome);

        long resultado = getWritableDatabase().insertWithOnConflict(
                "perfil", null, dados, SQLiteDatabase.CONFLICT_REPLACE
        );

        if (resultado == -1) {
            throw new IllegalStateException("Não foi possível salvar o nome.");
        }
    }

    public String buscarNome() {
        try (Cursor cursor = getReadableDatabase().query(
                "perfil",
                new String[]{"nome"},
                "id = ?",
                new String[]{"1"},
                null, null, null
        )) {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        }
    }

    public void salvarTarefa(String nome, long prazo, int prioridade) {
        ContentValues dados = new ContentValues();
        dados.put("nome", nome);
        dados.put("prazo", prazo);
        dados.put("prioridade", prioridade);
        dados.put("concluida", 0);

        getWritableDatabase().insertOrThrow("tarefas", null, dados);
    }

    public void editarTarefa(long id, String nome, long prazo, int prioridade) {
        ContentValues dados = new ContentValues();
        dados.put("nome", nome);
        dados.put("prazo", prazo);
        dados.put("prioridade", prioridade);
        if (getWritableDatabase().update("tarefas", dados, "id = ?",
                new String[]{String.valueOf(id)}) != 1) {
            throw new IllegalStateException("Tarefa não encontrada.");
        }
    }

    public void atualizarConclusao(long id, boolean concluida) {
        ContentValues dados = new ContentValues();
        dados.put("concluida", concluida ? 1 : 0);

        int alteradas = getWritableDatabase().update(
                "tarefas",
                dados,
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        if (alteradas != 1) {
            throw new IllegalStateException("Não foi possível atualizar a tarefa.");
        }
    }

    public List<Tarefa> buscarTarefas(long inicio, long fim) {
        List<Tarefa> tarefas = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().query(
                "tarefas",
                new String[]{"id", "nome", "prazo", "concluida", "prioridade"},
                "prazo >= ? AND prazo < ?",
                new String[]{String.valueOf(inicio), String.valueOf(fim)},
                null,
                null,
                "concluida ASC, prioridade DESC, prazo ASC, id ASC"
        )) {
            while (cursor.moveToNext()) {
                tarefas.add(new Tarefa(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getInt(3) == 1,
                        cursor.getInt(4)
                ));
            }
        }

        return tarefas;
    }

    public void excluirTarefa(long id) {
        int removidas = getWritableDatabase().delete(
                "tarefas",
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        if (removidas != 1) {
            throw new IllegalStateException(
                    "Não foi possível excluir a tarefa."
            );
        }
    }

    public static class Tarefa {
        public final long id;
        public final String nome;
        public final long prazo;
        public final boolean concluida;
        public final int prioridade;

        public Tarefa(long id, String nome, long prazo, boolean concluida, int prioridade) {
            this.prioridade = prioridade;
            this.id = id;
            this.nome = nome;
            this.prazo = prazo;
            this.concluida = concluida;
        }
    }
}
