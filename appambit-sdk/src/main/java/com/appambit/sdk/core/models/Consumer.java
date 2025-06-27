package com.appambit.sdk.core.models;

import com.appambit.sdk.core.utils.JsonKey;

public class Consumer {

    public Consumer(String appKey, String deviceId, String deviceModel, String userId, String userEmail, String os, String country, String language) {
        this.appKey = appKey;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.userId = userId;
        this.userEmail = userEmail;
        this.os = os;
        this.country = country;
        this.language = language;
    }

    @JsonKey("app_key")
    private String appKey;
    @JsonKey("device_id")
    private String deviceId;
    @JsonKey("device_model")
    private String deviceModel;
    @JsonKey("user_id")
    private String userId;
    @JsonKey("user_email")
    private String userEmail;
    @JsonKey("os")
    private String os;
    @JsonKey("country")
    private String country;
    @JsonKey("language")
    private String language;

    // Getters and Setters
    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}