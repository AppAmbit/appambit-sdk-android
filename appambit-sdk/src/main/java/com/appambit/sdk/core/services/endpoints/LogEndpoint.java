package com.appambit.sdk.core.services.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.logs.Log;
import com.appambit.sdk.core.services.interfaces.IEndpoint;

public class LogEndpoint extends BaseEndpoint implements IEndpoint {
    public LogEndpoint(Log log) {
        this.setUrl("/log");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(log);
    }
}
