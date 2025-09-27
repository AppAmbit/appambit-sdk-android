package com.appambit.sdk;

import android.content.Context;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.ApplicationInfoService;
import com.appambit.sdk.services.HttpApiService;
import com.appambit.sdk.services.interfaces.AppInfoService;
import com.appambit.sdk.services.StorageService;
import com.appambit.sdk.services.interfaces.Storable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceLocator {
    private static volatile ServiceLocator INSTANCE;
    private static Storable storable;

    private static ApiService apiService;
    private static ExecutorService executorService;
    private static AppInfoService appInfoService;
    private final Context applicationContext;

    private ServiceLocator(Context context) {
        this.applicationContext = context;
        initializeServices();
    }

    public static void initialize(Context context) {
        if (INSTANCE == null) {
            synchronized (ServiceLocator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServiceLocator(context);
                }
            }
        }
    }

    private void initializeServices() {
        executorService = Executors.newSingleThreadExecutor();
        storable = new StorageService(applicationContext);
        apiService = new HttpApiService(applicationContext, executorService);
        appInfoService = new ApplicationInfoService(applicationContext);
    }

    public static Storable getStorageService() {
        return storable;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static ApiService getApiService() { return apiService; }

    public static AppInfoService getAppInfoService() { return appInfoService; }

    public static Context getContext() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ServiceLocator is not initialized, call initialize(..) method first.");
        }
        return INSTANCE.applicationContext;
    }
}