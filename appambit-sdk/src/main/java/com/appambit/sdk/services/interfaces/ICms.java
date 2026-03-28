package com.appambit.sdk.services.interfaces;

import org.json.JSONObject;
import java.util.concurrent.ExecutorService;

public interface ICms {

    void initialize(ApiService apiService, ExecutorService executorService, Storable storageService);

    ICmsQuery<JSONObject> content(String contentType);

    <T> ICmsQuery<T> content(String contentType, Class<T> modelClass);

    void clearCache(String contentType);

    void clearAllCache();
}
