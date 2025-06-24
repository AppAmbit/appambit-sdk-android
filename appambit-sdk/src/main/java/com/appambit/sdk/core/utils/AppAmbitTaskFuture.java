package com.appambit.sdk.core.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AppAmbitTaskFuture<T> {

    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final List<Callback<T>> successCallbacks = new ArrayList<>();
    private final List<ErrorCallback> errorCallbacks = new ArrayList<>();

    private T result;
    private Throwable error;


    private boolean isCompleted = false;
    private Throwable mError;

    public interface Callback<T> {
        void onSuccess(T success);
    }

    public interface ErrorCallback {
        void onError(Throwable error);
    }

    public synchronized void then(Callback<T> callback) {
        if (isCompleted && mError == null) {
            mHandler.post(() -> callback.onSuccess(result));
        } else {
            successCallbacks.add(callback);
        }
    }

    public synchronized void onError(ErrorCallback callback) {
        if (isCompleted && mError != null) {
            mHandler.post(() -> callback.onError(error));
        } else {
            errorCallbacks.add(callback);
        }
    }

    public synchronized  void complete(T value) {
        if (isCompleted)  {
            return;
        }

        result = value;
        isCompleted = true;

        mCountDownLatch.countDown();

        for (Callback<T> cb: successCallbacks ) {
            mHandler.post(() -> cb.onSuccess(value));
        }

        successCallbacks.clear();
        errorCallbacks.clear();
    }

    public synchronized void fail(Throwable throwable) {
        if (isCompleted) return;

        error = throwable;
        mCountDownLatch.countDown();

        for (ErrorCallback cb: errorCallbacks ) {
            mHandler.post(() -> cb.onError(throwable));
        }

        successCallbacks.clear();
        errorCallbacks.clear();
    }

    public T getBlocking() throws InterruptedException {
        mCountDownLatch.await();
        return result;
    }

    public boolean isDone() {
        return mCountDownLatch.getCount() == 0;
    }

    public boolean isFailed() {
        return isDone() && error != null;
    }
}