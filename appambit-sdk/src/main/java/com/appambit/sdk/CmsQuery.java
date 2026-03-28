package com.appambit.sdk;

import android.util.Log;

import com.appambit.sdk.services.interfaces.ICmsQuery;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.endpoints.CmsEndpoint;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.JsonDeserializer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CmsQuery<T> implements ICmsQuery<T> {
    private static final String TAG = "AppAmbitCMS";

    private final String contentType;
    private final Class<T> modelClass;

    private final StringBuilder sqlClause = new StringBuilder();
    private final List<String> selectionArgs = new ArrayList<>();
    private String orderByClause;
    private int page = 1;
    private int perPage = 20;

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

    private void addCondition(String field, String operator, String value) {
        if (sqlClause.length() > 0) sqlClause.append(" AND ");
        sqlClause.append("json_extract(value, '$.").append(field).append("') ").append(operator).append(" ?");
        selectionArgs.add(value);
    }

    private void addNumericCondition(String field, String operator, Number value) {
        if (sqlClause.length() > 0) sqlClause.append(" AND ");
        sqlClause.append("CAST(json_extract(value, '$.").append(field).append("') AS REAL) ").append(operator).append(" ?");
        selectionArgs.add(String.valueOf(value));
    }

    @Override
    public ICmsQuery<T> search(String query) {
        String trimmed = query != null ? query.trim() : "";
        if (!trimmed.isEmpty()) {
            if (sqlClause.length() > 0) sqlClause.append(" AND ");
            sqlClause.append("value LIKE ?");
            selectionArgs.add("%" + trimmed + "%");
        }
        return this;
    }

    @Override
    public ICmsQuery<T> equals(String field, String value) { addCondition(field, "=", value); return this; }
    @Override
    public ICmsQuery<T> notEquals(String field, String value) { addCondition(field, "!=", value); return this; }
    @Override
    public ICmsQuery<T> contains(String field, String value) { addCondition(field, "LIKE", "%" + value + "%"); return this; }
    @Override
    public ICmsQuery<T> startsWith(String field, String value) { addCondition(field, "LIKE", value + "%"); return this; }
    @Override
    public ICmsQuery<T> greaterThan(String field, Number value) { addNumericCondition(field, ">", value); return this; }
    @Override
    public ICmsQuery<T> greaterThanOrEqual(String field, Number value) { addNumericCondition(field, ">=", value); return this; }
    @Override
    public ICmsQuery<T> lessThan(String field, Number value) { addNumericCondition(field, "<", value); return this; }
    @Override
    public ICmsQuery<T> lessThanOrEqual(String field, Number value) { addNumericCondition(field, "<=", value); return this; }

    @Override
    public ICmsQuery<T> inList(String field, List<String> values) {
        if (sqlClause.length() > 0) sqlClause.append(" AND ");
        sqlClause.append("json_extract(value, '$.").append(field).append("') IN (");
        for (int i = 0; i < values.size(); i++) { sqlClause.append("?"); if (i < values.size() - 1) sqlClause.append(","); selectionArgs.add(values.get(i)); }
        sqlClause.append(")"); return this;
    }

    @Override
    public ICmsQuery<T> notInList(String field, List<String> values) {
        if (sqlClause.length() > 0) sqlClause.append(" AND ");
        sqlClause.append("json_extract(value, '$.").append(field).append("') NOT IN (");
        for (int i = 0; i < values.size(); i++) { sqlClause.append("?"); if (i < values.size() - 1) sqlClause.append(","); selectionArgs.add(values.get(i)); }
        sqlClause.append(")"); return this;
    }

    @Override
    public ICmsQuery<T> orderByAscending(String field) {
        this.orderByClause = "json_extract(value, '$." + field + "') ASC";
        return this;
    }

    @Override
    public ICmsQuery<T> orderByDescending(String field) {
        this.orderByClause = "json_extract(value, '$." + field + "') DESC";
        return this;
    }

    @Override
    public ICmsQuery<T> getPage(int page) { this.page = page; return this; }
    @Override
    public ICmsQuery<T> getPerPage(int perPage) { this.perPage = perPage; return this; }

    @Override
    public CmsQueryResult<T> getList() throws Exception {
        int limit = perPage;
        int offset = (page - 1) * perPage;

        synchronized (Cms.mFetchedContentTypes) {
            if (Cms.mFetchedContentTypes.contains(contentType)) {
                Log.d(TAG, "Session cache hit for: " + contentType + " – skipping remote fetch");
                List<T> cached = queryLocalCache(orderByClause, limit, offset);
                AppAmbitTaskFuture<List<T>> done = new AppAmbitTaskFuture<>();
                done.complete(cached != null ? cached : new ArrayList<>());
                return new CmsQueryResult<>(done);
            }
            Cms.mFetchedContentTypes.add(contentType);
        }

        List<T> cached = queryLocalCache(orderByClause, limit, offset);
        AppAmbitTaskFuture<List<T>> future = new AppAmbitTaskFuture<>();

        if (cached == null || cached.isEmpty()) {
            Log.d(TAG, "Cache empty for: " + contentType + " – fetching synchronously");
            List<T> freshData = fetchAndReturn(orderByClause, limit, offset);
            future.complete(freshData);
        } else {
            Log.d(TAG, "Cache hit for: " + contentType + " – silent background refresh");
            future.complete(cached);
            refreshCacheInBackground();
        }

        return new CmsQueryResult<>(future);
    }

    private List<T> queryLocalCache(String orderByClause, int limit, int offset) throws Exception {
        List<String> jsonResults = Cms.mStorageService.queryCmsData(
                contentType,
                sqlClause.toString(),
                selectionArgs.toArray(new String[0]),
                orderByClause,
                limit,
                offset);

        if (jsonResults == null || jsonResults.isEmpty()) return null;

        List<T> results = new ArrayList<>();
        for (String json : jsonResults) {
            JSONObject jsonObject = new JSONObject(json);
            if (modelClass == null || modelClass == Object.class) {
                results.add((T) jsonObject);
            } else {
                results.add(JsonDeserializer.deserializeFromJSONStringContent(jsonObject, modelClass));
            }
        }
        return results;
    }

    private String fetchAllRemoteDataSync() {
        try {
            int page = 1;
            int perPage = 20;
            ApiResult<String> firstResult = Cms.mApiService.executeRequest(new CmsEndpoint(contentType, page, perPage), String.class);

            if (firstResult == null || firstResult.data == null) return null;

            JSONObject firstJsonResponse = new JSONObject(firstResult.data);
            if (!firstJsonResponse.has("data")) return firstResult.data;

            JSONArray allData = firstJsonResponse.getJSONArray("data");

            if (firstJsonResponse.has("meta")) {
                JSONObject meta = firstJsonResponse.getJSONObject("meta");
                int total = meta.optInt("total", 0);
                int totalPages = (int) Math.ceil((double) total / perPage);

                for (int p = 2; p <= totalPages; p++) {
                    Log.d(TAG, "Fetching parallel/next page " + p + " for " + contentType);
                    ApiResult<String> nextPageResult = Cms.mApiService.executeRequest(new CmsEndpoint(contentType, p, perPage), String.class);
                    if (nextPageResult != null && nextPageResult.data != null) {
                        JSONObject nextPageJson = new JSONObject(nextPageResult.data);
                        if (nextPageJson.has("data")) {
                            JSONArray nextPageData = nextPageJson.getJSONArray("data");
                            for (int i = 0; i < nextPageData.length(); i++) {
                                allData.put(nextPageData.get(i));
                            }
                        }
                    }
                }
            }

            firstJsonResponse.put("data", allData);
            return firstJsonResponse.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error fetching all remote data for: " + contentType, e);
            return null;
        }
    }

    private void fetchRemoteDataSync() {
        try {
            String remoteJson = fetchAllRemoteDataSync();
            if (remoteJson != null) {
                String localJson = Cms.mStorageService.getCmsData(contentType);
                if (localJson == null || !localJson.equals(remoteJson)) {
                    Cms.mStorageService.putCmsData(contentType, remoteJson);
                    Log.d(TAG, "CMS data stored (sync) for: " + contentType);
                }
            } else {
                Log.w(TAG, "Empty response on sync fetch for: " + contentType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on sync fetch for: " + contentType, e);
        }
    }

    private List<T> fetchAndReturn(String orderByClause, int limit, int offset) throws Exception {
        fetchRemoteDataSync();
        List<T> result = queryLocalCache(orderByClause, limit, offset);
        return result != null ? result : new ArrayList<>();
    }

    private void refreshCacheInBackground() {
        Cms.mExecutorService.execute(() -> {
            try {
                String remoteJson = fetchAllRemoteDataSync();
                if (remoteJson != null) {
                    String localJson = Cms.mStorageService.getCmsData(contentType);
                    if (localJson == null || !localJson.equals(remoteJson)) {
                        Cms.mStorageService.putCmsData(contentType, remoteJson);
                        Log.d(TAG, "CMS cache updated for: " + contentType);
                    } else {
                        Log.d(TAG, "CMS cache unchanged for: " + contentType);
                    }
                } else {
                    Log.w(TAG, "Empty response on background refresh for: " + contentType);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error on background refresh for: " + contentType, e);
            }
        });
    }
}
