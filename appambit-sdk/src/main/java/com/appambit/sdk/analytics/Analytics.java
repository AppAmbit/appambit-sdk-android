package com.appambit.sdk.analytics;

import static com.appambit.sdk.core.AppConstants.TRACK_EVENT_NAME_MAX_LIMIT;

import android.util.Log;

import com.appambit.sdk.core.AppConstants;
import com.appambit.sdk.core.ServiceLocator;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.models.analytics.Event;
import com.appambit.sdk.core.models.analytics.EventEntity;
import com.appambit.sdk.core.api.interfaces.ApiService;
import com.appambit.sdk.core.models.responses.ApiResult;
import com.appambit.sdk.core.models.responses.EventResponse;
import com.appambit.sdk.core.models.responses.EventsBatchResponse;
import com.appambit.sdk.core.api.endpoints.EventBatchEndpoint;
import com.appambit.sdk.core.api.endpoints.EventEndpoint;
import com.appambit.sdk.core.storage.Storable;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.core.utils.DateUtils;

import java.util.Date;
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

    public static void trackEvent(String eventTitle, Map<String, String> data, Date createdAt) {
        SendOrSaveEvent(eventTitle, data, createdAt);
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

            }catch (Exception e) {
                Log.d(TAG, "Error sending event batches - Api" + e.getMessage());
            }

            Log.d(TAG, "Event batch sent");
            AppAmbitTaskFuture<Void> deleteEvents = deleteEvents(events);
            deleteEvents.then(v -> deleteEvents.complete(null));

            deleteEvents.onError(erroDelete -> Log.d(TAG, "Error to delete event batch"));
        });

    }

    public static void generateTestEvent() {
        Map<String, String> map = new HashMap<>();
        map.put("Event", "Custom Event");
        SendOrSaveEvent("Test Event", map, null);
    }

    public static void enableManualSession() {
        isManualSessionEnabled = true;
    }

    public static boolean isManualSessionEnabled() {
        return isManualSessionEnabled;
    }

    private static void SendOrSaveEvent(String eventTitle, Map<String, String> data, Date createdAt) {
        data = processData(data);

        eventTitle = truncate(eventTitle, TRACK_EVENT_NAME_MAX_LIMIT);

        Event eventRequest = new Event();

        eventRequest.setName(eventTitle);
        eventRequest.setData(data);

        AppAmbitTaskFuture<ApiResult<EventResponse>> response = sendEventEndpoint(eventRequest);

        response.then(result -> {

            if (result.errorType != ApiErrorType.None) {
                EventEntity toSaveEvent = new EventEntity();
                toSaveEvent.setId(UUID.randomUUID());
                toSaveEvent.setName(eventRequest.getName());
                toSaveEvent.setCreatedAt(createdAt != null ? createdAt : DateUtils.getUtcNow());
                toSaveEvent.setData(eventRequest.getData());

                AppAmbitTaskFuture<Void> saveFuture = saveEventLocally(toSaveEvent);

                saveFuture.then(v -> saveFuture.complete(null));
                saveFuture.onError(saveFuture::fail);

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

            String truncatedKey = truncate(entry.getKey(), AppConstants.TRACK_EVENT_PROPERTY_MAX_CHARACTERS);
            if (result.containsKey(truncatedKey)) {
                continue;
            }

            String truncatedValue = truncate(entry.getValue(), AppConstants.TRACK_EVENT_PROPERTY_MAX_CHARACTERS);
            result.put(truncatedKey, truncatedValue);
        }

        return result;
    }

    private static AppAmbitTaskFuture<ApiResult<EventResponse>> sendEventEndpoint(Event event) {
        AppAmbitTaskFuture<ApiResult<EventResponse>> result = new AppAmbitTaskFuture<>();

        mExecutorService.execute(() -> {
            try {
                ApiResult<EventResponse> apiResponse = mApiService
                        .executeRequest(new EventEndpoint(event), EventResponse.class);

                result.complete(apiResponse);
            } catch (Exception e) {
                result.fail(e);
            }
        });

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

}
