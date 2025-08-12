package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.app.Consumer;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class RegisterEndpoint extends BaseEndpoint implements IEndpoint {
    public RegisterEndpoint(Consumer consumer) {
        this.setUrl("/consumer");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(consumer);
    }
}