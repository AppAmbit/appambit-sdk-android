package com.appambit.sdk.services.interfaces;

import java.io.Closeable;
import java.util.List;
import java.util.UUID;

import com.appambit.sdk.models.analytics.EventEntity;
import com.appambit.sdk.models.analytics.SessionBatch;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.models.logs.LogEntity;

public interface Storable extends Closeable {

    void putDeviceId(String deviceId);

    String getDeviceId();

    void putAppId(String appId);

    String getAppId();

    void putUserId(String userId);

    String getUserId();

    void putUserEmail(String email);

    String getUserEmail();

    void putConsumerId(String consumerId);

    String getConsumerId();

    void putSessionId(String sessionId);

    String getSessionId();

    void putLogEvent(LogEntity logEntity);

    void putLogAnalyticsEvent(EventEntity logEntity);

    void putSessionData(SessionData sessionData);

    List<SessionBatch> getOldest100Session();

    void deleteSessionList(List<SessionBatch> sessions);

    void deleteSessionById(UUID sessionId);

    boolean isSessionOpen();

    SessionData getLastStartSession();
    void updateLogsAndEventsId(String localId, String remoteId);

    List<SessionData> getSessionEnd();

    void deleteLogList(List<LogEntity> logs);

    List<LogEntity> getOldest100Logs();

    List<EventEntity> getOldest100Events();

    void deleteEventList(List<EventEntity> events);

}