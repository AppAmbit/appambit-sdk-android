package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.analytics.EventBatchPayload;
import com.appambit.sdk.models.analytics.EventEntity;

import java.util.List;

public class EventBatchEndpoint extends BaseEndpoint {
    public EventBatchEndpoint(List<EventEntity> eventBatch) {
        this.setUrl("/events/batch");
        this.setMethod(HttpMethodEnum.POST);
        EventBatchPayload eventBatchPayload = new EventBatchPayload();
        eventBatchPayload.setEvents(eventBatch);
        this.setPayload(eventBatchPayload);
    }
}
