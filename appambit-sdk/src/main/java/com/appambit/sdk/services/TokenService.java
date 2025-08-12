package com.appambit.sdk.services;

import com.appambit.sdk.ServiceLocator;
import com.appambit.sdk.models.app.ConsumerToken;
import com.appambit.sdk.services.endpoints.TokenEndpoint;
import com.appambit.sdk.services.interfaces.Storable;

public class TokenService {
   static Storable mStorageService;

    public static void initialize(Storable storageService) {
        mStorageService = storageService;
    }

    public static TokenEndpoint createTokenendpoint() {
        try {
            String appKey = mStorageService.getAppId();
            String consumerId = mStorageService.getConsumerId();

            return new TokenEndpoint(new ConsumerToken( appKey, consumerId));
        } catch (Exception e) {
            return new TokenEndpoint(new ConsumerToken("", ""));
        }
    }
}
