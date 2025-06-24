package com.appambit.sdk.core;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.util.Log;

import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.services.HttpApiService;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;

public final class AppAmbit {

    private static String appKey;
    private static boolean isInitialized = false;


    public static void init(Context context, String appKey) {
        if (!isInitialized) {
            registerLifecycleObserver(context, appKey);
            isInitialized = true;
        }
    }


    private static void registerLifecycleObserver(Context context, String appKey) {
        Application app = (Application) context.getApplicationContext();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onCreate(LifecycleOwner owner) {
                InitializeServices(context);
                InitializeConsumer(context, appKey);
                Analytics.sendBatchesLogs();
                Analytics.sendBatchesEvents();
                Log.d("AppAmbit","onCreate");
            }

            @Override
            public void onStart(LifecycleOwner owner) {
                Log.d("AppAmbit","onStart");
            }

            @Override
            public void onResume(LifecycleOwner owner) {
                Log.d("AppAmbit","onResume");
            }

            @Override
            public void onPause(LifecycleOwner owner) {
                Log.d("AppAmbit","onPause");
            }

            @Override
            public void onStop(LifecycleOwner owner) {
                Log.d("AppAmbit","onStop");
            }

            @Override
            public void onDestroy(LifecycleOwner owner) {
                Log.d("AppAmbit","onDestroy");
            }
        });
    }

    private static void InitializeServices(Context context) {
        ServiceLocator.initialize(context);
        Analytics.Initialize(ServiceLocator.getStorageService(), ServiceLocator.getExecutorService());
    }

    private static void InitializeConsumer(Context context, String appKey) {
        AppAmbitTaskFuture<ApiErrorType> currentTokenRenewalTask = ServiceLocator.getApiService().GetNewToken(appKey);
        currentTokenRenewalTask.then(result -> Log.d("[APIService]", "Token renewal successful: " + result));
        currentTokenRenewalTask.onError(error -> Log.d("[APIService]", "Error during token renewal: " + error));
    }
}