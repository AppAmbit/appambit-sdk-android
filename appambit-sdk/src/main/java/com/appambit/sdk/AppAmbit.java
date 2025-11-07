package com.appambit.sdk;

import static com.appambit.sdk.utils.InternetConnection.hasInternetConnection;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;

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

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.services.ConsumerService;
import com.appambit.sdk.services.TokenService;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.FileUtils;
import com.appambit.sdk.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class AppAmbit {
    private static final String TAG = AppAmbit.class.getSimpleName();

    private static final Object TOKEN_LOCK = new Object();
    private static boolean isRefreshingToken = false;
    private static final List<Runnable> tokenWaiters = new ArrayList<>();
    private static String mAppKey;
    private static boolean isInitialized = false;
    private static boolean hasStartedSession = false;
    private static boolean isReadyForSendingBatches = false;
    private static int startedActivities = 0;
    private static int resumedActivities = 0;
    private static boolean foreground = false;
    private static boolean isWaitingPause = false;
    private static boolean firstConnectivityEvent = true;
    private static final long ACTIVITY_DELAY = 700;
    private static String lastPageClassName = null;

    static void safeRun(@Nullable Runnable r) {
        if (r == null) return;
        try {
            r.run();
        } catch (Throwable t) {
            Log.e(TAG, "Callback threw", t);
        }
    }

    private static void finishTokenOperation(boolean success) {
        List<Runnable> callbacks;
        synchronized (TOKEN_LOCK) {
            isRefreshingToken = false;
            callbacks = new ArrayList<>(tokenWaiters);
            tokenWaiters.clear();
        }
        if (success) {
            for (Runnable r : callbacks) safeRun(r);
        } else {
            Log.d(TAG, "Token operation failed; callbacks dropped");
        }
    }

    public static void start(Context context, String appKey) {
        mAppKey = appKey;
        if (!isInitialized) {
            CrashHandler.initialize(context);
            registerLifecycleObserver(context);
            onStartApp(context);
            isInitialized = true;
            Log.d(TAG, "onCreate (App Level)");
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

    private static void registerLifecycleObserver(@NonNull Context context) {
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

                trackPageChange(activity);
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

                if (startedActivities == 0 && !activity.isChangingConfigurations()) {
                    Log.d(TAG, "onStop (App in background)");
                    onEnd();
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (startedActivities == 0 && resumedActivities == 0 && !activity.isChangingConfigurations()) {
                    Log.d(TAG, "onDestroy (App Level)");
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }
        });
    }

    private static void InitializeServices(Context context) {
        ServiceLocator.initialize(context);
        FileUtils.initialize(context);
        Analytics.Initialize(ServiceLocator.getStorageService(), ServiceLocator.getExecutorService(), ServiceLocator.getApiService());
        SessionManager.initialize(ServiceLocator.getApiService(), ServiceLocator.getExecutorService(), ServiceLocator.getStorageService());
        ConsumerService.initialize(ServiceLocator.getStorageService(), ServiceLocator.getAppInfoService(), ServiceLocator.getApiService());
        TokenService.initialize(ServiceLocator.getStorageService());
        BreadcrumbManager.initialize(ServiceLocator.getApiService(), ServiceLocator.getExecutorService(), ServiceLocator.getStorageService());
    }

    private static void onStartApp(Context context) {
        InitializeServices(context);
        registerNetworkCallback(context);
        initializeConsumer();

        hasStartedSession = true;
        final Runnable batchesTasks = () -> {
            Analytics.sendBatchesEvents();
            Crashes.sendBatchesLogs();
            BreadcrumbManager.sendBatchBreadcrumbs();
        };
        Crashes.Initialize();
        Crashes.loadCrashFileIfExists(context);
        BreadcrumbManager.loadBreadcrumbsFromFile();
        SessionManager.sendBatchSessions(batchesTasks);
    }

    private static void initializeConsumer() {
        if (!Analytics.isManualSessionEnabled()) {
            SessionManager.saveSessionEndToDatabaseIfExist();
        }

        Runnable initializeTasks = () -> {
            if (Analytics.isManualSessionEnabled()) {
                Log.d(TAG, "Manual session management is enabled");
                return;
            }
            Runnable initializeSession = () -> {
                SessionManager.sendEndSessionFromFile();

                AppAmbitTaskFuture<String> sessionFuture = SessionManager.startSession();

                sessionFuture.then(sessionId -> {
                    BreadcrumbManager.addAsync(BreadcrumbsConstants.onStart);
                });

                sessionFuture.onError(error -> {
                    Log.e(TAG, "Failed to start session before adding breadcrumb.", error);
                });
            };
            SessionManager.sendEndSessionFromDatabase(initializeSession);
        };
        if (!tokenIsValid()) {
            getNewToken(null);
            initializeTasks.run();
        } else {
            initializeTasks.run();
        }
    }

    private static void onSleep() {
        if (!Analytics.isManualSessionEnabled()) {
            BreadcrumbManager.saveToFile(BreadcrumbsConstants.onPause);
            SessionManager.saveEndSession();
        }
    }

    private static void onEnd() {
        if (!Analytics.isManualSessionEnabled()) {
            BreadcrumbManager.saveToFile(BreadcrumbsConstants.onPause);
            SessionManager.saveEndSession();
        }
    }

    private static void onResumeApp() {
        if (Analytics.isManualSessionEnabled() || !isReadyForSendingBatches) {
            isReadyForSendingBatches = true;
            return;
        }

        Runnable resumeTasks = () -> {
            if (!Analytics.isManualSessionEnabled() && isInitialized) {
                SessionManager.removeSavedEndSession();
            }

            Crashes.sendBatchesLogs();
            Analytics.sendBatchesEvents();
            BreadcrumbManager.loadBreadcrumbsFromFile();
            BreadcrumbManager.sendBatchBreadcrumbs();
        };

        if (!tokenIsValid()) {
            getNewToken(null);
            resumeTasks.run();
        } else {
            resumeTasks.run();
        }

        BreadcrumbManager.addAsync(BreadcrumbsConstants.onResume);
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
                    if (!hasInternetConnection(context) || Analytics.isManualSessionEnabled() || firstConnectivityEvent) {
                        firstConnectivityEvent = false;
                        return;
                    }
                    try {
                        InitializeServices(context);
                        final Runnable batchTasks = () -> {
                            Crashes.sendBatchesLogs();
                            Analytics.sendBatchesEvents();
                            BreadcrumbManager.sendBatchBreadcrumbs();
                        };
                        final Runnable connectionTasks = () -> {
                            Crashes.loadCrashFileIfExists(context);
                            SessionManager.sendEndSessionFromDatabase(null);
                            SessionManager.sendStartSessionIfExist();
                            SessionManager.sendBatchSessions(batchTasks);
                            BreadcrumbManager.loadBreadcrumbsFromFile();
                            BreadcrumbManager.addAsync(BreadcrumbsConstants.online);
                        };
                        getNewToken(null);
                        connectionTasks.run();
                    } catch (Exception e) {
                        Log.d(TAG, "Error on connectivity restored " + e);
                    }
                }, 3000);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                BreadcrumbManager.saveToFile(BreadcrumbsConstants.offline);
                Log.d(TAG, "Internet connection lost");
            }
        });
    }

    private static boolean tokenIsValid() {
        String token = ServiceLocator.getApiService().getToken();
        return !StringUtils.isNullOrBlank(token);
    }

    private static void getNewToken(@Nullable Runnable onSuccess) {
        synchronized (TOKEN_LOCK) {
            if (isRefreshingToken) {
                if (onSuccess != null) tokenWaiters.add(onSuccess);
                Log.d(TAG, "Token operation in progress, callback queued");
                return;
            }
            isRefreshingToken = true;
            if (onSuccess != null) tokenWaiters.add(onSuccess);
        }

        if (tokenIsValid()) {
            Log.d(TAG, "Token already valid; skipping refresh");
            finishTokenOperation(true);
            return;
        }

        final ApiService api = ServiceLocator.getApiService();
        final Storable storage = ServiceLocator.getStorageService();

        String consumerId = null;
        try {
            ConsumerService.updateAppKeyIfNeeded(mAppKey);
            consumerId = storage.getConsumerId();
        } catch (Exception e) {
            Log.w(TAG, "Error reading consumerId", e);
        }

        if (!StringUtils.isNullOrBlank(consumerId)) {
            final AppAmbitTaskFuture<ApiErrorType> future = api.GetNewToken();
            future.then(result -> {
                boolean ok = (result == ApiErrorType.None);
                Log.d(TAG, "GetNewToken finished: " + result);
                finishTokenOperation(ok);
            });
            future.onError(error -> {
                Log.e(TAG, "GetNewToken error", error);
                finishTokenOperation(false);
            });
            return;
        }

        final AppAmbitTaskFuture<ApiErrorType> createFuture = ConsumerService.createConsumer();
        createFuture.then(createResult -> {
            if (createResult != ApiErrorType.None) {
                Log.e(TAG, "CreateConsumer failed: " + createResult);
                finishTokenOperation(false);
                return;
            }
            Log.d(TAG, "Consumer successfully created, requesting token...");
            final AppAmbitTaskFuture<ApiErrorType> ft = api.GetNewToken();
            ft.then(res -> {
                boolean ok = (res == ApiErrorType.None);
                Log.d(TAG, "GetNewToken after create finished: " + res);
                finishTokenOperation(ok);
            });
            ft.onError(err -> {
                Log.e(TAG, "GetNewToken after create error", err);
                finishTokenOperation(false);
            });
        });
        createFuture.onError(err -> {
            Log.e(TAG, "CreateConsumer error", err);
            finishTokenOperation(false);
        });
    }

    private static void trackPageChange(@NonNull Activity activity) {
        if (isDialogLike(activity)) return;
        String className = activity.getClass().getName();
        if (lastPageClassName == null) {
            lastPageClassName = className;
            return;
        }
        if (!className.equals(lastPageClassName)) {
            String previousDisplay = simpleNameOf(lastPageClassName);
            String currentDisplay = activity.getClass().getSimpleName();
            BreadcrumbManager.addAsync(BreadcrumbsConstants.onDisappear + ": " + previousDisplay);
            lastPageClassName = className;
            BreadcrumbManager.addAsync(BreadcrumbsConstants.onAppear + ": " + currentDisplay);
        }
    }

    private static boolean isDialogLike(@NonNull Activity activity) {
        try {
            int[] attrs = new int[]{android.R.attr.windowIsTranslucent, android.R.attr.windowIsFloating};
            TypedArray ta = activity.getTheme().obtainStyledAttributes(attrs);
            boolean translucent = ta.getBoolean(0, false);
            boolean floating = ta.getBoolean(1, false);
            ta.recycle();
            return translucent || floating;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String simpleNameOf(@NonNull String fqcn) {
        int i = fqcn.lastIndexOf('.');
        return i >= 0 ? fqcn.substring(i + 1) : fqcn;
    }
}
