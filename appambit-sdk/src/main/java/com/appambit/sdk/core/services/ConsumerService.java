package com.appambit.sdk.core.services;

import androidx.annotation.NonNull;
import com.appambit.sdk.core.ServiceLocator;
import com.appambit.sdk.core.models.Consumer;
import com.appambit.sdk.core.api.endpoints.RegisterEndpoint;
import com.appambit.sdk.core.services.interfaces.AppInfoService;
import com.appambit.sdk.core.storage.Storable;
import java.util.UUID;

public class ConsumerService {

    Storable storageService = ServiceLocator.getStorageService();
    AppInfoService appInfoService = ServiceLocator.getAppInfoService();

    public RegisterEndpoint RegisterConsumer(@NonNull String appKey) {
        String appId;
        String deviceId = storageService.getDeviceId();
        String userId = storageService.getUserId();
        String userEmail = storageService.getUserEmail();
        String deviceModel = appInfoService.getDeviceModel();

        if(!appKey.isEmpty()) {
            appId = appKey;
            storageService.putAppId(appKey);
        }else {
            appId = storageService.getAppId();
        }

        if(deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            storageService.putDeviceId(deviceId);
        }

        if(userId == null) {
            userId = UUID.randomUUID().toString();
            storageService.putUserId(userId);
        }

        Consumer consumer = new Consumer(
            appId,
            deviceId,
            deviceModel,
            userId,
            userEmail,
            appInfoService.getOs(),
            appInfoService.getCountry(),
            appInfoService.getLanguage()
        );

        return new RegisterEndpoint(consumer);
    }
}