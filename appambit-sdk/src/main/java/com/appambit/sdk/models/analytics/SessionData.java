package com.appambit.sdk.models.analytics;

import com.appambit.sdk.enums.SessionType;
import com.appambit.sdk.utils.Identifiable;
import com.appambit.sdk.utils.JsonKey;

import java.util.Date;
import java.util.UUID;

public class SessionData implements Identifiable {
    @JsonKey("id")
    private UUID id;
    @JsonKey("session_id")
    private String sessionId;
    @JsonKey("timestamp")
    private Date timestamp;
    @JsonKey("session_type")
    private SessionType sessionType;

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
