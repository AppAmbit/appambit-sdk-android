package com.appambit.sdk.models.cloudcode;

import androidx.annotation.Nullable;

public final class CloudCodeResult<T> {
    private final T data;
    private final int statusCode;
    private final String requestId;

    public CloudCodeResult(@Nullable T data, int statusCode, @Nullable String requestId) {
        this.data = data;
        this.statusCode = statusCode;
        this.requestId = requestId;
    }

    @Nullable
    public T getData() {
        return data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }
}
