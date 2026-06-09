package com.appambit.javaapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.appambit.javaapp.models.TaskModel;
import com.appambit.sdk.AppAmbitDb;
import com.appambit.sdk.models.db.DbStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseFragment extends Fragment {

    private static class DemoItem {
        final String label;
        final Runnable action;
        DemoItem(String label, Runnable action) { this.label = label; this.action = action; }
        @Override public String toString() { return label; }
    }

    private EditText editSql;
    private TextView txtStatus;
    private ProgressBar progressLoading;
    private LinearLayout headerRow;
    private LinearLayout dataRows;
    private LinearLayout tableCard;
    private TextView txtRowCount;
    private List<DemoItem> demos;
    private int selectedIndex = 0;

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
        progressLoading = view.findViewById(R.id.progress_loading);
        headerRow = view.findViewById(R.id.header_row);
        dataRows = view.findViewById(R.id.data_rows);
        tableCard = view.findViewById(R.id.table_card);
        txtRowCount = view.findViewById(R.id.txt_row_count);

        buildDemos();

        Spinner spinner = view.findViewById(R.id.spinner_function);
        ArrayAdapter<DemoItem> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, demos);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                selectedIndex = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        view.findViewById(R.id.btn_run).setOnClickListener(v -> {
            if (selectedIndex >= 0 && selectedIndex < demos.size()) {
                showStatus("Running: " + demos.get(selectedIndex).label, false);
                setLoading(true);
                resetTable();
                demos.get(selectedIndex).action.run();
            }
        });
    }

    private void buildDemos() {
        demos = Arrays.asList(
            new DemoItem("Raw SQL → execute(sql)",                         this::demoExecute),
            new DemoItem("Raw SQL → execute(sql, params)",                 this::demoExecuteParams),
            new DemoItem("Schema → CREATE TABLE tasks",                    this::demoCreateTable),
            new DemoItem("Schema → DROP TABLE tasks",                      this::demoDropTable),
            new DemoItem("Batch → batch()",                                this::demoBatch),
            new DemoItem("Batch → batchInTransaction()",                   this::demoBatchInTransaction),
            new DemoItem("Fluent SELECT → select+where+orderByDesc+limit", this::demoFluentSelect),
            new DemoItem("Fluent SELECT → where(col, val)",                this::demoWhereEquality),
            new DemoItem("Fluent SELECT → whereIn()",                      this::demoWhereIn),
            new DemoItem("Fluent SELECT → limit+offset",                   this::demoOffset),
            new DemoItem("Fluent SELECT → first()",                        this::demoFirst),
            new DemoItem("Fluent SELECT → count()",                        this::demoCount),
            new DemoItem("Fluent WRITE → insert()",                        this::demoInsert),
            new DemoItem("Fluent WRITE → insert() high priority",          this::demoInsertHigh),
            new DemoItem("Fluent WRITE → insert() raw SQL",                this::demoInsertRawSQL),
            new DemoItem("Fluent WRITE → insert many (batch)",             this::demoInsertMany),
            new DemoItem("Fluent WRITE → update()",                        this::demoUpdate),
            new DemoItem("Fluent WRITE → delete()",                        this::demoDelete),
            new DemoItem("Typed Model → from(tasks, TaskModel.class)",     this::demoTypedModel),
            new DemoItem("Preset → List tables",                           this::demoPresetTables),
            new DemoItem("Preset → SELECT * WHERE priority='high'",        this::demoPresetHighPriority)
        );
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private void setupTable(List<String> columns, List<Map<String, Object>> rows) {
        headerRow.removeAllViews();
        dataRows.removeAllViews();

        if (columns.isEmpty()) {
            tableCard.setVisibility(View.GONE);
            return;
        }

        for (String col : columns) {
            headerRow.addView(makeHeaderCell(col));
        }

        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        columns.size() * dp(140), dp(1)));
                divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
                dataRows.addView(divider);
            }
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundColor(i % 2 == 0 ? Color.parseColor("#FAFAFA") : Color.WHITE);
            for (String col : columns) {
                row.addView(makeDataCell(rows.get(i).get(col)));
            }
            dataRows.addView(row);
        }

        int n = rows.size();
        txtRowCount.setText(n == 0 ? "(no rows)" : n + (n == 1 ? " row" : " rows"));
        tableCard.setVisibility(View.VISIBLE);
    }

    private TextView makeHeaderCell(String col) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(140), LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setText(col);
        tv.setTextSize(12f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#1A237E"));
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        return tv;
    }

    private TextView makeDataCell(Object value) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(140), LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setText(value != null ? value.toString() : "null");
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(value != null ? Color.parseColor("#212121") : Color.parseColor("#9E9E9E"));
        tv.setMaxLines(2);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        return tv;
    }

    private void resetTable() {
        tableCard.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private int dp(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // ── Demo methods ──────────────────────────────────────────────────────────

    private void demoExecute() {
        String sql = editSql.getText().toString().trim();
        if (sql.isEmpty()) { sql = "SELECT * FROM tasks LIMIT 10"; editSql.setText(sql); }
        var future = AppAmbitDb.execute(sql);
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("execute(sql) — rows_read=" + result.getRowsRead()
                        + "  rows_written=" + result.getRowsWritten(), false);
                setupTable(result.getColumns(), result.toMaps());
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoExecuteParams() {
        var future = AppAmbitDb.execute("SELECT * FROM tasks WHERE is_completed = ? LIMIT ?", 0, 10);
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("execute(sql, 0, 10) — pending tasks, rows_read=" + result.getRowsRead(), false);
                setupTable(result.getColumns(), result.toMaps());
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoCreateTable() {
        var future = AppAmbitDb.execute(
                "CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, is_completed INTEGER DEFAULT 0, priority TEXT, due_date TEXT)");
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else showStatus("CREATE TABLE OK — rows_read=" + result.getRowsRead()
                    + "  rows_written=" + result.getRowsWritten(), false);
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoDropTable() {
        var future = AppAmbitDb.execute("DROP TABLE IF EXISTS tasks");
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else showStatus("DROP TABLE OK — rows_read=" + result.getRowsRead()
                    + "  rows_written=" + result.getRowsWritten(), false);
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoBatch() {
        var future = AppAmbitDb.batch(
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Buy coffee", 0, "low", "2026-06-10"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Review PR", 0, "high", "2026-06-05"),
                DbStatement.of("SELECT COUNT(*) AS total FROM tasks")
        );
        future.then(results -> {
            setLoading(false);
            int written = 0; for (var r : results) written += r.getRowsWritten();
            List<String> cols = Arrays.asList("statement", "rows_written", "rows_read");
            List<Map<String, Object>> maps = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("statement", i + 1);
                m.put("rows_written", results.get(i).getRowsWritten());
                m.put("rows_read", results.get(i).getRowsRead());
                maps.add(m);
            }
            showStatus("batch() — " + written + " row(s) written across " + results.size()
                    + " statements (no transaction)", false);
            setupTable(cols, maps);
        });
        future.onError(e -> { setLoading(false); showStatus("Batch error: " + e.getMessage(), true); });
    }

    private void demoBatchInTransaction() {
        var future = AppAmbitDb.batchInTransaction(
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Team meeting", 0, "high", "2026-06-06"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Prepare agenda", 0, "medium", "2026-06-06")
        );
        future.then(results -> {
            setLoading(false);
            int written = 0; for (var r : results) written += r.getRowsWritten();
            List<String> cols = Arrays.asList("statement", "rows_written");
            List<Map<String, Object>> maps = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("statement", i + 1);
                m.put("rows_written", results.get(i).getRowsWritten());
                maps.add(m);
            }
            showStatus("batchInTransaction() — " + written + " row(s) written, rolled back on failure", false);
            setupTable(cols, maps);
        });
        future.onError(e -> { setLoading(false); showStatus("Transaction error: " + e.getMessage(), true); });
    }

    private void demoFluentSelect() {
        var future = AppAmbitDb.from("tasks")
                .select("id", "title", "priority", "due_date")
                .where("is_completed", "=", 0)
                .orderByDesc("due_date")
                .limit(5).get();
        future.then(maps -> {
            setLoading(false);
            if (maps.isEmpty()) showStatus("No pending tasks", false);
            else {
                showStatus("from().select().where().orderByDesc().limit(5) — " + maps.size() + " row(s)", false);
                setupTable(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoWhereEquality() {
        var future = AppAmbitDb.from("tasks").where("is_completed", 0).get();
        future.then(maps -> {
            setLoading(false);
            if (maps.isEmpty()) showStatus("No pending tasks", false);
            else {
                showStatus("where(is_completed, 0) — " + maps.size() + " pending task(s)", false);
                setupTable(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoWhereIn() {
        var future = AppAmbitDb.from("tasks")
                .whereIn("priority", Arrays.asList("high", "medium"))
                .orderBy("due_date").get();
        future.then(maps -> {
            setLoading(false);
            if (maps.isEmpty()) showStatus("No high/medium tasks", false);
            else {
                showStatus("whereIn(priority, [high, medium]) — " + maps.size() + " row(s)", false);
                setupTable(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoOffset() {
        var future = AppAmbitDb.from("tasks").orderBy("due_date").limit(5).offset(0).get();
        future.then(maps -> {
            setLoading(false);
            if (maps.isEmpty()) showStatus("No tasks", false);
            else {
                showStatus("limit(5).offset(0) — page 1, " + maps.size() + " row(s)", false);
                setupTable(new ArrayList<>(maps.get(0).keySet()), maps);
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoFirst() {
        var future = AppAmbitDb.from("tasks").where("is_completed", "=", 0).orderBy("due_date").first();
        future.then(item -> {
            setLoading(false);
            if (item == null) showStatus("first() — no pending tasks", false);
            else {
                showStatus("first() — next task due", false);
                setupTable(new ArrayList<>(item.keySet()), List.of(item));
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoCount() {
        var future = AppAmbitDb.from("tasks").where("is_completed", 0).count();
        future.then(count -> {
            setLoading(false);
            showStatus("count() — " + count + " pending task(s)", false);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pending_tasks", count);
            setupTable(List.of("pending_tasks"), List.of(row));
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoInsert() {
        var future = AppAmbitDb.from("tasks").insert(new LinkedHashMap<>() {{
            put("title", "New task"); put("is_completed", 0);
            put("priority", "medium"); put("due_date", "2026-06-10");
        }});
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("insert() — task created, rows_written=" + result.getRowsWritten(), false);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rows_written", result.getRowsWritten());
                setupTable(List.of("rows_written"), List.of(row));
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoInsertHigh() {
        var future = AppAmbitDb.from("tasks").insert(new LinkedHashMap<>() {{
            put("title", "Fix critical bug"); put("is_completed", 0);
            put("priority", "high"); put("due_date", "2026-06-05");
        }});
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("insert() high priority — task created, rows_written=" + result.getRowsWritten(), false);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rows_written", result.getRowsWritten());
                setupTable(List.of("rows_written"), List.of(row));
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoInsertRawSQL() {
        var future = AppAmbitDb.execute(
                "INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)",
                "Raw SQL insert", 0, "medium", "2026-06-12");
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("execute() INSERT OK — rows_written=" + result.getRowsWritten(), false);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rows_written", result.getRowsWritten());
                setupTable(List.of("rows_written"), List.of(row));
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoInsertMany() {
        var future = AppAmbitDb.batchInTransaction(
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Write unit tests", 0, "high", "2026-06-07"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Update documentation", 0, "low", "2026-06-15"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Code review", 0, "medium", "2026-06-08"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Deploy to staging", 0, "high", "2026-06-09"),
                DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Monitor metrics", 0, "low", "2026-06-20")
        );
        future.then(results -> {
            setLoading(false);
            int written = 0; for (var r : results) written += r.getRowsWritten();
            showStatus("insert many — " + written + " rows inserted via batch", false);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rows_inserted", written);
            setupTable(List.of("rows_inserted"), List.of(row));
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoUpdate() {
        var future = AppAmbitDb.from("tasks")
                .where("title", "New task")
                .update(Map.of("is_completed", 1));
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("update() — task completed, rows_written=" + result.getRowsWritten(), false);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rows_written", result.getRowsWritten());
                setupTable(List.of("rows_written"), List.of(row));
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoDelete() {
        var future = AppAmbitDb.from("tasks").where("is_completed", 1).delete();
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("delete() — completed tasks deleted, rows_written=" + result.getRowsWritten(), false);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rows_written", result.getRowsWritten());
                setupTable(List.of("rows_written"), List.of(row));
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoTypedModel() {
        var future = AppAmbitDb.from("tasks", TaskModel.class)
                .select("id", "title", "is_completed", "priority", "due_date")
                .limit(5).get();
        future.then(tasks -> {
            setLoading(false);
            List<String> cols = Arrays.asList("id", "title", "isCompleted", "priority", "dueDate");
            List<Map<String, Object>> maps = new ArrayList<>();
            for (TaskModel t : tasks) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id); m.put("title", t.title);
                m.put("isCompleted", t.isCompleted); m.put("priority", t.priority);
                m.put("dueDate", t.dueDate);
                maps.add(m);
            }
            showStatus("from(tasks, TaskModel.class) — " + tasks.size() + " typed row(s)", false);
            setupTable(cols, maps);
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoPresetTables() {
        String q = "SELECT name FROM sqlite_master WHERE type = 'table'";
        editSql.setText(q);
        var future = AppAmbitDb.execute(q);
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("sqlite_master tables — " + result.getRowsRead() + " row(s)", false);
                setupTable(result.getColumns(), result.toMaps());
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
    }

    private void demoPresetHighPriority() {
        String q = "SELECT * FROM tasks WHERE priority = 'high'";
        editSql.setText(q);
        var future = AppAmbitDb.execute(q);
        future.then(result -> {
            setLoading(false);
            if (result.hasError()) showStatus("Error: " + result.getError(), true);
            else {
                showStatus("tasks WHERE priority='high' — " + result.getRowsRead() + " row(s)", false);
                setupTable(result.getColumns(), result.toMaps());
            }
        });
        future.onError(e -> { setLoading(false); showStatus("Error: " + e.getMessage(), true); });
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
}
