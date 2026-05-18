package com.appambit.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class NotificationOpenedReceiver extends BroadcastReceiver {

    private static final String TAG = "AppAmbitPushSDK";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        PushKernel.handleNotificationOpened(context, intent);

        String clickAction = intent.getStringExtra("appambit_click_action");
        Intent launch;
        if (!TextUtils.isEmpty(clickAction)) {
            launch = new Intent(clickAction);
            launch.setPackage(context.getPackageName());
        } else {
            launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        }

        if (launch == null) {
            Log.w(TAG, "No launch intent found for package; cannot open app.");
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(launch);
    }
}
