package com.appambit.sdk.models.remoteConfigs;

import com.appambit.sdk.utils.JsonKey;
import java.util.UUID;

public class RemoteConfigEntity extends RemoteConfig {
    @JsonKey("id")
    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
