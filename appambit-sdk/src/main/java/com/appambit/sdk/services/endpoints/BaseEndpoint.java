package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.services.interfaces.IEndpoint;
import java.util.Map;

public class BaseEndpoint implements IEndpoint {

    private String url;
    private String baseUrl = "https://staging-appambit.com/api";
    private boolean skipAuthorization;
    private Object payload;
    private Map<String, String> customHeader;
    private HttpMethodEnum method;

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public Object getPayload() {
        return payload;
    }

    @Override
    public HttpMethodEnum getMethod() {
        return method;
    }

    @Override
    public Map<String, String> getCustomHeader() {
        return customHeader;
    }

    @Override
    public boolean isSkipAuthorization() {
        return skipAuthorization;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setSkipAuthorization(boolean skipAuthorization) {
        this.skipAuthorization = skipAuthorization;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public void setCustomHeader(Map<String, String> customHeader) {
        this.customHeader = customHeader;
    }

    public void setMethod(HttpMethodEnum method) {
        this.method = method;
    }
}