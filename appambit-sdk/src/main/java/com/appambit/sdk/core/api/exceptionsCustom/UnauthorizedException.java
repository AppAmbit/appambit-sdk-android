package com.appambit.sdk.core.api.exceptionsCustom;

public class UnauthorizedException extends Exception {
    public UnauthorizedException() {
        super("Unauthorized");
    }
}