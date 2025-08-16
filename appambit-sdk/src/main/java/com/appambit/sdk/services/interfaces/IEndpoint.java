package com.appambit.sdk.services.interfaces;

import com.appambit.sdk.enums.HttpMethodEnum;

import java.util.Map;

public interface IEndpoint {

    String getUrl();

    String getBaseUrl();

    Object getPayload();

    HttpMethodEnum getMethod();

    Map<String, String> getCustomHeader();

    boolean isSkipAuthorization();
}