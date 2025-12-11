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

    /**
     * A listener for the notification permission request result.
     * This is a proxy for {@link PushKernel.PermissionListener}.
     */
    public interface PermissionListener extends PushKernel.PermissionListener {}

    /**
     * A customizer to modify a notification before it is displayed.
     * This is a proxy for {@link PushKernel.NotificationCustomizer}.
     */
    public interface NotificationCustomizer extends PushKernel.NotificationCustomizer {}

    // endregion

    // region Public Configuration

    /**
     * Sets a customizer to modify notifications before they are displayed.
     * This method delegates the call to the underlying {@link PushKernel}.
     *
     * @param customizer The customizer to set.
     */
    public static void setNotificationCustomizer(@Nullable NotificationCustomizer customizer) {
        PushKernel.setNotificationCustomizer(customizer);
    }

    /**
     * Gets the currently configured notification customizer.
     * This method delegates the call to the underlying {@link PushKernel}.
     *
     * @return The currently configured customizer.
     */
    @Nullable
    public static PushKernel.NotificationCustomizer getNotificationCustomizer() {
        return PushKernel.getNotificationCustomizer();
    }

    // endregion

    // region Public Methods

    /**
     * Starts the Push Notifications SDK and integrates it with the AppAmbit Core SDK.
     * This method ensures the Core SDK is initialized and sets up a listener to automatically
     * update the consumer's push token.
     *
     * @param context The application context.
     */
    public static void start(@NonNull Context context) {
        if (!AppAmbit.isInitialized()) {
            Log.e(TAG, "AppAmbit SDK has not been started. Please call AppAmbit.start() before starting the Push SDK.");
            return;
        }

        Log.d(TAG, "Starting Push SDK and binding to AppAmbit Core.");

        // 1. Bridge the PushKernel token updates to the Core SDK's ConsumerService.
        PushKernel.setTokenListener(token -> {
            Log.d(TAG, "FCM token received, updating consumer via AppAmbit Core.");
            ConsumerService.updateConsumer(token, true);
        });

        // 2. Start the underlying push kernel.
        PushKernel.start(context);
    }

    /**
     * Requests the POST_NOTIFICATIONS permission.
     * This method delegates the call to the underlying {@link PushKernel}.
     *
     * @param activity The activity to register the permission result with.
     */
    public static void requestNotificationPermission(@NonNull ComponentActivity activity) {
        PushKernel.requestNotificationPermission(activity, null);
    }

    /**
     * Requests the POST_NOTIFICATIONS permission with a result listener.
     * This method delegates the call to the underlying {@link PushKernel}.
     *
     * @param activity The activity to register the permission result with.
     * @param listener A listener to be notified of the result.
     */
    public static void requestNotificationPermission(@NonNull ComponentActivity activity, @Nullable PermissionListener listener) {
        PushKernel.requestNotificationPermission(activity, listener);
    }

    // endregion
}
