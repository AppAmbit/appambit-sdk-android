package com.appambit.sdk.core.services.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.logs.LogBatch;

public class LogBatchEndpoint extends BaseEndpoint {
    public LogBatchEndpoint(LogBatch logBatch) {
        this.setUrl("/log/batch");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(logBatch);
    }
}