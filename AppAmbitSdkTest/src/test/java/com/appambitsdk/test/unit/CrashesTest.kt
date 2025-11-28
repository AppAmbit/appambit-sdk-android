package com.appambitsdk.test.unit

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.appambit.sdk.Crashes
import com.appambit.sdk.ServiceLocator
import com.appambit.sdk.SessionManager
import com.appambit.sdk.enums.ApiErrorType
import com.appambit.sdk.enums.LogType
import com.appambit.sdk.enums.SessionType
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.models.logs.LogEntity
import com.appambit.sdk.models.responses.ApiResult
import com.appambit.sdk.models.responses.EventResponse
import com.appambit.sdk.services.interfaces.ApiService
import com.appambit.sdk.services.interfaces.Storable
import com.appambit.sdk.utils.DateUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import java.util.concurrent.ExecutorService

@OptIn(ExperimentalCoroutinesApi::class)
class CrashesTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var context: Application

    @RelaxedMockK
    private lateinit var mockStorable: Storable

    @RelaxedMockK
    private lateinit var mockExecutorService: ExecutorService

    @RelaxedMockK
    private lateinit var mockApiService: ApiService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(Dispatchers.Unconfined)
        mockkStatic(Log::class)
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        every { Log.v(any(), any()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)
        setStaticField(SessionManager::class.java, "mExecutorService", mockExecutorService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `log error generates and persists a valid error log`() {
        // Given
        Crashes.Initialize()
        ServiceLocator.initialize(context)

        val log = LogEntity().apply {
            id = UUID.randomUUID()
            type = LogType.ERROR
            message = "boom!"
            createdAt = DateUtils.getUtcNow()
        }

        setStaticField(ServiceLocator::class.java, "apiService", mockApiService)

        every {
            mockApiService.executeRequest(any(), EventResponse::class.java)
        } returns ApiResult(
            EventResponse(), ApiErrorType.None, null
        )

        setStaticField(Crashes::class.java, "mExecutorService", mockExecutorService)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        mockStorable.putLogEvent(log)

        // When
        Crashes.logError("boom!")
        // Then
        assertEquals(LogType.ERROR, log.type)
        assertEquals("boom!", log.message)
        assertTrue(log.createdAt != null)
    }

    @Test
    fun `log error from exception persists crash file path`() {
        // Given
        Crashes.Initialize()
        ServiceLocator.initialize(context)

        val log = LogEntity().apply {
            id = UUID.randomUUID()
            type = LogType.ERROR
            message = "bad!"
            createdAt = DateUtils.getUtcNow()
        }

        setStaticField(ServiceLocator::class.java, "apiService", mockApiService)
        setStaticField(ServiceLocator::class.java, "storable", mockStorable)


        every {
            mockApiService.executeRequest(any(), EventResponse::class.java)
        } returns ApiResult(
            EventResponse(), ApiErrorType.None, null
        )

        setStaticField(Crashes::class.java, "mExecutorService", mockExecutorService)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        mockStorable.putLogEvent(log)

        // When
        ensureSession(mockStorable)
        Crashes.logError(Exception("bad!"))
        // Then
        assertEquals(LogType.ERROR, log.type)
        assertTrue(log.message.contains("bad!"))
        assertTrue(log.createdAt != null)
    }

    @Test
    fun `send batch logs with api success removes logs from storage`() {
        // Given
        Crashes.Initialize()
        ServiceLocator.initialize(context)

        setStaticField(ServiceLocator::class.java, "apiService", mockApiService)

        every {
            mockApiService.executeRequest(any(), EventResponse::class.java)
        } returns ApiResult(
            EventResponse(), ApiErrorType.None, null
        )

        setStaticField(Crashes::class.java, "mExecutorService", mockExecutorService)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        mockStorable.putLogEvent(
            LogEntity().apply {
                id = UUID.randomUUID()
                type = LogType.ERROR
                message = "old-1"
                createdAt = Date.from(
                    Instant.now().minus(1, ChronoUnit.DAYS)
                )
            }
        )
        mockStorable.putLogEvent(
            LogEntity().apply {
                id = UUID.randomUUID()
                type = LogType.ERROR
                message = "old-2"
                createdAt = DateUtils.getUtcNow()
            }
        )

        // When
        Crashes.sendBatchesLogs()

        // Then
        assert(mockStorable.oldest100Logs.isEmpty())
    }

    @Test
    fun `send batch logs with backdated logs still sends batch`() {
        // Given
        Crashes.Initialize()
        ServiceLocator.initialize(context)

        setStaticField(ServiceLocator::class.java, "apiService", mockApiService)

        every {
            mockApiService.executeRequest(any(), EventResponse::class.java)
        } returns ApiResult(
            EventResponse(), ApiErrorType.None, null
        )

        setStaticField(Crashes::class.java, "mExecutorService", mockExecutorService)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        val baseInstant = Instant.now().minus(3, ChronoUnit.DAYS)

        val time1 = Date.from(baseInstant)
        val time2 = Date.from(baseInstant.plus(5, ChronoUnit.MINUTES))

        mockStorable.putLogEvent(
            LogEntity().apply {
                id = UUID.randomUUID()
                type = LogType.ERROR
                message = "past-1"
                createdAt = time1
            }
        )

        mockStorable.putLogEvent(
            LogEntity().apply {
                id = UUID.randomUUID()
                type = LogType.ERROR
                message = "past-2"
                createdAt = time2
            }
        )

        // When
        Crashes.sendBatchesLogs()

        // Then
        assert(mockStorable.oldest100Logs.isEmpty())
    }

    private fun ensureSession(mockStorable: Storable) {
        if (!SessionManager.isSessionActivate) {
            SessionManager.startSession()

            if (!SessionManager.isSessionActivate) {
                setStaticField(SessionManager::class.java, "_isSessionActive", true)
                setStaticField(SessionManager::class.java, "_sessionId", UUID.randomUUID().toString())

                mockStorable.putSessionData(
                    SessionData().apply {
                        id = UUID.randomUUID()
                        sessionId = SessionManager.getSessionId()
                        sessionType = SessionType.START
                        timestamp = DateUtils.getUtcNow()
                    }
                )
            }
        }
    }

    private fun setStaticField(clazz: Class<*>, fieldName: String, value: Any?) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(null, value)
    }

}