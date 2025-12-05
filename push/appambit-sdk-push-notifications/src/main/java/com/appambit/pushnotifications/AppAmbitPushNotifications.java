package com.appambit.pushnotifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public final class AppAmbitPushNotifications {

    private static final String TAG = "AppAmbitPushSDK";
    private static volatile TokenListener tokenListener;

    private AppAmbitPushNotifications() {}

    public static void initialize(@NonNull Context context) {
        boolean hasFirebaseApp = true;
        try {
            FirebaseApp app = FirebaseApp.initializeApp(context.getApplicationContext());
            if (app == null && FirebaseApp.getApps(context.getApplicationContext()).isEmpty()) {
                hasFirebaseApp = false;
            }
        } catch (IllegalStateException ignored) {
        }
        if (!hasFirebaseApp) {
            Log.w(TAG, "FirebaseApp no se inicializó. Verifica google-services.json y el plugin de Google Services.");
            return;
        }
        fetchToken();
    }

    public static void fetchToken() {
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    logAndNotify("Current FCM registration token", token);
                });
    }

    public static void setTokenListener(@Nullable TokenListener listener) {
        tokenListener = listener;
    }

    static void handleNewToken(@NonNull String token) {
        logAndNotify("FCM registration token refreshed", token);
    }

    private static void logAndNotify(@NonNull String prefix, @NonNull String token) {
        Log.i(TAG, prefix + ": " + token);
        TokenListener listener = tokenListener;
        if (listener != null) {
            listener.onNewToken(token);
        }
    }

    public interface TokenListener {
        void onNewToken(@NonNull String token);
    }
}
