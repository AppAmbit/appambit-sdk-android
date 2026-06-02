package com.appambit.sdk.services;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.db.DbResponse;
import com.appambit.sdk.models.db.DbStatement;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.endpoints.DbBatchEndpoint;
import com.appambit.sdk.services.endpoints.DbQueryEndpoint;
import com.appambit.sdk.services.exceptionsCustom.DbException;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class DbService {
    private final ApiService apiService;
    private final ExecutorService executorService;

    public DbService(ApiService apiService, ExecutorService executorService) {
        this.apiService = apiService;
        this.executorService = executorService;
    }

    public AppAmbitTaskFuture<DbResponse> query(String sql, List<Object> params) {
        AppAmbitTaskFuture<DbResponse> future = new AppAmbitTaskFuture<>();
        executorService.execute(() -> {
            try {
                ApiResult<String> result = apiService.executeRequest(
                        new DbQueryEndpoint(sql, params), String.class);
                if (result.errorType != ApiErrorType.None) {
                    future.fail(new DbException(result.errorType, result.message));
                } else {
                    future.complete(DbResponse.fromJson(result.data));
                }
            } catch (Exception e) {
                future.fail(e);
            }
        });
        return future;
    }

    public AppAmbitTaskFuture<DbResponse> batch(List<DbStatement> statements, boolean transaction) {
        AppAmbitTaskFuture<DbResponse> future = new AppAmbitTaskFuture<>();
        executorService.execute(() -> {
            try {
                ApiResult<String> result = apiService.executeRequest(
                        new DbBatchEndpoint(statements, transaction), String.class);
                if (result.errorType != ApiErrorType.None) {
                    future.fail(new DbException(result.errorType, result.message));
                } else {
                    future.complete(DbResponse.fromJson(result.data));
                }
            } catch (Exception e) {
                future.fail(e);
            }
        });
        return future;
    }
}
