package com.appambit.sdk;

import android.util.Log;

import androidx.annotation.Nullable;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.breadcrumbs.BreadcrumbData;
import com.appambit.sdk.models.breadcrumbs.BreadcrumbEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class BreadcrumbManager {
    private static final String TAG = "BreadcrumbManager";
    private static ApiService mApiService;
    private static Storable mStorageService;
    private static ExecutorService mExecutorService;

    private static final Object SEND_LOCK = new Object();
    private static boolean isSending = false;

    private static final Object LAST_LOCK = new Object();
    private static String lastBreadcrumb;
    private static long lastBreadcrumbAtMs;

    public static void initialize(ApiService apiService, ExecutorService executorService, Storable storageService) {
        mApiService = apiService;
        mExecutorService = executorService;
        mStorageService = storageService;
    }

    public static void addAsync(String name) {
        if (mApiService == null || mExecutorService == null || mStorageService == null) return;
        if (isDuplicate(name)) return;
        BreadcrumbEntity entity = createEntity(name);
        AppAmbitTaskFuture<Void> send = sendBreadcrumbEndpoint(entity);
        send.then(r -> Log.d(TAG, "Send breadcrumbs"));
        send.onError(error -> Log.d(TAG, "Error to Send breadcrumbs"));
    }

    public static void saveToFile(String name) {
        mExecutorService.execute(() -> {
            try {
                Log.d(TAG, "Got to save breadcrumbs: " + name);
                if (isDuplicate(name)) return;
                BreadcrumbData data = BreadcrumbMappings.toData(createEntity(name));
                FileUtils.getSaveJsonArray(BreadcrumbsConstants.fileName, data, BreadcrumbData.class);
            } catch (Throwable t) {
                Log.d(TAG, "SaveToFile error: " + t.getMessage());
            }
        });
    }

    public static void loadBreadcrumbsFromFile() {
        try {
            List<BreadcrumbData> files = FileUtils.getSaveJsonArray(BreadcrumbsConstants.fileName, BreadcrumbData.class);
            List<BreadcrumbData> notSent = new ArrayList<>();
            if (files.isEmpty()) return;
            for (BreadcrumbData item : files) {
                if (item == null) continue;
                try {
                    if (mStorageService != null) {
                        mStorageService.addBreadcrumb(BreadcrumbMappings.toEntity(item));
                    }
                } catch (Throwable t) {
                    notSent.add(item);
                }
            }
            FileUtils.updateJsonArray(BreadcrumbsConstants.fileName, notSent);
        } catch (Throwable t) {
            Log.d(TAG, t.toString());
        }
    }

    public static void loadBreadcrumbsFromFileAsync(@Nullable Runnable onComplete) {
        if (mExecutorService == null) {
            loadBreadcrumbsFromFile();
            if (onComplete != null) {
                try { onComplete.run(); } catch (Throwable ignored) {}
            }
            return;
        }
        mExecutorService.execute(() -> {
            try {
                List<BreadcrumbData> files = FileUtils.getSaveJsonArray(BreadcrumbsConstants.fileName, BreadcrumbData.class);
                List<BreadcrumbData> notSent = new ArrayList<>();
                if (!files.isEmpty()) {
                    for (BreadcrumbData item : files) {
                        if (item == null) continue;
                        try {
                            if (mStorageService != null) {
                                mStorageService.addBreadcrumb(BreadcrumbMappings.toEntity(item));
                            }
                        } catch (Throwable t) {
                            notSent.add(item);
                        }
                    }
                    FileUtils.updateJsonArray(BreadcrumbsConstants.fileName, notSent);
                }
            } catch (Throwable t) {
                Log.d(TAG, t.toString());
            } finally {
                if (onComplete != null) {
                    try { onComplete.run(); } catch (Throwable ignored) {}
                }
            }
        });
    }

    public static void sendBatchBreadcrumbs() {
        if (mApiService == null || mExecutorService == null || mStorageService == null) return;
        synchronized (SEND_LOCK) {
            if (isSending) return;
            isSending = true;
        }
        mExecutorService.execute(() -> {
            try {
                List<BreadcrumbEntity> items = mStorageService.getOldest100Breadcrumbs();
                if (items == null || items.isEmpty()) {
                    finishSend();
                    return;
                }
                AppAmbitTaskFuture<ApiResult<BatchResponse>> batchFuture = sendBatchEndpoint(items);
                batchFuture.then(result -> {
                    if (result == null || result.errorType != ApiErrorType.None) {
                        Log.d(TAG, "Batch not sent (kept for retry). ErrorType=" + (result != null ? result.errorType : "null"));
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

    private static BreadcrumbEntity createEntity(String name) {
        BreadcrumbEntity e = new BreadcrumbEntity();
        e.setId(UUID.randomUUID());
        e.setCreatedAt(DateUtils.getUtcNow());
        e.setSessionId(SessionManager.getSessionId());
        e.setName(name);
        return e;
    }

    private static AppAmbitTaskFuture<Void> sendBreadcrumbEndpoint(BreadcrumbEntity entity) {
        AppAmbitTaskFuture<Void> result = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                ApiResult<Object> apiResponse = mApiService.executeRequest(new BreadcrumbEndpoint(entity), Object.class);
                if (apiResponse != null && apiResponse.errorType == ApiErrorType.None) {
                    result.complete(null);
                } else {
                    mStorageService.addBreadcrumb(entity);
                    result.fail(new RuntimeException("Breadcrumb send failed"));
                }
            } catch (Exception e) {
                result.fail(e);
            }
        });
        return result;
    }

    private static AppAmbitTaskFuture<ApiResult<BatchResponse>> sendBatchEndpoint(List<BreadcrumbEntity> items) {
        AppAmbitTaskFuture<ApiResult<BatchResponse>> future = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                ApiResult<BatchResponse> apiResponse =
                        mApiService.executeRequest(new BreadcrumbsBatchEndpoint(BreadcrumbMappings.toDataList(items)), BatchResponse.class);
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

    private static boolean isDuplicate(String name) {
        synchronized (LAST_LOCK) {
            long now = System.currentTimeMillis();
            boolean dup = lastBreadcrumb != null && lastBreadcrumb.equals(name) && (now - lastBreadcrumbAtMs) < 2000;
            if (!dup) {
                lastBreadcrumb = name;
                lastBreadcrumbAtMs = now;
            }
            return dup;
        }
    }
}
