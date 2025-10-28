package com.appambit.sdk;

import static com.appambit.sdk.AppConstants.TRACK_EVENT_NAME_MAX_LIMIT;

import android.util.Log;

import androidx.annotation.NonNull;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.analytics.Event;
import com.appambit.sdk.models.analytics.EventEntity;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.models.responses.EventResponse;
import com.appambit.sdk.models.responses.EventsBatchResponse;
import com.appambit.sdk.services.endpoints.EventBatchEndpoint;
import com.appambit.sdk.services.endpoints.EventEndpoint;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.DateUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public final class Analytics {
    private static final String TAG = Analytics.class.getSimpleName();

    private static Storable mStorable;
    private static ExecutorService mExecutorService;
    private static ApiService mApiService;
    private static boolean isManualSessionEnabled = false;

    public static void Initialize(Storable storable, ExecutorService executorService, ApiService apiService) {
        mStorable = storable;
        mExecutorService = executorService;
        mApiService = apiService;
    }

    public static void startSession() {
        SessionManager.startSession();
    }

    public static void endSession() {
        SessionManager.endSession();
    }

    public static void trackEvent(@NonNull String eventTitle, Map<String, String> data) {
        SendOrSaveEvent(eventTitle, data);
    }

    public static void sendBatchesEvents() {
        mExecutorService.execute(() -> {
            List<EventEntity> events = mStorable.getOldest100Events();

            if (events.isEmpty()) {
                Log.d(TAG, "No events to send");
                return;
            }

            try {
                ApiResult<EventsBatchResponse> responseApi = mApiService
                        .executeRequest(new EventBatchEndpoint(events), EventsBatchResponse.class);
                if (responseApi.errorType != ApiErrorType.None) {
                    return;
                }

            } catch (Exception e) {
                Log.d(TAG, "Error sending event batches - Api" + e.getMessage());
            }

            Log.d(TAG, "Event batch sent");
            AppAmbitTaskFuture<Void> deleteEvents = deleteEvents(events);
            deleteEvents.then(v -> deleteEvents.complete(null));

            deleteEvents.onError(errorDelete -> Log.d(TAG, "Error to delete event batch"));
        });

    }

    public static void generateTestEvent() {
        Map<String, String> map = new HashMap<>();
        map.put("Event", "Custom Event");
        SendOrSaveEvent("Test Event", map);
    }

    public static void enableManualSession() {
        isManualSessionEnabled = true;
    }

    public static boolean isManualSessionEnabled() {
        return isManualSessionEnabled;
    }

    private static void SendOrSaveEvent(String eventTitle, Map<String, String> data) {
        if (!SessionManager.isSessionActivate) {
            return;
        }
        data = processData(data);

        eventTitle = truncate(eventTitle, TRACK_EVENT_NAME_MAX_LIMIT);

        Event eventRequest = new Event();

        eventRequest.setName(eventTitle);
        eventRequest.setData(data);

        AppAmbitTaskFuture<Void> appAmbitTaskFuture = new AppAmbitTaskFuture<>();

        mExecutorService.execute(() -> {
            try {

                ApiResult<EventResponse> response = mApiService.executeRequest(new EventEndpoint(eventRequest), EventResponse.class);

                if (response == null || response.errorType != ApiErrorType.None) {
                    EventEntity toSaveEvent = new EventEntity();
                    toSaveEvent.setId(UUID.randomUUID());
                    toSaveEvent.setSessionId(SessionManager.getSessionId());
                    toSaveEvent.setName(eventRequest.getName());
                    toSaveEvent.setCreatedAt(DateUtils.getUtcNow());
                    toSaveEvent.setData(eventRequest.getData());

                    AppAmbitTaskFuture<Void> saveFuture = saveEventLocally(toSaveEvent);

                    saveFuture.then(v -> saveFuture.complete(null));
                    saveFuture.onError(saveFuture::fail);
                }
                appAmbitTaskFuture.then(result -> {
                    Log.d(TAG, "Event sent: " + eventRequest.getName());
                });
            } catch (Exception e) {
                appAmbitTaskFuture.onError(error -> {
                    Log.d(TAG, "Error sending event - Api: " + e.getMessage());
                });
            }
        });
    }

    private static Map<String, String> processData(Map<String, String> data) {
        Map<String, String> input = (data != null ? data : new HashMap<>());
        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (result.size() >= AppConstants.TRACK_EVENT_MAX_PROPERTY_LIMIT) {
                break;
            }

            String rawValue = entry.getValue();
            if (rawValue == null) {
                continue;
            }
            String trimmedValue = rawValue.trim();
            if (trimmedValue.isEmpty()) {
                continue;
            }

            String rawKey = entry.getKey();
            if (rawKey == null) {
                continue;
            }

            String truncatedKey = truncate(rawKey, AppConstants.TRACK_EVENT_PROPERTY_MAX_CHARACTERS);
            if (truncatedKey == null || truncatedKey.trim().isEmpty()) {
                continue;
            }

            if (result.containsKey(truncatedKey)) {
                continue;
            }

            String truncatedValue = truncate(trimmedValue, AppConstants.TRACK_EVENT_PROPERTY_MAX_CHARACTERS);
            result.put(truncatedKey, truncatedValue);
        }

        return result;
    }


    private static AppAmbitTaskFuture<Void> saveEventLocally(EventEntity entity) {
        AppAmbitTaskFuture<Void> future = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                ServiceLocator.getStorageService()
                        .putLogAnalyticsEvent(entity);
                future.complete(null);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private static AppAmbitTaskFuture<Void> deleteEvents(List<EventEntity> events) {
        AppAmbitTaskFuture<Void> future = new AppAmbitTaskFuture<>();
        try {
            ServiceLocator.getStorageService()
                    .deleteEventList(events);
            future.complete(null);
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return value.length() <= maxLength ? value : value.substring(0, maxLength);

    }

    public static void setUserId(String userId) {
        mStorable.putUserId(userId);
    }

    public static void setUserEmail(String userEmail) {
        mStorable.putUserEmail(userEmail);
    }

    public static void clearToken() {
        ServiceLocator.getApiService().setToken("");
    }
}
