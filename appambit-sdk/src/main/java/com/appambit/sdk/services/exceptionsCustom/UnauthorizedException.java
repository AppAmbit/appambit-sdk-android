package com.appambit.sdk.services.exceptionsCustom;

public class UnauthorizedException extends Exception {
    public UnauthorizedException() {
        super("Unauthorized");
    }
}