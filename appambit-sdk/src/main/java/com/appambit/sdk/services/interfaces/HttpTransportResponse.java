package com.appambit.sdk.services.interfaces;

import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public final class HttpTransportResponse {
    private final Integer statusCode;
    private final byte[] body;
    private final Map<String, String> headers;
    private final Throwable error;

    public HttpTransportResponse(
            @Nullable Integer statusCode,
            @Nullable byte[] body,
            @Nullable Map<String, String> headers,
            @Nullable Throwable error) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers == null ? Collections.emptyMap() : headers;
        this.error = error;
    }

    @Nullable
    public Integer getStatusCode() {
        return statusCode;
    }

    @Nullable
    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }
}
