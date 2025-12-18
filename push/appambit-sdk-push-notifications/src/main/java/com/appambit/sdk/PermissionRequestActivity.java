package com.appambit.sdk;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PermissionRequestActivity extends ComponentActivity {

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                PushKernel.PermissionListener listener = PermissionCallbackHolder.getInstance().getListener();
                if (listener != null) {
                    listener.onPermissionResult(isGranted);
                }
                PermissionCallbackHolder.getInstance().clearListener();
                finish();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
