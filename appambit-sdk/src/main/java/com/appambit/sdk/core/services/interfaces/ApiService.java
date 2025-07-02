package com.appambit.sdk.core.services.interfaces;

import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.models.responses.ApiResult;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;

public interface ApiService {
    public <T> ApiResult<T> executeRequest(IEndpoint endpoint, Class<T> clazz);
    public AppAmbitTaskFuture<ApiErrorType> GetNewToken(String appKey);

    public String getToken();
    public void setToken(String token);
}
