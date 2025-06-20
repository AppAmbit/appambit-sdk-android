package com.appambit.sdk.core.models.analytics;

import java.util.HashMap;
import java.util.Map;

public class Event {
    private String name;
    private Map<String, String> data = new HashMap<>();

    public Event() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}

