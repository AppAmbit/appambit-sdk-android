package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.app.ConsumerToken;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class TokenEndpoint extends BaseEndpoint implements IEndpoint {
    public  TokenEndpoint(ConsumerToken consumerToken) {
        this.setUrl("/consumer/token");
        this.setMethod(HttpMethodEnum.GET);
        this.setPayload(consumerToken);
    }
}
