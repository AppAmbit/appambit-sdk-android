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
import com.appambit.sdk.utils.AppAmbitTaskFuture
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
import org.junit.Assert.assertFalse
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

        RemoteConfig.initialize(context, mockExecutorService, apiService, storable, appInfoService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `fetch success should store configs in memory and return true`() {
        // Given
        val mockResponse = RemoteConfigResponse()
        setInternalState(mockResponse, "configs", mapOf("welcome_msg" to "Hello"))
        
        every { 
            apiService.executeRequest(any<RemoteConfigEndpoint>(), RemoteConfigResponse::class.java) 
        } returns ApiResult(mockResponse, ApiErrorType.None, null)

        every { appInfoService.getAppVersion() } returns "1.0.0"

        // When
        var result = false
        val task = RemoteConfig.fetch()
        task.then { result = it }

        // Then
        assertTrue(result)
        // Verify internal memory is updated (implicitly by checking no errors) or strict verify
        verify { apiService.executeRequest(any<RemoteConfigEndpoint>(), RemoteConfigResponse::class.java) }
    }

    @Test
    fun `fetch failure should return false`() {
        // Given
        every { 
            apiService.executeRequest(any<RemoteConfigEndpoint>(), RemoteConfigResponse::class.java) 
        } returns ApiResult(null, ApiErrorType.NetworkUnavailable, "Error")

        every { appInfoService.getAppVersion() } returns "1.0.0"

        // When
        var result = true
        val task = RemoteConfig.fetch()
        task.then { result = it }

        // Then
        assertFalse(result)
    }

    @Test
    fun `activate should valid fetched config to storable`() {
        // Given
        // 1. Mock fetch first to populate mRemoteConfig
        val mockResponse = RemoteConfigResponse()
        setInternalState(mockResponse, "configs", mapOf("feature_enabled" to true))
        
        every { 
            apiService.executeRequest(any<RemoteConfigEndpoint>(), RemoteConfigResponse::class.java) 
        } returns ApiResult(mockResponse, ApiErrorType.None, null)
        RemoteConfig.fetch()

        // When
        var activated = false
        val task = RemoteConfig.activate()
        task.then { activated = it }

        // Then
        assertTrue(activated)
        
        val slot = slot<List<RemoteConfigEntity>>()
        verify { storable.putConfigs(capture(slot)) }
        
        assertEquals(1, slot.captured.size)
        assertEquals("feature_enabled", slot.captured[0].key)
        assertEquals("true", slot.captured[0].value)
    }

    @Test
    fun `getString should return value from storable`() {
        // Given
        every { storable.getConfig("banner_text") } returns "Welcome User"

        // When
        val value = RemoteConfig.getString("banner_text")

        // Then
        assertEquals("Welcome User", value)
        verify { storable.getConfig("banner_text") }
    }

    @Test
    fun `getString should fallback to defaults if storable returns null`() {
        // Given
        every { storable.getConfig("banner_text") } returns null
        RemoteConfig.setDefaultsAsync(mapOf("banner_text" to "Default Welcome"))

        // When
        val value = RemoteConfig.getString("banner_text")

        // Then
        assertEquals("Default Welcome", value)
    }

    @Test
    fun `getInt should return parsed integer from storable`() {
        // Given
        every { storable.getConfig("max_items") } returns "10"

        // When
        val value = RemoteConfig.getInt("max_items")

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
