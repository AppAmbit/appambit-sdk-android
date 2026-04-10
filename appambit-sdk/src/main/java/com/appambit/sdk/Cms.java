package com.appambit.sdk;

import com.appambit.sdk.services.interfaces.ICmsQuery;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import org.json.JSONObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Cms {
    private static Storable mStorageService;
    static final Set<String> mFetchedContentTypes = Collections.synchronizedSet(new HashSet<>());

    public static void initialize(Storable storageService) {
        mStorageService = storageService;
    }

    public static ICmsQuery<JSONObject> content(String contentType) {
        return new CmsQuery<>(contentType, JSONObject.class);
    }

    public static <T> ICmsQuery<T> content(String contentType, Class<T> modelClass) {
        return new CmsQuery<>(contentType, modelClass);
    }

    private static void resetFetchState(String contentType) {
        if (contentType != null) {
            mFetchedContentTypes.remove(contentType);
        } else {
            mFetchedContentTypes.clear();
        }
    }

    public static void clearCache(String contentType) {
        if (mStorageService != null) {
            mStorageService.deleteCmsEntry(contentType);
            resetFetchState(contentType);
        }
    }

    public static void clearAllCache() {
        if (mStorageService != null) {
            mStorageService.deleteAllCmsEntries();
            resetFetchState(null);
        }
    }
}
