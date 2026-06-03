package com.appambitsdk.test.unit

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.appambit.sdk.RemoteConfig
import com.appambit.sdk.enums.ApiErrorType
import com.appambit.sdk.models.remoteConfigs.RemoteConfigEntity
import com.appambit.sdk.models.responses.ApiResult
import com.appambit.sdk.models.responses.RemoteConfigResponse
import com.appambit.sdk.services.endpoints.RemoteConfigEndpoint
import com.appambit.sdk.services.interfaces.ApiService
import com.appambit.sdk.services.interfaces.AppInfoService
import com.appambit.sdk.services.interfaces.Storable
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutorService

class RemoteConfigTest {

    @RelaxedMockK
    private lateinit var context: Application

    @RelaxedMockK
    private lateinit var mockExecutorService: ExecutorService

    @RelaxedMockK
    private lateinit var apiService: ApiService

    @RelaxedMockK
    private lateinit var storable: Storable

    @RelaxedMockK
    private lateinit var appInfoService: AppInfoService

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { mockExecutorService.execute(any()) } answers {
            firstArg<Runnable>().run()
        }

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        RemoteConfig.initialize(mockExecutorService, apiService, storable, appInfoService)

        // Reset static state — isEnable must be true (SDK default) so fetch tests can run
        setStaticField(RemoteConfig::class.java, "isEnable", true)
        setStaticField(RemoteConfig::class.java, "isFetchCompleted", false)
    }

    private fun setStaticField(cls: Class<*>, fieldName: String, value: Any?) {
        val field = cls.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(null, value)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `fetch success should store configs to storable`() {
        // Given
        val mockResponse = RemoteConfigResponse()
        setInternalState(mockResponse, "configs", mapOf("welcome_msg" to "Hello"))
        
        every { 
            apiService.executeRequest(any<RemoteConfigEndpoint>(), RemoteConfigResponse::class.java) 
        } returns ApiResult(mockResponse, ApiErrorType.None, null)

        every { appInfoService.getAppVersion() } returns "1.0.1"
        
        every { storable.getConfig(com.appambit.sdk.AppConstants.LIVE_SESSION_STREAMING) } returns null

        // When
        RemoteConfig.fetchAndStoreConfig()

        // Then
        // Verify internal memory is updated (implicitly by checking no errors) or strict verify
        verify { apiService.executeRequest(any<RemoteConfigEndpoint>(), RemoteConfigResponse::class.java) }
        
        val slot = slot<List<RemoteConfigEntity>>()
        verify { storable.putConfigs(capture(slot)) }
        
        assertEquals(1, slot.captured.size)
        val welcomeEntity = slot.captured.find { it.key == "welcome_msg" }
        assertEquals("Hello", welcomeEntity?.value)
        val liveSessionEntity = slot.captured.find { it.key == com.appambit.sdk.AppConstants.LIVE_SESSION_STREAMING }
        assertEquals(null, liveSessionEntity)
    }

    @Test
    fun `fetch failure should not store configs`() {
        // Given
        every { 
            apiService.executeRequest(any<RemoteConfigEndpoint>(), RemoteConfigResponse::class.java) 
        } returns ApiResult(null, ApiErrorType.NetworkUnavailable, "Error")

        every { appInfoService.getAppVersion() } returns "1.0.1"

        // When
        RemoteConfig.fetchAndStoreConfig()

        // Then
        verify(exactly = 0) { storable.putConfigs(any()) }
    }

    @Test
    fun `getLong should return parsed integer from storable`() {
        // Given
        every { storable.getConfig("max_items") } returns "10"

        // When
        val value = RemoteConfig.getLong("max_items")

        // Then
        assertEquals(10, value)
    }

    @Test
    fun `getDouble should return parsed double from storable`() {
        // Given
        every { storable.getConfig("discount_rate") } returns "0.5"

        // When
        val value = RemoteConfig.getDouble("discount_rate")

        // Then
        assertEquals(0.5, value, 0.001)
    }

    @Test
    fun `getBoolean should return parsed boolean from storable`() {
        // Given
        every { storable.getConfig("is_new_ui") } returns "true"

        // When
        val value = RemoteConfig.getBoolean("is_new_ui")

        // Then
        assertTrue(value)
    }

    private fun setInternalState(target: Any, fieldName: String, value: Any) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
