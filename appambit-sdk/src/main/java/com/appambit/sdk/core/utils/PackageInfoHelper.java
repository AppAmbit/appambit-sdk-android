package com.appambit.sdk.core.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.Nullable;

public class PackageInfoHelper {
    private static final String TAG = PackageInfoHelper.class.getSimpleName();
    @Nullable
    public static PackageInfo getPackageInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo(context.getPackageName(), 0);
        }catch (Exception e) {
            Log.d(TAG, "Error getting package info: " + e.getMessage());
            return null;
        }
    }
}