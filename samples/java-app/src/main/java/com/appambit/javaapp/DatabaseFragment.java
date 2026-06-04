package com.appambit.javaapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appambit.javaapp.models.UserModel;
import com.appambit.sdk.AppAmbitDb;
import com.appambit.sdk.models.db.DbStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseFragment extends Fragment {

    private EditText editSql;
    private TextView txtStatus;
    private DbRowAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_database, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editSql = view.findViewById(R.id.edit_sql);
        txtStatus = view.findViewById(R.id.txt_status);

        RecyclerView recycler = view.findViewById(R.id.recycler_results);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DbRowAdapter(new ArrayList<>(), new ArrayList<>());
        recycler.setAdapter(adapter);

        view.findViewById(R.id.btn_execute).setOnClickListener(v ->
                demoExecute(editSql.getText().toString().trim()));

        view.findViewById(R.id.btn_execute_params).setOnClickListener(v ->
                demoExecuteParams());

        view.findViewById(R.id.btn_batch).setOnClickListener(v -> demoBatch());

        view.findViewById(R.id.btn_batch_tx).setOnClickListener(v -> demoBatchInTransaction());

        view.findViewById(R.id.btn_fluent_select).setOnClickListener(v -> demoFluentSelect());

        view.findViewById(R.id.btn_where_eq).setOnClickListener(v -> demoWhereEquality());

        view.findViewById(R.id.btn_where_in).setOnClickListener(v -> demoWhereIn());

        view.findViewById(R.id.btn_offset).setOnClickListener(v -> demoOffset());

        view.findViewById(R.id.btn_first).setOnClickListener(v -> demoFirst());

        view.findViewById(R.id.btn_count).setOnClickListener(v -> demoCount());

        view.findViewById(R.id.btn_insert).setOnClickListener(v -> demoInsert());

        view.findViewById(R.id.btn_update).setOnClickListener(v -> demoUpdate());

        view.findViewById(R.id.btn_delete).setOnClickListener(v -> demoDelete());

        view.findViewById(R.id.btn_typed_model).setOnClickListener(v -> demoTypedModel());

        view.findViewById(R.id.btn_preset_tables).setOnClickListener(v -> {
            String q = "SELECT name FROM sqlite_master WHERE type = 'table'";
            editSql.setText(q);
            demoExecute(q);
        });
        view.findViewById(R.id.btn_preset_select1).setOnClickListener(v -> {
            String q = "SELECT * FROM tasks WHERE priority = 'high'";
            editSql.setText(q);
            demoExecute(q);
        });
    }

    private void demoExecute(String sql) {
        if (sql.isEmpty()) {
            sql = "SELECT * FROM tasks LIMIT 10";
            editSql.setText(sql);
        }
        showStatus("Running...", false);
        var future = AppAmbitDb.execute(sql);
        future.then(result -> {
            if (result.hasError()) {
                showStatus("Error: " + result.getError(), true);
                adapter.update(new ArrayList<>(), new ArrayList<>());
            } else {
                showStatus("execute(sql) — rows_read=" + result.getRowsRead()
                        + "  rows_written=" + result.getRowsWritten(), false);
                adapter.update(result.getColumns(), result.toMaps());
            }
        });
        future.onError(e -> {
            showStatus("Error: " + e.getMessage(), true);
            adapter.update(new ArrayList<>(), new ArrayList<>());
        });
    }

    private void demoExecuteParams() {
        showStatus("Running parameterized query...", false);
        var future = AppAmbitDb.execute("SELECT * FROM tasks WHERE is_completed = ? LIMIT ?", 0, 10);
        future.then(result -> {
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("tareas pendientes — rows_read=" + result.getRowsRead(), false);
                adapter.update(result.getColumns(), result.toMaps());
            }
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoBatch() {
        showStatus("Running batch (no transaction)...", false);
        var future = AppAmbitDb.batch(
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Comprar café", 0, "low", "2026-06-10"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Revisar PR", 0, "high", "2026-06-05"),
                DbStatement.of("SELECT COUNT(*) AS total FROM tasks")
        );
        future.then(results -> {
            int written = 0;
            for (var r : results) written += r.getRowsWritten();
            List<String> cols = Arrays.asList("statement", "rows_written");
            List<Map<String, Object>> maps = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("statement", i + 1);
                m.put("rows_written", results.get(i).getRowsWritten());
                maps.add(m);
            }
            showStatus("batch() — " + written + " row(s) written across " + results.size()
                    + " statements (no transaction)", false);
            adapter.update(cols, maps);
        });
        future.onError(e -> showStatus("Batch error: " + e.getMessage(), true));
    }

    private void demoBatchInTransaction() {
        showStatus("Running batch in transaction...", false);
        var future = AppAmbitDb.batchInTransaction(
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Reunión de equipo", 0, "high", "2026-06-06"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Preparar agenda", 0, "medium", "2026-06-06")
        );
        future.then(results -> {
            int written = 0;
            for (var r : results) written += r.getRowsWritten();
            showStatus("batchInTransaction() — " + written + " row(s) written, rolled back on failure", false);
            adapter.update(new ArrayList<>(), new ArrayList<>());
        });
        future.onError(e -> showStatus("Transaction error: " + e.getMessage(), true));
    }

    private void demoFluentSelect() {
        showStatus("Fluent select...", false);
        var future = AppAmbitDb.from("tasks")
                .select("id", "title", "priority", "due_date")
                .where("is_completed", "=", 0)
                .orderByDesc("due_date")
                .limit(5)
                .get();
        future.then(maps -> {
            if (maps.isEmpty()) showStatus("No hay tareas pendientes", false);
            else {
                showStatus("tareas pendientes por vencer — " + maps.size() + " row(s)", false);
                adapter.update(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoWhereEquality() {
        showStatus("where equality...", false);
        var future = AppAmbitDb.from("tasks")
                .where("is_completed", 0)
                .get();
        future.then(maps -> {
            if (maps.isEmpty()) showStatus("No hay tareas pendientes", false);
            else {
                showStatus("where(is_completed, 0) — " + maps.size() + " tarea(s) pendiente(s)", false);
                adapter.update(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoWhereIn() {
        showStatus("whereIn...", false);
        var future = AppAmbitDb.from("tasks")
                .whereIn("priority", Arrays.asList("high", "medium"))
                .orderBy("due_date")
                .get();
        future.then(maps -> {
            if (maps.isEmpty()) showStatus("No hay tareas high/medium", false);
            else {
                showStatus("whereIn(priority,[high,medium]) — " + maps.size() + " row(s)", false);
                adapter.update(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoOffset() {
        showStatus("limit+offset...", false);
        var future = AppAmbitDb.from("tasks")
                .orderBy("due_date")
                .limit(5)
                .offset(0)
                .get();
        future.then(maps -> {
            if (maps.isEmpty()) showStatus("No hay tareas", false);
            else {
                showStatus("limit(5).offset(0) — página 1, " + maps.size() + " row(s)", false);
                adapter.update(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoFirst() {
        showStatus("first()...", false);
        var future = AppAmbitDb.from("tasks")
                .where("is_completed", "=", 0)
                .orderBy("due_date")
                .first();
        future.then(item -> {
            if (item == null) showStatus("first() — no hay tareas pendientes", false);
            else {
                showStatus("first() — próxima tarea a vencer", false);
                adapter.update(new ArrayList<>(item.keySet()), List.of(item));
            }
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoCount() {
        showStatus("count()...", false);
        var future = AppAmbitDb.from("tasks").where("is_completed", 0).count();
        future.then(count -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tareas_pendientes", count);
            showStatus("count() — " + count + " tarea(s) pendiente(s)", false);
            adapter.update(List.of("tareas_pendientes"), List.of(row));
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoInsert() {
        showStatus("insert()...", false);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", "Nueva tarea");
        data.put("is_completed", 0);
        data.put("priority", "medium");
        data.put("due_date", "2026-06-10");
        var future = AppAmbitDb.from("tasks").insert(data);
        future.then(result -> {
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else showStatus("insert() — tarea creada, rows_written=" + result.getRowsWritten(), false);
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoUpdate() {
        showStatus("update()...", false);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("is_completed", 1);
        var future = AppAmbitDb.from("tasks")
                .where("title", "Nueva tarea")
                .update(data);
        future.then(result -> {
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else showStatus("update() — tarea completada, rows_written=" + result.getRowsWritten(), false);
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoDelete() {
        showStatus("delete()...", false);
        var future = AppAmbitDb.from("tasks")
                .where("is_completed", 1)
                .delete();
        future.then(result -> {
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else showStatus("delete() — completadas eliminadas, rows_written=" + result.getRowsWritten(), false);
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void demoTypedModel() {
        showStatus("Typed model query...", false);
        var future = AppAmbitDb.from("tasks", UserModel.class)
                .select("id", "title", "is_completed", "priority", "due_date")
                .limit(5)
                .get();
        future.then(tasks -> {
            List<String> cols = Arrays.asList("id", "title", "isCompleted", "priority", "dueDate");
            List<Map<String, Object>> maps = new ArrayList<>();
            for (UserModel t : tasks) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id);
                m.put("title", t.title);
                m.put("isCompleted", t.isCompleted);
                m.put("priority", t.priority);
                m.put("dueDate", t.dueDate);
                maps.add(m);
            }
            showStatus("from(tasks, UserModel.class) — " + tasks.size() + " typed row(s)", false);
            adapter.update(cols, maps);
        });
        future.onError(e -> showStatus("Error: " + e.getMessage(), true));
    }

    private void showStatus(String message, boolean isError) {
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText(message);
        txtStatus.setBackgroundColor(isError
                ? Color.parseColor("#FFEBEE")
                : Color.parseColor("#E8F5E9"));
        txtStatus.setTextColor(isError
                ? Color.parseColor("#C62828")
                : Color.parseColor("#1B5E20"));
    }

    private static class DbRowAdapter extends RecyclerView.Adapter<DbRowAdapter.RowViewHolder> {
        private List<String> columns;
        private List<Map<String, Object>> rows;

        DbRowAdapter(List<String> columns, List<Map<String, Object>> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        void update(List<String> columns, List<Map<String, Object>> rows) {
            this.columns = columns;
            this.rows = rows;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_db_row, parent, false);
            return new RowViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
            Map<String, Object> row = rows.get(position);
            holder.rowContainer.removeAllViews();
            for (String col : columns) {
                LinearLayout cell = new LinearLayout(holder.itemView.getContext());
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setPadding(8, 4, 8, 4);

                TextView header = new TextView(holder.itemView.getContext());
                header.setText(col);
                header.setTextSize(10f);
                header.setTextColor(Color.parseColor("#757575"));

                TextView value = new TextView(holder.itemView.getContext());
                Object val = row.get(col);
                value.setText(val != null ? val.toString() : "null");
                value.setTextSize(13f);
                value.setTextColor(val != null ? Color.BLACK : Color.parseColor("#BDBDBD"));
                value.setTypeface(android.graphics.Typeface.MONOSPACE);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        dpToPx(holder.itemView.getContext(), 120),
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cell.setLayoutParams(params);
                cell.addView(header);
                cell.addView(value);
                holder.rowContainer.addView(cell);
            }
        }

        @Override
        public int getItemCount() { return rows.size(); }

        private int dpToPx(android.content.Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }

        static class RowViewHolder extends RecyclerView.ViewHolder {
            LinearLayout rowContainer;

            RowViewHolder(@NonNull View itemView) {
                super(itemView);
                rowContainer = itemView.findViewById(R.id.row_container);
            }
        }
    }
}
