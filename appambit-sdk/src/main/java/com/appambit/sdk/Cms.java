package com.appambit.sdk;

import com.appambit.sdk.services.interfaces.ICmsQuery;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class Cms {
    static ApiService mApiService;
    static Storable mStorageService;
    static ExecutorService mExecutorService;

    static final Set<String> mFetchedContentTypes = new HashSet<>();

    public static void initialize(ApiService apiService, ExecutorService executorService, Storable storageService) {
        mApiService = apiService;
        mExecutorService = executorService;
        mStorageService = storageService;
    }

    public static ICmsQuery<JSONObject> content(String contentType) {
        return new CmsQuery<>(contentType, JSONObject.class);
    }

    public static <T> ICmsQuery<T> content(String contentType, Class<T> modelClass) {
        return new CmsQuery<>(contentType, modelClass);
    }

    public static void clearCache(String contentType) {
        if (mStorageService != null) {
            mStorageService.deleteCmsEntry(contentType);
        }
    }

    public static void clearAllCache() {
        if (mStorageService != null) {
            mStorageService.deleteAllCmsEntries();
        }
    }
}
