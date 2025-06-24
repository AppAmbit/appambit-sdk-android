package com.appambit.sdk.core;

import android.content.Context;

import com.appambit.sdk.core.services.ApiService;
import com.appambit.sdk.core.services.HttpApiService;
import com.appambit.sdk.core.storage.StorageService;
import com.appambit.sdk.core.storage.Storable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceLocator {
    private static volatile ServiceLocator INSTANCE;
    private static Storable storable;

    private static ApiService apiService;
    private static ExecutorService executorService;
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
    }

    public static Storable getStorageService() {
        return storable;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static ApiService getApiService() { return apiService; }
}
