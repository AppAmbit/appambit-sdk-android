package com.appambit.sdk.models.remoteConfigs;

import com.appambit.sdk.utils.JsonKey;
import java.util.Map;

public class RemoteConfigModel {

    @JsonKey("configs")
    private Map<String, Object> configs;

    public Map<String, Object> getConfigs() {
        return configs;
    }
}
