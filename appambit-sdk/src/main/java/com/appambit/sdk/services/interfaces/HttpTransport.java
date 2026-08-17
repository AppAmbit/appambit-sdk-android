package com.appambit.sdk.services.interfaces;

import androidx.annotation.NonNull;

import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public interface HttpTransport {
    void executeRaw(
            @NonNull IEndpoint endpoint,
            int timeoutMillis,
            @NonNull Callback callback);

    default HttpTransportResponse executeBlocking(
            @NonNull IEndpoint endpoint,
            int timeoutMillis) {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<HttpTransportResponse> result = new AtomicReference<>();
        executeRaw(endpoint, timeoutMillis, response -> {
            result.set(response);
            completed.countDown();
        });
        try {
            if (!completed.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                return new HttpTransportResponse(null, null, null,
                        new SocketTimeoutException("HTTP request timed out"));
            }
            return result.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return new HttpTransportResponse(null, null, null, error);
        }
    }

    interface Callback {
        void onComplete(HttpTransportResponse response);
    }
}
