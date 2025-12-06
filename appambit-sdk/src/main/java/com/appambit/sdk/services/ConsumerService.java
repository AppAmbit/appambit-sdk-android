package com.appambit.sdk.services;

import android.util.Log;

import com.appambit.sdk.ServiceLocator;
import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.app.Consumer;
import com.appambit.sdk.models.app.UpdateConsumer;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.models.responses.TokenResponse;
import com.appambit.sdk.services.endpoints.RegisterEndpoint;
import com.appambit.sdk.services.endpoints.UpdateConsumerEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.AppInfoService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;

public class ConsumerService {
    private static final String TAG = ConsumerService.class.getSimpleName();
    static Storable mStorageService;
    static AppInfoService mAppInfoService;
    static ApiService mApiService;

    public static void initialize(Storable storageService, AppInfoService appInfoService, ApiService apiService) {
        mStorageService = storageService;
        mAppInfoService = appInfoService;
        mApiService = apiService;
    }

    private static RegisterEndpoint buildRegisterEndpoint() {
        String appId = null;
        String deviceId = "";
        String userId = "";
        String userEmail = null;

        try {
            if (mStorageService == null || mAppInfoService == null) {
                return new RegisterEndpoint(new Consumer("", "", "", "", "", null, "", "", ""));
            }

            deviceId = mStorageService.getDeviceId();
            userId = mStorageService.getUserId();
            String emailFromStorage = mStorageService.getUserEmail();
            userEmail = (emailFromStorage == null || emailFromStorage.trim().isEmpty())
                    ? null
                    : emailFromStorage.trim();

            appId = mStorageService.getAppId();

            if (isBlank(deviceId)) {
                deviceId = java.util.UUID.randomUUID().toString();
                mStorageService.putDeviceId(deviceId);
            }

            if (isBlank(userId)) {
                userId = java.util.UUID.randomUUID().toString();
                mStorageService.putUserId(userId);
            }


            Consumer consumer = new Consumer(
                    ensureValue(appId, ""),
                    ensureValue(deviceId, ""),
                    mAppInfoService != null ? ensureValue(mAppInfoService.getDeviceModel(), "") : "",
                    mAppInfoService != null ? ensureValue(mAppInfoService.getAppVersion(), "") : "",
                    ensureValue(userId, ""),
                    userEmail,
                    mAppInfoService != null ? ensureValue(mAppInfoService.getOs(), "") : "",
                    mAppInfoService != null ? ensureValue(mAppInfoService.getCountry(), "") : "",
                    mAppInfoService != null ? ensureValue(mAppInfoService.getLanguage(), "") : ""
            );

            return new RegisterEndpoint(consumer);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to build RegisterEndpoint", ex);
        }
    }

    public static void updateAppKeyIfNeeded(String appKey) {
        if (mStorageService == null) {
            Log.d(TAG, "updateAppKeyIfNeeded: mStorageService is null, skipping.");
            return;
        }

        String newKey = (appKey == null) ? "" : appKey.trim();

        if (isBlank(newKey)) {
            return;
        }

        String storedKey = mStorageService.getAppId();
        if (equalsNullable(storedKey, newKey)) {
            return;
        }

        mStorageService.putConsumerId("");
        mStorageService.putAppId(newKey);
    }

    public static AppAmbitTaskFuture<ApiErrorType> createConsumer() {
        final AppAmbitTaskFuture<ApiErrorType> promise = new AppAmbitTaskFuture<>();

        if (mApiService == null) {
            promise.complete(ApiErrorType.Unknown);
            return promise;
        }

        ServiceLocator.getExecutorService().execute(() -> {
            try {
                final RegisterEndpoint registerEndpoint = buildRegisterEndpoint();
                ApiResult<TokenResponse> responseApi =
                        mApiService.executeRequest(registerEndpoint, TokenResponse.class);

                if (responseApi == null) {
                    promise.complete(ApiErrorType.Unknown);
                    return;
                }

                ApiErrorType errorType = responseApi.errorType;
                TokenResponse data = responseApi.data;

                if (errorType == null || errorType == ApiErrorType.None) {
                    try {
                        if (data != null && data.getConsumerId() != null && !data.getConsumerId().trim().isEmpty()) {
                            mStorageService.putConsumerId(data.getConsumerId());
                        }

                        if (data != null && data.getToken() != null && !data.getToken().trim().isEmpty()) {
                            mApiService.setToken(data.getToken());
                        }
                    } catch (Exception ex) {
                        Log.d(TAG, "Error getting token: " + ex.getMessage());
                    }
                    promise.complete(ApiErrorType.None);
                } else {
                    promise.complete(errorType);
                }
            } catch (Throwable t) {
                promise.fail(t);
            }
        });
        return promise;
    }

    public static void updateConsumer(String deviceToken, boolean pushEnabled) {
        if (mStorageService == null || mApiService == null) {
            Log.e(TAG, "Cannot update consumer, services not initialized.");
            return;
        }

        mStorageService.putDeviceToken(deviceToken);
        mStorageService.putPushEnabled(pushEnabled);

        String consumerId = mStorageService.getConsumerId();
        if (isBlank(consumerId)) {
            Log.w(TAG, "Cannot update consumer, consumerId is missing.");
            return;
        }

        UpdateConsumer request = new UpdateConsumer(deviceToken, pushEnabled);
        UpdateConsumerEndpoint endpoint = new UpdateConsumerEndpoint(consumerId, request);

        ServiceLocator.getExecutorService().execute(() -> {
            try {
                mApiService.executeRequest(endpoint, Void.class);
                Log.d(TAG, "Consumer update request sent.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to send consumer update request.", e);
            }
        });
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String ensureValue(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static boolean equalsNullable(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}