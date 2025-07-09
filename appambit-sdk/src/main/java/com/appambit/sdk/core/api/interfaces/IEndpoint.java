package com.appambit.sdk.core.api.interfaces;

import com.appambit.sdk.core.enums.HttpMethodEnum;

import java.util.Map;

public interface IEndpoint {

    String getUrl();

    String getBaseUrl();

    Object getPayload();

    HttpMethodEnum getMethod();

    Map<String, String> getCustomHeader();

    boolean isSkipAuthorization();
}