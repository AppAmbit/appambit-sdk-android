package com.appambit.sdk.core;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.util.Log;

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
                Log.d("AppAmbit","onCreate");
            }

            @Override
            public void onStart(LifecycleOwner owner) {
                Log.d("AppAmbit","onStart");
            }

            @Override
            public void onResume(LifecycleOwner owner) {
                Log.d("AppAmbit","onResume");
            }

            @Override
            public void onPause(LifecycleOwner owner) {
                Log.d("AppAmbit","onPause");
            }

            @Override
            public void onStop(LifecycleOwner owner) {
                Log.d("AppAmbit","onStop");
            }

            @Override
            public void onDestroy(LifecycleOwner owner) {
                Log.d("AppAmbit","onDestroy");
            }
        });
    }



}