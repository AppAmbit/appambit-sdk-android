package com.appambit.kotlinapp

import android.util.Log
import androidx.annotation.Keep
import com.appambit.sdk.IAppAmbitNotificationServiceExtension
import com.appambit.sdk.models.AppAmbitNotification

@Keep
class SampleNotificationExtension : IAppAmbitNotificationServiceExtension {

    companion object {
        private const val TAG = "AppAmbitSample"
    }

    override fun onNotificationForeground(notification: AppAmbitNotification) {
        Log.d(TAG, "[FOREGROUND] Notification received while app is open")
        Log.d(TAG, "  Title : ${notification.title}")
        Log.d(TAG, "  Body  : ${notification.body}")
        Log.d(TAG, "  Data  : ${notification.data}")
    }

    override fun onNotificationBackground(notification: AppAmbitNotification) {
        Log.d(TAG, "[BACKGROUND/KILLED] Notification received while app is closed")
        Log.d(TAG, "  Title : ${notification.title}")
        Log.d(TAG, "  Body  : ${notification.body}")
        Log.d(TAG, "  Data  : ${notification.data}")
    }
}
