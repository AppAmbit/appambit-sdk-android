package com.appambit.sdk.models.db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class DbResponse {
    private List<DbResult> results = new ArrayList<>();
    private String requestId;

    private DbResponse() {}

    public static DbResponse fromJson(String jsonString) throws JSONException {
        DbResponse response = new DbResponse();
        JSONObject json = new JSONObject(jsonString);
        response.requestId = json.optString("request_id", null);
        JSONArray resultsArray = json.optJSONArray("results");
        if (resultsArray != null) {
            for (int i = 0; i < resultsArray.length(); i++) {
                response.results.add(DbResult.fromJson(resultsArray.getJSONObject(i)));
            }
        }
        return response;
    }

    public List<DbResult> getResults() { return results; }
    public String getRequestId() { return requestId; }

    public DbResult getFirst() {
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }
}
