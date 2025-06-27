package com.appambit.sdk.core.models.analytics;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.json.JSONException;

public class EventEntity extends Event {

    private UUID id;
    private String dataJson = "{}";
    private Date createdAt;

    public EventEntity() {
        super();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, String> getData() {
        Map<String, String> map = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(dataJson);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, jsonObject.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void setData(Map<String, String> data) {
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.dataJson = jsonObject.toString();
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}