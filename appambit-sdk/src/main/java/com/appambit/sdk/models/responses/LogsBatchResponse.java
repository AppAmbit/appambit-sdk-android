package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;

public class LogsBatchResponse {
    @JsonKey("message")
    private String message;
    @JsonKey("status")
    private String status;
    @JsonKey("jobUuid")
    private String jobUuid;

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public String getJobUuid() {
        return jobUuid;
    }
}
