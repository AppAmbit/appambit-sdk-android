package com.appambit.sdk.models.remoteConfigs;

import com.appambit.sdk.utils.JsonKey;

public class RemoteConfig {
    @JsonKey("key")
    private String key;

    @JsonKey("value")
    private String value;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
