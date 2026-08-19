package com.appambit.sdk.models.cloudcode;

import androidx.annotation.Nullable;

public final class CloudCodeError extends Exception {
    public enum Code {
        NOT_INITIALIZED,
        INVALID_FUNCTION,
        INVALID_METHOD,
        INVALID_QUERY,
        INVALID_BODY,
        INVALID_HEADER,
        INVALID_RESPONSE_TYPE,
        CANCELLED,
        NETWORK_UNAVAILABLE,
        TIMED_OUT,
        INVALID_URL,
        TRANSPORT,
        DECODING,
        HTTP
    }

    private final Code code;
    private final String function;
    private final String header;
    private final int statusCode;
    private final Object body;
    private final String rawBody;
    private final String requestId;

    private CloudCodeError(
            Code code,
            String message,
            @Nullable String function,
            @Nullable String header,
            int statusCode,
            @Nullable Object body,
            @Nullable String rawBody,
            @Nullable String requestId,
            @Nullable Throwable cause) {
        super(message, cause);
        this.code = code;
        this.function = function;
        this.header = header;
        this.statusCode = statusCode;
        this.body = body;
        this.rawBody = rawBody;
        this.requestId = requestId;
    }

    public static CloudCodeError notInitialized() {
        return new CloudCodeError(Code.NOT_INITIALIZED,
                "Cloud Code is not initialized. Call AppAmbit.start() first.",
                null, null, -1, null, null, null, null);
    }

    public static CloudCodeError invalidFunction(String function) {
        return new CloudCodeError(Code.INVALID_FUNCTION,
                "Invalid Cloud Code function slug: " + function,
                function, null, -1, null, null, null, null);
    }

    public static CloudCodeError invalidMethod() {
        return new CloudCodeError(Code.INVALID_METHOD,
                "Cloud Code HTTP method is required.",
                null, null, -1, null, null, null, null);
    }

    public static CloudCodeError invalidQuery(String detail) {
        return new CloudCodeError(Code.INVALID_QUERY,
                "Invalid Cloud Code query parameter: " + detail,
                null, null, -1, null, null, null, null);
    }

    public static CloudCodeError invalidBody(Throwable cause) {
        return new CloudCodeError(Code.INVALID_BODY,
                "The Cloud Code body is not valid JSON.",
                null, null, -1, null, null, null, cause);
    }

    public static CloudCodeError invalidHeader(String header) {
        return new CloudCodeError(Code.INVALID_HEADER,
                "The Cloud Code header is reserved and cannot be overridden: " + header,
                null, header, -1, null, null, null, null);
    }

    public static CloudCodeError invalidResponseType() {
        return new CloudCodeError(Code.INVALID_RESPONSE_TYPE,
                "Cloud Code response type is required.",
                null, null, -1, null, null, null, null);
    }

    public static CloudCodeError cancelled() {
        return new CloudCodeError(Code.CANCELLED,
                "Cloud Code request was cancelled.",
                null, null, -1, null, null, null, null);
    }

    public static CloudCodeError networkUnavailable(@Nullable Throwable cause) {
        return new CloudCodeError(Code.NETWORK_UNAVAILABLE,
                "Cloud Code is unavailable because the network is offline.",
                null, null, -1, null, null, null, cause);
    }

    public static CloudCodeError timedOut(@Nullable Throwable cause) {
        return new CloudCodeError(Code.TIMED_OUT,
                "Cloud Code request timed out.",
                null, null, -1, null, null, null, cause);
    }

    public static CloudCodeError invalidUrl(@Nullable Throwable cause) {
        return new CloudCodeError(Code.INVALID_URL,
                "Cloud Code URL is invalid.",
                null, null, -1, null, null, null, cause);
    }

    public static CloudCodeError transport(String message, @Nullable Throwable cause) {
        return new CloudCodeError(Code.TRANSPORT,
                "Cloud Code network request failed: " + message,
                null, null, -1, null, null, null, cause);
    }

    public static CloudCodeError decoding(String message, @Nullable Throwable cause) {
        return new CloudCodeError(Code.DECODING,
                "Cloud Code response could not be decoded: " + message,
                null, null, -1, null, null, null, cause);
    }

    public static CloudCodeError http(
            int statusCode,
            @Nullable Object body,
            @Nullable String rawBody,
            @Nullable String requestId) {
        String message = "Cloud Code returned HTTP " + statusCode + ".";
        if (rawBody != null && !rawBody.isEmpty()) message += " Body: " + rawBody;
        if (requestId != null && !requestId.isEmpty()) message += " Request ID: " + requestId;
        return new CloudCodeError(Code.HTTP, message, null, null, statusCode,
                body, rawBody, requestId, null);
    }

    public Code getCode() {
        return code;
    }

    @Nullable
    public String getFunction() {
        return function;
    }

    @Nullable
    public String getHeader() {
        return header;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public Object getBody() {
        return body;
    }

    @Nullable
    public String getRawBody() {
        return rawBody;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }
}
