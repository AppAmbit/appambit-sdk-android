package com.appambit.sdk.core.services.endpoints;

import com.appambit.sdk.analytics.EventBatchPayload;
import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.analytics.EventEntity;

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
