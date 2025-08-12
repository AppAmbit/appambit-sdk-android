package com.appambit.sdk.services;

import android.nfc.Tag;
import android.util.Log;

import com.appambit.sdk.Analytics;
import com.appambit.sdk.ServiceLocator;
import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.app.Consumer;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.models.responses.TokenResponse;
import com.appambit.sdk.services.endpoints.RegisterEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.AppInfoService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;

import java.util.UUID;

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

    private static RegisterEndpoint buildRegisterEndpoint(String appKey) {
        String appId = null;
        String deviceId = "";
        String userId = "";
        String userEmail = null;

        try {
            if (mStorageService == null || mAppInfoService == null) {
                return new RegisterEndpoint(new Consumer(
                        "", "", "", "", null, "", "", ""
                ));
            }

            deviceId = mStorageService.getDeviceId();
            userId = mStorageService.getUserId();

            String emailFromStorage = mStorageService.getUserEmail();
            userEmail = (emailFromStorage == null || emailFromStorage.trim().isEmpty())
                    ? null
                    : emailFromStorage.trim();

            String storedAppKey = mStorageService.getAppId();

            if (!equalsNullable(storedAppKey, appKey)) {
                mStorageService.putConsumerId("");
            }

            if (!isBlank(appKey)) {
                appId = appKey;
                mStorageService.putAppId(appKey);
            } else {
                appId = nvl(storedAppKey, "");
            }

            if (isBlank(deviceId)) {
                deviceId = java.util.UUID.randomUUID().toString();
                mStorageService.putDeviceId(deviceId);
            }

            if (isBlank(userId)) {
                userId = java.util.UUID.randomUUID().toString();
                mStorageService.putUserId(userId);
            }

        } catch (Exception ex) {
            System.out.println("[ConsumerService] Error getting data for ConsumerService: " + ex);
        }

        Consumer consumer = new Consumer(
                nvl(appId, ""),
                nvl(deviceId, ""),
                mAppInfoService != null ? nvl(mAppInfoService.getDeviceModel(), "") : "",
                nvl(userId, ""),
                // userEmail es null si venía vacío/en blanco
                userEmail,
                mAppInfoService != null ? nvl(mAppInfoService.getOs(), "") : "",
                mAppInfoService != null ? nvl(mAppInfoService.getCountry(), "") : "",
                mAppInfoService != null ? nvl(mAppInfoService.getLanguage(), "") : ""
        );

        return new RegisterEndpoint(consumer);
    }

    public static AppAmbitTaskFuture<ApiErrorType> createConsumer(final String appKey) {
        final AppAmbitTaskFuture<ApiErrorType> promise = new AppAmbitTaskFuture<>();

        if (mApiService == null) {
            promise.complete(ApiErrorType.Unknown);
            return promise;
        }

        final RegisterEndpoint registerEndpoint = buildRegisterEndpoint(appKey);

        ServiceLocator.getExecutorService().execute(() -> {
            try {
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nvl(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static boolean equalsNullable(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
