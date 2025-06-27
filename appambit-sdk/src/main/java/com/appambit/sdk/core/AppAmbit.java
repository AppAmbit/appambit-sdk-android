package com.appambit.sdk.core;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.crashes.Crashes;

public final class AppAmbit {

    private static String appKey;
    private static boolean isInitialized = false;
    private static final String TAG = AppAmbit.class.getSimpleName();

    public static void init(Context context, String appKey) {
        if (!isInitialized) {
            registerLifecycleObserver(context, appKey);
            Crashes.initialize(context);
            registerNetworkCallback(context);
            isInitialized = true;
        }
    }

    private static void registerLifecycleObserver(@NonNull Context context, String appKey) {
        Application app = (Application) context.getApplicationContext();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                InitializeServices(context);
                InitializeConsumer(context, appKey);
                Crashes.sendBatchesLogs();
                Analytics.sendBatchesEvents();
                Log.d(TAG,"onCreate");
            }

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onStart");
            }

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onResume");
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onPause");
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onStop");
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                Log.d(TAG,"onDestroy");
            }
        });
    }

    private static void registerNetworkCallback(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Internet connection available");

                ServiceLocator.getExecutorService().execute(() -> {
                    try {
                        InitializeServices(context);

                        if (!validToken()) {
                            ServiceLocator.getApiService().GetNewToken(appKey);
                        }

                        Crashes.loadCrashFileIfExists(context);
                        Crashes.sendBatchesLogs();
                        Analytics.sendBatchesEvents();
                    } catch (Exception e) {
                        Log.d(TAG, "Error on connectivity restored" + e);
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.d(TAG, "Internet connection lost");
            }
        });
    }

    public static boolean validToken() {
        String token = ServiceLocator.getApiService().getToken();
        return token != null && !token.isEmpty();
    }

    private static void InitializeServices(Context context) {
        ServiceLocator.initialize(context);
        Analytics.Initialize(ServiceLocator.getStorageService(), ServiceLocator.getExecutorService(), ServiceLocator.getApiService());
    }

    private static void InitializeConsumer(Context context, String appKey) {
        AppAmbitTaskFuture<ApiErrorType> currentTokenRenewalTask = ServiceLocator.getApiService().GetNewToken(appKey);
        currentTokenRenewalTask.then(result -> Log.d("[APIService]", "Token renewal successful: " + result));
        currentTokenRenewalTask.onError(error -> Log.d("[APIService]", "Error during token renewal: " + error));
    }
}