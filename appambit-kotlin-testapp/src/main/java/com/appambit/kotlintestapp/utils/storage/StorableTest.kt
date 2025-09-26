package com.appambit.kotlintestapp.utils.storage

import com.appambit.sdk.models.analytics.SessionData
import java.io.Closeable

interface StorableTest : Closeable {
    fun putSessionData(sessionData: SessionData?)
    fun updateLogSessionId(sessionId: String?)
    fun updateEventSessionId(sessionId: String?)
}