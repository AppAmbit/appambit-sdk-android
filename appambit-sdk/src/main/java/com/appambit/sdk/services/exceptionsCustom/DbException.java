package com.appambit.sdk.services.exceptionsCustom;

import com.appambit.sdk.enums.ApiErrorType;

public class DbException extends RuntimeException {
    private final ApiErrorType errorType;

    public DbException(ApiErrorType errorType, String message) {
        super(message != null ? message : errorType.name());
        this.errorType = errorType;
    }

    public DbException(String message) {
        super(message);
        this.errorType = ApiErrorType.Unknown;
    }

    public ApiErrorType getErrorType() {
        return errorType;
    }
}
