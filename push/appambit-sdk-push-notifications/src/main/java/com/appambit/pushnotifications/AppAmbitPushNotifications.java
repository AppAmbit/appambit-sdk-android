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

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public final class AppAmbitPushNotifications {

    private static final String TAG = "AppAmbitPushSDK";

    private AppAmbitPushNotifications() {}

    public static void start(@NonNull Context context) {
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
                    Log.i(TAG, "FCM registration token: " + token);
                });
    }

    static void handleNewToken(@NonNull String token) {
        Log.i(TAG, "FCM registration token refreshed: " + token);
    }
}