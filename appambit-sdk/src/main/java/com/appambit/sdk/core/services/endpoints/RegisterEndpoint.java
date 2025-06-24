package com.appambit.sdk.core.services.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.Consumer;
import com.appambit.sdk.core.services.interfaces.IEndpoint;

public class RegisterEndpoint extends BaseEndpoint implements IEndpoint {
    public RegisterEndpoint(Consumer consumer) {
        this.setUrl("/consumer");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(consumer);
    }
}