package com.appambit.sdk.core.storage;
import java.io.Closeable;
import java.util.List;

import com.appambit.sdk.core.models.analytics.EventEntity;
import com.appambit.sdk.core.models.logs.LogEntity;
public interface StorageService extends Closeable {

    void putDeviceId(String deviceId);

    String getDeviceId();

    void putAppId(String appId);

    String getAppId();

    void putUserId(String userId);

    String getUserId();

    void putUserEmail(String email);

    String getUserEmail();

    void putSessionId(String sessionId);

    String getSessionId();

    void putLogEvent(LogEntity logEntity);

    void putLogAnalyticsEvent(EventEntity logEntity);

    void deleteLogList(List<LogEntity> logs);

    List<LogEntity> getOldest100Logs();

    List<EventEntity> getOldest100Events();

    void deleteEventList(List<EventEntity> events);

}