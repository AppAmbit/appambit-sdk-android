package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.app.UpdateConsumer;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class UpdateConsumerEndpoint extends BaseEndpoint implements IEndpoint {

    public UpdateConsumerEndpoint(String consumerId, UpdateConsumer request) {
        this.setUrl("/consumer/" + consumerId);
        this.setMethod(HttpMethodEnum.PUT);
        this.setPayload(request);
    }
}
