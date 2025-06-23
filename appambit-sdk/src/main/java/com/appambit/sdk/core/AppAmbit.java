package com.appambit.sdk.core;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.util.Log;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.services.ApiService;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;

public final class AppAmbit {

    private static String appKey;
    private static boolean isInitialized = false;

    public static void init(Context context, String appKey) {
        if (!isInitialized) {
            AppAmbit.appKey = appKey;
            registerLifecycleObserver(context, appKey);
            isInitialized = true;
        }
    }


    private static void registerLifecycleObserver(Context context, String appKey) {
        Application app = (Application) context.getApplicationContext();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                InitializeConsumer(context, appKey);
                Log.d("AppAmbit","onCreate");
            }

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                Log.d("AppAmbit","onStart");
            }

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                Log.d("AppAmbit","onResume");
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                Log.d("AppAmbit","onPause");
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                Log.d("AppAmbit","onStop");
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                Log.d("AppAmbit","onDestroy");
            }
        });
    }

    private static void InitializeConsumer(Context context, String appKey) {
        AppAmbitTaskFuture<ApiErrorType> currentTokenRenewalTask = new ApiService(context).GetNewToken(appKey);
        currentTokenRenewalTask.then(result -> Log.d("[APIService]", "Token renewal successful: " + result));
        currentTokenRenewalTask.onError(error -> Log.d("[APIService]", "Error during token renewal: " + error));
    }

}