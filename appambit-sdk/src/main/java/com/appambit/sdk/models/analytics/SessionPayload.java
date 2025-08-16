package com.appambit.sdk.models.analytics;

import com.appambit.sdk.utils.JsonKey;

import java.util.List;

public class SessionPayload {
    @JsonKey("sessions")
    private List<SessionBatch> sessions;

    public List<SessionBatch> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionBatch> sessions) {
        this.sessions = sessions;
    }
}
