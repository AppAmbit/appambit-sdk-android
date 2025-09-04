package com.appambit.testapp.utils.storage;

import com.appambit.sdk.models.analytics.SessionData;

import java.io.Closeable;

public interface StorableTest extends Closeable {
    void putSessionData(SessionData sessionData);
    void updateLogSessionId(String sessionId);
    void updateEventSessionId(String sessionId);
    void updateAllEventsWithSessionId(String sessionId);
}