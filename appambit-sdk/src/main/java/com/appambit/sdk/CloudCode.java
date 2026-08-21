package com.appambit.sdk;

import androidx.annotation.Nullable;
import com.appambit.sdk.enums.CloudCodeHttpMethod;
import com.appambit.sdk.models.cloudcode.CloudCodeCancellationToken;
import com.appambit.sdk.models.cloudcode.CloudCodeError;
import com.appambit.sdk.models.cloudcode.CloudCodeRequest;
import com.appambit.sdk.models.cloudcode.CloudCodeResponse;
import com.appambit.sdk.models.cloudcode.CloudCodeResult;
import com.appambit.sdk.services.CloudCodeService;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import java.util.Map;

public final class CloudCode {
    private static volatile CloudCodeService service;

    private CloudCode() {}

    static void initialize(CloudCodeService cloudCodeService) {
        service = cloudCodeService;
    }

    public static CloudCodeRequest<CloudCodeResponse> call(String function) {
        return call(function, CloudCodeHttpMethod.POST, null, null, null);
    }

    public static CloudCodeRequest<CloudCodeResponse> call(
            String function,
            @Nullable Map<String, Object> body) {
        return call(function, CloudCodeHttpMethod.POST, null, body, null);
    }

    public static CloudCodeRequest<CloudCodeResponse> call(
            String function,
            CloudCodeHttpMethod method,
            @Nullable Map<String, String> query,
            @Nullable Map<String, Object> body,
            @Nullable Map<String, String> headers) {
        CloudCodeService current = service;
        if (current == null) return notInitializedUntyped();
        return current.call(function, method, query, body, headers);
    }

    public static <T> CloudCodeRequest<CloudCodeResult<T>> call(
            String function,
            CloudCodeHttpMethod method,
            @Nullable Map<String, String> query,
            @Nullable Map<String, Object> body,
            @Nullable Map<String, String> headers,
            Class<T> responseType) {
        CloudCodeService current = service;
        if (current == null) return notInitializedTyped();
        return current.callTyped(function, method, query, body, headers, responseType);
    }

    private static CloudCodeRequest<CloudCodeResponse> notInitializedUntyped() {
        AppAmbitTaskFuture<CloudCodeResponse> future = new AppAmbitTaskFuture<>();
        future.fail(CloudCodeError.notInitialized());
        return new CloudCodeRequest<>(future, new CloudCodeCancellationToken());
    }

    private static <T> CloudCodeRequest<CloudCodeResult<T>> notInitializedTyped() {
        AppAmbitTaskFuture<CloudCodeResult<T>> future = new AppAmbitTaskFuture<>();
        future.fail(CloudCodeError.notInitialized());
        return new CloudCodeRequest<>(future, new CloudCodeCancellationToken());
    }
}
