package com.appambit.sdk;

import android.util.Log;

import androidx.annotation.Nullable;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.responses.RemoteConfigResponse;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.endpoints.RemoteConfigEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.AppInfoService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;

import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import com.appambit.sdk.models.remoteConfigs.RemoteConfigEntity;

public class RemoteConfig {

    private static ExecutorService mExecutorService;
    private static ApiService mApiService;
    private static Storable mStorable;
    private static AppInfoService mAppInfoService;
    private static final String TAG = "RemoteConfig";

    public static void initialize(ExecutorService executorService, ApiService apiService,
            Storable storable, AppInfoService appInfoService) {
        mExecutorService = executorService;
        mApiService = apiService;
        mStorable = storable;
        mAppInfoService = appInfoService;
        isFetchCompleted = false;
        applyCachedConfigToBreadcrumbManager();
    }

    private static boolean isEnable = false;
    private static boolean isFetchCompleted = false;
    private static boolean isFetching = false;
    private static final List<AppAmbitTaskFuture<Boolean>> pendingFutures = new ArrayList<>();
    private static final Object FETCH_LOCK = new Object();

    public static boolean isEnable() {
        return isEnable;
    }

    public static void enable() {
        isEnable = true;
        applyCachedConfigToBreadcrumbManager();
    }

    private static void applyCachedConfigToBreadcrumbManager() {
        if (!isEnable)
            return;

        Object configVal = getValue(AppConstants.LIVE_SESSION_STREAMING);
        if (configVal != null) {
            String stringVal = String.valueOf(configVal);
            boolean remoteValue = Boolean.parseBoolean(stringVal);
            BreadcrumbManager.isCrashOnlyMode = !remoteValue;
        } else {
            BreadcrumbManager.isCrashOnlyMode = false;
        }
        Log.d(TAG, "applyCachedConfigToBreadcrumbManager: isCrashOnlyMode=" + BreadcrumbManager.isCrashOnlyMode);
    }

    public static AppAmbitTaskFuture<Boolean> fetchAndStoreConfig() {
        final AppAmbitTaskFuture<Boolean> future = new AppAmbitTaskFuture<>();

        if (!isEnable) {
            Log.d(TAG, "RemoteConfig: fetchAndStoreConfig skipped -> !isEnable");
            future.complete(false);
            return future;
        }

        if (isFetchCompleted) {
            Log.d(TAG, "RemoteConfig: fetchAndStoreConfig skipped -> isFetchCompleted");
            future.complete(false);
            return future;
        }

        if (mExecutorService == null || mApiService == null) {
            Log.d(TAG, "No initialized services");
            future.complete(false);
            return future;
        }

        synchronized (FETCH_LOCK) {
            if (isFetching) {
                Log.d(TAG, "RemoteConfig: fetch in progress, queuing future");
                pendingFutures.add(future);
                return future;
            }
            isFetching = true;
            pendingFutures.add(future);
        }

        mExecutorService.execute(() -> {
            try {
                ApiResult<RemoteConfigResponse> result = mApiService.executeRequest(
                        new RemoteConfigEndpoint(mAppInfoService.getAppVersion()), RemoteConfigResponse.class);

                if (result.errorType == ApiErrorType.None) {
                    isFetchCompleted = true;
                    if (result.data != null && result.data.getConfigs() != null
                            && !result.data.getConfigs().isEmpty()) {
                        List<RemoteConfigEntity> configEntities = new ArrayList<>();
                        boolean hasLiveStreamKey = false;
                        Object liveStreamVal = null;

                        for (Map.Entry<String, Object> entry : result.data.getConfigs().entrySet()) {
                            String key = entry.getKey();
                            if (AppConstants.LIVE_SESSION_STREAMING.equals(key)) {
                                hasLiveStreamKey = true;
                                liveStreamVal = entry.getValue();
                                key = AppConstants.LIVE_SESSION_STREAMING;
                            }
                            RemoteConfigEntity entity = new RemoteConfigEntity();
                            entity.setId(UUID.randomUUID());
                            entity.setKey(key);
                            entity.setValue(String.valueOf(entry.getValue()));
                            configEntities.add(entity);
                        }
                        if (!hasLiveStreamKey) {
                            RemoteConfigEntity entity = new RemoteConfigEntity();
                            entity.setId(UUID.randomUUID());
                            entity.setKey(AppConstants.LIVE_SESSION_STREAMING);
                            entity.setValue("true");
                            configEntities.add(entity);
                            liveStreamVal = "true";
                        }

                        mStorable.putConfigs(configEntities);

                        String stringVal = String.valueOf(liveStreamVal);
                        boolean remoteValue = Boolean.parseBoolean(stringVal);
                        BreadcrumbManager.isCrashOnlyMode = !remoteValue;

                    } else {
                        List<RemoteConfigEntity> configEntities = new ArrayList<>();
                        RemoteConfigEntity entity = new RemoteConfigEntity();
                        entity.setId(UUID.randomUUID());
                        entity.setKey(AppConstants.LIVE_SESSION_STREAMING);
                        entity.setValue("true");
                        configEntities.add(entity);
                        mStorable.putConfigs(configEntities);
                        BreadcrumbManager.isCrashOnlyMode = false;
                    }

                    Log.d(TAG, "RemoteConfig: Fetch succeeded, isCrashOnlyMode = " + BreadcrumbManager.isCrashOnlyMode);
                    completePendingFutures(true, null);
                } else {
                    Log.d(TAG, "RemoteConfig: Fetch failed: " + result.errorType);
                    completePendingFutures(false, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchAndStoreConfig error", e);
                completePendingFutures(false, e);
            }
        });

        return future;
    }

    private static void completePendingFutures(boolean success, Exception exception) {
        List<AppAmbitTaskFuture<Boolean>> futuresToComplete;
        synchronized (FETCH_LOCK) {
            isFetching = false;
            futuresToComplete = new ArrayList<>(pendingFutures);
            pendingFutures.clear();
        }

        for (AppAmbitTaskFuture<Boolean> f : futuresToComplete) {
            if (exception != null) {
                f.fail(exception);
            } else {
                f.complete(success);
            }
        }
    }

    @Nullable
    public static String getString(String key) {
        Object value = getValue(key);
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? String.valueOf(value) : null;
    }

    public static boolean getBoolean(String key) {
        Object value = getValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    public static int getInt(String key) {
        Object value = getValue(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "Error: Integer number couldn't be parsed");
            }
        }
        return 0;
    }

    public static double getDouble(String key) {
        Object value = getValue(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "Error: Double number couldn't be parsed");
            }
        }
        return 0.0;
    }

    @Nullable
    private static Object getValue(String key) {
        if (mStorable != null) {
            return mStorable.getConfig(key);
        }
        return null;
    }

}
