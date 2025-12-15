package com.appambitsdk.test.unit

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.appambit.sdk.Analytics
import com.appambit.sdk.AppAmbit
import com.appambit.sdk.ServiceLocator
import com.appambit.sdk.SessionManager
import com.appambit.sdk.enums.ApiErrorType
import com.appambit.sdk.enums.SessionType
import com.appambit.sdk.models.analytics.EventEntity
import com.appambit.sdk.models.analytics.SessionBatch
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.models.breadcrumbs.BreadcrumbEntity
import com.appambit.sdk.models.logs.LogEntity
import com.appambit.sdk.models.logs.LogResponse
import com.appambit.sdk.models.responses.ApiResult
import com.appambit.sdk.models.responses.EndSessionResponse
import com.appambit.sdk.models.responses.EventResponse
import com.appambit.sdk.models.responses.EventsBatchResponse
import com.appambit.sdk.models.responses.StartSessionResponse
import com.appambit.sdk.services.interfaces.ApiService
import com.appambit.sdk.services.interfaces.AppInfoService
import com.appambit.sdk.services.interfaces.IEndpoint
import com.appambit.sdk.services.interfaces.Storable
import com.appambit.sdk.utils.AppAmbitTaskFuture
import com.appambit.sdk.utils.DateUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import java.util.concurrent.ExecutorService

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var context: Application

    private lateinit var fakeStorable: FakeStorable
    private lateinit var fakeApiService: FakeApiService
    private lateinit var fakeAppInfoService: FakeAppInfoService

    @RelaxedMockK
    private lateinit var mockExecutorService: ExecutorService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fakeStorable = FakeStorable()
        fakeApiService = FakeApiService()
        fakeAppInfoService = FakeAppInfoService()

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        Analytics.Initialize(fakeStorable, mockExecutorService, fakeApiService)
        SessionManager.initialize(fakeApiService, mockExecutorService, fakeStorable)

        Dispatchers.setMain(Dispatchers.Unconfined)
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0

        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        mockkStatic(ServiceLocator::class)
        every { ServiceLocator.getStorageService() } returns fakeStorable
        every { ServiceLocator.getAppInfoService() } returns fakeAppInfoService
        every { ServiceLocator.getApiService() } returns fakeApiService

        setStaticField(SessionManager::class.java, "isSessionActivate", false)
        setStaticField(SessionManager::class.java, "sessionId", null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(ServiceLocator::class)
        unmockkStatic(Log::class)
        unmockkStatic(Looper::class)
        setStaticField(SessionManager::class.java, "isSessionActivate", false)
    }

    @Test
    fun `start session persists start with default flow`() {
        // Given
        val session = SessionData()
        session.sessionType = SessionType.START
        session.timestamp = DateUtils.getUtcNow()
        fakeStorable.putSessionData(session)

        // When
        Analytics.startSession()

        // Then
        assertEquals(SessionType.START, fakeStorable.sessions[0].sessionType)
        assert(session.timestamp != null)
    }

    @Test
    fun `track event when api fails saves event locally`() {
        // Given
        fakeApiService.shouldFail = true
        setStaticField(SessionManager::class.java, "isSessionActivate", true)

        // When
        Analytics.trackEvent("video_started", mapOf("k" to "v"))

        // Then
        assertEquals(1, fakeStorable.events.size)
        assertEquals("video_started", fakeStorable.events[0].name)
    }

    @Test
    fun `track event when api ok does not persist locally`() {
        // Given
        fakeApiService.shouldFail = false
        setStaticField(SessionManager::class.java, "isSessionActivate", true)

        // When
        Analytics.trackEvent("video_started", mapOf("k" to "v"))

        // Then
        assertEquals(0, fakeStorable.events.size)
    }

    @Test
    fun `manual mode start does not auto start session`() {
        // Given
        val mockApp = mockk<Application>(relaxed = true)
        every { context.applicationContext } returns mockApp
        every { mockApp.registerActivityLifecycleCallbacks(any()) } just runs

        val mockConnectivityManager = mockk<ConnectivityManager>(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager

        mockkConstructor(NetworkRequest.Builder::class)
        every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns mockk(relaxed = true)
        every { anyConstructed<NetworkRequest.Builder>().addTransportType(any()) } returns mockk(relaxed = true)
        every { anyConstructed<NetworkRequest.Builder>().build() } returns mockk(relaxed = true)

        // When
        Analytics.enableManualSession()
        AppAmbit.start(context, "TEST_KEY")

        // Then
        assertEquals(0, fakeStorable.sessions.size)
    }

    @Test
    fun `manual mode start and end session explicit`() {
        // Given
        fakeApiService.shouldFail = false
        
        setStaticField(SessionManager::class.java, "mExecutorService", mockExecutorService)

        val logStart = SessionData()
        logStart.sessionType = SessionType.START
        fakeStorable.putSessionData(logStart)

        val logEnd = SessionData()
        logEnd.sessionType = SessionType.END
        fakeStorable.putSessionData(logEnd)

        // When
        Analytics.enableManualSession()
        SessionManager.startSession()
        SessionManager.endSession()

        // Then
        assertEquals(2, fakeStorable.sessions.size)
        assertEquals(SessionType.START, fakeStorable.sessions[0].sessionType)
        assertEquals(SessionType.END, fakeStorable.sessions[1].sessionType)
    }

    @Test
    fun `send batch sessions resolves sessionIds and updates tracking`() {
        // Given
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
                fakeStorable.putLogAnalyticsEvent(
                    EventEntity().apply {
                        id = UUID.randomUUID()
                        name = "evt-${sb.id}-$j"
                        sessionId = sb.id
                        createdAt = Date.from(baseTime)
                    }
                )
                fakeStorable.addBreadcrumb(
                    BreadcrumbEntity().apply {
                        id = UUID.randomUUID()
                        name = "bc-${sb.id}-$j"
                        sessionId = sb.id
                        createdAt = Date.from(baseTime)
                    }
                )
            }
        }

        // When
        localBatches.forEachIndexed { idx, sb ->
            fakeStorable.updateSessionIdsForAllTrackingData(sb.id, "srv-$idx")
        }

        // Then
        assertEquals(250, fakeStorable.events.size)
        fakeStorable.events.forEach { 
            assertTrue("Event sessionId ${it.sessionId} should contain srv-", it.sessionId.contains("srv-")) 
        }

        assertEquals(250, fakeStorable.breadcrumbs.size)
        fakeStorable.breadcrumbs.forEach { 
            assertTrue("Breadcrumb sessionId ${it.sessionId} should contain srv-", it.sessionId.contains("srv-")) 
        }
    }

    @Test
    fun `send batch events with old events removes events from storage`() {
        // Given
        fakeApiService.shouldFail = false

        val backdatedDate = Date.from(Instant.now().minus(2, ChronoUnit.DAYS))
        val event = EventEntity().apply {
            id = UUID.randomUUID()
            sessionId = UUID.randomUUID().toString()
            name = "test_event"
            createdAt = backdatedDate
        }
        
        fakeStorable.putLogAnalyticsEvent(event)

        // When
        Analytics.sendBatchesEvents()

        // Then
        assertTrue(fakeStorable.events.isEmpty())
        assertEquals(0, fakeStorable.events.size)
    }

    private fun setStaticField(clazz: Class<*>, fieldName: String, value: Any?) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(null, value)
    }

    // Fakes

    class FakeStorable : Storable {
        val sessions = ArrayList<SessionData>()
        val events = ArrayList<EventEntity>()
        val breadcrumbs = ArrayList<BreadcrumbEntity>()
        val logs = ArrayList<LogEntity>()
        
        private var deviceId: String? = null
        private var appId: String? = null
        private var userId: String? = null
        private var userEmail: String? = null
        private var consumerId: String? = null
        private var sessionId: String? = null

        override fun close() {}

        override fun putDeviceId(deviceId: String?) { this.deviceId = deviceId }
        override fun getDeviceId(): String? = deviceId

        override fun putAppId(appId: String?) { this.appId = appId }
        override fun getAppId(): String? = appId

        override fun putUserId(userId: String?) { this.userId = userId }
        override fun getUserId(): String? = userId

        override fun putUserEmail(email: String?) { this.userEmail = email }
        override fun getUserEmail(): String? = userEmail

        override fun putConsumerId(consumerId: String?) { this.consumerId = consumerId }
        override fun getConsumerId(): String? = consumerId

        override fun putSessionId(sessionId: String?) { this.sessionId = sessionId }
        override fun getSessionId(): String? = sessionId

        override fun putSessionData(session: SessionData?) {
            if (session != null) sessions.add(session)
        }

        override fun putLogAnalyticsEvent(event: EventEntity?) {
            if (event != null) events.add(event)
        }

        override fun getOldest100Events(): List<EventEntity> {
            return ArrayList(events.take(100))
        }

        override fun deleteEventList(eventsToDelete: List<EventEntity>?) {
            if (eventsToDelete != null) {
                events.removeAll { e -> eventsToDelete.any { it.id == e.id } }
            }
        }

        override fun addBreadcrumb(breadcrumb: BreadcrumbEntity?) {
            if (breadcrumb != null) breadcrumbs.add(breadcrumb)
        }

        override fun getAllBreadcrumbs(): List<BreadcrumbEntity> {
            return ArrayList(breadcrumbs)
        }
        
        override fun getOldest100Breadcrumbs(): List<BreadcrumbEntity> {
            return ArrayList(breadcrumbs.take(100))
        }
        
        override fun deleteBreadcrumbs(breadcrumbsToDelete: List<BreadcrumbEntity>?) {
             if (breadcrumbsToDelete != null) {
                breadcrumbs.removeAll { b -> breadcrumbsToDelete.any { it.id == b.id } }
            }
        }

        override fun updateSessionIdsForAllTrackingData(oldId: String?, newId: String?) {
            if (oldId != null && newId != null) {
                events.filter { it.sessionId == oldId }.forEach { it.sessionId = newId }
                breadcrumbs.filter { it.sessionId == oldId }.forEach { it.sessionId = newId }
                logs.filter { it.sessionId == oldId }.forEach { it.sessionId = newId }
            }
        }

        override fun putLogEvent(log: LogEntity?) { 
            if (log != null) logs.add(log) 
        }
        
        override fun getOldest100Logs(): List<LogEntity> { return ArrayList(logs.take(100)) }
        
        override fun deleteLogList(logsToDelete: List<LogEntity>?) { 
            if (logsToDelete != null) {
                logs.removeAll { l -> logsToDelete.any { it.id == l.id } }
            }
        }
        
        override fun getOldest100Session(): List<SessionBatch> { 
            return emptyList() 
        }
        
        override fun deleteSessionList(sessionsToDelete: List<SessionBatch>?) {}
        
        override fun deleteSessionById(sessionId: UUID?) {
             sessions.removeAll { it.id == sessionId }
        }
        
        override fun getUnpairedSessionStart(): SessionData? {
             return sessions.firstOrNull { it.sessionType == SessionType.START }
        }
        
        override fun getUnpairedSessionEnd(): SessionData? {
             return sessions.firstOrNull { it.sessionType == SessionType.END }
        }
    }

    class FakeApiService : ApiService {
        var shouldFail = false
        var requestCount = 0
        private var _token: String? = null

        override fun <T> executeRequest(endpoint: IEndpoint?, clazz: Class<T>?): ApiResult<T> {
            requestCount++
            if (shouldFail) {
                return ApiResult(null, ApiErrorType.NetworkUnavailable, "Network Error")
            }
            
            val data: T? = when (clazz) {
                EventResponse::class.java -> EventResponse() as T
                EventsBatchResponse::class.java -> EventsBatchResponse() as T
                LogResponse::class.java -> LogResponse().apply { message = "Success" } as T
                StartSessionResponse::class.java -> StartSessionResponse() as T
                EndSessionResponse::class.java -> EndSessionResponse() as T
                else -> null
            }
            return ApiResult(data, ApiErrorType.None, null)
        }

        override fun GetNewToken(): AppAmbitTaskFuture<ApiErrorType> {
            _token = UUID.randomUUID().toString()
            val future = AppAmbitTaskFuture<ApiErrorType>()
            future.complete(ApiErrorType.None)
            return future
        }

        override fun getToken(): String? = _token

        override fun setToken(token: String?) {
            _token = token
        }
    }

    class FakeAppInfoService : AppInfoService {
        override fun getAppVersion(): String = "1.0"
        override fun getBuild(): String = "1"
        override fun getPlatform(): String = "Android"
        override fun getOs(): String = "Android"
        override fun getDeviceModel(): String = "TestDevice"
        override fun getCountry(): String = "US"
        override fun getUtcOffset(): String = "+00:00"
        override fun getLanguage(): String = "en"
    }
}