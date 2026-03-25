package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.services.interfaces.IEndpoint;

public class CmsEndpoint extends BaseEndpoint implements IEndpoint {
    public CmsEndpoint(String contentType) {
        this.setUrl("/" + contentType);
        this.setMethod(HttpMethodEnum.GET);
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrlCms;
    }
}
