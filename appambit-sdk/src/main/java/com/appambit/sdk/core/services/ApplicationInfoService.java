package com.appambit.sdk.core.services;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import com.appambit.sdk.core.services.interfaces.AppInfoService;
import com.appambit.sdk.core.utils.DateUtils;
import com.appambit.sdk.core.utils.PackageInfoHelper;
import java.util.Locale;

public class ApplicationInfoService implements AppInfoService {

    public ApplicationInfoService(Context context) {

        PackageInfo mPackageInfoHelper = PackageInfoHelper.getPackageInfo(context);

        assert mPackageInfoHelper != null;
        this.appVersion = mPackageInfoHelper.versionName;
        this.build = String.valueOf(Build.VERSION.SDK_INT);
        this.platform = "Android";
        this.os = Build.VERSION.RELEASE;
        this.deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        this.country = Locale.getDefault().getCountry();
        this.language = Locale.getDefault().getLanguage();
        this.utcOffset = DateUtils.getUtcNow().toString();
    }

    public String appVersion;
    public String build;
    public String platform;
    public String os;
    public String deviceModel;
    public String country;
    public String utcOffset;
    public String language;

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUtcOffset() {
        return UtcOffset;
    }

    public void setUtcOffset(String utcOffset) {
        this.utcOffset = utcOffset;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}