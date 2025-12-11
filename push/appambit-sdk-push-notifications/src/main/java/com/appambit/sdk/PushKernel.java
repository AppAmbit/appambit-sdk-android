package com.appambit.sdk;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.appambit.sdk.models.AppAmbitNotification;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * The decoupled kernel for handling FCM logic.
 * This class has no dependency on the AppAmbit Core SDK.
 * It can be used directly by bridge layers like .NET/MAUI.
 */
public final class PushKernel {

    private static final String TAG = "AppAmbitPushKernel";

    private static TokenListener tokenListener;
    private static NotificationCustomizer notificationCustomizer;
    private static String currentToken;
    private static boolean isStarted = false;

    private PushKernel() {}

    // region Public Interfaces

    /**
     * Listener to receive updates for the FCM registration token.
     */
    public interface TokenListener {
        void onNewToken(@NonNull String token);
    }

    /**
     * Listener to receive the result of the notification permission request.
     */
    public interface PermissionListener {
        void onPermissionResult(boolean isGranted);
    }

    /**
     * Customizer to modify a notification before it is displayed.
     */
    public interface NotificationCustomizer {
        void customize(@NonNull Context context, @NonNull NotificationCompat.Builder builder, @NonNull AppAmbitNotification notification);
    }

    // endregion

    // region Public Configuration

    /**
     * Sets a listener to be notified of FCM token updates.
     * @param listener The listener to set.
     */
    public static void setTokenListener(@Nullable TokenListener listener) {
        tokenListener = listener;
        // If a token is already available and a new listener is set, notify it immediately.
        if (isStarted && currentToken != null && tokenListener != null) {
            tokenListener.onNewToken(currentToken);
        }
    }

    /**
     * Sets a customizer to modify notifications before they are displayed.
     * @param customizer The customizer to set.
     */
    public static void setNotificationCustomizer(@Nullable NotificationCustomizer customizer) {
        notificationCustomizer = customizer;
    }

    /**
     * @return The currently configured NotificationCustomizer.
     */
    @Nullable
    public static NotificationCustomizer getNotificationCustomizer() {
        return notificationCustomizer;
    }

    // endregion

    // region Public Methods

    /**
     * Initializes the Firebase App and starts fetching the FCM token.
     * This is the entry point for decoupled usage (e.g., from .NET).
     * @param context The application context.
     */
    public static void start(@NonNull Context context) {
        if (isStarted) {
            Log.d(TAG, "PushKernel already started.");
            return;
        }

        try {
            FirebaseApp.initializeApp(context.getApplicationContext());
        } catch (IllegalStateException e) {
            Log.i(TAG, "FirebaseApp already initialized.");
        }

        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.e(TAG, "FirebaseApp not initialized. Please ensure your google-services.json is correctly configured.");
            return;
        }

        Log.d(TAG, "PushKernel started successfully.");
        isStarted = true;
        fetchToken();
    }

    /**
     * Requests the POST_NOTIFICATIONS permission if needed on Android 13+.
     * @param activity The activity to register the permission result with.
     * @param listener An optional listener to be notified of the result.
     */
    public static void requestNotificationPermission(@NonNull ComponentActivity activity, @Nullable PermissionListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission is already granted.");
                if (listener != null) {
                    listener.onPermissionResult(true);
                }
                return;
            }

            ActivityResultLauncher<String> requestPermissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Log.d(TAG, "POST_NOTIFICATIONS permission result: " + (isGranted ? "Granted" : "Denied"));
                if (listener != null) {
                    listener.onPermissionResult(isGranted);
                }
            });
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // On older versions, permission is granted by default.
            Log.d(TAG, "SDK < 33. No runtime permission required.");
            if (listener != null) {
                listener.onPermissionResult(true);
            }
        }
    }

    // endregion

    // region Internal and Private Methods

    /**
     * Fetches the current FCM registration token.
     */
    private static void fetchToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed.", task.getException());
                return;
            }
            String token = task.getResult();
            if (token != null) {
                handleNewToken(token);
            }
        });
    }

    /**
     * Internal method to handle token updates from Firebase.
     * Called by MessagingService and on initial fetch.
     * @param token The new FCM token.
     */
    static void handleNewToken(@NonNull String token) {
        if (token.equals(currentToken)) {
            return; // No change
        }
        currentToken = token;
        Log.d(TAG, "New FCM Token received: " + token);
        if (tokenListener != null) {
            tokenListener.onNewToken(token);
        }
    }

    // endregion
}
