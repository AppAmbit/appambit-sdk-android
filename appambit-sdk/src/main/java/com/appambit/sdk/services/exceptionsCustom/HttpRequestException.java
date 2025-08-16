package com.appambit.sdk.services.exceptionsCustom;

public class HttpRequestException extends Exception {
    public HttpRequestException(String message) {
        super(message);
    }
}