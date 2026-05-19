package com.appambit.javaapp;

import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.appambit.sdk.IAppAmbitNotificationServiceExtension;
import com.appambit.sdk.models.AppAmbitNotification;

@Keep
public class SampleMessagingService implements IAppAmbitNotificationServiceExtension {

    private static final String TAG = "AppAmbitSample";

    @Override
    public void onNotificationForeground(@NonNull AppAmbitNotification notification) {
        Log.d(TAG, "[FOREGROUND] Notification received while app is open");
        Log.d(TAG, "  Title : " + notification.getTitle());
        Log.d(TAG, "  Body  : " + notification.getBody());
        Log.d(TAG, "  Data  : " + notification.getData());
    }

    @Override
    public void onNotificationBackground(@NonNull AppAmbitNotification notification) {
        Log.d(TAG, "[BACKGROUND/KILLED] Notification received while app is closed");
        Log.d(TAG, "  Title : " + notification.getTitle());
        Log.d(TAG, "  Body  : " + notification.getBody());
        Log.d(TAG, "  Data  : " + notification.getData());
    }
}
