package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.logs.LogBatch;

public class LogBatchEndpoint extends BaseEndpoint {
    public LogBatchEndpoint(LogBatch logBatch) {
        this.setUrl("/log/batch");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(logBatch);
    }
}