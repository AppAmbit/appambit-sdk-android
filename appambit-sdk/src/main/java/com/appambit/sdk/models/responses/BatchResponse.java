package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;

public class BatchResponse {
    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    @JsonKey("message")
    private String Message;
}
