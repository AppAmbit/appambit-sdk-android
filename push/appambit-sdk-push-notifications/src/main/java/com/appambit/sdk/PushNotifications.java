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

    // region Public Interfaces (as proxies to PushKernel for API stability)

    public interface PermissionListener extends PushKernel.PermissionListener {}

    public interface NotificationCustomizer extends PushKernel.NotificationCustomizer {}

    // endregion

    // region Public Configuration

    public static void setNotificationCustomizer(@Nullable NotificationCustomizer customizer) {
        PushKernel.setNotificationCustomizer(customizer);
    }

    @Nullable
    public static PushKernel.NotificationCustomizer getNotificationCustomizer() {
        return PushKernel.getNotificationCustomizer();
    }

    // endregion

    // region Public Methods

    public static void start(@NonNull Context context) {
        if (!AppAmbit.isInitialized()) {
            Log.e(TAG, "AppAmbit SDK has not been started. Please call AppAmbit.start() before starting the Push SDK.");
            return;
        }

        Log.d(TAG, "Starting Push SDK and binding to AppAmbit Core.");

        // Set the token listener. This listener will be invoked whenever a new token is fetched.
        PushKernel.setTokenListener(token -> {
            // IMPORTANT: Only update the backend if notifications are actually enabled by the user.
            if (PushKernel.areNotificationsEnabled(context)) {
                Log.d(TAG, "FCM token received and notifications are enabled, updating consumer via AppAmbit Core.");
                ConsumerService.updateConsumer(token, true);
            } else {
                Log.d(TAG, "FCM token received, but notifications are disabled by the user. Skipping backend update.");
            }
        });

        // Start the underlying push kernel, which may or may not fetch a token based on the enabled status.
        PushKernel.start(context);
    }

    /**
     * Enables or disables push notifications at both the business and FCM levels.
     *
     * <p>When set to {@code false}, this method will:
     * <ol>
     *     <li>Update the AppAmbit backend to reflect that the user has opted out.</li>
     *     <li>Delete the local FCM token to stop the device from receiving push notifications.</li>
     * </ol>
     *
     * <p>When set to {@code true}, a new FCM token will be fetched and sent to the AppAmbit backend.
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

        // If disabling, we must notify the backend *before* deleting the token.
        if (!enabled) {
            String currentToken = PushKernel.getCurrentToken();
            if (currentToken != null) {
                Log.d(TAG, "Disabling notifications: Updating consumer with 'false' flag.");
                ConsumerService.updateConsumer(currentToken, false);
            }
        }

        // Delegate the FCM-level logic (token deletion/creation) to the kernel.
        // If enabling, the kernel will fetch a new token, which will trigger the listener we set in start().
        PushKernel.setNotificationsEnabled(context, enabled);
    }

    /**
     * Checks if push notifications are currently enabled by the user.
     *
     * @param context The application context.
     * @return {@code true} if notifications are enabled, {@code false} otherwise.
     */
    public static boolean areNotificationsEnabled(@NonNull Context context) {
        return PushKernel.areNotificationsEnabled(context);
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity) {
        PushKernel.requestNotificationPermission(activity, null);
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity, @Nullable PermissionListener listener) {
        PushKernel.requestNotificationPermission(activity, listener);
    }

    // endregion
}
