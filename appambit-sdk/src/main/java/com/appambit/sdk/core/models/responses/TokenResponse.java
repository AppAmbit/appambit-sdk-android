package com.appambit.sdk.core.models.responses;

import com.appambit.sdk.core.utils.JsonKey;

public class TokenResponse {

    @JsonKey("id")
    private String id;
    @JsonKey("token")
    private String token;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}