package com.appambit.sdk.models.cloudcode;

import androidx.annotation.NonNull;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import java.util.concurrent.TimeUnit;

public final class CloudCodeRequest<T> {
    private final AppAmbitTaskFuture<T> future;
    private final CloudCodeCancellationToken cancellationToken;

    public CloudCodeRequest(
            @NonNull AppAmbitTaskFuture<T> future,
            @NonNull CloudCodeCancellationToken cancellationToken) {
        this.future = future;
        this.cancellationToken = cancellationToken;
    }

    public CloudCodeRequest<T> then(AppAmbitTaskFuture.Callback<T> callback) {
        future.then(value -> {
            if (!cancellationToken.isCancelled()) callback.onSuccess(value);
        });
        return this;
    }

    public CloudCodeRequest<T> onError(AppAmbitTaskFuture.ErrorCallback callback) {
        future.onError(error -> {
            if (!cancellationToken.isCancelled()) callback.onError(error);
        });
        return this;
    }

    public void cancel() {
        cancellationToken.cancel();
        future.fail(CloudCodeError.cancelled());
    }

    public boolean isCancelled() {
        return cancellationToken.isCancelled();
    }

    public T getBlocking() throws InterruptedException {
        return future.getBlocking();
    }

    public T getBlocking(long timeout, TimeUnit unit) throws InterruptedException {
        return future.getBlocking(timeout, unit);
    }

    public boolean isDone() {
        return future.isDone();
    }
}
