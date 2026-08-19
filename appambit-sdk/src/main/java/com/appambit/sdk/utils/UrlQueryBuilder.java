package com.appambit.sdk.utils;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class UrlQueryBuilder {
    private UrlQueryBuilder() {}

    public static String append(String path, @Nullable Map<String, String> query) {
        if (query == null || query.isEmpty()) return path;

        List<String> keys = new ArrayList<>(query.keySet());
        Collections.sort(keys);
        StringBuilder result = new StringBuilder(path);
        result.append(path.contains("?") ? '&' : '?');
        boolean first = true;
        for (String key : keys) {
            if (key == null || query.get(key) == null) continue;
            if (!first) result.append('&');
            result.append(encode(key)).append('=').append(encode(query.get(key)));
            first = false;
        }
        return first ? path : result.toString();
    }

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException error) {
            throw new IllegalStateException("UTF-8 is not available", error);
        }
    }
}
