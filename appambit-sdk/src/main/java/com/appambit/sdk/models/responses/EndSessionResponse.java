package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;

public class EndSessionResponse {

    @JsonKey("session_id")
    private String sessionId;

    @JsonKey("consumer_id")
    private String consumerId;

    @JsonKey("started_at")
    private String startedAt;

    @JsonKey("end_at")
    private String endAt;


    public String getSessionId() {
        return sessionId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getEndAt() {
        return endAt;
    }

}
