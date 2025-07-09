package com.appambit.sdk.core.api.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.analytics.SessionData;
import com.appambit.sdk.core.api.interfaces.IEndpoint;

public class EndSessionEndpoint extends BaseEndpoint implements IEndpoint {
    public EndSessionEndpoint(SessionData sessionData ) {
        this.setUrl("/session/end");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(sessionData);
    }
}