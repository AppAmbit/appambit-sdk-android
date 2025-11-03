package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.breadcrumbs.BreadcrumbData;
import com.appambit.sdk.models.breadcrumbs.BreadcrumbsPayload;
import com.appambit.sdk.services.interfaces.IEndpoint;

import java.util.List;

public class BreadcrumbsBatchEndpoint extends BaseEndpoint implements IEndpoint {
    public BreadcrumbsBatchEndpoint(List<BreadcrumbData> batch) {
        this.setUrl("/breadcrumbs");
        this.setMethod(HttpMethodEnum.POST);
        BreadcrumbsPayload bathPayload = new BreadcrumbsPayload();
        bathPayload.setBreadcrumbs(batch);
        this.setPayload(bathPayload);
    }
}
