package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.services.interfaces.IEndpoint;

import java.util.LinkedHashMap;
import java.util.Map;
import com.appambit.sdk.utils.UrlQueryBuilder;

public class CmsEndpoint extends BaseEndpoint implements IEndpoint {

    public CmsEndpoint(String contentType) {
        this.setUrl("/" + contentType);
        this.setMethod(HttpMethodEnum.GET);
    }

    public CmsEndpoint(String contentType, Map<String, String> queryParams) {
        this.setUrl(buildUrl("/" + contentType, queryParams));
        this.setMethod(HttpMethodEnum.GET);
    }

    public CmsEndpoint(String contentType, String searchQuery, Map<String, String> queryParams) {
        Map<String, String> params = new LinkedHashMap<>(queryParams);
        params.put("q", searchQuery);
        this.setUrl(buildUrl("/" + contentType + "/search", params));
        this.setMethod(HttpMethodEnum.GET);
    }

    private static String buildUrl(String path, Map<String, String> queryParams) {
        return UrlQueryBuilder.append(path, queryParams);
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrlCms;
    }
}
