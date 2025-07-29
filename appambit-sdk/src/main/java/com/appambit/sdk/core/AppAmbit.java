package com.appambit.sdk.core;

import static com.appambit.sdk.core.utils.InternetConnection.hasInternetConnection;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.appambit.sdk.analytics.SessionManager;
import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.core.utils.StringUtils;
import com.appambit.sdk.crashes.CrashHandler;
import com.appambit.sdk.crashes.Crashes;
import com.appambit.sdk.core.utils.FileUtils;

public final class AppAmbit {
    private static final String TAG = AppAmbit.class.getSimpleName();
    private static boolean isAppStarted = false;
    private static String mAppKey;
    private static boolean isInitialized = false;
    private static boolean hasStartedSession = false;

    public static void init(Context context, String appKey) {
        mAppKey = appKey;
        CrashHandler.install(context);
        if (!isInitialized) {
            registerLifecycleObserver(context);
            isInitialized = true;
        }
    }

    private static void registerLifecycleObserver(Context context) {
        Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                Log.d(TAG,"onCreate");
                if (!isAppStarted) {
                    onStartApp(context);
                    isAppStarted = true;
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                Log.d(TAG,"onStart");
                if (!isAppStarted) {
                    onStartApp(context);
                    isAppStarted = true;
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                Log.d(TAG,"onResume");
                if (isAppStarted) {
                    onResumeApp();
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                Log.d(TAG,"onPause");
                onSleep();
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                Log.d(TAG,"onStop");
                onSleep();
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                Log.d(TAG,"onDestroy");
                onEnd();
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

        });
    }

    private static void InitializeServices(Context context) {
        ServiceLocator.initialize(context);
        FileUtils.initialize(context);
        Analytics.Initialize(ServiceLocator.getStorageService(), ServiceLocator.getExecutorService(), ServiceLocator.getApiService());
        SessionManager.initialize(ServiceLocator.getApiService(), ServiceLocator.getExecutorService());
    }

    private static void onStartApp(Context context) {
        InitializeServices(context);
        registerNetworkCallback(context);
        initializeConsumer();
        hasStartedSession = true;
        Crashes.loadCrashFileIfExists(context);
        Analytics.sendBatchesEvents();
        Crashes.sendBatchesLogs();
        SessionManager.sendBatchSessions();
    }

    private static void initializeConsumer() {
        getNewTokenAndThen();
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
        if(Analytics.isManualSessionEnabled()) {
            return;
        }
        if (!tokenIsValid()) {
            getNewTokenAndThen();
        }
        if (!Analytics.isManualSessionEnabled() && hasStartedSession) {
            SessionManager.removeSavedEndSession();
        }

        Crashes.sendBatchesLogs();
        Analytics.sendBatchesEvents();
    }

    private static void getNewTokenAndThen() {
        AppAmbitTaskFuture<ApiErrorType> future = ServiceLocator.getApiService().GetNewToken(mAppKey);
        future.then(result -> {
            Log.d(TAG, "Token obtained successfully.");
        });
        future.onError(error -> {
            Log.e(TAG, "Error getting token: ", error);
        });
    }

    private static void registerNetworkCallback(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Internet connection available");
                new Handler().postDelayed(() -> {
                    if(!hasInternetConnection(context) || Analytics.isManualSessionEnabled()) {
                        return;
                    }
                    try {
                        InitializeServices(context);

                        if (!tokenIsValid()) {
                            getNewTokenAndThen();
                        }
                        Crashes.loadCrashFileIfExists(context);
                        Crashes.sendBatchesLogs();
                        Analytics.sendBatchesEvents();
                        SessionManager.sendBatchSessions();
                    } catch (Exception e) {
                        Log.d(TAG, "Error on connectivity restored" + e);
                    }
                }, 3000);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.d(TAG, "Internet connection lost");
            }
        });
    }

    private static boolean tokenIsValid() {
        String token = ServiceLocator.getApiService().getToken();
        return !StringUtils.isNullOrBlank(token);
    }
}