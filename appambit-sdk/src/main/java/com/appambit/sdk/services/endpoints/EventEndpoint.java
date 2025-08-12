package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.analytics.Event;

public class EventEndpoint extends BaseEndpoint {
    public EventEndpoint(Event event) {
        this.setUrl("/events");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(event);
    }
}
