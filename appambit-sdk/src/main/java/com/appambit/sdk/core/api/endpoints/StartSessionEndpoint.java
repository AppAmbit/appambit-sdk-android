package com.appambit.sdk.core.api.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.analytics.SessionData;
import com.appambit.sdk.core.api.interfaces.IEndpoint;

import java.util.Date;

public class StartSessionEndpoint extends BaseEndpoint implements IEndpoint {
    public StartSessionEndpoint(Date dateNow) {
        this.setUrl("/session/start");
        this.setMethod(HttpMethodEnum.POST);
        SessionData sessionData = new SessionData();
        sessionData.setTimestamp(dateNow);
        this.setPayload(sessionData);
    }
}
