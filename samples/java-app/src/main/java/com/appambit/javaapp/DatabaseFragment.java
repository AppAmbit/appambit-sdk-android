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

import com.appambit.sdk.AppAmbitDb;
import com.appambit.sdk.models.db.DbStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseFragment extends Fragment {

    private EditText editSql;
    private TextView txtStatus;
    private DbRowAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
                runSql(editSql.getText().toString().trim()));

        view.findViewById(R.id.btn_batch_demo).setOnClickListener(v -> runBatchDemo());

        view.findViewById(R.id.btn_preset_tables).setOnClickListener(v -> {
            String q = "SELECT name FROM sqlite_master WHERE type = 'table'";
            editSql.setText(q);
            runSql(q);
        });

        view.findViewById(R.id.btn_preset_users).setOnClickListener(v ->
                runFluent("users"));

        view.findViewById(R.id.btn_preset_products).setOnClickListener(v ->
                runFluent("products"));

        view.findViewById(R.id.btn_preset_select1).setOnClickListener(v -> {
            editSql.setText("SELECT 1 AS result");
            runSql("SELECT 1 AS result");
        });
    }

    private void runSql(String sql) {
        if (sql.isEmpty()) return;
        showStatus("Running...", false);
        AppAmbitDb.execute(sql)
                .then(result -> {
                    if (result.hasError()) {
                        showStatus("Error: " + result.getError(), true);
                        adapter.update(new ArrayList<>(), new ArrayList<>());
                    } else {
                        showStatus("rows_read=" + result.getRowsRead() + "  rows_written=" + result.getRowsWritten(), false);
                        adapter.update(result.getColumns(), result.toMaps());
                    }
                })
                .onError(error -> {
                    showStatus("Error: " + error.getMessage(), true);
                    adapter.update(new ArrayList<>(), new ArrayList<>());
                });
    }

    private void runFluent(String table) {
        showStatus("Running fluent query on \"" + table + "\"...", false);
        AppAmbitDb.from(table)
                .limit(10)
                .get()
                .then(maps -> {
                    if (maps.isEmpty()) {
                        showStatus("No rows found in \"" + table + "\"", false);
                        adapter.update(new ArrayList<>(), new ArrayList<>());
                    } else {
                        List<String> columns = new ArrayList<>(maps.get(0).keySet());
                        showStatus(maps.size() + " row(s) via fluent builder", false);
                        adapter.update(columns, maps);
                    }
                })
                .onError(error -> showStatus("Error: " + error.getMessage(), true));
    }

    private void runBatchDemo() {
        showStatus("Running batch...", false);
        AppAmbitDb.batchInTransaction(
                DbStatement.of("INSERT INTO demo_log (event) VALUES (?)", "batch_start"),
                DbStatement.of("INSERT INTO demo_log (event) VALUES (?)", "batch_end")
        ).then(results -> {
            int written = 0;
            for (var r : results) written += r.getRowsWritten();
            showStatus("Batch complete — " + written + " row(s) written across " + results.size() + " statements", false);
            adapter.update(new ArrayList<>(), new ArrayList<>());
        }).onError(error -> showStatus("Batch error: " + error.getMessage(), true));
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_db_row, parent, false);
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
                        dpToPx(holder.itemView.getContext(), 110),
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
