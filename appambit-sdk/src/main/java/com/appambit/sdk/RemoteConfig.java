package com.appambit.sdk;

import android.content.Context;
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
    private static Context mContext;
    private static Storable mStorable;
    private static AppInfoService mAppInfoService;
    private static final String TAG = "RemoteConfig";

    public static void initialize(Context context, ExecutorService executorService, ApiService apiService,
            Storable storable, AppInfoService appInfoService) {
        mContext = context;
        mExecutorService = executorService;
        mApiService = apiService;
        mStorable = storable;
        mAppInfoService = appInfoService;
    }

    private static boolean isEnable = false;
    private static boolean isFetchCompleted = false;

    public static boolean enable() {
        return isEnable = true;
    }

    public static void fetchAndStoreConfig() {
        if (!isEnable || isFetchCompleted)
            return;

        final AppAmbitTaskFuture<Boolean> future = new AppAmbitTaskFuture<>();

        if (mExecutorService == null || mApiService == null) {
            Log.d(TAG, "No initialized services");
            future.complete(false);
            return;
        }

        mExecutorService.execute(() -> {
            try {
                ApiResult<RemoteConfigResponse> result = mApiService.executeRequest(
                        new RemoteConfigEndpoint(mAppInfoService.getAppVersion()), RemoteConfigResponse.class);

                if (result.errorType == ApiErrorType.None) {
                    if (result.data != null && result.data.getConfigs() != null) {
                        List<RemoteConfigEntity> configEntities = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : result.data.getConfigs().entrySet()) {
                            RemoteConfigEntity entity = new RemoteConfigEntity();
                            entity.setId(UUID.randomUUID());
                            entity.setKey(entry.getKey());
                            entity.setValue(String.valueOf(entry.getValue()));
                            configEntities.add(entity);
                        }
                        mStorable.putConfigs(configEntities);
                    }
                    isFetchCompleted = true;
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            } catch (Exception e) {
                future.fail(e);
            }
        });

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

    public static long getLong(String key) {
        Object value = getValue(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "Error: Long number couldn't be parsed");
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
