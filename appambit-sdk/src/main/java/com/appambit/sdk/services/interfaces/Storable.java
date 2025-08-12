package com.appambit.sdk.services.interfaces;
import java.io.Closeable;
import java.util.List;

import com.appambit.sdk.models.analytics.EventEntity;
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
    void deleteLogList(List<LogEntity> logs);
    List<LogEntity> getOldest100Logs();
    List<EventEntity> getOldest100Events();
    void deleteEventList(List<EventEntity> events);

}