package com.appambit.sdk.services.endpoints;

import androidx.annotation.Nullable;
import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.utils.CloudCodeJson;
import java.util.LinkedHashMap;
import java.util.Map;
import com.appambit.sdk.utils.UrlQueryBuilder;

public final class CloudCodeEndpoint extends BaseEndpoint {
    private final String function;

    public CloudCodeEndpoint(
            String function,
            HttpMethodEnum method,
            @Nullable Map<String, String> query,
            @Nullable Map<String, Object> body,
            @Nullable Map<String, String> headers) {
        this.function = function;
        setUrl(buildPath(function, query));
        setMethod(method);
        try {
            setPayload(body == null ? null : CloudCodeJson.snapshotObject(body));
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid Cloud Code body", error);
        }
        setCustomHeader(headers == null ? null : new LinkedHashMap<>(headers));
    }

    public String getFunction() {
        return function;
    }

    private static String buildPath(String function, @Nullable Map<String, String> query) {
        return UrlQueryBuilder.append("/fn/" + UrlQueryBuilder.encode(function), query);
    }
}
