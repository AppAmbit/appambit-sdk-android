package com.appambit.sdk.core.models.logs;

import java.util.Date;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.UUID;

public class LogEntity extends Log {
    private UUID id;
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
