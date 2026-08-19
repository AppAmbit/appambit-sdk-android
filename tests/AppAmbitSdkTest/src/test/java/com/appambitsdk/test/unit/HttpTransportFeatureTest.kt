package com.appambitsdk.test.unit

import android.content.Context
import android.util.Log
import com.appambit.sdk.enums.HttpMethodEnum
import com.appambit.sdk.enums.LogType
import com.appambit.sdk.models.app.UpdateConsumer
import com.appambit.sdk.models.analytics.EventEntity
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.models.analytics.SessionPayload
import com.appambit.sdk.models.breadcrumbs.BreadcrumbData
import com.appambit.sdk.models.breadcrumbs.BreadcrumbEntity
import com.appambit.sdk.models.db.DbStatement
import com.appambit.sdk.models.logs.Log as SdkLog
import com.appambit.sdk.models.logs.LogEntity
import com.appambit.sdk.models.logs.LogBatch
import com.appambit.sdk.models.responses.RemoteConfigResponse
import com.appambit.sdk.services.HttpApiService
import com.appambit.sdk.services.endpoints.BaseEndpoint
import com.appambit.sdk.services.endpoints.BreadcrumbsBatchEndpoint
import com.appambit.sdk.services.endpoints.BreadcrumbEndpoint
import com.appambit.sdk.services.endpoints.DbBatchEndpoint
import com.appambit.sdk.services.endpoints.DbQueryEndpoint
import com.appambit.sdk.services.endpoints.EndSessionEndpoint
import com.appambit.sdk.services.endpoints.EventEndpoint
import com.appambit.sdk.services.endpoints.EventBatchEndpoint
import com.appambit.sdk.services.endpoints.LogBatchEndpoint
import com.appambit.sdk.services.endpoints.LogEndpoint
import com.appambit.sdk.services.endpoints.RemoteConfigEndpoint
import com.appambit.sdk.services.endpoints.StartSessionEndpoint
import com.appambit.sdk.services.endpoints.SessionBatchEndpoint
import com.appambit.sdk.services.endpoints.UpdateConsumerEndpoint
import com.appambit.sdk.services.interfaces.HttpTransportResponse
import com.appambit.sdk.utils.InternetConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HttpTransportFeatureTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `analytics logs remote config database and breadcrumb endpoints use shared transport`() {
        val endpoints = listOf(
            RemoteConfigEndpoint("1.0") to "/sdk/config",
            DbQueryEndpoint("SELECT 1", emptyList()) to "/db/query",
            DbBatchEndpoint(listOf(DbStatement.of("SELECT 1")), false) to "/db/batch",
            EventBatchEndpoint(listOf(EventEntity().apply { setName("event") })) to "/events/batch",
            SessionBatchEndpoint(SessionPayload().apply { setSessions(emptyList()) }) to "/session/batch",
            BreadcrumbsBatchEndpoint(listOf(BreadcrumbData().apply { setName("screen") })) to "/breadcrumbs/batch",
            LogEndpoint(SdkLog().apply {
                setMessage("boom")
                setType(LogType.ERROR)
            }) to "/log",
            LogBatchEndpoint(LogBatch().apply { setLogs(emptyList()) }) to "/log/batch"
        )
        val server = CloudCodeTest.HttpProbeServer(
            endpoints.associate { (_, path) ->
                path to mutableListOf(CloudCodeTest.HttpProbeServer.Response(200, "{}"))
            }
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val service = HttpApiService(context, executor)
            service.setToken("feature-token")

            endpoints.forEach { (endpoint, expectedPath) ->
                endpoint.setBaseUrl(server.baseUrl)
                val completed = CountDownLatch(1)
                var error: Throwable? = null

                service.executeRaw(endpoint, 2_000) {
                    error = it.error
                    completed.countDown()
                }

                assertTrue(completed.await(5, TimeUnit.SECONDS))
                assertNull("${endpoint.javaClass.simpleName} failed", error)
                val request = server.requests.last()
                assertEquals(expectedPath, request.path)
                assertEquals(endpoint.method.name, request.method)
                assertEquals("application/json", request.headers["accept"])
                assertEquals("AppAmbitSDK (Android)", request.headers["user-agent"])
                assertEquals("Bearer feature-token", request.headers["authorization"])
                if (endpoint.method != HttpMethodEnum.GET) {
                    assertTrue("${endpoint.javaClass.simpleName} sent no body", request.body.isNotEmpty())
                }
            }

            assertEquals(endpoints.size, server.requests.size)
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `analytics event and session endpoints preserve payloads`() {
        val server = serverFor("/events", "/session/start", "/session/end")
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val service = HttpApiService(context, executor)
            service.setToken("analytics-token")
            val event = EventEntity().apply {
                setName("purchase")
                setData(mapOf("source" to "test"))
            }
            val start = StartSessionEndpoint(Date(1_700_000_000_000L))
            val end = SessionData().apply {
                setSessionId("session-1")
                setTimestamp(Date(1_700_000_001_000L))
            }.let(::EndSessionEndpoint)

            val endpoints = listOf(
                EventEndpoint(event) to "/events",
                start to "/session/start",
                end to "/session/end"
            )

            endpoints.forEach { (endpoint, expectedPath) ->
                endpoint.setBaseUrl(server.baseUrl)
                val response = executeRaw(service, endpoint)
                assertNull(response.error)
                val request = server.requests.last()
                assertEquals(expectedPath, request.path)
                assertEquals("POST", request.method)
                assertEquals("Bearer analytics-token", request.headers["authorization"])
                assertTrue(request.body.isNotEmpty())
            }

            assertTrue(server.requests[0].body.contains("purchase"))
            assertTrue(server.requests[0].body.contains("source"))
            assertTrue(server.requests[1].body.contains("timestamp"))
            assertTrue(server.requests[2].body.contains("session-1"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `crash log endpoints preserve multipart payloads`() {
        val server = serverFor("/log", "/log/batch")
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val service = HttpApiService(context, executor)
            service.setToken("crash-token")
            val single = SdkLog().apply {
                setMessage("single-crash")
                setType(LogType.ERROR)
            }
            val batchLog = LogEntity().apply {
                setMessage("batched-crash")
                setType(LogType.ERROR)
            }
            val endpoints = listOf(
                LogEndpoint(single) to "/log",
                LogBatchEndpoint(LogBatch().apply { setLogs(listOf(batchLog)) }) to "/log/batch"
            )

            endpoints.forEach { (endpoint, expectedPath) ->
                endpoint.setBaseUrl(server.baseUrl)
                val response = executeRaw(service, endpoint)
                assertNull(response.error)
                val request = server.requests.last()
                assertEquals(expectedPath, request.path)
                assertEquals("POST", request.method)
                assertTrue(request.headers["content-type"]!!.startsWith("multipart/form-data; boundary="))
                assertEquals("Bearer crash-token", request.headers["authorization"])
                assertTrue(request.body.isNotEmpty())
            }

            assertTrue(server.requests[0].body.contains("single-crash"))
            assertTrue(server.requests[1].body.contains("batched-crash"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `consumer update uses PUT and JSON payload`() {
        val server = serverFor("/consumer/consumer-1")
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = UpdateConsumerEndpoint("consumer-1", UpdateConsumer("device-token", true))
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("consumer-token")

            val response = executeRaw(service, endpoint)
            val request = server.requests.single()

            assertNull(response.error)
            assertEquals("/consumer/consumer-1", request.path)
            assertEquals("PUT", request.method)
            assertEquals("application/json", request.headers["content-type"])
            assertEquals("Bearer consumer-token", request.headers["authorization"])
            val body = JSONObject(request.body)
            assertEquals("device-token", body.getString("device_token"))
            assertTrue(body.getBoolean("push_enabled"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `remote config uses encoded version query and decodes response`() {
        val server = serverFor(
            "/sdk/config",
            responseBody = "{\"configs\":{\"welcome_msg\":\"Hello\",\"max_items\":10}}"
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = RemoteConfigEndpoint("1.0").apply { setBaseUrl(server.baseUrl) }
            val service = HttpApiService(context, executor)
            service.setToken("config-token")
            val result = service.executeRequest(endpoint, RemoteConfigResponse::class.java)

            assertEquals(com.appambit.sdk.enums.ApiErrorType.None, result.errorType)
            assertEquals("Hello", result.data!!.configs["welcome_msg"])
            assertEquals(10, (result.data!!.configs["max_items"] as Number).toInt())
            val request = server.requests.single()
            assertEquals("/sdk/config?app_version=1.0", request.target)
            assertEquals("GET", request.method)
            assertEquals("Bearer config-token", request.headers["authorization"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `database query and batch endpoints preserve SQL payloads`() {
        val server = serverFor("/db/query", "/db/batch")
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val service = HttpApiService(context, executor)
            service.setToken("database-token")
            val query = DbQueryEndpoint("SELECT * FROM tasks WHERE id = ?", listOf(7))
            val batch = DbBatchEndpoint(listOf(DbStatement.of("UPDATE tasks SET done = ?", true)), true)

            listOf(query to "/db/query", batch to "/db/batch").forEach { (endpoint, expectedPath) ->
                endpoint.setBaseUrl(server.baseUrl)
                val response = executeRaw(service, endpoint)
                assertNull(response.error)
                val request = server.requests.last()
                assertEquals(expectedPath, request.path)
                assertEquals("POST", request.method)
                assertEquals("Bearer database-token", request.headers["authorization"])
                assertEquals("application/json", request.headers["content-type"])
                assertTrue(request.body.contains("SELECT") || request.body.contains("UPDATE"))
            }

            assertTrue(server.requests[1].body.contains("\"transaction\":true"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `single breadcrumb endpoint preserves session metadata`() {
        val server = serverFor("/breadcrumbs")
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = BreadcrumbEndpoint(BreadcrumbEntity().apply {
                setName("screen_view")
                setSessionId("session-1")
            })
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("breadcrumb-token")

            val response = executeRaw(service, endpoint)
            val request = server.requests.single()

            assertNull(response.error)
            assertEquals("/breadcrumbs", request.path)
            assertEquals("POST", request.method)
            assertEquals("Bearer breadcrumb-token", request.headers["authorization"])
            assertTrue(request.body.contains("screen_view"))
            assertTrue(request.body.contains("session-1"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    private fun executeRaw(
        service: HttpApiService,
        endpoint: BaseEndpoint
    ): HttpTransportResponse {
        val completed = CountDownLatch(1)
        var response: HttpTransportResponse? = null
        service.executeRaw(endpoint, 2_000) {
            response = it
            completed.countDown()
        }
        assertTrue(completed.await(5, TimeUnit.SECONDS))
        return response ?: error("HTTP transport did not return a response")
    }

    private fun serverFor(
        vararg paths: String,
        responseBody: String = "{}"
    ): CloudCodeTest.HttpProbeServer {
        return CloudCodeTest.HttpProbeServer(
            paths.associateWith {
                mutableListOf(CloudCodeTest.HttpProbeServer.Response(200, responseBody))
            }
        )
    }
}
