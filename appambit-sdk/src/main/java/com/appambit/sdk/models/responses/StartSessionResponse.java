package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;

public class StartSessionResponse {
    @JsonKey("session_id")
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

}
