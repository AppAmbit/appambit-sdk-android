package com.appambit.sdk.core.models.logs;

import com.appambit.sdk.core.utils.JsonKey;

import java.util.Date;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.UUID;

public class LogEntity extends Log {
    @JsonKey("id")
    private UUID id;
    @JsonKey("created_at")
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
