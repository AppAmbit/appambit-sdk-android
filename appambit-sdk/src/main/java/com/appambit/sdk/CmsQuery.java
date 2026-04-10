package com.appambit.sdk;

import android.util.Log;

import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.ICmsQuery;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.endpoints.CmsEndpoint;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.JsonDeserializer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class CmsQuery<T> implements ICmsQuery<T> {
    private static final String TAG = "AppAmbitCMS";

    private static ApiService mApiService;
    private static Storable mStorageService;
    private static ExecutorService mExecutorService;

    private final String contentType;
    private final Class<T> modelClass;
    
    static final Set<String> mRefreshInProgress = Collections.synchronizedSet(new HashSet<>());
    static final Map<String, Object> mFetchLocks = new HashMap<>();

    private final StringBuilder sqlClause = new StringBuilder();
    private final List<String> selectionArgs = new ArrayList<>();
    private String orderByClause;
    private int page = -1;
    private int perPage = -1;

    public static void initialize(ApiService apiService, ExecutorService executorService, Storable storageService) {
        mApiService = apiService;
        mExecutorService = executorService;
        mStorageService = storageService;
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

    private void addCondition(String field, String operator, String value) {
        if (sqlClause.length() > 0) sqlClause.append(" AND ");
        String lowerValue = value.toLowerCase();
        if (lowerValue.equals("true")) {
            sqlClause.append("json_extract(value, '$.").append(field).append("') ").append(operator).append(" 1");
        } else if (lowerValue.equals("false")) {
            sqlClause.append("json_extract(value, '$.").append(field).append("') ").append(operator).append(" 0");
        } else {
            sqlClause.append("json_extract(value, '$.").append(field).append("') ").append(operator).append(" ?");
            selectionArgs.add(value);
        }
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
    public ICmsQuery<T> equals(String field, String value) { 
        if ("true".equalsIgnoreCase(value)) {
            if (sqlClause.length() > 0) sqlClause.append(" AND ");
            sqlClause.append("json_extract(value, '$.").append(field).append("') = 1");
        } else if ("false".equalsIgnoreCase(value)) {
            if (sqlClause.length() > 0) sqlClause.append(" AND ");
            sqlClause.append("json_extract(value, '$.").append(field).append("') = 0");
        } else {
            addCondition(field, "=", value); 
        }
        return this; 
    }
    
    @Override
    public ICmsQuery<T> notEquals(String field, String value) { 
        if ("true".equalsIgnoreCase(value)) {
            if (sqlClause.length() > 0) sqlClause.append(" AND ");
            sqlClause.append("json_extract(value, '$.").append(field).append("') != 1");
        } else if ("false".equalsIgnoreCase(value)) {
            if (sqlClause.length() > 0) sqlClause.append(" AND ");
            sqlClause.append("json_extract(value, '$.").append(field).append("') != 0");
        } else {
            addCondition(field, "!=", value); 
        }
        return this; 
    }

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
        appendListCondition(field, values, false);
        return this;
    }

    @Override
    public ICmsQuery<T> notInList(String field, List<String> values) {
        if (sqlClause.length() > 0) sqlClause.append(" AND ");
        appendListCondition(field, values, true);
        return this;
    }

    private void appendListCondition(String field, List<String> values, boolean negate) {
        List<String> plainValues = new ArrayList<>();
        List<JSONObject> jsonValues = new ArrayList<>();

        for (String val : values) {
            try {
                jsonValues.add(new JSONObject(val));
            } catch (JSONException e) {
                plainValues.add(val);
            }
        }

        if (!negate) {
            List<String> orClauses = new ArrayList<>();

            if (!plainValues.isEmpty()) {
                StringBuilder inClause = new StringBuilder("(");
                inClause.append("json_extract(value, '$.").append(field).append("') IN (");
                for (int i = 0; i < plainValues.size(); i++) {
                    inClause.append("?");
                    if (i < plainValues.size() - 1) inClause.append(",");
                    selectionArgs.add(plainValues.get(i));
                }
                inClause.append(")");

                inClause.append(" OR (json_type(value, '$.").append(field).append("') = 'array' AND EXISTS (")
                        .append("SELECT 1 FROM json_each(json_each.value, '$.").append(field).append("') AS je WHERE je.value IN (");
                for (int i = 0; i < plainValues.size(); i++) {
                    inClause.append("?");
                    if (i < plainValues.size() - 1) inClause.append(",");
                    selectionArgs.add(plainValues.get(i));
                }
                inClause.append("))))");

                orClauses.add(inClause.toString());
            }

            for (JSONObject jsonObj : jsonValues) {
                String objStr = jsonObj.toString();
                orClauses.add("(json_extract(value, '$." + field + "') = json(?) OR " +
                              "(json_type(value, '$." + field + "') = 'array' AND EXISTS (" +
                              "SELECT 1 FROM json_each(json_each.value, '$." + field + "') AS je WHERE je.value = json(?))))");
                selectionArgs.add(objStr);
                selectionArgs.add(objStr);
            }

            if (orClauses.isEmpty()) { sqlClause.append("1 = 0"); return; }

            if (orClauses.size() == 1) {
                sqlClause.append(orClauses.get(0));
            } else {
                sqlClause.append("(");
                for (int i = 0; i < orClauses.size(); i++) {
                    if (i > 0) sqlClause.append(" OR ");
                    sqlClause.append(orClauses.get(i));
                }
                sqlClause.append(")");
            }

        } else {
            List<String> andClauses = new ArrayList<>();

            if (!plainValues.isEmpty()) {
                StringBuilder notInClause = new StringBuilder("(");
                notInClause.append("json_extract(value, '$.").append(field).append("') NOT IN (");
                for (int i = 0; i < plainValues.size(); i++) {
                    notInClause.append("?");
                    if (i < plainValues.size() - 1) notInClause.append(",");
                    selectionArgs.add(plainValues.get(i));
                }
                notInClause.append(")");

                notInClause.append(" AND (json_type(value, '$.").append(field).append("') != 'array' OR NOT EXISTS (")
                           .append("SELECT 1 FROM json_each(json_each.value, '$.").append(field).append("') AS je WHERE je.value IN (");
                for (int i = 0; i < plainValues.size(); i++) {
                    notInClause.append("?");
                    if (i < plainValues.size() - 1) notInClause.append(",");
                    selectionArgs.add(plainValues.get(i));
                }
                notInClause.append("))))");

                andClauses.add(notInClause.toString());
            }

            for (JSONObject jsonObj : jsonValues) {
                String objStr = jsonObj.toString();
                String extracted = "json_extract(value, '$." + field + "')";
                andClauses.add("((" + extracted + " IS NULL OR " + extracted + " != json(?)) AND " +
                               "(json_type(value, '$." + field + "') != 'array' OR NOT EXISTS (" +
                               "SELECT 1 FROM json_each(json_each.value, '$." + field + "') AS je WHERE je.value = json(?))))");
                selectionArgs.add(objStr);
                selectionArgs.add(objStr);
            }

            if (andClauses.isEmpty()) { sqlClause.append("1 = 1"); return; }

            if (andClauses.size() == 1) {
                sqlClause.append(andClauses.get(0));
            } else {
                sqlClause.append("(");
                for (int i = 0; i < andClauses.size(); i++) {
                    if (i > 0) sqlClause.append(" AND ");
                    sqlClause.append(andClauses.get(i));
                }
                sqlClause.append(")");
            }
        }
    }


    @Override
    public ICmsQuery<T> orderByAscending(String field) {
        this.orderByClause = "CASE WHEN CAST(json_extract(value, '$." + field + "') AS REAL) != 0 OR json_extract(value, '$." + field + "') IN (0, '0', '0.0') THEN CAST(json_extract(value, '$." + field + "') AS REAL) ELSE NULL END ASC, lower(json_extract(value, '$." + field + "')) ASC";
        return this;
    }

    @Override
    public ICmsQuery<T> orderByDescending(String field) {
        this.orderByClause = "CASE WHEN CAST(json_extract(value, '$." + field + "') AS REAL) != 0 OR json_extract(value, '$." + field + "') IN (0, '0', '0.0') THEN CAST(json_extract(value, '$." + field + "') AS REAL) ELSE NULL END DESC, lower(json_extract(value, '$." + field + "')) DESC";
        return this;
    }

    @Override
    public ICmsQuery<T> getPage(int page) { this.page = page; return this; }
    @Override
    public ICmsQuery<T> getPerPage(int perPage) { this.perPage = perPage; return this; }

    @Override
    public CmsQueryResult<T> getList() {
        if (this.page == 0 || this.perPage == 0) {
            AppAmbitTaskFuture<List<T>> future = new AppAmbitTaskFuture<>();
            future.complete(new ArrayList<>());
            return new CmsQueryResult<>(future);
        }

        int limit = perPage > 0 ? perPage : Integer.MAX_VALUE;
        int offset = (page > 0 && perPage > 0) ? (page - 1) * perPage : 0;

        AppAmbitTaskFuture<List<T>> future = new AppAmbitTaskFuture<>();

        boolean alreadyFetched;
        synchronized (Cms.mFetchedContentTypes) {
            alreadyFetched = Cms.mFetchedContentTypes.contains(contentType);
        }

        mExecutorService.execute(() -> {
            try {
                if (alreadyFetched) {
                    List<T> cached = queryLocalCache(orderByClause, limit, offset);
                    future.complete(cached != null ? cached : new ArrayList<>());
                    return;
                }

                Object lock;
                synchronized (mFetchLocks) {
                    lock = mFetchLocks.get(contentType);
                    if (lock == null) {
                        lock = new Object();
                        mFetchLocks.put(contentType, lock);
                    }
                }
                
                synchronized (lock) {
                    boolean reFetched;
                    synchronized (Cms.mFetchedContentTypes) {
                        reFetched = Cms.mFetchedContentTypes.contains(contentType);
                    }
                    if (reFetched) {
                        List<T> cached = queryLocalCache(orderByClause, limit, offset);
                        future.complete(cached != null ? cached : new ArrayList<>());
                        return;
                    }

                    List<T> cached = queryLocalCache(orderByClause, limit, offset);
                    if (cached == null || cached.isEmpty()) {
                        fetchRemoteDataSync();
                        List<T> fresh = queryLocalCache(orderByClause, limit, offset);
                        future.complete(fresh != null ? fresh : new ArrayList<>());
                    } else {
                        synchronized (Cms.mFetchedContentTypes) {
                            Cms.mFetchedContentTypes.add(contentType);
                        }
                        future.complete(cached);
                        refreshCacheInBackground();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in getList for: " + contentType, e);
                future.complete(new ArrayList<>());
            }
        });

        return new CmsQueryResult<>(future);
    }

    private List<T> queryLocalCache(String orderByClause, int limit, int offset) throws JSONException {
        List<String> jsonResults = mStorageService.queryCmsData(
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
            ApiResult<String> firstResult = mApiService.executeRequest(new CmsEndpoint(contentType, page), String.class);

            if (firstResult == null || firstResult.data == null) return null;

            JSONObject firstJsonResponse = new JSONObject(firstResult.data);
            if (!firstJsonResponse.has("data")) return firstResult.data;

            JSONArray allData = firstJsonResponse.optJSONArray("data");
            if (allData == null) {
                return firstResult.data;
            }

            if (firstJsonResponse.has("meta")) {
                JSONObject meta = firstJsonResponse.getJSONObject("meta");
                int lastPage = meta.optInt("last_page", 1);

                for (int p = 2; p <= lastPage; p++) {
                    Log.d(TAG, "Fetching parallel/next page " + p + " for " + contentType);
                    ApiResult<String> nextPageResult = mApiService.executeRequest(new CmsEndpoint(contentType, p), String.class);
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
                Cms.mFetchedContentTypes.add(contentType);
                String localJson = mStorageService.getCmsData(contentType);
                if (localJson == null || !localJson.equals(remoteJson)) {
                    mStorageService.putCmsData(contentType, remoteJson);
                    Log.d(TAG, "CMS data stored (sync) for: " + contentType);
                }
            } else {
                Log.w(TAG, "Empty response on sync fetch for: " + contentType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on sync fetch for: " + contentType, e);
        }
    }

    private List<T> fetchAndReturn(String orderByClause, int limit, int offset) throws JSONException {
        fetchRemoteDataSync();
        List<T> result = queryLocalCache(orderByClause, limit, offset);
        return result != null ? result : new ArrayList<>();
    }

    private void refreshCacheInBackground() {
        synchronized (mRefreshInProgress) {
            if (mRefreshInProgress.contains(contentType)) {
                Log.d(TAG, "Refresh already running for: " + contentType + " — skip");
                return;
            }
            mRefreshInProgress.add(contentType);
        }
        mExecutorService.execute(() -> {
            try {
                String remoteJson = fetchAllRemoteDataSync();
                if (remoteJson != null) {
                    Cms.mFetchedContentTypes.add(contentType);
                    String localJson = mStorageService.getCmsData(contentType);
                    if (localJson == null || !localJson.equals(remoteJson)) {
                        mStorageService.putCmsData(contentType, remoteJson);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error on refresh for: " + contentType, e);
            } finally {
                synchronized (mRefreshInProgress) {
                    mRefreshInProgress.remove(contentType);
                }
            }
        });
    }
}
