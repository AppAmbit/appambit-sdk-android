package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;

public class EventResponseData {

    @JsonKey("id")
    private String id;

    @JsonKey("key")
    private String key;

    @JsonKey("value")
    private String value;

    @JsonKey("count")
    private int count;

    @JsonKey("event_id")
    private int eventId;

    public EventResponseData() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }
}
