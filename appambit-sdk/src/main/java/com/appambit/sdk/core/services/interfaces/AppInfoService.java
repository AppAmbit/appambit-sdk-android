package com.appambit.sdk.core.services.interfaces;

public interface AppInfoService {
    String appVersion = "";
    String build = "";
    String platform = "";
    String os = "";
    String deviceModel = "";
    String country = "";
    String UtcOffset = "";
    String language = "";

    public String getAppVersion();
    public void setAppVersion(String appVersion);
    public String getBuild();
    public void setBuild(String build);

    public String getPlatform();

    public void setPlatform(String platform);

    public String getOs();

    public void setOs(String os);

    public String getDeviceModel();

    public void setDeviceModel(String deviceModel);

    public String getCountry();

    public void setCountry(String country);

    public String getUtcOffset();

    public void setUtcOffset(String utcOffset);

    public String getLanguage();

    public void setLanguage(String language);

}