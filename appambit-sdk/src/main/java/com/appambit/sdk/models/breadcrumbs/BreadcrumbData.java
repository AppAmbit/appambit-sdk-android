package com.appambit.sdk.models.breadcrumbs;

import com.appambit.sdk.utils.Identifiable;
import com.appambit.sdk.utils.JsonKey;

import java.util.Date;
import java.util.UUID;

public class BreadcrumbData implements Identifiable {
    @JsonKey("id")
    private UUID id;

    @JsonKey("session_id")
    private String sessionId;

    @JsonKey("created_at")
    private Date timestamp;

    @JsonKey("name")
    private String name;

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
