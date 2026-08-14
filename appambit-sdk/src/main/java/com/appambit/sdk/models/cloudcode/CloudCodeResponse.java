package com.appambit.sdk.models.cloudcode;

import androidx.annotation.Nullable;

public final class CloudCodeResponse {
    private final Object data;
    private final int statusCode;
    private final String requestId;

    public CloudCodeResponse(@Nullable Object data, int statusCode, @Nullable String requestId) {
        this.data = data;
        this.statusCode = statusCode;
        this.requestId = requestId;
    }

    @Nullable
    public Object getData() {
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
