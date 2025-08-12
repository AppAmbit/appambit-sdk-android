package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.logs.Log;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class LogEndpoint extends BaseEndpoint implements IEndpoint {
    public LogEndpoint(Log log) {
        this.setUrl("/log");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(log);
    }
}
