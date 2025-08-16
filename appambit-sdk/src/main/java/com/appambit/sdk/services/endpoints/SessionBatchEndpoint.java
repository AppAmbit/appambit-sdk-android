package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.models.analytics.SessionPayload;
import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class SessionBatchEndpoint extends BaseEndpoint implements IEndpoint {
    public SessionBatchEndpoint(SessionPayload sessionPayload) {
        this.setUrl("/session/batch");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(sessionPayload);
    }
}
