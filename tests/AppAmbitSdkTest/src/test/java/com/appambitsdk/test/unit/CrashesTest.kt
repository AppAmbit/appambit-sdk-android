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
import com.appambit.sdk.models.analytics.EventEntity
import com.appambit.sdk.models.analytics.SessionBatch
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.models.breadcrumbs.BreadcrumbEntity
import com.appambit.sdk.models.logs.ExceptionInfo
import com.appambit.sdk.models.logs.LogEntity
import com.appambit.sdk.models.remoteConfigs.RemoteConfigEntity
import io.mockk.unmockkStatic
import com.appambit.sdk.models.logs.LogResponse
import com.appambit.sdk.models.responses.ApiResult
import com.appambit.sdk.models.responses.EventResponse
import com.appambit.sdk.services.interfaces.ApiService
import com.appambit.sdk.services.interfaces.AppInfoService
import com.appambit.sdk.services.interfaces.IEndpoint
import com.appambit.sdk.services.interfaces.Storable
import com.appambit.sdk.utils.AppAmbitTaskFuture
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
    private lateinit var mockExecutorService: ExecutorService

    private lateinit var fakeStorable: FakeStorable
    private lateinit var fakeApiService: FakeApiService
    private lateinit var fakeAppInfoService: FakeAppInfoService

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
        
        // Initialize Fakes
        fakeStorable = FakeStorable()
        fakeApiService = FakeApiService()
        fakeAppInfoService = FakeAppInfoService()

        // Reset SessionManager state
        setStaticField(SessionManager::class.java, "isSessionActivate", false)
        setStaticField(SessionManager::class.java, "sessionId", null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        setStaticField(SessionManager::class.java, "isSessionActivate", false)
    }

    private fun initializeSdk(apiErrorType: ApiErrorType = ApiErrorType.None) {
        ServiceLocator.initialize(context)

        fakeApiService = FakeApiService(apiErrorType)

        setStaticField(ServiceLocator::class.java, "storable", fakeStorable)
        setStaticField(ServiceLocator::class.java, "apiService", fakeApiService)
        setStaticField(ServiceLocator::class.java, "appInfoService", fakeAppInfoService)
        setStaticField(ServiceLocator::class.java, "executorService", mockExecutorService)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        SessionManager.initialize(fakeApiService, mockExecutorService, fakeStorable)

        Crashes.Initialize()

        setStaticField(Crashes::class.java, "mExecutorService", mockExecutorService)
        setStaticField(SessionManager::class.java, "mExecutorService", mockExecutorService)
    }

    @Test
    fun `log error generates and persists a valid error log`() {
        // Given
        initializeSdk(ApiErrorType.NetworkUnavailable)
        ensureSession(fakeStorable)

        // When
        Crashes.logError("boom!")
        
        // Then
        val persistedLog = fakeStorable.logs.lastOrNull()
        assertTrue(persistedLog != null)
        assertEquals(LogType.ERROR, persistedLog?.type)
        assertEquals("boom!", persistedLog?.message)
        assertTrue(persistedLog?.createdAt != null)
    }

    @Test
    fun `log error from exception persists crash file path`() {
        // Given
        initializeSdk(ApiErrorType.NetworkUnavailable)
        ensureSession(fakeStorable)

        mockkStatic(ExceptionInfo::class)
        every { ExceptionInfo.fromException(any(), any()) } answers {
            val ex = secondArg<Exception>()
            val info = ExceptionInfo()
            info.message = ex.message
            info.type = ex.javaClass.name
            info.createdAt = Date()
            info
        }

        // When
        Crashes.logError(Exception("bad!"))

        // Then
        val persistedLog = fakeStorable.logs.lastOrNull()
        assertTrue(persistedLog != null)
        assertEquals(LogType.ERROR, persistedLog?.type)
        assertTrue(persistedLog?.message?.contains("bad!") == true)
        assertTrue(persistedLog?.createdAt != null)

        unmockkStatic(ExceptionInfo::class)
    }

    @Test
    fun `send batch logs with api success removes logs from storage`() {
        // Given
        initializeSdk()

        fakeStorable.putLogEvent(
            LogEntity().apply {
                id = UUID.randomUUID()
                type = LogType.ERROR
                message = "old-1"
                createdAt = Date.from(
                    Instant.now().minus(1, ChronoUnit.DAYS)
                )
            }
        )
        fakeStorable.putLogEvent(
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
        assertTrue(fakeStorable.logs.isEmpty())
        assertEquals(1, fakeApiService.requestCount)
    }

    @Test
    fun `send batch logs with backdated logs still sends batch`() {
        // Given
        initializeSdk()

        val baseInstant = Instant.now().minus(3, ChronoUnit.DAYS)
        val time1 = Date.from(baseInstant)
        val time2 = Date.from(baseInstant.plus(5, ChronoUnit.MINUTES))

        fakeStorable.putLogEvent(
            LogEntity().apply {
                id = UUID.randomUUID()
                type = LogType.ERROR
                message = "past-1"
                createdAt = time1
            }
        )

        fakeStorable.putLogEvent(
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
        assertTrue(fakeStorable.logs.isEmpty())
        assertEquals(1, fakeApiService.requestCount)
    }

    private fun ensureSession(storable: Storable) {
        if (!SessionManager.isSessionActivate) {
            SessionManager.startSession()

            if (!SessionManager.isSessionActivate) {
                setStaticField(SessionManager::class.java, "isSessionActivate", true)
                setStaticField(SessionManager::class.java, "sessionId", UUID.randomUUID().toString())

                storable.putSessionData(
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

    // Fakes

    class FakeApiService(private val errorType: ApiErrorType = ApiErrorType.None) : ApiService {
        var requestCount = 0
        private var _token: String? = null

        override fun <T> executeRequest(endpoint: IEndpoint?, clazz: Class<T>?): ApiResult<T> {
            requestCount++
            val data: T? = when (clazz) {
                EventResponse::class.java -> EventResponse() as T
                LogResponse::class.java -> LogResponse().apply { message = "Success" } as T
                else -> null
            }
            return ApiResult(data, errorType, null)
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

    class FakeStorable : Storable {
        val logs = mutableListOf<LogEntity>()
        val sessions = mutableListOf<SessionData>()
        val events = mutableListOf<EventEntity>()
        val breadcrumbs = mutableListOf<BreadcrumbEntity>()
        
        private var deviceId: String? = null
        private var appId: String? = null
        private var userId: String? = null
        private var userEmail: String? = null
        private var consumerId: String? = null
        private var sessionId: String? = null
        private var deviceToken: String? = null
        private var pushEnabled: Boolean? = null

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

        override fun putDeviceToken(deviceToken: String?) { this.deviceToken = deviceToken }
        override fun getDeviceToken(): String? = deviceToken

        override fun putPushEnabled(pushEnabled: Boolean) { this.pushEnabled = pushEnabled }
        override fun getPushEnabled(): Boolean? = pushEnabled

        override fun putLogEvent(logEntity: LogEntity?) {
            if (logEntity != null) logs.add(logEntity)
        }

        override fun putLogAnalyticsEvent(logEntity: EventEntity?) {
            if (logEntity != null) events.add(logEntity)
        }

        override fun putSessionData(sessionData: SessionData?) {
            if (sessionData != null) sessions.add(sessionData)
        }

        override fun getOldest100Session(): List<SessionBatch> {
            return emptyList()
        }

        override fun deleteSessionList(sessionsToDelete: List<SessionBatch>?) {
        }

        override fun deleteSessionById(sessionId: UUID?) {
            sessions.removeAll { it.id == sessionId }
        }

        override fun getUnpairedSessionStart(): SessionData? {
            return sessions.firstOrNull { it.sessionType == SessionType.START }
        }

        override fun updateSessionIdsForAllTrackingData(localId: String?, remoteId: String?) {
        }

        override fun getUnpairedSessionEnd(): SessionData? {
            return sessions.firstOrNull { it.sessionType == SessionType.END }
        }

        override fun deleteLogList(logsToDelete: List<LogEntity>?) {
            if (logsToDelete != null) {
                logs.removeAll { l -> logsToDelete.any { it.id == l.id } }
            }
        }

        override fun getOldest100Logs(): List<LogEntity> {
            return ArrayList(logs)
        }

        override fun getOldest100Events(): List<EventEntity> {
            return ArrayList(events)
        }

        override fun deleteEventList(eventsToDelete: List<EventEntity>?) {
            if (eventsToDelete != null) {
                events.removeAll { e -> eventsToDelete.any { it.id == e.id } }
            }
        }

        override fun getAllBreadcrumbs(): List<BreadcrumbEntity> {
            return ArrayList(breadcrumbs)
        }

        override fun addBreadcrumb(breadcrumb: BreadcrumbEntity?) {
            if (breadcrumb != null) breadcrumbs.add(breadcrumb)
        }

        override fun deleteBreadcrumbs(breadcrumbsToDelete: List<BreadcrumbEntity>?) {
            if (breadcrumbsToDelete != null) {
                breadcrumbs.removeAll { b -> breadcrumbsToDelete.any { it.id == b.id } }
            }
        }

        override fun getOldest100Breadcrumbs(): List<BreadcrumbEntity> {
            return ArrayList(breadcrumbs)
        }

        override fun putConfigs(configs: List<RemoteConfigEntity>?) {
            // No-op
        }

        override fun getConfig(key: String?): String? {
            return null
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