package com.appambit.sdk.services.interfaces;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.utils.AppAmbitTaskFuture;

public interface ApiService {
    public <T> ApiResult<T> executeRequest(IEndpoint endpoint, Class<T> clazz);
    public AppAmbitTaskFuture<ApiErrorType> GetNewToken(String appKey);

    public String getToken();
    public void setToken(String token);
}
