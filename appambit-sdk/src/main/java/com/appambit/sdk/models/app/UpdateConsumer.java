package com.appambit.sdk.models.app;

import com.appambit.sdk.utils.JsonKey;

public class UpdateConsumer {

    @JsonKey("device_token")
    private final String deviceToken;

    @JsonKey("push_enabled")
    private final boolean pushEnabled;

    public UpdateConsumer(String deviceToken, boolean pushEnabled) {
        this.deviceToken = deviceToken;
        this.pushEnabled = pushEnabled;
    }
}
