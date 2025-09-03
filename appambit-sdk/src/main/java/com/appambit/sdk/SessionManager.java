package com.appambit.sdk;

import static com.appambit.sdk.AppAmbit.safeRun;
import static com.appambit.sdk.utils.FileUtils.deleteSingleObject;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appambit.sdk.models.analytics.SessionPayload;
import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.enums.SessionType;
import com.appambit.sdk.models.analytics.SessionBatch;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.models.responses.EndSessionResponse;
import com.appambit.sdk.models.responses.StartSessionResponse;
import com.appambit.sdk.services.endpoints.EndSessionEndpoint;
import com.appambit.sdk.services.endpoints.SessionBatchEndpoint;
import com.appambit.sdk.services.endpoints.StartSessionEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.FileUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class SessionManager {
    private static final String TAG = SessionManager.class.getSimpleName();
    private static ApiService mApiService;
    private static ExecutorService mExecutorService;
    private static final Object SESSION_LOCK = new Object();
    private static boolean isSendingBatch = false;
    private static String currentSessionId;
    private static final List<Runnable> sessionWaiters = new ArrayList<>();
    private static Storable mStorageService;
    public static boolean isSessionActivate = false;

    public static void initialize(ApiService apiService, ExecutorService executorService, Storable storageService) {
        mApiService = apiService;
        mExecutorService = executorService;
        mStorageService = storageService;
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
                currentSessionId = UUID.randomUUID().toString();
                saveLocallyStartSession(utcNow);
                Log.d(TAG, "Start Session - save locally");
                isSessionActivate = true;
                return;
            }
            currentSessionId = result.data != null ? result.data.getSessionId() : UUID.randomUUID().toString();
        });
        isSessionActivate = true;
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
        sessionData.setSessionId(currentSessionId);

        closeCurrentSession(sessionData);

    }

    public static void sendEndSessionIfExists() {

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
            endSession.setSessionId(currentSessionId);
            endSession.setTimestamp(DateUtils.getUtcNow());
            endSession.setSessionType(SessionType.END);

            mExecutorService.execute(() -> {
                try {
                    FileUtils.saveToFile(endSession);
                    Log.d(TAG, "End session saved locally");
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
        FileUtils.deleteSingleObject(SessionData.class);
    }

    public static void sendBatchSessions(@Nullable Runnable onSuccess) {
        synchronized (SESSION_LOCK) {
            if (isSendingBatch) {
                if (onSuccess != null) sessionWaiters.add(onSuccess);
                Log.d(TAG, "Session batch already in progress, callback queued");
                return;
            }
            isSendingBatch = true;
            if (onSuccess != null) sessionWaiters.add(onSuccess);
        }

        Log.d(TAG, "Send session batch...");

        Runnable batchSessionTask = () -> {
            try {
                List<SessionBatch> sessions = mStorageService.getOldest100Session();

                if (sessions.isEmpty()) {
                    Log.d(TAG, "No offline sessions to send");
                    finishSessionOperation(true);
                    return;
                }

                AppAmbitTaskFuture<ApiResult<SessionBatch>> response = sendBatchEndpoint(sessions);

                response.then(result -> {
                    if (result.errorType != ApiErrorType.None) {
                        Log.d(TAG, "Unset sessions");
                        finishSessionOperation(true);
                        return;
                    }

                    List<SessionBatch> sorted = new ArrayList<>();
                    if (result.data instanceof List) {
                        sorted.addAll((List<SessionBatch>) result.data);
                    } else if (result.data != null) {
                        sorted.add(result.data);
                    }
                    updateOfflineSessionsLogsEvents(sorted, sessions);
                    finishSessionOperation(true);
                });

                response.onError(error -> {
                    Log.d(TAG, "Error to Call End Session", error);
                    finishSessionOperation(false);
                });
            } catch (Throwable t) {
                Log.e(TAG, "Unexpected error in session batch processing", t);
                finishSessionOperation(false);
            }
        };
        batchSessionTask.run();
    }

    public static void saveSessionEndToDatabaseIfExist() {
        SessionData sessionData = FileUtils.getSavedSingleObject(SessionData.class);

        if(sessionData != null && sessionData.getSessionId() != null
           && !TextUtils.isDigitsOnly(sessionData.getSessionId())
           && mStorageService.isSessionOpen()) {
            mStorageService.putSessionData(sessionData);
            FileUtils.deleteSingleObject(SessionData.class);
            Log.d(TAG, "Saved end session from file to database");
        }
    }

    public static void sendUnpairedSessions(Runnable onComplete) {

        List<SessionData> unpairedSessions = mStorageService.getSessionsEnd();

        if (unpairedSessions.isEmpty()) {
            Log.d(TAG, "No unpaired sessions to send");
            if (onComplete != null) safeRun(onComplete);
            return;
        }

        for(SessionData sessionData : unpairedSessions) {
            sendSession(sessionData);
        }
        Log.d(TAG, "All unpaired sessions sent successfully");
        if (onComplete != null) safeRun(onComplete);
    }

    private static void sendSession(SessionData sessionData) {

        if(sessionData.getSessionType() == SessionType.END) {
            AppAmbitTaskFuture<ApiResult<EndSessionResponse>> response = sendEndSessionEndpoint(sessionData);

            response.then(result -> {
                if (result.errorType == ApiErrorType.None) {
                    Log.d(TAG, "Unpaired session sent successfully, deleting " + sessionData.getSessionId());
                    mStorageService.deleteSessionById(sessionData.getId());
                }
            });

            response.onError(error -> {
                Log.d(TAG, "Error to Call End Session");
            });
        }else {
            AppAmbitTaskFuture<ApiResult<StartSessionResponse>> response = sendStartSessionEndpoint(sessionData.getTimestamp());

            response.then(result -> {
                if (result.errorType == ApiErrorType.None) {
                    Log.d(TAG, "Unpaired session sent successfully, deleting " + sessionData.getSessionId());
                    mStorageService.deleteSessionById(sessionData.getId());
                }
            });

            response.onError(error -> {
                Log.d(TAG, "Error to Call End Session");
            });
        }

    }

    public static void sendSessionEndIfExist() {

        sendUnpairedSessions(null);

        SessionData sessionData = mStorageService.getLastStartSession();

        if(sessionData == null) {
            return;
        }

        AppAmbitTaskFuture<ApiResult<StartSessionResponse>> response = sendStartSessionEndpoint(sessionData.getTimestamp());

        response.then(result -> {
            if (result.errorType == ApiErrorType.None) {
                Log.d(TAG, "Start session sent successfully, deleting " + sessionData.getId());
                currentSessionId = result.data.getSessionId();
                mStorageService.updateLogsAndEventsId(sessionData.getId().toString(), currentSessionId);
                mStorageService.deleteSessionById(sessionData.getId());
                Crashes.sendBatchesLogs();
                Analytics.sendBatchesEvents();
            }
        });

        response.onError(error -> Log.d(TAG, "Error to Call Start Session"));
    }

    private static void updateOfflineSessionsLogsEvents(@NonNull List<SessionBatch> sorted, List<SessionBatch> sessions) {

        if (sorted.isEmpty()) {
            Log.d(TAG, "No session batches to send");
            return;
        }

        Map<String, SessionBatch> remoteFingerprintMap = new HashMap<>();
        for (SessionBatch remote : sorted) {
            String fingerprint = buildFingerprint(remote.getStartedAt(), remote.getEndedAt());
            remoteFingerprintMap.put(fingerprint, remote);
        }

        for (SessionBatch local : sessions) {
            String localFingerprint = buildFingerprint(local.getStartedAt(), local.getEndedAt());

            if (remoteFingerprintMap.containsKey(localFingerprint)) {
                SessionBatch remoteMatch = remoteFingerprintMap.get(localFingerprint);

                String localId = local.getId();
                assert remoteMatch != null;
                String remoteId = remoteMatch.getSessionId();

                Log.d(TAG, "Match -> localId: " + localId + " remoteId: " + remoteId);

                mStorageService.updateLogsAndEventsId(localId, remoteId);
            }
        }
        if(!sessions.isEmpty()) {
            AppAmbitTaskFuture<Void> deleteFuture = deleteSessions(sessions);
            deleteFuture.complete(null);
            deleteFuture.then(result -> Log.d(TAG, "Sessions deleted successfully"));
            deleteFuture.onError(error -> Log.e(TAG, "Error sessions logs", error));
        }
    }

    @NonNull
    private static AppAmbitTaskFuture<Void> deleteSessions(List<SessionBatch> sessions) {
        AppAmbitTaskFuture<Void> future = new AppAmbitTaskFuture<>();
        try {
            mStorageService.deleteSessionList(sessions);
            future.complete(null);
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    @NonNull
    private static String buildFingerprint(Date startedAt, Date endedAt) {
        String started = DateUtils.toIsoUtcNoMillis(startedAt);
        String ended = DateUtils.toIsoUtcNoMillis(endedAt);
        return started + "-" + ended;
    }

    private static void saveLocallyStartSession(Date dateUtc) {
        SessionData sessionData = new SessionData();
        sessionData.setId(UUID.fromString(currentSessionId));
        sessionData.setSessionType(SessionType.START);
        sessionData.setTimestamp(dateUtc);

        if(TextUtils.isDigitsOnly(currentSessionId)) {
            sessionData.setSessionId(currentSessionId);
        }else {
            sessionData.setSessionId(null);
        }

        mStorageService.putSessionData(sessionData);
    }

    private static void closeCurrentSession(SessionData sessionData) {

        if(!TextUtils.isDigitsOnly(sessionData.getSessionId())) {
            sessionData.setSessionId(null);
        }

        AppAmbitTaskFuture<ApiResult<EndSessionResponse>> response = sendEndSessionEndpoint(sessionData);

        response.then(result -> {
            if (result.errorType != ApiErrorType.None) {
                mStorageService.putSessionData(sessionData);
                Log.d(TAG, "End Session - saved locally and file deleted");
            }
            deleteSingleObject(SessionData.class);
        });

        response.onError(error -> {
            Log.d(TAG, "Error to Call End Session");
        });

        isSessionActivate = false;
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

    private static AppAmbitTaskFuture<ApiResult<SessionBatch>> sendBatchEndpoint(List<SessionBatch> sessionBatches) {
        AppAmbitTaskFuture<ApiResult<SessionBatch>> response = new AppAmbitTaskFuture<>();
        mExecutorService.execute(() -> {
            try {
                SessionPayload sessionPayload = new SessionPayload();
                sessionPayload.setSessions(sessionBatches);
                ApiResult<SessionBatch> apiResponse = mApiService.executeRequest(
                        new SessionBatchEndpoint(sessionPayload), SessionBatch.class);
                response.complete(apiResponse);
            } catch (Exception e) {
                response.fail(e);
            }
        });

        return response;
    }

    private static void finishSessionOperation(boolean success) {
        List<Runnable> callbacks;
        synchronized (SESSION_LOCK) {
            isSendingBatch = false;
            callbacks = new ArrayList<>(sessionWaiters);
            sessionWaiters.clear();
        }
        if (success) {
            for (Runnable r : callbacks) safeRun(r);
        } else {
            Log.d(TAG, "Session batch operation failed; callbacks dropped");
        }
    }

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    public static void setCurrentSessionId(String sessionId) {
        currentSessionId = sessionId;
    }

}
