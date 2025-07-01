package com.appambit.sdk.analytics;

import com.appambit.sdk.core.models.analytics.EventEntity;
import com.appambit.sdk.core.utils.JsonKey;

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
