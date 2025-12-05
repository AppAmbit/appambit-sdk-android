package com.appambit.pushnotifications;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class NotificationPermissionHelper {

    private static final String TAG = "NotificationHelper";
    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String> requestPermissionLauncher;

    public NotificationPermissionHelper(@NonNull AppCompatActivity activity) {
        this.activity = activity;
        this.requestPermissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted.");
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied.");
            }
        });
    }

    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission was already granted.");
            }
        }
    }
}
