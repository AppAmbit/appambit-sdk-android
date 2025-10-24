package com.appambit.sdk;

import android.util.Log;
import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.breadcrumbs.BreadcrumEntity;
import com.appambit.sdk.services.endpoints.BreadcrumbEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.FileUtils;
import java.util.ArrayList;
import java.util.Date;
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
        send.then(result -> {
            if (result == null) mStorageService.addBreadcrumb(entity);
        });
        send.onError(error -> mStorageService.addBreadcrumb(entity));
    }

    public static void SaveToFile(String name) {
        try {
            BreadcrumEntity entity = createEntity(name);
            FileUtils.saveToFile(entity);
        } catch (Throwable t) {
            Log.d(TAG, "SaveToFile error");
        }
    }

    public static void SendFromFile() {
        try {
            BreadcrumEntity entity = FileUtils.getSavedSingleObject(BreadcrumEntity.class);
            if (entity == null) return;
            AppAmbitTaskFuture<Void> fut = sendBreadcrumbEndpoint(entity);
            fut.then(r -> {
                deleteSingleObject(BreadcrumEntity.class);
            });
            fut.onError(err -> {});
        } catch (Throwable t) {}
    }

    public static void SendPending() {
        if (mApiService == null || mExecutorService == null || mStorageService == null) return;
        synchronized (SEND_LOCK) {
            if (isSending) return;
            isSending = true;
        }
        mExecutorService.execute(() -> {
            try {
                List<BreadcrumEntity> pending = mStorageService.getAllBreadcrumbs();
                if (pending == null || pending.isEmpty()) {
                    finishSend();
                    return;
                }
                List<BreadcrumEntity> sent = new ArrayList<>();
                sendSequentially(pending, 0, sent, () -> {
                    if (!sent.isEmpty()) {
                        try {
                            mStorageService.deleteBreadcrumbs(sent);
                        } catch (Throwable t) {}
                    }
                    finishSend();
                });
            } catch (Throwable t) {
                finishSend();
            }
        });
    }

    private static void sendSequentially(List<BreadcrumEntity> items, int index, List<BreadcrumEntity> sent, Runnable onComplete) {
        if (index >= items.size()) {
            onComplete.run();
            return;
        }
        BreadcrumEntity payload = items.get(index);
        AppAmbitTaskFuture<Void> fut = sendBreadcrumbEndpoint(payload);
        fut.then(r -> {
            sent.add(payload);
            sendSequentially(items, index + 1, sent, onComplete);
        });
        fut.onError(err -> sendSequentially(items, index + 1, sent, onComplete));
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
                var apiResponse = mApiService.executeRequest(new BreadcrumbEndpoint(entity), Object.class);
                if (apiResponse.errorType == ApiErrorType.None) {
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

    private static void finishSend() {
        synchronized (SEND_LOCK) {
            isSending = false;
        }
    }
}
