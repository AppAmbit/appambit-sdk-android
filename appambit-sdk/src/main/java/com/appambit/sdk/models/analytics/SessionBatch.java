package com.appambit.sdk.models.analytics;

import com.appambit.sdk.utils.JsonKey;

import java.util.Date;

public class SessionBatch {

    @JsonKey("id")
    private String id;

    @JsonKey("session_id")
    private String sessionId;

    @JsonKey("started_at")
    private Date startedAt;

    @JsonKey("ended_at")
    private Date endedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }
}
