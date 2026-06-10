package com.appambit.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appambit.sdk.models.db.DbResponse;
import com.appambit.sdk.models.db.DbResult;
import com.appambit.sdk.models.db.DbStatement;
import com.appambit.sdk.services.DbService;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class AppAmbitDb {
    private static DbService mDbService;

    private AppAmbitDb() {}

    static void initialize(DbService dbService) {
        mDbService = dbService;
    }

    private static void ensureInitialized() {
        if (mDbService == null) {
            throw new IllegalStateException("AppAmbit SDK is not initialized. Call AppAmbit.start() first.");
        }
    }

    /**
     * Execute a raw SQL statement with no parameters.
     */
    public static AppAmbitTaskFuture<DbResult> execute(@NonNull String sql) {
        return executeInternal(sql, null);
    }

    /**
     * Execute a raw SQL statement with positional parameters.
     * Use ? placeholders in the SQL string.
     */
    public static AppAmbitTaskFuture<DbResult> execute(@NonNull String sql, @Nullable Object... params) {
        List<Object> paramList = (params != null && params.length > 0)
                ? Arrays.asList(params)
                : null;
        return executeInternal(sql, paramList);
    }

    private static AppAmbitTaskFuture<DbResult> executeInternal(String sql, List<Object> params) {
        ensureInitialized();
        AppAmbitTaskFuture<DbResult> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> queryFuture = mDbService.query(sql, params);
        queryFuture.then(response -> {
            DbResult result = response.getFirst();
            if (result == null) {
                future.fail(new RuntimeException("No result returned from database"));
            } else {
                future.complete(result);
            }
        });
        queryFuture.onError(future::fail);
        return future;
    }

    /**
     * Execute multiple SQL statements in a single request.
     */
    public static AppAmbitTaskFuture<List<DbResult>> batch(DbStatement... statements) {
        return batchInternal(Arrays.asList(statements), false);
    }

    /**
     * Execute multiple SQL statements in a single request, wrapped in a transaction.
     * If any statement fails, all changes are rolled back.
     */
    public static AppAmbitTaskFuture<List<DbResult>> batchInTransaction(DbStatement... statements) {
        return batchInternal(Arrays.asList(statements), true);
    }

    private static AppAmbitTaskFuture<List<DbResult>> batchInternal(List<DbStatement> statements, boolean transaction) {
        ensureInitialized();
        AppAmbitTaskFuture<List<DbResult>> future = new AppAmbitTaskFuture<>();
        AppAmbitTaskFuture<DbResponse> batchFuture = mDbService.batch(statements, transaction);
        batchFuture.then(response -> {
            List<DbResult> results = response.getResults();
            for (DbResult r : results) {
                if (r.hasError()) {
                    future.fail(new RuntimeException("Batch statement failed: " + r.getError()));
                    return;
                }
            }
            future.complete(results);
        });
        batchFuture.onError(future::fail);
        return future;
    }

    /**
     * Start a fluent query builder for the given table, mapping results to the provided model class.
     * Fields are matched by name or by the {@code @DbColumn} annotation.
     */
    public static <T> DbQueryBuilder<T> from(@NonNull String table, @NonNull Class<T> modelClass) {
        ensureInitialized();
        return new DbQueryBuilder<>(table, modelClass, mDbService);
    }

    /**
     * Start a fluent query builder for the given table, returning results as maps.
     */
    @SuppressWarnings("unchecked")
    public static DbQueryBuilder<Map<String, Object>> from(@NonNull String table) {
        ensureInitialized();
        return new DbQueryBuilder<>(table, (Class<Map<String, Object>>) (Class<?>) Map.class, mDbService);
    }
}
