package com.appambit.sdk.core;

import static com.appambit.sdk.core.utils.InternetConnection.hasInternetConnection;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
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
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onCreate");
                onStartApp(context);
            }

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onStart");
                onResumeApp();
            }

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onResume");
                onResumeApp();
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onPause");
                onSleep();
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onStop");
                onSleep();
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onDestroy");
                onEnd();
            }
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
        if(Analytics.isManualSessionEnabled()) {
            return;
        }
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