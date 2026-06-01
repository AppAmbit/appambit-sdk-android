package com.appambit.sdk;

import android.util.Log;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.endpoints.CmsEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.ICmsQuery;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.JsonDeserializer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class CmsQuery<T> implements ICmsQuery<T> {
    private static final String TAG = "AppAmbitCMS";

    private static ApiService mApiService;
    private static ExecutorService mExecutorService;

    private final String contentType;
    private final Class<T> modelClass;

    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private String searchQuery;
    private int page = -1;
    private int perPage = -1;

    public static void initialize(ApiService apiService, ExecutorService executorService) {
        mApiService = apiService;
        mExecutorService = executorService;
    }

    public CmsQuery(String contentType, Class<T> modelClass) {
        this.contentType = contentType;
        this.modelClass = modelClass;
    }

    public static class CmsQueryResult<T> {
        private final AppAmbitTaskFuture<List<T>> future;

        CmsQueryResult(AppAmbitTaskFuture<List<T>> future) {
            this.future = future;
        }

        public void then(AppAmbitTaskFuture.Callback<List<T>> callback) {
            if (future != null) {
                future.then(callback);
            }
        }
    }

    @Override
    public ICmsQuery<T> search(String query) {
        String trimmed = query != null ? query.trim() : "";
        if (!trimmed.isEmpty()) {
            this.searchQuery = trimmed;
        }
        return this;
    }

    @Override
    public ICmsQuery<T> equals(String field, String value) {
        queryParams.put("filter[" + field + "]", value);
        return this;
    }

    @Override
    public ICmsQuery<T> notEquals(String field, String value) {
        queryParams.put("filter[" + field + "][neq]", value);
        return this;
    }

    @Override
    public ICmsQuery<T> contains(String field, String value) {
        queryParams.put("filter[" + field + "][contains]", value);
        return this;
    }

    @Override
    public ICmsQuery<T> startsWith(String field, String value) {
        queryParams.put("filter[" + field + "][starts_with]", value);
        return this;
    }

    @Override
    public ICmsQuery<T> greaterThan(String field, Number value) {
        queryParams.put("filter[" + field + "][gt]", String.valueOf(value));
        return this;
    }

    @Override
    public ICmsQuery<T> greaterThanOrEqual(String field, Number value) {
        queryParams.put("filter[" + field + "][gte]", String.valueOf(value));
        return this;
    }

    @Override
    public ICmsQuery<T> lessThan(String field, Number value) {
        queryParams.put("filter[" + field + "][lt]", String.valueOf(value));
        return this;
    }

    @Override
    public ICmsQuery<T> lessThanOrEqual(String field, Number value) {
        queryParams.put("filter[" + field + "][lte]", String.valueOf(value));
        return this;
    }

    @Override
    public ICmsQuery<T> inList(String field, List<String> values) {
        queryParams.put("filter[" + field + "][in]", joinValues(values));
        return this;
    }

    @Override
    public ICmsQuery<T> notInList(String field, List<String> values) {
        queryParams.put("filter[" + field + "][nin]", joinValues(values));
        return this;
    }

    @Override
    public ICmsQuery<T> orderByAscending(String field) {
        queryParams.put("sort", field);
        return this;
    }

    @Override
    public ICmsQuery<T> orderByDescending(String field) {
        queryParams.put("sort", "-" + field);
        return this;
    }

    @Override
    public ICmsQuery<T> getPage(int page) {
        this.page = page;
        return this;
    }

    @Override
    public ICmsQuery<T> getPerPage(int perPage) {
        this.perPage = perPage;
        return this;
    }

    @Override
    public CmsQueryResult<T> getList() {
        AppAmbitTaskFuture<List<T>> future = new AppAmbitTaskFuture<>();

        mExecutorService.execute(() -> {
            try {
                Map<String, String> params = buildQueryParams();
                CmsEndpoint endpoint = searchQuery != null
                        ? new CmsEndpoint(contentType, searchQuery, params)
                        : new CmsEndpoint(contentType, params);

                ApiResult<String> result = mApiService.executeRequest(endpoint, String.class);

                if (result == null || result.data == null || result.errorType != ApiErrorType.None) {
                    Log.w(TAG, "Empty or error response for: " + contentType);
                    future.complete(new ArrayList<>());
                    return;
                }

                List<T> items = parseResponse(result.data);
                future.complete(items);
            } catch (Exception e) {
                Log.e(TAG, "Error in getList for: " + contentType, e);
                future.complete(new ArrayList<>());
            }
        });

        return new CmsQueryResult<>(future);
    }

    private Map<String, String> buildQueryParams() {
        Map<String, String> params = new LinkedHashMap<>(queryParams);
        if (page > 0) params.put("page", String.valueOf(page));
        if (perPage > 0) params.put("per_page", String.valueOf(perPage));
        return params;
    }

    private List<T> parseResponse(String json) throws JSONException {
        JSONObject response = new JSONObject(json);
        JSONArray data = response.optJSONArray("data");
        if (data == null) return new ArrayList<>();

        List<T> results = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (modelClass == null || modelClass == Object.class || modelClass == JSONObject.class) {
                results.add((T) item);
            } else {
                results.add(JsonDeserializer.deserializeFromJSONStringContent(item, modelClass));
            }
        }
        return results;
    }

    private static String joinValues(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(values.get(i));
        }
        return sb.toString();
    }
}
