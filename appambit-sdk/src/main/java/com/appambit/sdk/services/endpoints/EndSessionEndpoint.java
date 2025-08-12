package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class EndSessionEndpoint extends BaseEndpoint implements IEndpoint {
    public EndSessionEndpoint(SessionData sessionData ) {
        this.setUrl("/session/end");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(sessionData);
    }
}