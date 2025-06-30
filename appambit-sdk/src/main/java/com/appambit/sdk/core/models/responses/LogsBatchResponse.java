package com.appambit.sdk.core.models.responses;

import com.appambit.sdk.core.utils.JsonKey;

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
