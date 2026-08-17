package com.appambit.sdk.utils;

import androidx.annotation.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Marks threads owned by SDK executors so blocking test helpers can reject them. */
public final class SdkThreadFactory implements ThreadFactory {
    private static final ThreadLocal<Boolean> SDK_THREAD = new ThreadLocal<>();

    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public SdkThreadFactory(@NonNull String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        return new Thread(() -> {
            SDK_THREAD.set(Boolean.TRUE);
            try {
                runnable.run();
            } finally {
                SDK_THREAD.remove();
            }
        }, namePrefix + "-" + threadNumber.getAndIncrement());
    }

    public static boolean isSdkThread() {
        return Boolean.TRUE.equals(SDK_THREAD.get());
    }
}
