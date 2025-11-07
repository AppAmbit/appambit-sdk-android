package com.appambit.sdk.models.breadcrumbs;

import com.appambit.sdk.utils.Identifiable;
import com.appambit.sdk.utils.JsonKey;
import com.appambit.sdk.utils.annotations.CustomDateTimeFormat;

import java.util.Date;
import java.util.UUID;

public class BreadcrumbEntity extends Breadcrumb implements Identifiable {
    @JsonKey("id")
    private UUID id;

    @JsonKey("session_id")
    private String sessionId;

    @JsonKey("created_at")
    @CustomDateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
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
