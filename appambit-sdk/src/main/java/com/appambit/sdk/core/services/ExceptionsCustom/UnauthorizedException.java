package com.appambit.sdk.core.services.ExceptionsCustom;

public class UnauthorizedException extends Exception {
    public UnauthorizedException() {
        super("Unauthorized");
    }
}