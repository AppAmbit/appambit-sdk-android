package com.appambit.sdk.utils;

import java.math.BigInteger;

public class StringValidation {
    private static boolean isUInt(String s) {
        if (s == null) {
            return false;
        }

        String trimmed = s.trim();

        if (!trimmed.matches("^\\d+$")) {
            return false;
        }

        try {
            BigInteger value = new BigInteger(trimmed);

            BigInteger maxUInt = new BigInteger("18446744073709551615");
            return value.compareTo(BigInteger.ZERO) >= 0 && value.compareTo(maxUInt) <= 0;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isUIntNumber(String s) {
        return isUInt(s);
    }
}
