package com.appambit.sdk.core.models.responses;

import com.appambit.sdk.core.utils.JsonKey;

public class StartSessionResponse {
    @JsonKey("session_id")
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

}
