package com.appambitsdk.test.unit

import android.util.Log
import com.appambit.sdk.ServiceLocator
import com.appambit.sdk.enums.ApiErrorType
import com.appambit.sdk.models.app.UpdateConsumer
import com.appambit.sdk.models.responses.ApiResult
import com.appambit.sdk.services.ConsumerService
import com.appambit.sdk.services.interfaces.ApiService
import com.appambit.sdk.services.interfaces.IEndpoint
import com.appambit.sdk.utils.AppAmbitTaskFuture
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ExecutorService

class ConsumerServiceTest {

    private lateinit var fakeStorable: AnalyticsTest.FakeStorable
    private lateinit var fakeAppInfoService: AnalyticsTest.FakeAppInfoService
    private lateinit var mockExecutorService: ExecutorService

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        fakeStorable = AnalyticsTest.FakeStorable()
        fakeAppInfoService = AnalyticsTest.FakeAppInfoService()
        mockExecutorService = mockk(relaxed = true)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        mockkStatic(ServiceLocator::class)
        every { ServiceLocator.getExecutorService() } returns mockExecutorService

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(ServiceLocator::class)
        unmockkStatic(Log::class)
    }

    @Test
    fun `updateConsumer uses stored token and defaults null pushEnabled to false`() {
        val apiService = CapturingApiService(ApiErrorType.None)
        ConsumerService.initialize(fakeStorable, fakeAppInfoService, apiService)

        fakeStorable.putConsumerId("consumer-1")
        fakeStorable.putDeviceToken("token-1")

        ConsumerService.updateConsumer(null, null)

        assertEquals(1, apiService.executedEndpoints.size)

        val payload = apiService.executedEndpoints.first().getPayload() as UpdateConsumer
        assertEquals("token-1", readDeviceToken(payload))
        assertFalse(readPushEnabled(payload))
    }

    @Test
    fun `updateConsumer stores incoming values and sends them to endpoint`() {
        val apiService = CapturingApiService(ApiErrorType.None)
        ConsumerService.initialize(fakeStorable, fakeAppInfoService, apiService)

        fakeStorable.putConsumerId("consumer-2")

        ConsumerService.updateConsumer("new-token", true)

        assertEquals("new-token", fakeStorable.getDeviceToken())
        assertTrue(fakeStorable.getPushEnabled() == true)
        assertEquals(1, apiService.executedEndpoints.size)

        val payload = apiService.executedEndpoints.first().getPayload() as UpdateConsumer
        assertEquals("new-token", readDeviceToken(payload))
        assertTrue(readPushEnabled(payload))
    }

    @Test
    fun `updateConsumer skips request when consumerId is missing`() {
        val apiService = CapturingApiService(ApiErrorType.None)
        ConsumerService.initialize(fakeStorable, fakeAppInfoService, apiService)

        fakeStorable.putDeviceToken("token-3")
        fakeStorable.putPushEnabled(true)

        ConsumerService.updateConsumer(null, null)

        assertEquals(0, apiService.executedEndpoints.size)
    }

    private fun readDeviceToken(updateConsumer: UpdateConsumer): String {
        val f = UpdateConsumer::class.java.getDeclaredField("deviceToken")
        f.isAccessible = true
        return f.get(updateConsumer) as String
    }

    private fun readPushEnabled(updateConsumer: UpdateConsumer): Boolean {
        val f = UpdateConsumer::class.java.getDeclaredField("pushEnabled")
        f.isAccessible = true
        return f.getBoolean(updateConsumer)
    }

    private class CapturingApiService(
        private val resultErrorType: ApiErrorType
    ) : ApiService {

        val executedEndpoints = mutableListOf<IEndpoint>()
        private var token: String? = UUID.randomUUID().toString()

        override fun <T> executeRequest(endpoint: IEndpoint?, clazz: Class<T>?): ApiResult<T> {
            if (endpoint != null) {
                executedEndpoints.add(endpoint)
            }
            return ApiResult(null, resultErrorType, null)
        }

        override fun GetNewToken(): AppAmbitTaskFuture<ApiErrorType> {
            val future = AppAmbitTaskFuture<ApiErrorType>()
            future.complete(ApiErrorType.None)
            return future
        }

        override fun getToken(): String? = token

        override fun setToken(token: String?) {
            this.token = token
        }
    }
}
