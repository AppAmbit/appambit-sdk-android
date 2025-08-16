package com.appambit.sdk.services.interfaces;

public interface AppInfoService {

    String getAppVersion();

    String getBuild();

    String getPlatform();

    String getOs();

    String getDeviceModel();

    String getCountry();

    String getUtcOffset();

    String getLanguage();

}