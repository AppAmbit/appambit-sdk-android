package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class RemoteConfigEndpoint extends BaseEndpoint implements IEndpoint {
    public RemoteConfigEndpoint() {
        this.setUrl("/sdk/config");
        this.setMethod(HttpMethodEnum.GET);
    }
}
