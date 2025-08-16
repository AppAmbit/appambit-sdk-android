package com.appambit.sdk.models.app;

import com.appambit.sdk.utils.JsonKey;

public class ConsumerToken {

    public ConsumerToken(String appKey, String consumerId) {
        this.appKey = appKey;
        this.consumerId = consumerId;
    }

    @JsonKey("app_key")
    private String appKey;

    @JsonKey("consumer_id")
    private String consumerId;

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}