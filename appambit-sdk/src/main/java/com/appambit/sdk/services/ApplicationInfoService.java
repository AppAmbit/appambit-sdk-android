package com.appambit.sdk.services;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import com.appambit.sdk.services.interfaces.AppInfoService;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.PackageInfoHelper;
import java.util.Locale;

public class ApplicationInfoService implements AppInfoService {

    private final String appVersion;
    private final String build;
    private final String platform;
    private final String os;
    private final String deviceModel;
    private final String country;
    private final String utcOffset;
    private final String language;

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

    public String getAppVersion() {
        return appVersion;
    }

    public String getBuild() {
        return build;
    }

    public String getPlatform() {
        return platform;
    }

    public String getOs() {
        return os;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getCountry() {
        return country;
    }

    public String getUtcOffset() {
        return utcOffset;
    }

    public String getLanguage() {
        return language;
    }
}