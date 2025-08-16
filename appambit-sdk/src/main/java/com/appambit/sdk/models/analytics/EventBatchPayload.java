package com.appambit.sdk.models.analytics;

import com.appambit.sdk.utils.JsonKey;

import java.util.List;

public class EventBatchPayload {

    @JsonKey("events")
    private List<EventEntity> events;

    public List<EventEntity> getEvents() {
        return events;
    }

    public void setEvents(List<EventEntity> events) {
        this.events = events;
    }
}
