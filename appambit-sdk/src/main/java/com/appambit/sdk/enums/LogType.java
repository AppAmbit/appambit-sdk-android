package com.appambit.sdk.enums;

public enum LogType {
    ERROR("error"),
    CRASH("crash");

    private final String value;

    LogType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LogType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        for (LogType type : LogType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown LogType: " + value);
    }
}
