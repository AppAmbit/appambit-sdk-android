package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;

public class TokenResponse {

    @JsonKey("id")
    private String consumerId;
    @JsonKey("token")
    private String token;

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String id) {
        this.consumerId = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}