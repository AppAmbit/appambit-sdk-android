package com.appambit.sdk.core;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.util.Log;

import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.analytics.SessionManager;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.core.utils.FileUtils;
import com.appambit.sdk.core.utils.StringUtils;

public final class AppAmbit {
    private static final String TAG = AppAmbit.class.getSimpleName();
    private static String mAppKey;
    private static boolean isInitialized = false;
    private static boolean hasStartedSession = false;

    public static void init(Context context, String appKey) {
        mAppKey = appKey;
        if (!isInitialized) {
            registerLifecycleObserver(context);
            isInitialized = true;
        }
    }

    private static void registerLifecycleObserver(Context context) {
        Application app = (Application) context.getApplicationContext();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                onCreateApp(context);
                Log.d("AppAmbit","onCreate");
            }

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                onResumeApp();
                Log.d("AppAmbit","onStart");
            }

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                onResumeApp();
                Log.d("AppAmbit","onResume");
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                onSleep();
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                onSleep();
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                onEnd();
            }
        });
    }

    private static void InitializeServices(Context context) {
        ServiceLocator.initialize(context);
        FileUtils.initialize(context);
        Analytics.Initialize(ServiceLocator.getStorageService(), ServiceLocator.getExecutorService());
        SessionManager.initialize(ServiceLocator.getApiService(), ServiceLocator.getExecutorService());
    }

    private static void onCreateApp(Context context) {
        InitializeServices(context);
        InitializeConsumer(context);
        hasStartedSession = true;
        Analytics.sendBatchesLogs();
        Analytics.sendBatchesEvents();
        SessionManager.sendBatchSessions();
    }

    private static void InitializeConsumer(Context context) {
        getNewToken(mAppKey);

        if (Analytics.isManualSessionEnabled()) {
            return;
        }

        SessionManager.sendEndSessionIfExists();
        SessionManager.startSession();
    }



    private static void onSleep()
    {
        if (!Analytics.isManualSessionEnabled()) {
            SessionManager.saveEndSession();
        }
    }

    private static void onEnd()
    {
        if (!Analytics.isManualSessionEnabled()) {
            SessionManager.saveEndSession();
        }
    }

    private static void onResumeApp() {
        if (!tokenIsValid()) {
            getNewToken(mAppKey);
        }

        if (!Analytics.isManualSessionEnabled() && hasStartedSession) {
            SessionManager.removeSavedEndSession();
        }

        Analytics.sendBatchesLogs();
        Analytics.sendBatchesEvents();
    }

    private static void getNewToken(String appKey)  {
        try {
            AppAmbitTaskFuture<ApiErrorType> currentTokenRenewalTask = ServiceLocator.getApiService().GetNewToken(appKey);
            currentTokenRenewalTask.getBlocking();
        } catch (Exception e) {
            Log.d(TAG, "Error -> " + e);
        }
    }

    private static boolean tokenIsValid() {
        String token = ServiceLocator.getApiService().getToken();
        return !StringUtils.isNullOrBlank(token);
    }
}