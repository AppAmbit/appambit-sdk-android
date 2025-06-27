package com.appambit.sdk.core.models.responses;

import com.appambit.sdk.core.utils.JsonKey;

public class EventsBatchResponse {

    @JsonKey("message")
    private String message;

    @JsonKey("status")
    private String status;

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonKey("jobUuid")
    private String jobUuid

            ;

}
