package com.appambit.sdk.core.models.responses;

import com.appambit.sdk.core.enums.ApiErrorType;

public class ApiResult<T> {
    public T data;
    public ApiErrorType errorType;
    public String message;

    public ApiResult(T _data, ApiErrorType _errorType, String _message)
    {
        data = _data;
        errorType = _errorType;
        message = _message;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(data, ApiErrorType.None, null);
    }
    public static <T> ApiResult<T> fail(ApiErrorType error, String message) {
        return new ApiResult<>(null, error, message);
    }
}