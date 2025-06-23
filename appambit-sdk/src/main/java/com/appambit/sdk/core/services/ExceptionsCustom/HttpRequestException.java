package com.appambit.sdk.core.services.ExceptionsCustom;

public class HttpRequestException extends Exception {
    public HttpRequestException(String message) {
        super(message);
    }
}