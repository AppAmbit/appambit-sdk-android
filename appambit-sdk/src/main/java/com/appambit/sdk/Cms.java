package com.appambit.sdk;

import com.appambit.sdk.services.interfaces.ICmsQuery;
import org.json.JSONObject;

public class Cms {

    public static ICmsQuery<JSONObject> content(String contentType) {
        return new CmsQuery<>(contentType, JSONObject.class);
    }

    public static <T> ICmsQuery<T> content(String contentType, Class<T> modelClass) {
        return new CmsQuery<>(contentType, modelClass);
    }
}
