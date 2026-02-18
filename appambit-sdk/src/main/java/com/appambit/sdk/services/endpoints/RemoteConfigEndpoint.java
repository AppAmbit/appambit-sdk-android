package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class RemoteConfigEndpoint extends BaseEndpoint implements IEndpoint {
    public RemoteConfigEndpoint(String appVersion) {
        this.setUrl("/sdk/config?app_version="+appVersion);
        this.setMethod(HttpMethodEnum.GET);
    }
}
