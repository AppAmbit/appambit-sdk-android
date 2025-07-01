package com.appambit.sdk.core.services.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.analytics.Event;

public class EventEndpoint extends BaseEndpoint {
    public EventEndpoint(Event event) {
        this.setUrl("/events");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(event);
    }
}
