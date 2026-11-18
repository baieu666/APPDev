package com.example.projetofaculdadeyourday;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.List;

public class BancoLocal extends SQLiteOpenHelper {

    public BancoLocal(Context context) {
        this(context, "your_day.db");
    }

    BancoLocal(Context context, String arquivo) {
        super(context.getApplicationContext(), arquivo, null, 5);
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
                        "prioridade INTEGER NOT NULL DEFAULT 1, " +
                        "lembrete_enviado INTEGER NOT NULL DEFAULT 0)"
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
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE tarefas ADD COLUMN lembrete_enviado INTEGER NOT NULL DEFAULT 0");
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
        dados.put("lembrete_enviado", prazo <= System.currentTimeMillis() ? 1 : 0);

        getWritableDatabase().insertOrThrow("tarefas", null, dados);
    }

    public void editarTarefa(long id, String nome, long prazo, int prioridade) {
        try (SQLiteStatement comando = getWritableDatabase().compileStatement(
                "UPDATE tarefas SET nome = ?, prioridade = ?, " +
                "lembrete_enviado = CASE WHEN prazo = ? THEN lembrete_enviado ELSE ? END, " +
                "prazo = ? WHERE id = ?")) {
            comando.bindString(1, nome);
            comando.bindLong(2, prioridade);
            comando.bindLong(3, prazo);
            comando.bindLong(4, prazo <= System.currentTimeMillis() ? 1 : 0);
            comando.bindLong(5, prazo);
            comando.bindLong(6, id);
            if (comando.executeUpdateDelete() != 1) {
                throw new IllegalStateException("Tarefa não encontrada.");
            }
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
                tarefas.add(lerTarefa(cursor));
            }
        }

        return tarefas;
    }

    private Tarefa lerTarefa(Cursor cursor) {
        return new Tarefa(cursor.getLong(0), cursor.getString(1), cursor.getLong(2),
                cursor.getInt(3) == 1, cursor.getInt(4));
    }

    // A data de ativação evita alertar tarefas que já venceram antes da adesão.
    private static final String LEMBRETE_PENDENTE =
            "concluida = 0 AND lembrete_enviado = 0 AND prazo >= ?";

    public long proximoPrazo(long desde) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT MIN(prazo) FROM tarefas WHERE " + LEMBRETE_PENDENTE,
                new String[]{String.valueOf(desde)})) {
            cursor.moveToFirst();
            return cursor.isNull(0) ? -1L : cursor.getLong(0);
        }
    }

    public List<Tarefa> lembretesVencidos(long desde, long agora) {
        List<Tarefa> tarefas = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, nome, prazo, concluida, prioridade FROM tarefas WHERE " +
                LEMBRETE_PENDENTE + " AND prazo <= ? ORDER BY prazo, id",
                new String[]{String.valueOf(desde), String.valueOf(agora)})) {
            while (cursor.moveToNext()) {
                tarefas.add(lerTarefa(cursor));
            }
        }
        return tarefas;
    }

    public void marcarLembreteEnviado(Tarefa tarefa) {
        ContentValues dados = new ContentValues();
        dados.put("lembrete_enviado", 1);
        getWritableDatabase().update("tarefas", dados, "id = ? AND prazo = ?",
                new String[]{String.valueOf(tarefa.id), String.valueOf(tarefa.prazo)});
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
