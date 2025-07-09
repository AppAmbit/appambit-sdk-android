package com.appambit.sdk.core.api.endpoints;

import com.appambit.sdk.core.enums.HttpMethodEnum;
import com.appambit.sdk.core.models.Consumer;
import com.appambit.sdk.core.api.interfaces.IEndpoint;

public class RegisterEndpoint extends BaseEndpoint implements IEndpoint {
    public RegisterEndpoint(Consumer consumer) {
        this.setUrl("/consumer");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(consumer);
    }
}