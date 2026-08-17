package com.appambit.sdk.models.cloudcode;

import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public final class CloudCodeResult<T> {
    private final T data;
    private final int statusCode;
    private final String requestId;
    private final Map<String, String> headers;

    public CloudCodeResult(@Nullable T data, int statusCode, @Nullable String requestId) {
        this(data, statusCode, requestId, Collections.emptyMap());
    }

    public CloudCodeResult(
            @Nullable T data,
            int statusCode,
            @Nullable String requestId,
            @Nullable Map<String, String> headers) {
        this.data = data;
        this.statusCode = statusCode;
        this.requestId = requestId;
        this.headers = headers == null ? Collections.emptyMap() : headers;
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

    public Map<String, String> getHeaders() {
        return headers;
    }
}
