package com.appambit.sdk.core.models.analytics;

import com.appambit.sdk.core.utils.JsonKey;
import com.appambit.sdk.core.utils.annotations.CustomDateTimeFormat;

import java.util.Date;
import java.util.UUID;

public class EventEntity extends Event {

    @JsonKey("id")
    private UUID id;

    @JsonKey("created_at")
    @CustomDateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

