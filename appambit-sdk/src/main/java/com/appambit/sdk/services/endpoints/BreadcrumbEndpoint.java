package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.breadcrumbs.BreadcrumEntity;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class BreadcrumbEndpoint extends BaseEndpoint implements IEndpoint {

    public BreadcrumbEndpoint(BreadcrumEntity entity) {
        this.setUrl("/breadcrumbs");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(entity);
    }
}
