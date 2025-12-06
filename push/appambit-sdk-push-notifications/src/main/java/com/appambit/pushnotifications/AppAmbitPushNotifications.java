package com.appambit.pushnotifications;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.services.ConsumerService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public final class AppAmbitPushNotifications {

    private static final String TAG = "AppAmbitPushSDK";

    private AppAmbitPushNotifications() {}

    public static void start(@NonNull Context context) {
        if (!AppAmbit.isInitialized()) {
            Log.e(TAG, "AppAmbit SDK has not been started. Please call AppAmbit.start() before starting the Push SDK.");
            return;
        }

        boolean hasFirebaseApp = true;
        try {
            FirebaseApp.initializeApp(context.getApplicationContext());
        } catch (IllegalStateException ignored) {
            Log.w(TAG, "FirebaseApp already initialized.");
        }
        if (FirebaseApp.getApps(context.getApplicationContext()).isEmpty()) {
            hasFirebaseApp = false;
        }

        if (!hasFirebaseApp) {
            Log.w(TAG, "FirebaseApp not initialized. Check your google-services.json file.");
            return;
        }
        fetchToken();
    }

    public static void requestNotificationPermission(@NonNull ComponentActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityResultLauncher<String> requestPermissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted.");
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission denied.");
                }
            });
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission was already granted.");
            }
        }
    }

    private static void fetchToken() {
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    handleNewToken(token);
                });
    }

    static void handleNewToken(@NonNull String token) {
        Log.d(TAG, "FCM registration token updated: " + token);

        if (!AppAmbit.isInitialized()) {
            Log.d(TAG, "AppAmbit SDK not initialized. Cannot update consumer with new token.");
        }

        ConsumerService.updateConsumer(token, true);
    }
}