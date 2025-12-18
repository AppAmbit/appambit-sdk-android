package com.appambit.sdk;

import android.content.Context;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appambit.sdk.services.ConsumerService;

/**
 * The public-facing class for integrating AppAmbit Push Notifications.
 * This class acts as a facade that connects the decoupled PushKernel with the AppAmbit Core SDK.
 * Native Android developers should use this class for a seamless integration.
 */
public final class PushNotifications {

    private static final String TAG = "AppAmbitPushSDK";

    private PushNotifications() {}

    public interface PermissionListener extends PushKernel.PermissionListener {}

    public interface NotificationCustomizer extends PushKernel.NotificationCustomizer {}

    public static void setNotificationCustomizer(@Nullable NotificationCustomizer customizer) {
        PushKernel.setNotificationCustomizer(customizer);
    }

    @Nullable
    public static PushKernel.NotificationCustomizer getNotificationCustomizer() {
        return PushKernel.getNotificationCustomizer();
    }

    public static void start(@NonNull Context context) {
        if (!AppAmbit.isInitialized()) {
            Log.e(TAG, "AppAmbit SDK has not been started. Please call AppAmbit.start() before starting the Push SDK.");
            return;
        }

        Log.d(TAG, "Starting Push SDK and binding to AppAmbit Core.");

        PushKernel.setTokenListener(token -> {
            if (PushKernel.isNotificationsEnabled(context)) {
                Log.d(TAG, "FCM token received and notifications are enabled, updating consumer via AppAmbit Core.");
                ConsumerService.updateConsumer(token, true);
            } else {
                Log.d(TAG, "FCM token received, but notifications are disabled by the user. Skipping backend update.");
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

    /**
     * Enables or disables push notifications at both the business and FCM levels.
     *
     * <p>When set to {@code false}, this method will:
     * <ol>
     *     <li>Update the AppAmbit dashboard to reflect that the user has opted out.</li>
     *     <li>Delete the local FCM token to stop the device from receiving push notifications.</li>
     * </ol>
     *
     * <p>When set to {@code true}, a new FCM token will be fetched and sent to the AppAmbit dashboard.
     *
     * @param context The application context.
     * @param enabled {@code true} to enable notifications, {@code false} to disable.
     */
    public static void setNotificationsEnabled(@NonNull Context context, boolean enabled) {
        if (!AppAmbit.isInitialized()) {
            Log.e(TAG, "AppAmbit SDK is not initialized. Cannot set notification status.");
            return;
        }

        Log.d(TAG, "Setting notifications enabled state to: " + enabled);

        String currentToken = PushKernel.getCurrentToken();
        ConsumerService.updateConsumer(currentToken, enabled);

        PushKernel.setNotificationsEnabled(context, enabled);
    }

    /**
     * Checks if push notifications are currently enabled by the user.
     *
     * @param context The application context.
     * @return {@code true} if notifications are enabled, {@code false} otherwise.
     */
    public static boolean isNotificationsEnabled(@NonNull Context context) {
        return PushKernel.isNotificationsEnabled(context);
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity) {
        PushKernel.requestNotificationPermission(activity, null);
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity, @Nullable PermissionListener listener) {
        PushKernel.requestNotificationPermission(activity, listener);
    }
}
