package com.appambit.sdk.core;

import android.content.Context;

import com.appambit.sdk.core.storage.StorageImplement;
import com.appambit.sdk.core.storage.StorageService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceLocator {
    private static volatile ServiceLocator INSTANCE;
    private static StorageService storageService;
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
        storageService = new StorageImplement(applicationContext);
    }

    public static StorageService getStorageService() {
        return storageService;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
