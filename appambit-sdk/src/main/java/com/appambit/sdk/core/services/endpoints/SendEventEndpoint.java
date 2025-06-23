package com.appambit.sdk.core.services.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.analytics.Event;
import com.appambit.sdk.core.services.interfaces.IEndpoint;

public class SendEventEndpoint extends BaseEndpoint implements IEndpoint {
    public SendEventEndpoint(Event event) {
        this.setUrl("/events");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(event);
    }
}