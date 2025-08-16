package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.services.interfaces.IEndpoint;

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
