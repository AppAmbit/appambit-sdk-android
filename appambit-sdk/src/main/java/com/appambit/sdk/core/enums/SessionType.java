package com.appambit.sdk.core.enums;

public enum SessionType {
    START,
    END;

    public static SessionType fromValue(String value) {
        try {
            return SessionType.valueOf(value.toLowerCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown SessionType: " + value);
        }
    }
}
