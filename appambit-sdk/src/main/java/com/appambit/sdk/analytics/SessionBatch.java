package com.appambit.sdk.analytics;

import com.appambit.sdk.core.utils.JsonKey;

import java.util.Date;

public class SessionBatch {
    @JsonKey("started_at")
    private Date startedAt;

    @JsonKey("ended_at")
    private Date endedAt;

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }
}
