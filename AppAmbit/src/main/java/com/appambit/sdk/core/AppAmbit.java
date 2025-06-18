package com.appambit.sdk.core;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;


public final class AppAmbit {

    private static String appKey;
    private static boolean isInitialized = false;


    public static void init(Context context, String appKey) {
        if (!isInitialized) {
            AppAmbit.appKey = appKey;
            registerLifecycleObserver(context);
            isInitialized = true;
        }
    }


    private static void registerLifecycleObserver(Context context) {
        Application app = (Application) context.getApplicationContext();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onCreate(LifecycleOwner owner) {
                System.out.println("onCreate");
            }

            @Override
            public void onStart(LifecycleOwner owner) {
                System.out.println("onStart");
            }

            @Override
            public void onResume(LifecycleOwner owner) {
                System.out.println("onResume");
            }

            @Override
            public void onPause(LifecycleOwner owner) {
                System.out.println("onPause");
            }

            @Override
            public void onStop(LifecycleOwner owner) {
                System.out.println("onStop");
            }

            @Override
            public void onDestroy(LifecycleOwner owner) {
                System.out.println("onDestroy");
            }
        });
    }



}