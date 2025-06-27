package com.appambit.sdk.core.utils;

public class StringUtils {

    public static boolean isNullOrBlank(String param) {
        return param == null || param.trim().isEmpty();
    }
}
