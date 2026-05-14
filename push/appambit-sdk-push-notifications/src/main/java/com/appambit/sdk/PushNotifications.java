package com.appambit.sdk;

import android.content.Context;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appambit.sdk.services.ConsumerService;

public final class PushNotifications {

    private static final String TAG = "AppAmbitPushSDK";
    private static boolean isInitialized = false;

    private PushNotifications() {}

    public interface PermissionListener extends PushKernel.PermissionListener {}

    public interface NotificationCustomizer extends PushKernel.NotificationCustomizer {}


    public interface OpenedNotificationListener extends PushKernel.OpenedNotificationListener {}

    public interface ForegroundNotificationListener extends PushKernel.ForegroundNotificationListener {}

    public interface BackgroundNotificationListener extends PushKernel.BackgroundNotificationListener {}

    public static void setNotificationCustomizer(@Nullable NotificationCustomizer customizer) {
        PushKernel.setNotificationCustomizer(customizer);
    }

    public static void setOpenedNotificationListener(@Nullable OpenedNotificationListener listener) {
        PushKernel.setOpenedNotificationListener(listener);
    }

    public static void setForegroundNotificationListener(@Nullable ForegroundNotificationListener listener) {
        PushKernel.setForegroundNotificationListener(listener);
    }

    public static void setBackgroundNotificationListener(@Nullable BackgroundNotificationListener listener) {
        PushKernel.setBackgroundNotificationListener(listener);
    }

    public static void start(@NonNull Context context) {
        if (!AppAmbit.isInitialized()) {
            Log.e(TAG, "AppAmbit SDK has not been started. Please call AppAmbit.start() before starting the Push SDK.");
            return;
        }

        if (!isInitialized) {
            isInitialized = true;
            Log.d(TAG, "Starting Push SDK and binding to AppAmbit Core.");

            PushKernel.setTokenListener(token -> {
                if (PushKernel.isNotificationsEnabled(context)) {
                    Log.d(TAG,
                            "FCM token received and notifications are enabled, updating consumer via AppAmbit Core.");
                    ConsumerService.updateConsumer(token, true);
                } else {
                    Log.d(TAG,
                            "FCM token received, but notifications are disabled by the user. Skipping backend update.");
                }
            });

            PushKernel.start(context);

            if (PushKernel.isNotificationsEnabled(context)) {
                String currentToken = PushKernel.getCurrentToken();
                if (currentToken != null && !currentToken.isEmpty()) {
                    Log.d(TAG, "Push SDK started. Syncing current token with backend.");
                    ConsumerService.updateConsumer(currentToken, true);
                }
            }
        }
    }

    public static void setNotificationsEnabled(@NonNull Context context, boolean enabled) {
        if (!AppAmbit.isInitialized()) {
            Log.e(TAG, "AppAmbit SDK is not initialized. Cannot set notification status.");
            return;
        }

        Log.d(TAG, "Setting notifications enabled state to: " + enabled);

        PushKernel.setNotificationsEnabled(context, enabled);

        String currentToken = PushKernel.getCurrentToken();
        ConsumerService.updateConsumer(currentToken, enabled);
    }

    public static boolean isNotificationsEnabled(@NonNull Context context) {
        return PushKernel.isNotificationsEnabled(context);
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity) {
        PushKernel.requestNotificationPermission(activity, null);
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity, @Nullable PermissionListener listener) {
        PushKernel.requestNotificationPermission(activity, listener);
    }

    public static void handleNotificationOpened(@NonNull Context context, @NonNull android.content.Intent intent) {
        PushKernel.handleNotificationOpened(context, intent);
    }
}
