package com.appambit.sdk.utils;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DbParamsUtils {

    private DbParamsUtils() {}

    /**
     * Converts SQL params to a list. Returns null if there are no params.
     */
    @Nullable
    public static List<Object> toParamList(@Nullable Object[] params) {
        // From Java, a single null param arrives as a null array: treat it as one NULL.
        if (params == null) params = new Object[]{ null };
        return params.length > 0 ? new ArrayList<>(Arrays.asList(params)) : null;
    }
}
