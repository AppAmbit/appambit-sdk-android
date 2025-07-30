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
import android.os.Looper;
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

    private static int startedActivities = 0;
    private static int resumedActivities = 0;
    private static boolean foreground = false;
    private static boolean isWaitingPause = false;
    private static final long ACTIVITY_DELAY = 700;

    public static void init(Context context, String appKey) {
        mAppKey = appKey;
        CrashHandler.install(context);
        if (!isInitialized) {
            onStartApp(context);
            registerLifecycleObserver(context);
            isInitialized = true;
        }
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable pauseRunnable = () -> {
        if (resumedActivities == 0 && foreground && isWaitingPause) {
            foreground = false;
            Log.d(TAG, "onPause (App in background)");
            onSleep();
        }
        isWaitingPause = false;
    };

    private static void registerLifecycleObserver(Context context) {
        Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (startedActivities == 0) {
                    Log.d(TAG, "onStart (Foreground)");
                }
                startedActivities++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                resumedActivities++;

                if (isWaitingPause) {
                    handler.removeCallbacks(pauseRunnable);
                    isWaitingPause = false;
                }

                if (!foreground) {
                    foreground = true;
                    Log.d(TAG, "onResume (App in foreground)");
                    onResumeApp();
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                resumedActivities = Math.max(0, resumedActivities - 1);

                if (resumedActivities == 0) {
                    isWaitingPause = true;
                    handler.postDelayed(pauseRunnable, ACTIVITY_DELAY);
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);

                if (startedActivities == 0) {
                    Log.d(TAG, "onStop (App in background)");
                    onEnd();
                }
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (startedActivities == 0 && resumedActivities == 0) {
                    Log.d(TAG, "onDestroy (App Level)");
                    onEnd();
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
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
        getNewTokenAndThen(() -> {
            if (Analytics.isManualSessionEnabled()) {
                return;
            }
            SessionManager.sendEndSessionIfExists();
            SessionManager.startSession();
        });
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
        Runnable resumeTasks = () -> {
            if (!Analytics.isManualSessionEnabled() && hasStartedSession) {
                SessionManager.removeSavedEndSession();
            }

            Crashes.sendBatchesLogs();
            Analytics.sendBatchesEvents();
        };

        if (!tokenIsValid()) {
            getNewTokenAndThen(resumeTasks);
        } else {
            resumeTasks.run();
        }
    }

    private static void getNewTokenAndThen(Runnable onSuccess) {
        AppAmbitTaskFuture<ApiErrorType> future = ServiceLocator.getApiService().GetNewToken(mAppKey);
        future.then(result -> {
            if (result == ApiErrorType.None) {
                Log.d(TAG, "Token obtained successfully.");
                onSuccess.run();
            } else {
                Log.e(TAG, "Failed to get token: " + result);
            }
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

                        Runnable connectionTasks = () -> {
                            Crashes.loadCrashFileIfExists(context);
                            Crashes.sendBatchesLogs();
                            Analytics.sendBatchesEvents();
                            SessionManager.sendBatchSessions();
                        };

                        if (!tokenIsValid()) {
                            getNewTokenAndThen(connectionTasks);
                        }else {
                            connectionTasks.run();
                        }
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