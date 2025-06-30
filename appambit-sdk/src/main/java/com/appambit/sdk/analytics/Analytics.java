package com.appambit.sdk.analytics;

import android.util.Log;
import com.appambit.sdk.core.models.analytics.EventEntity;
import com.appambit.sdk.core.storage.Storable;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class Analytics {

    private static Storable mStorable;
    private static ExecutorService mExecutorService;
    private static boolean isManualSessionEnabled = false;

    public static void Initialize(Storable storable, ExecutorService executorService) {
        mStorable = storable;
        mExecutorService = executorService;
    }

    public static void sendBatchesEvents() {
        mExecutorService.execute(() -> {
            try {
                List<EventEntity> events = mStorable.getOldest100Events();
                if (!events.isEmpty()) {
                    mStorable.deleteEventList(events);
                }
            } catch (Exception ex) {
                Log.e(Analytics.class.getSimpleName(), "Error to process Events", ex);
            }
        });
    }

    public static  void startSession() {
        SessionManager.startSession();
    }

    public static void endSession() {
        SessionManager.endSession();
    }


    public static void enableManualSession() {
        isManualSessionEnabled = true;
    }

    public static boolean isManualSessionEnabled() {
        return isManualSessionEnabled;
    }

    public static void setUserId(String userId) {
        mStorable.putUserId(userId);
    }

    public static void setUserEmail(String userEmail) {
        mStorable.putUserEmail(userEmail);
    }
}
