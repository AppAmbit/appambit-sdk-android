package com.appambit.sdk.analytics;

import android.util.Log;
import com.appambit.sdk.core.models.analytics.EventEntity;
import com.appambit.sdk.core.services.interfaces.ApiService;
import com.appambit.sdk.core.storage.Storable;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class Analytics {

    private static Storable mStorable;
    private static ExecutorService mExecutorService;
    private static final String TAG = Analytics.class.getSimpleName();

    public static void Initialize(Storable storable, ExecutorService executorService, ApiService apiService) {
        mStorable = storable;
        mExecutorService = executorService;
    }

    public static AppAmbitTaskFuture<Void> sendBatchesEvents() {
        mExecutorService.execute(() -> {
            AppAmbitTaskFuture<Void> appAmbitTaskFuture = new AppAmbitTaskFuture<>();
            try {
                List<EventEntity> events = mStorable.getOldest100Events();
                if (!events.isEmpty()) {
                    mStorable.deleteEventList(events);
                    appAmbitTaskFuture.then(result -> Log.d(TAG, "Events sent successfully: " + events.size()));
                }
            } catch (Exception ex) {
                appAmbitTaskFuture.then(result -> Log.e(TAG, "Error to process Events", ex));
            }
        });
        return null;
    }

    public static void setUserId(String userId) {
        mStorable.putUserId(userId);
    }

    public static void setUserEmail(String userEmail) {
        mStorable.putUserEmail(userEmail);
    }
}