package com.appambit.sdk.core.models.analytics;

import android.util.Log;

import com.appambit.sdk.core.utils.JsonKey;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Event {

    @JsonKey("name")
    private String name;
    private String dataJson = "{}";
    @JsonKey("metadata")
    private Map<String, String> data = new HashMap<>();

    public Map<String, String> getData() {
        try {
            JSONObject jsonObject = new JSONObject(dataJson);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                this.data.put(key, jsonObject.getString(key));
            }
        } catch (JSONException e) {
            Log.d("EventEntity", e.toString());
        }
        return this.data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            Log.d("EventEntity", e.toString());
        }
        this.dataJson = jsonObject.toString();
    }
    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        try {
            JSONObject jsonObject = new JSONObject(dataJson);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                this.data.put(key, jsonObject.getString(key));
            }
        } catch (JSONException e) {
            Log.d("EventEntity", e.toString());
        }
        this.dataJson = dataJson;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

