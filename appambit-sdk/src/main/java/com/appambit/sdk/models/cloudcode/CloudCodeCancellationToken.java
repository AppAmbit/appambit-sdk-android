package com.appambit.sdk.models.cloudcode;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CloudCodeCancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
