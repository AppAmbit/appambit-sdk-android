package com.appambit.sdk;

import android.util.Log;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.breadcrumbs.BreadcrumEntity;
import com.appambit.sdk.models.breadcrumbs.BreadcrumbMappings;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.models.responses.BatchResponse;
import com.appambit.sdk.services.endpoints.BreadcrumbEndpoint;
import com.appambit.sdk.services.endpoints.BreadcrumbsBatchEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.FileUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static com.appambit.sdk.utils.FileUtils.deleteSingleObject;
import static com.appambit.sdk.utils.StringValidation.isUIntNumber;

public class BreadcrumbManager {
    private static final String TAG = "BreadcrumbManager";
    private static ApiService mApiService;
    private static Storable mStorageService;
    private static ExecutorService mExecutorService;

    private static final Object SEND_LOCK = new Object();
    private static boolean isSending = false;

    public static void initialize(ApiService apiService, ExecutorService executorService, Storable storageService) {
        mApiService = apiService;
        mExecutorService = executorService;
        mStorageService = storageService;
    }

    public static void AddAsync(String name) {
        if (mApiService == null || mExecutorService == null || mStorageService == null) return;

        BreadcrumEntity entity = createEntity(name);

        AppAmbitTaskFuture<Void> send = sendBreadcrumbEndpoint(entity);
        send.then(r -> {
            Log.d(TAG, "Send breadcrumbs");
        });
        send.onError(error -> {
            Log.d(TAG, "Error to Send breadcrumbs");
        });
    }

    public static void SaveToFile(String name) {
        try {
            BreadcrumEntity entity = createEntity(name);
            FileUtils.saveToFile(entity);
        } catch (Throwable t) {
            Log.d(TAG, "SaveToFile error: " + t.getMessage());
        }
    }

    public static void SendFromFile() {
        try {
            BreadcrumEntity entity = FileUtils.getSavedSingleObject(BreadcrumEntity.class);
            if (entity == null) return;

            AppAmbitTaskFuture<Void> fut = sendBreadcrumbEndpoint(entity);
            fut.then(r -> deleteSingleObject(BreadcrumEntity.class));
            fut.onError(err -> {
                Log.d(TAG, "SendFromFile error: " + err.getLocalizedMessage());
            });
        } catch (Throwable t) {
            Log.d(TAG, "SendFromFile error: " + t.getMessage());
        }
    }

    public static void SendPending() {
        if (mApiService == null || mExecutorService == null || mStorageService == null) return;

        synchronized (SEND_LOCK) {
            if (isSending) return;
            isSending = true;
        }

        mExecutorService.execute(() -> {
            try {
                List<BreadcrumEntity> items = mStorageService.getOldest100Breadcrumbs();
                if (items == null || items.isEmpty()) {
                    finishSend();
                    return;
                }

                AppAmbitTaskFuture<ApiResult<BatchResponse>> batchFuture = sendBatchEndpoint(items);

                batchFuture.then(result -> {
                    if (result == null || result.errorType != ApiErrorType.None) {
                        Log.d(TAG, "Batch not sent (kept for retry). ErrorType=" +
                                (result != null ? result.errorType : "null"));
                        finishSend();
                        return;
                    }

                    try {
                        mStorageService.deleteBreadcrumbs(items);
                        Log.d(TAG, "Finished Breadcrumbs Batch");
                    } catch (Throwable t) {
                        Log.d(TAG, "Error deleting sent breadcrumbs: " + t.getMessage());
                    }
                    finishSend();
                });

                batchFuture.onError(error -> {
                    Log.d(TAG, "SendPending batch error: " + error.getMessage());
                    finishSend();
                });
            } catch (Throwable t) {
                Log.d(TAG, "SendPending unexpected error: " + t.getMessage());
                finishSend();
            }
        });
    }

    private static BreadcrumEntity createEntity(String name) {
        BreadcrumEntity e = new BreadcrumEntity();
        e.setId(UUID.randomUUID());
        e.setCreatedAt(DateUtils.getUtcNow());

        String sid = SessionManager.getSessionId();
        e.setSessionId(isUIntNumber(sid) ? sid : null);

        e.setName(name);
        return e;
    }

    private static AppAmbitTaskFuture<Void> sendBreadcrumbEndpoint(BreadcrumEntity entity) {
        AppAmbitTaskFuture<Void> result = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                ApiResult<Object> apiResponse =  mApiService.executeRequest(new BreadcrumbEndpoint(entity), Object.class);

                if (apiResponse != null && apiResponse.errorType == ApiErrorType.None) {
                    mStorageService.addBreadcrumb(entity);
                    result.complete(null);
                } else {
                    result.fail(new RuntimeException("Breadcrumb send failed"));
                }
            } catch (Exception e) {
                result.fail(e);
            }
        });
        return result;
    }

    private static AppAmbitTaskFuture<ApiResult<BatchResponse>> sendBatchEndpoint(List<BreadcrumEntity> items) {
        AppAmbitTaskFuture<ApiResult<BatchResponse>> future = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                ApiResult<BatchResponse> apiResponse =
                        mApiService.executeRequest(new BreadcrumbsBatchEndpoint(BreadcrumbMappings.toDataList(items)),
                                BatchResponse.class);
                future.complete(apiResponse);
            } catch (Exception e) {
                future.fail(e);
            }
        });
        return future;
    }

    private static void finishSend() {
        synchronized (SEND_LOCK) {
            isSending = false;
        }
    }
}
