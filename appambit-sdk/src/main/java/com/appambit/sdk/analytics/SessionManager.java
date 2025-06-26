package com.appambit.sdk.analytics;

import android.util.Log;

import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.enums.SessionType;
import com.appambit.sdk.core.models.analytics.SessionData;
import com.appambit.sdk.core.models.responses.ApiResult;
import com.appambit.sdk.core.models.responses.EndSessionResponse;
import com.appambit.sdk.core.models.responses.EventsBatchResponse;
import com.appambit.sdk.core.models.responses.StartSessionResponse;
import com.appambit.sdk.core.services.ApiService;
import com.appambit.sdk.core.services.endpoints.EndSessionEndpoint;
import com.appambit.sdk.core.services.endpoints.SessionBatchEndpoint;
import com.appambit.sdk.core.services.endpoints.StartSessionEndpoint;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.core.utils.DateUtils;
import com.appambit.sdk.core.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class SessionManager {
    private static final String TAG = SessionManager.class.getSimpleName();
    private static ApiService mApiService;
    private static ExecutorService  mExecutorService;
    private static String sessionId;
    public static boolean isSessionActivate = false;
    private static final String offlineSessionsFile = "OfflineSessions";


    public static void initialize(ApiService apiService, ExecutorService executorService) {
        mApiService = apiService;
        mExecutorService = executorService;
    }

    public static void startSession() {
        Log.d(TAG, "Start Session Called");

        if (isSessionActivate) {
            return;
        }

        Date utcNow = DateUtils.getUtcNow();

        AppAmbitTaskFuture<ApiResult<StartSessionResponse>> response = sendStartSessionEndpoint(utcNow);


        response.then(result -> {
            if (result.errorType != ApiErrorType.None) {
                saveLocallyStartSession(utcNow);
                isSessionActivate = true;
                Log.d(TAG, "Start Session - save locally");
                return;
            }

            sessionId = result.data.getSessionId();
            isSessionActivate = true;
        });

        response.onError(error -> {
            Log.d(TAG, Objects.requireNonNull(error.getMessage()));
        });
    }

    public static void endSession() {
        if (!isSessionActivate) {
            return;
        }

        SessionData sessionData = new SessionData();
        sessionData.setId(UUID.randomUUID());
        sessionData.setSessionType(SessionType.END);
        sessionData.setTimestamp(DateUtils.getUtcNow());
        sessionData.setSessionId(sessionId);

        closeCurrentSession(sessionData);

    }

    public static void sendEndSessionIfExists() {

        String file = FileUtils.getFilePath(FileUtils.getFileName(SessionData.class));
        Log.d(TAG, "File: " + file);

        SessionData sessionData = FileUtils.getSavedSingleObject(SessionData.class);

        if (sessionData == null) {
            return;
        }

        closeCurrentSession(sessionData);
    }

    public static void saveEndSession() {
        try {
            SessionData endSession = new SessionData();
            endSession.setId(UUID.randomUUID());
            endSession.setSessionId(sessionId);
            endSession.setTimestamp(DateUtils.getUtcNow());
            endSession.setSessionType(SessionType.END);

            mExecutorService.execute(() -> {
                try {
                    FileUtils.saveToFile(endSession);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            });

        } catch (Exception ex) {
            Log.e(TAG, "Error in saveEndSession: " + ex.getMessage(), ex);
        }
    }

    public static boolean isSessionActivate() {
        return isSessionActivate;
    }

    public static void removeSavedEndSession() {
        FileUtils.getSavedSingleObject(SessionData.class);
    }

    public static void sendBatchSessions() {
        Log.d(TAG, "Send session batch...");

        List<SessionData> sessions = FileUtils.getSaveJsonArray(offlineSessionsFile, SessionData.class, null);

        if (sessions.isEmpty()) {
            return;
        }

        List<SessionBatch> sessionBatches = buildSessionBatches(sessions);

        AppAmbitTaskFuture<ApiResult<EventsBatchResponse>> response = sendBatchEndpoint(sessionBatches);

        response.then(result -> {
            if (result.errorType != ApiErrorType.None) {
                Log.d(TAG, "Unset sessions");
                return;
            }

            updateOfflineSessionsFile(sessions);
        });

        response.onError(error -> {
            Log.d(TAG, "Error to Call End Session");
        });
    }

    public static List<SessionBatch> buildSessionBatches(List<SessionData> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }

        List<SessionData> sorted = new ArrayList<>(sessions);
        Collections.sort(sorted, (o1, o2) -> {
            Date t1 = o1.getTimestamp();
            Date t2 = o2.getTimestamp();
            return t1.compareTo(t2);
        });

        List<SessionData> batchSessions = setLimitListSessionData(sorted);

        List<Date> starts = new ArrayList<>();
        List<Date> ends   = new ArrayList<>();
        for (SessionData sd : batchSessions) {
            if (sd.getSessionType() == SessionType.START) {
                starts.add(sd.getTimestamp());
            } else if (sd.getSessionType() == SessionType.END) {
                ends.add(sd.getTimestamp());
            }
        }

        int pairs = Math.min(starts.size(), ends.size());
        List<SessionBatch> result = new ArrayList<>(pairs);
        for (int i = 0; i < pairs; i++) {
            SessionBatch batch = new SessionBatch();
            batch.setStartedAt(starts.get(i));
            batch.setEndedAt(ends.get(i));
            result.add(batch);
        }

        return result;
    }

    private static List<SessionData> setLimitListSessionData(List<SessionData> sessionData) {
        int limit = Math.min(200, sessionData.size());
        return sessionData.subList(0, limit);
    }

    private static void updateOfflineSessionsFile(List<SessionData> sessions) {
        List<SessionData> remaining = skipAndTake(sessions, 200, 200);

        FileUtils.updateJsonArray(offlineSessionsFile, remaining);
    }

    private static List<SessionData> skipAndTake(List<SessionData> sessionData, int skip, int take) {
        if (sessionData == null || sessionData.isEmpty()) return Collections.emptyList();

        int fromIndex = Math.min(skip, sessionData.size());
        int toIndex = Math.min(fromIndex + take, sessionData.size());

        return sessionData.subList(fromIndex, toIndex);
    }


    private static void saveLocallyStartSession(Date dateUtc) {
        SessionData sessionData = new SessionData();
        sessionData.setId(UUID.randomUUID());
        sessionData.setSessionType(SessionType.START);
        sessionData.setTimestamp(dateUtc);
        sessionData.setSessionId(sessionId);

        FileUtils.getSaveJsonArray(offlineSessionsFile, SessionData.class, sessionData);
    }

    private static void closeCurrentSession(SessionData sessionData) {

        AppAmbitTaskFuture<ApiResult<EndSessionResponse>> response = sendEndSessionEndpoint(sessionData);

        response.then(result -> {
            if (result.errorType != ApiErrorType.None) {
                saveLocalEndSession(sessionData);
            }
        });

        response.onError(error -> {
            Log.d(TAG, "Error to Call End Session");
        });

        sessionId = "";
        isSessionActivate = false;
    }

    private static void saveLocalEndSession(SessionData sessionData) {
        FileUtils.getSaveJsonArray(offlineSessionsFile, SessionData.class, sessionData);
    }

    private static AppAmbitTaskFuture<ApiResult<StartSessionResponse>> sendStartSessionEndpoint(Date utcNow) {
        AppAmbitTaskFuture<ApiResult<StartSessionResponse>> result = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                ApiResult<StartSessionResponse> apiResponse =
                        mApiService.executeRequest(new StartSessionEndpoint(utcNow), StartSessionResponse.class);

                result.complete(apiResponse);
            } catch (Exception e) {
                result.fail(e);
            }
        });

        return result;
    }

    private static AppAmbitTaskFuture<ApiResult<EndSessionResponse>> sendEndSessionEndpoint(SessionData sessionData) {
        AppAmbitTaskFuture<ApiResult<EndSessionResponse>> result = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                ApiResult<EndSessionResponse> apiResponse = mApiService.executeRequest(new EndSessionEndpoint(sessionData), EndSessionResponse.class);
                result.complete(apiResponse);
            } catch (Exception e) {
                result.fail(e);
            }
        });

        return result;
    }

    private static AppAmbitTaskFuture<ApiResult<EventsBatchResponse>> sendBatchEndpoint(List<SessionBatch> sessionBatches) {
        AppAmbitTaskFuture<ApiResult<EventsBatchResponse>> response = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                SessionPayload sessionPayload = new SessionPayload();
                sessionPayload.setSessions(sessionBatches);
                ApiResult<EventsBatchResponse> apiResponse = mApiService.executeRequest(
                        new SessionBatchEndpoint(sessionPayload), EventsBatchResponse.class);
                response.complete(apiResponse);
            } catch (Exception e) {
                response.fail(e);
            }
        });

        return response;
    }
}
