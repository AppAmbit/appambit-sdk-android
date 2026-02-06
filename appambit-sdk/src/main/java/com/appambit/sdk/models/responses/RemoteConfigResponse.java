package com.appambit.sdk.models.responses;

import com.appambit.sdk.utils.JsonKey;
import java.util.Map;

public class RemoteConfigResponse {

    @JsonKey("configs")
    private Map<String, Object> configs;

    public Map<String, Object> getConfigs() {
        return configs;
    }
}
