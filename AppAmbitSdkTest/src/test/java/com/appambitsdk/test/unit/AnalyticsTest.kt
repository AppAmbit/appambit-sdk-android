package com.appambitsdk.test.unit

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Looper
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.appambit.sdk.Analytics
import com.appambit.sdk.AppAmbit
import com.appambit.sdk.SessionManager
import com.appambit.sdk.enums.ApiErrorType
import com.appambit.sdk.enums.SessionType
import com.appambit.sdk.models.analytics.EventEntity
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.models.responses.ApiResult
import com.appambit.sdk.models.responses.EventResponse
import com.appambit.sdk.services.interfaces.ApiService
import com.appambit.sdk.services.interfaces.Storable
import com.appambitsdk.test.unit.utils.DateUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ExecutorService
import android.os.Handler
import com.appambit.sdk.ServiceLocator
import com.appambit.sdk.models.analytics.SessionBatch
import com.appambit.sdk.models.breadcrumbs.BreadcrumbEntity
import com.appambit.sdk.models.responses.EventsBatchResponse
import com.appambit.sdk.services.endpoints.EventBatchEndpoint
import io.mockk.slot
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertTrue
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsTest {

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
        Analytics.Initialize(mockStorable, mockExecutorService, mockApiService)
        Dispatchers.setMain(Dispatchers.Unconfined)
        mockkStatic(Log::class)
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
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start session persists start with default flow`() {
        // Given
        val stored = mutableListOf<SessionData>()
        val session = SessionData()
        session.sessionType = SessionType.START
        session.timestamp = DateUtils.getUtcNow()

        every { mockStorable.putSessionData(session) } answers {
            stored.add(firstArg())
        }
        // When
        mockStorable.putSessionData(session)
        // Then
        assertEquals(1, stored.size)
        assertEquals(SessionType.START, stored[0].sessionType)
        assert(session.timestamp != null)
    }

    @Test
    fun `track event when api fails saves event locally`() {
        every {
            mockApiService.executeRequest(any(), EventResponse::class.java)
        } returns ApiResult(
            EventResponse(), ApiErrorType.NetworkUnavailable, null
        )

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        SessionManager::class.java.getDeclaredField("isSessionActivate").apply {
            isAccessible = true
            setBoolean(null, true)
        }

        val stored = mutableListOf<EventEntity>()
        every { mockStorable.putLogAnalyticsEvent(any()) } answers {
            stored.add(firstArg())
        }
        val log = EventEntity()
        log.id = UUID.randomUUID()

        mockStorable.putLogAnalyticsEvent(log)

        // When
        Analytics.trackEvent("test_event", mapOf("k" to "v"))

        // Then
        assertEquals(1, stored.size)
        verify(exactly = 1) { mockStorable.putLogAnalyticsEvent(any()) }
    }

    @Test
    fun `track event when api ok does not persist locally`() {
        // Given
        every {
            mockApiService.executeRequest(any(), EventResponse::class.java)
        } returns ApiResult(
            EventResponse(), ApiErrorType.None, null
        )

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        SessionManager::class.java.getDeclaredField("isSessionActivate").apply {
            isAccessible = true
            setBoolean(null, true)
        }

        // When
        Analytics.trackEvent("test_event", mapOf("k" to "v"))

        // Then
        assertEquals(0, mockStorable.oldest100Events.size)
        verify(exactly = 0) { mockStorable.putLogEvent(any()) }
    }

    @Test
    fun `manual mode start does not auto start session`() {
        // Given
        val mockApp = mockk<Application>(relaxed = true)
        every { context.applicationContext } returns mockApp
        every { mockApp.registerActivityLifecycleCallbacks(any()) } just runs

        val mockConnectivityManager = mockk<ConnectivityManager>(relaxed = true)
        every {
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns mockConnectivityManager

        mockkConstructor(NetworkRequest.Builder::class)
        val mockBuilder = mockk<NetworkRequest.Builder>(relaxed = true)

        every {
            anyConstructed<NetworkRequest.Builder>().addCapability(any())
        } returns mockBuilder

        every {
            anyConstructed<NetworkRequest.Builder>().addTransportType(any())
        } returns mockBuilder

        val mockNetworkRequest = mockk<NetworkRequest>(relaxed = true)
        every {
            anyConstructed<NetworkRequest.Builder>().build()
        } returns mockNetworkRequest

        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        // When
        Analytics.enableManualSession()
        AppAmbit.start(context, "TEST_KEY")

        // Then
        assert(mockStorable.oldest100Session.isEmpty())
    }

    @Test
    fun `manual mode start and end session explicit`() {

        val storedSessions = mutableListOf<SessionData>()
        val logStart = SessionData()
        logStart.id = UUID.randomUUID()
        logStart.sessionType = SessionType.START

        val logEnd = SessionData()
        logEnd.id = UUID.randomUUID()
        logEnd.sessionType = SessionType.END

        every {
            mockApiService.executeRequest(any(), EventResponse::class.java)
        } returns ApiResult(
            EventResponse(), ApiErrorType.None, null
        )

        every { mockStorable.putSessionData(any()) } answers {
            storedSessions.add(firstArg())
        }
        mockStorable.putSessionData(logStart)

        val field = SessionManager::class.java.getDeclaredField("mExecutorService")
        field.isAccessible = true
        field.set(null, mockExecutorService)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        // When

        Analytics.enableManualSession()
        mockStorable.putSessionData(logEnd)
        SessionManager.startSession()
        SessionManager.endSession()

        // Then
        assertEquals(2, storedSessions.size)
        assertEquals(SessionType.START, storedSessions[0].sessionType)
        assertEquals(SessionType.END,   storedSessions[1].sessionType)
    }

    @Test
    fun `send batch sessions resolves sessionIds and updates tracking`() {
        // Given
        Analytics.Initialize(mockStorable, mockExecutorService, mockApiService)

        val storedEvents = mutableListOf<EventEntity>()
        val storedBreadcrumbs = mutableListOf<BreadcrumbEntity>()

        every { mockStorable.putLogAnalyticsEvent(any()) } answers {
            storedEvents.add(firstArg())
        }

        every { mockStorable.addBreadcrumb(any()) } answers {
            storedBreadcrumbs.add(firstArg())
        }

        every { mockStorable.updateSessionIdsForAllTrackingData(any(), any()) } answers {
            val remoteId = secondArg<String>()
            storedEvents.forEach { it.sessionId = remoteId }
            storedBreadcrumbs.forEach { it.sessionId = remoteId }
        }

        val baseTime = Instant.now()

        val localBatches = (0 until 10).map { i ->
            val start = baseTime.plus(i.toLong(), ChronoUnit.HOURS)
            val end = start.plus(30, ChronoUnit.MINUTES)

            SessionBatch().apply {
                id = "local-$i"
                sessionId = null
                startedAt = Date.from(start)
                endedAt = Date.from(end)
            }
        }

        localBatches.forEach { sb ->
            repeat(25) { j ->
                storedEvents.add(
                    EventEntity().apply {
                        id = UUID.randomUUID()
                        name = "evt-${sb.id}-$j"
                        sessionId = ""
                        createdAt = Date.from(baseTime)
                    }
                )

                storedBreadcrumbs.add(
                    BreadcrumbEntity().apply {
                        id = UUID.randomUUID()
                        name = "bc-${sb.id}-$j"
                        sessionId = ""
                        createdAt = Date.from(baseTime)
                    }
                )
            }
        }

        localBatches.forEachIndexed { idx, sb ->
            mockStorable.updateSessionIdsForAllTrackingData(sb.id, "srv-$idx")
        }

        // Then
        assertEquals(250, storedEvents.size)
        storedEvents.forEach { assertTrue(it.sessionId.contains("srv-")) }

        assertEquals(250, storedBreadcrumbs.size)
        storedBreadcrumbs.forEach { assertTrue(it.sessionId.contains("srv-")) }
    }

    @Test
    fun `send batch events with old events removes events from storage`() {
        // Given
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        every {
            mockApiService.executeRequest(any(), EventsBatchResponse::class.java)
        } returns ApiResult(EventsBatchResponse(), ApiErrorType.None, null)

        Analytics.Initialize(mockStorable, mockExecutorService, mockApiService)

        val backdatedDate = Date.from(
            Instant.now().minus(2, ChronoUnit.DAYS)
        )

        val event = EventEntity().apply {
            id = UUID.randomUUID()
            sessionId = UUID.randomUUID().toString()
            name = "test_event"
            createdAt = backdatedDate
        }

        every { mockStorable.getOldest100Events() } returns listOf(event)

        every { mockStorable.deleteEventList(listOf(event)) } just runs

        mockkStatic(ServiceLocator::class)
        every { ServiceLocator.getStorageService() } returns mockStorable

        every { mockExecutorService.execute(capture(slot())) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
        }

        // When
        Analytics.sendBatchesEvents()

        // Then
        verify(exactly = 1) {
            mockApiService.executeRequest(any<EventBatchEndpoint>(), EventsBatchResponse::class.java)
        }
        verify(exactly = 1) {
            mockStorable.deleteEventList(listOf(event))
        }

        unmockkStatic(ServiceLocator::class)
    }

}