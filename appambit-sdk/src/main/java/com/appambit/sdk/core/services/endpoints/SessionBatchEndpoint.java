package com.appambit.sdk.core.services.endpoints;

import com.appambit.sdk.analytics.SessionPayload;
import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.services.interfaces.IEndpoint;

public class SessionBatchEndpoint extends BaseEndpoint implements IEndpoint {
    public SessionBatchEndpoint(SessionPayload sessionPayload) {
        this.setUrl("/session/batch");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(sessionPayload);
    }
}
