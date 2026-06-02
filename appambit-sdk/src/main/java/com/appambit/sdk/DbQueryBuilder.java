package com.appambit.sdk;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.db.DbResponse;
import com.appambit.sdk.models.db.DbResult;
import com.appambit.sdk.services.DbService;
import com.appambit.sdk.services.exceptionsCustom.DbException;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DbQueryBuilder<T> {
    private final String table;
    private final Class<T> modelClass;
    private final DbService dbService;

    private final List<String> selectedColumns = new ArrayList<>();
    private final List<String> whereConditions = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    private String orderByColumn;
    private boolean orderByDesc = false;
    private int limitValue = -1;
    private int offsetValue = -1;

    DbQueryBuilder(String table, Class<T> modelClass, DbService dbService) {
        this.table = table;
        this.modelClass = modelClass;
        this.dbService = dbService;
    }

    public DbQueryBuilder<T> select(String... columns) {
        for (String col : columns) {
            selectedColumns.add(col);
        }
        return this;
    }

    public DbQueryBuilder<T> where(String column, Object value) {
        whereConditions.add(quoteIdentifier(column) + " = ?");
        whereParams.add(value);
        return this;
    }

    public DbQueryBuilder<T> where(String column, String operator, Object value) {
        whereConditions.add(quoteIdentifier(column) + " " + operator + " ?");
        whereParams.add(value);
        return this;
    }

    public DbQueryBuilder<T> whereIn(String column, List<?> values) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) placeholders.append(", ");
            placeholders.append("?");
            whereParams.add(values.get(i));
        }
        whereConditions.add(quoteIdentifier(column) + " IN (" + placeholders + ")");
        return this;
    }

    public DbQueryBuilder<T> orderBy(String column) {
        this.orderByColumn = column;
        this.orderByDesc = false;
        return this;
    }

    public DbQueryBuilder<T> orderByDesc(String column) {
        this.orderByColumn = column;
        this.orderByDesc = true;
        return this;
    }

    public DbQueryBuilder<T> limit(int n) {
        this.limitValue = n;
        return this;
    }

    public DbQueryBuilder<T> offset(int n) {
        this.offsetValue = n;
        return this;
    }

    public AppAmbitTaskFuture<List<T>> get() {
        String sql = buildSelectSql(-1);
        AppAmbitTaskFuture<List<T>> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> queryFuture = dbService.query(sql, new ArrayList<>(whereParams));
        queryFuture.then(response -> {
            DbResult result = response.getFirst();
            if (result == null) {
                future.complete(new ArrayList<>());
                return;
            }
            if (result.hasError()) {
                future.fail(new DbException(result.getError()));
                return;
            }
            future.complete(deserializeResult(result));
        });
        queryFuture.onError(future::fail);
        return future;
    }

    public AppAmbitTaskFuture<T> first() {
        String sql = buildSelectSql(1);
        AppAmbitTaskFuture<T> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> queryFuture = dbService.query(sql, new ArrayList<>(whereParams));
        queryFuture.then(response -> {
            DbResult result = response.getFirst();
            if (result == null || result.getRows().isEmpty()) {
                future.complete(null);
                return;
            }
            if (result.hasError()) {
                future.fail(new DbException(result.getError()));
                return;
            }
            List<T> items = deserializeResult(result);
            future.complete(items.isEmpty() ? null : items.get(0));
        });
        queryFuture.onError(future::fail);
        return future;
    }

    public AppAmbitTaskFuture<Integer> count() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(quoteIdentifier(table));
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(joinConditions());
        }
        AppAmbitTaskFuture<Integer> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> queryFuture = dbService.query(sql.toString(), new ArrayList<>(whereParams));
        queryFuture.then(response -> {
            DbResult result = response.getFirst();
            if (result == null || result.getRows().isEmpty()) {
                future.complete(0);
                return;
            }
            if (result.hasError()) {
                future.fail(new DbException(result.getError()));
                return;
            }
            Object val = result.getRows().get(0).get(0);
            int count = val instanceof Number
                    ? ((Number) val).intValue()
                    : Integer.parseInt(val.toString());
            future.complete(count);
        });
        queryFuture.onError(future::fail);
        return future;
    }

    public AppAmbitTaskFuture<DbResult> insert(Map<String, Object> data) {
        List<String> cols = new ArrayList<>(data.keySet());
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(quoteIdentifier(table)).append(" (");
        List<Object> params = new ArrayList<>();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                placeholders.append(", ");
            }
            sql.append(quoteIdentifier(cols.get(i)));
            placeholders.append("?");
            params.add(data.get(cols.get(i)));
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        AppAmbitTaskFuture<DbResult> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> queryFuture = dbService.query(sql.toString(), params);
        queryFuture.then(response -> {
            DbResult result = response.getFirst();
            if (result == null) {
                future.fail(new DbException("No result from INSERT"));
            } else {
                future.complete(result);
            }
        });
        queryFuture.onError(future::fail);
        return future;
    }

    public AppAmbitTaskFuture<DbResult> update(Map<String, Object> data) {
        List<String> cols = new ArrayList<>(data.keySet());
        StringBuilder sql = new StringBuilder("UPDATE ").append(quoteIdentifier(table)).append(" SET ");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(quoteIdentifier(cols.get(i))).append(" = ?");
            params.add(data.get(cols.get(i)));
        }
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(joinConditions());
            params.addAll(whereParams);
        }

        AppAmbitTaskFuture<DbResult> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> queryFuture = dbService.query(sql.toString(), params);
        queryFuture.then(response -> {
            DbResult result = response.getFirst();
            if (result == null) {
                future.fail(new DbException("No result from UPDATE"));
            } else {
                future.complete(result);
            }
        });
        queryFuture.onError(future::fail);
        return future;
    }

    public AppAmbitTaskFuture<DbResult> delete() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(quoteIdentifier(table));
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(joinConditions());
        }

        AppAmbitTaskFuture<DbResult> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> queryFuture = dbService.query(sql.toString(), new ArrayList<>(whereParams));
        queryFuture.then(response -> {
            DbResult result = response.getFirst();
            if (result == null) {
                future.fail(new DbException("No result from DELETE"));
            } else {
                future.complete(result);
            }
        });
        queryFuture.onError(future::fail);
        return future;
    }

    @SuppressWarnings("unchecked")
    private List<T> deserializeResult(DbResult result) {
        if (modelClass == null || Map.class.isAssignableFrom(modelClass)) {
            return (List<T>) result.toMaps();
        }
        return result.mapTo(modelClass);
    }

    private String buildSelectSql(int overrideLimit) {
        StringBuilder sql = new StringBuilder("SELECT ");
        if (selectedColumns.isEmpty()) {
            sql.append("*");
        } else {
            for (int i = 0; i < selectedColumns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(quoteIdentifier(selectedColumns.get(i)));
            }
        }
        sql.append(" FROM ").append(quoteIdentifier(table));
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(joinConditions());
        }
        if (orderByColumn != null) {
            sql.append(" ORDER BY ").append(quoteIdentifier(orderByColumn));
            if (orderByDesc) sql.append(" DESC");
        }
        int effectiveLimit = overrideLimit > 0 ? overrideLimit : limitValue;
        if (effectiveLimit > 0) sql.append(" LIMIT ").append(effectiveLimit);
        if (offsetValue > 0) sql.append(" OFFSET ").append(offsetValue);
        return sql.toString();
    }

    private String joinConditions() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < whereConditions.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(whereConditions.get(i));
        }
        return sb.toString();
    }

    private String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
