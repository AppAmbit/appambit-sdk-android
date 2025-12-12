package com.appambit.sdk;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.ComponentActivity;
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
    private static final String PREFS_NAME = "com.appambit.sdk.push.prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    private static TokenListener tokenListener;
    private static NotificationCustomizer notificationCustomizer;
    private static String currentToken;
    private static boolean isStarted = false;

    private PushKernel() {}

    public interface TokenListener {
        void onNewToken(@NonNull String token);
    }

    public interface PermissionListener {
        void onPermissionResult(boolean isGranted);
    }

    public interface NotificationCustomizer {
        void customize(@NonNull Context context, @NonNull NotificationCompat.Builder builder, @NonNull AppAmbitNotification notification);
    }

    public static void setTokenListener(@Nullable TokenListener listener) {
        tokenListener = listener;
    }

    public static void setNotificationCustomizer(@Nullable NotificationCustomizer customizer) {
        notificationCustomizer = customizer;
    }

    @Nullable
    public static NotificationCustomizer getNotificationCustomizer() {
        return notificationCustomizer;
    }

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

        if (isNotificationsEnabled(context)) {
            fetchToken();
        } else {
            Log.d(TAG, "Notifications are disabled by user. Skipping token fetch.");
        }
    }

    public static void setNotificationsEnabled(@NonNull Context context, boolean enabled) {
        Log.d(TAG, "Setting notifications enabled status to: " + enabled);
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();

        if (enabled) {
            fetchToken();
        } else {
            currentToken = null;
            FirebaseMessaging.getInstance().deleteToken();
        }
    }

    public static boolean isNotificationsEnabled(@NonNull Context context) {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity, @Nullable PermissionListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                if (listener != null) {
                    listener.onPermissionResult(true);
                }
                return;
            }

            PermissionCallbackHolder.getInstance().setListener(listener);
            Intent intent = new Intent(activity, PermissionRequestActivity.class);
            activity.startActivity(intent);
        } else {
            if (listener != null) {
                listener.onPermissionResult(true);
            }
        }
    }

    // endregion

    // region Internal and Private Methods

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    static String getCurrentToken() {
        return currentToken;
    }

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

    static void handleNewToken(@NonNull String token) {
        if (token.equals(currentToken)) {
            return;
        }
        currentToken = token;
        Log.d(TAG, "New FCM Token received.");
        if (tokenListener != null) {
            tokenListener.onNewToken(token);
        }
    }
}
