package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class EventResponse {

    @JsonKey("id")
    private int id;

    @JsonKey("name")
    private String name;

    @JsonKey("count")
    private int count;

    @JsonKey("consumer_id")
    private int consumerId;

    @JsonKey("created_at")
    private Date createdAt;

    @JsonKey("updated_at")
    private Date updatedAt;

    @JsonKey("event_data")
    private List<EventResponseData> eventData = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(int consumerId) {
        this.consumerId = consumerId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<EventResponseData> getEventData() {
        return eventData;
    }

    public void setEventData(List<EventResponseData> eventData) {
        this.eventData = eventData;
    }
}
