package com.appambitsdk.test.unit

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.content.Context
import com.appambit.sdk.AppAmbit
import com.appambit.sdk.enums.CloudCodeHttpMethod as HttpMethodEnum
import com.appambit.sdk.enums.HttpMethodEnum as TransportHttpMethod
import com.appambit.sdk.enums.ApiErrorType
import com.appambit.sdk.models.cloudcode.CloudCodeError
import com.appambit.sdk.models.cloudcode.CloudCodeResponse
import com.appambit.sdk.models.cloudcode.CloudCodeResult
import com.appambit.sdk.services.CloudCodeService
import com.appambit.sdk.services.HttpApiService
import com.appambit.sdk.services.TokenService
import com.appambit.sdk.services.endpoints.CloudCodeEndpoint
import com.appambit.sdk.services.endpoints.TokenEndpoint
import com.appambit.sdk.services.endpoints.CmsEndpoint
import com.appambit.sdk.services.endpoints.RegisterEndpoint
import com.appambit.sdk.services.endpoints.EventEndpoint
import com.appambit.sdk.services.endpoints.StartSessionEndpoint
import com.appambit.sdk.models.app.Consumer
import com.appambit.sdk.models.app.ConsumerToken
import com.appambit.sdk.models.analytics.Event
import com.appambit.sdk.services.interfaces.HttpTransport
import com.appambit.sdk.services.interfaces.HttpTransportResponse
import com.appambit.sdk.utils.AppAmbitTaskFuture
import com.appambit.sdk.utils.InternetConnection
import com.appambit.sdk.utils.JsonKey
import com.appambit.sdk.utils.SdkThreadFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertThrows
import java.io.IOException
import java.net.ConnectException
import java.net.MalformedURLException
import java.nio.charset.StandardCharsets
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.Socket
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.Date
import org.json.JSONObject

class CloudCodeTest {
    open class NestedBase {
        @JvmField
        @JsonKey("base_name")
        var baseName: String = ""
    }

    class NestedChild {
        @JvmField var name: String = ""
    }

    class NestedParent : NestedBase() {
        @JvmField var child: NestedChild? = null
        @JvmField var children: List<NestedChild> = emptyList()
    }

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

        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)
        every { Looper.myLooper() } returns null
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `endpoint encodes slug and sorted query`() {
        val endpoint = CloudCodeEndpoint(
            "hello world?",
            HttpMethodEnum.POST,
            mapOf("source" to "android", "message" to "hello world"),
            mapOf("count" to 2),
            mapOf("X-Trace" to "test")
        )

        assertEquals("/fn/hello%20world%3F?message=hello%20world&source=android", endpoint.url)
        assertEquals("POST", endpoint.method.name)
        assertEquals("test", endpoint.customHeader["X-Trace"])
    }

    @Test
    fun `endpoint percent encodes plus and email query values`() {
        val endpoint = CloudCodeEndpoint(
            "search",
            HttpMethodEnum.GET,
            mapOf("email" to "user+test@example.com"),
            null,
            null
        )

        assertEquals("/fn/search?email=user%2Btest%40example.com", endpoint.url)
    }

    @Test
    fun `CMS endpoint uses the same encoded sorted query contract`() {
        val endpoint = CmsEndpoint(
            "posts",
            linkedMapOf("space key" to "hello world", "filter[title]" to "a+b")
        )

        assertEquals(
            "/posts?filter%5Btitle%5D=a%2Bb&space%20key=hello%20world",
            endpoint.url
        )
    }

    @Test
    fun `cloud code forwards PATCH through the request contract`() {
        val transport = FakeTransport()
        transport.response = response(200, "{}")
        val service = CloudCodeService(transport)

        service.call(
            "patch",
            HttpMethodEnum.PATCH,
            mapOf("scope" to "profile"),
            mapOf<String, Any>("id" to 7, "name" to "Ada"),
            mapOf("X-Probe" to "patch")
        )

        assertEquals(TransportHttpMethod.PATCH, transport.lastEndpoint!!.method)
        assertEquals("/fn/patch?scope=profile", transport.lastEndpoint!!.url)
        assertEquals(
            mapOf("id" to 7, "name" to "Ada"),
            transport.lastEndpoint!!.payload
        )
        assertEquals("patch", transport.lastEndpoint!!.customHeader["X-Probe"])
    }

    @Test
    fun `legacy API response uses the shared HTTP transport`() {
        val server = HttpProbeServer(
            mapOf("/consumer/token" to mutableListOf(
                HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"shared-token\"}")
            ))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = TokenEndpoint(ConsumerToken("app-key", "consumer-id"))
            endpoint.setBaseUrl(server.baseUrl)
            val result = HttpApiService(context, executor)
                .executeRequest(endpoint, com.appambit.sdk.models.responses.TokenResponse::class.java)

            assertEquals(com.appambit.sdk.enums.ApiErrorType.None, result.errorType)
            assertEquals("shared-token", result.data!!.getToken())
            assertEquals("requests=${server.requests}", 1, server.requestCount("/consumer/token"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `legacy API JSON request uses the shared HTTP transport`() {
        val server = HttpProbeServer(
            mapOf("/consumer" to mutableListOf(
                HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"registered-token\"}")
            ))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = RegisterEndpoint(
                Consumer("app-key", "device-id", "Pixel", "1.0", "user", "user@example.com", "android", "US", "en")
            )
            endpoint.setBaseUrl(server.baseUrl)
            val result = HttpApiService(context, executor)
                .executeRequest(endpoint, com.appambit.sdk.models.responses.TokenResponse::class.java)

            assertEquals(com.appambit.sdk.enums.ApiErrorType.None, result.errorType)
            assertEquals("registered-token", result.data!!.getToken())
            assertTrue(server.requests.first().body.contains("\"app_key\":\"app-key\""))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `legacy event and session requests preserve authorization and POST bodies`() {
        val server = HttpProbeServer(
            mapOf(
                "/events" to mutableListOf(HttpProbeServer.Response(200, "{}")),
                "/session/start" to mutableListOf(HttpProbeServer.Response(200, "{\"session_id\":\"session-1\"}"))
            )
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val service = HttpApiService(context, executor)
            service.setToken("legacy-token")

            val event = Event().apply {
                setName("purchase")
                setData(mapOf("source" to "test"))
            }
            val eventEndpoint = EventEndpoint(event)
            eventEndpoint.setBaseUrl(server.baseUrl)
            val eventResult = service.executeRequest(eventEndpoint, com.appambit.sdk.models.responses.EventResponse::class.java)

            val sessionEndpoint = StartSessionEndpoint(Date())
            sessionEndpoint.setBaseUrl(server.baseUrl)
            val sessionResult = service.executeRequest(sessionEndpoint, com.appambit.sdk.models.responses.StartSessionResponse::class.java)

            assertEquals(com.appambit.sdk.enums.ApiErrorType.None, eventResult.errorType)
            assertEquals(com.appambit.sdk.enums.ApiErrorType.None, sessionResult.errorType)
            assertEquals(2, server.requests.size)
            assertTrue(server.requests.all { it.method == "POST" })
            assertTrue(server.requests.all { it.headers["authorization"] == "Bearer legacy-token" })
            assertTrue(server.requests.first { it.path == "/events" }.body.contains("\"name\":\"purchase\""))
            assertTrue(server.requests.first { it.path == "/session/start" }.body.isNotEmpty())
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `mutating event 401 refreshes and retries once`() {
        val server = HttpProbeServer(
            mapOf(
                "/events" to mutableListOf(
                    HttpProbeServer.Response(401, "{\"error\":\"expired\"}"),
                    HttpProbeServer.Response(200, "{}")
                ),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"new-token\"}")
                )
            )
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val service = HttpApiService(context, executor)
            service.setToken("old-token")
            val eventEndpoint = EventEndpoint(Event().apply { setName("purchase") })
            eventEndpoint.setBaseUrl(server.baseUrl)

            val result = service.executeRequest(eventEndpoint, com.appambit.sdk.models.responses.EventResponse::class.java)

            assertEquals(com.appambit.sdk.enums.ApiErrorType.None, result.errorType)
            assertEquals(2, server.requestCount("/events"))
            assertEquals(1, server.requestCount("/consumer/token"))
            assertEquals("Bearer old-token", server.requests.first { it.path == "/events" }.headers["authorization"])
            assertEquals("Bearer new-token", server.requests.last { it.path == "/events" }.headers["authorization"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `legacy event refreshes before sending when token is missing`() {
        val server = HttpProbeServer(
            mapOf(
                "/events" to mutableListOf(HttpProbeServer.Response(200, "{}")),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"fresh-token\"}")
                )
            )
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val service = HttpApiService(context, executor)
            service.setToken("")
            val eventEndpoint = EventEndpoint(Event().apply { setName("purchase") })
            eventEndpoint.setBaseUrl(server.baseUrl)

            val result = service.executeRequest(eventEndpoint, com.appambit.sdk.models.responses.EventResponse::class.java)

            assertEquals(com.appambit.sdk.enums.ApiErrorType.None, result.errorType)
            assertEquals("requests=${server.requests}", 1, server.requestCount("/consumer/token"))
            assertEquals(1, server.requestCount("/events"))
            assertEquals("Bearer fresh-token", server.requests.first { it.path == "/events" }.headers["authorization"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `concurrent legacy requests with missing token share one preflight refresh`() {
        val server = HttpProbeServer(
            mapOf(
                "/events" to mutableListOf(
                    HttpProbeServer.Response(200, "{}"),
                    HttpProbeServer.Response(200, "{}")
                ),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"fresh-token\"}")
                )
            )
        )
        val executor = Executors.newFixedThreadPool(2)
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val service = HttpApiService(context, executor)
            service.setToken("")
            val first = EventEndpoint(Event().apply { setName("first") }).also { it.setBaseUrl(server.baseUrl) }
            val second = EventEndpoint(Event().apply { setName("second") }).also { it.setBaseUrl(server.baseUrl) }
            val completed = CountDownLatch(2)
            executor.submit {
                service.executeRequest(first, com.appambit.sdk.models.responses.EventResponse::class.java)
                completed.countDown()
            }
            executor.submit {
                service.executeRequest(second, com.appambit.sdk.models.responses.EventResponse::class.java)
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(1, server.requestCount("/consumer/token"))
            assertEquals(2, server.requestCount("/events"))
            assertTrue(server.requests.filter { it.path == "/events" }
                .all { it.headers["authorization"] == "Bearer fresh-token" })
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `cloud code request with missing token refreshes before the first call`() {
        val server = HttpProbeServer(
            mapOf(
                "/fn/hello" to mutableListOf(HttpProbeServer.Response(200, "{\"ok\":true}")),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"cloud-token\"}")
                )
            )
        )
        val executor = Executors.newFixedThreadPool(2)
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val service = HttpApiService(context, executor)
            service.setToken("")
            val endpoint = CloudCodeEndpoint("hello", HttpMethodEnum.GET, null, null, null)
            endpoint.setBaseUrl(server.baseUrl)
            val completed = CountDownLatch(1)
            var response: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                response = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(200, response!!.statusCode)
            assertEquals(1, server.requestCount("/consumer/token"))
            assertEquals(1, server.requestCount("/fn/hello"))
            assertEquals("Bearer cloud-token", server.requests.first { it.path == "/fn/hello" }.headers["authorization"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `Cloud Code raw requests use their dedicated executor`() {
        val server = HttpProbeServer(
            mapOf("/fn/hello" to mutableListOf(HttpProbeServer.Response(200, "{}")))
        )
        val sdkExecutor = Executors.newSingleThreadExecutor()
        val cloudCodeExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "cloud-code-test")
        }
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val service = HttpApiService(context, sdkExecutor, cloudCodeExecutor)
            service.setToken("cloud-token")
            val endpoint = CloudCodeEndpoint("hello", HttpMethodEnum.GET, null, null, null)
            endpoint.setBaseUrl(server.baseUrl)
            val completed = CountDownLatch(1)
            var callbackThread: String? = null

            service.executeRaw(endpoint, 2_000) {
                callbackThread = Thread.currentThread().name
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals("cloud-code-test", callbackThread)
        } finally {
            sdkExecutor.shutdownNow()
            cloudCodeExecutor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `cloud code refresh ignores an in-flight token renewal invalidated before completion`() {
        val tokenStarted = CountDownLatch(1)
        val releaseTokenResponse = CountDownLatch(1)
        val server = HttpProbeServer(
            mapOf(
                "/fn/hello" to mutableListOf(HttpProbeServer.Response(200, "{\"ok\":true}")),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(
                        200,
                        "{\"id\":\"consumer\",\"token\":\"stale-token\"}",
                        beforeResponse = {
                            tokenStarted.countDown()
                            releaseTokenResponse.await(5, TimeUnit.SECONDS)
                        }
                    ),
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"fresh-token\"}")
                )
            )
        )
        val executor = Executors.newFixedThreadPool(2)
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val service = HttpApiService(context, executor)
            service.setToken("old-token")

            val inFlightRenewal = service.GetNewToken()
            assertTrue(tokenStarted.await(5, TimeUnit.SECONDS))

            service.setToken("")
            releaseTokenResponse.countDown()

            assertEquals(ApiErrorType.Unknown, inFlightRenewal.getBlocking(5, TimeUnit.SECONDS))
            assertNull(service.getToken())

            val endpoint = CloudCodeEndpoint("hello", HttpMethodEnum.GET, null, null, null)
            endpoint.setBaseUrl(server.baseUrl)
            val completed = CountDownLatch(1)
            var response: HttpTransportResponse? = null
            service.executeRaw(endpoint, 2_000) {
                response = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(200, response!!.statusCode)
            assertEquals(2, server.requestCount("/consumer/token"))
            assertEquals(1, server.requestCount("/fn/hello"))
            assertEquals(
                "Bearer fresh-token",
                server.requests.first { it.path == "/fn/hello" }.headers["authorization"]
            )
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `legacy 403 is terminal and never retries a mutable request`() {
        val server = HttpProbeServer(
            mapOf("/events" to mutableListOf(HttpProbeServer.Response(403, "<html>forbidden</html>")))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val service = HttpApiService(context, executor)
            service.setToken("legacy-token")
            val eventEndpoint = EventEndpoint(Event().apply { setName("purchase") })
            eventEndpoint.setBaseUrl(server.baseUrl)

            val result = service.executeRequest(eventEndpoint, com.appambit.sdk.models.responses.EventResponse::class.java)

            assertEquals(com.appambit.sdk.enums.ApiErrorType.Unknown, result.errorType)
            assertEquals(1, server.requestCount("/events"))
            assertEquals(1, server.requests.size)
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `invalid slug and reserved header fail before transport`() {
        val transport = FakeTransport()
        val service = CloudCodeService(transport)

        var invalidSlug: Throwable? = null
        service.call("invalid/function", HttpMethodEnum.POST, null, null, null)
            .onError { invalidSlug = it }
        assertTrue(invalidSlug is CloudCodeError)
        assertEquals(CloudCodeError.Code.INVALID_FUNCTION, (invalidSlug as CloudCodeError).code)
        assertEquals(0, transport.requestCount)

        listOf("hello world", "hello\u0001world").forEach { slug ->
            var error: Throwable? = null
            service.call(slug, HttpMethodEnum.GET, null, null, null)
                .onError { error = it }
            assertEquals(CloudCodeError.Code.INVALID_FUNCTION, (error as CloudCodeError).code)
        }
        assertEquals(0, transport.requestCount)

        var invalidHeader: Throwable? = null
        service.call("hello", HttpMethodEnum.POST, null, null, mapOf("authorization" to "spoofed"))
            .onError { invalidHeader = it }
        assertEquals(CloudCodeError.Code.INVALID_HEADER, (invalidHeader as CloudCodeError).code)
        assertEquals(0, transport.requestCount)
    }

    @Test
    fun `dynamic result supports JSON values and metadata`() {
        val transport = FakeTransport()
        transport.response = response(201, "{\"ok\":true,\"count\":3}", mapOf("X-Request-Id" to "header-id"))
        val service = CloudCodeService(transport)

        var received: CloudCodeResponse? = null
        var error: Throwable? = null
        service.call("hello", HttpMethodEnum.POST, null, mapOf("name" to "Ada"), null)
            .then { received = it }
            .onError { error = it }

        assertNull(error)
        assertNotNull(received)
        assertEquals(201, received!!.statusCode)
        assertEquals("header-id", received!!.requestId)
        assertEquals("header-id", received!!.headers["X-Request-Id"])
        assertEquals(true, (received!!.data as Map<*, *>) ["ok"])
        assertEquals(3, (received!!.data as Map<*, *>) ["count"])
    }

    @Test
    fun `Cloud Code uses the sixty second operation timeout`() {
        val transport = FakeTransport()
        transport.response = response(200, "{}")

        CloudCodeService(transport).call("hello", HttpMethodEnum.GET, null, null, null)

        assertEquals(60_000, transport.lastTimeoutMillis)
    }

    @Test
    fun `dynamic result supports primitive and null JSON values`() {
        val transport = FakeTransport()
        val service = CloudCodeService(transport)

        transport.response = response(200, "\"hello\"")
        var stringResult: Any? = "unset"
        service.call("string", HttpMethodEnum.GET, null, null, null)
            .then { stringResult = it.data }
        assertEquals("hello", stringResult)

        transport.response = response(200, "7")
        var numberResult: Any? = null
        service.call("number", HttpMethodEnum.GET, null, null, null)
            .then { numberResult = it.data }
        assertEquals(7, numberResult)

        transport.response = response(200, "true")
        var booleanResult: Any? = null
        service.call("boolean", HttpMethodEnum.GET, null, null, null)
            .then { booleanResult = it.data }
        assertEquals(true, booleanResult)

        transport.response = response(200, "null")
        var nullResult: Any? = "unset"
        service.call("null", HttpMethodEnum.GET, null, null, null)
            .then { nullResult = it.data }
        assertNull(nullResult)

        transport.response = response(200, null, mapOf("X-Request-Id" to "empty-body-id"))
        var emptyResponse: CloudCodeResponse? = null
        service.call("empty", HttpMethodEnum.GET, null, null, null)
            .then { emptyResponse = it }
        assertNotNull(emptyResponse)
        assertNull(emptyResponse!!.data)
        assertEquals(200, emptyResponse!!.statusCode)
        assertEquals("empty-body-id", emptyResponse!!.requestId)
        assertEquals("empty-body-id", emptyResponse!!.headers["X-Request-Id"])
    }

    @Test
    fun `typed result decodes model and request id body fallback`() {
        class Greeting {
            @JvmField var greeting: String = ""
        }

        val transport = FakeTransport()
        transport.response = response(200, "{\"greeting\":\"hello\",\"request_id\":\"body-id\"}")
        val service = CloudCodeService(transport)

        var received: CloudCodeResult<Greeting>? = null
        var error: Throwable? = null
        service.callTyped("typed", HttpMethodEnum.GET, null, null, null, Greeting::class.java)
            .then { received = it }
            .onError { error = it }

        assertNull(error)
        assertEquals("hello", received!!.data!!.greeting)
        assertEquals("body-id", received!!.requestId)
    }

    @Test
    fun `typed response reports when no model field matches`() {
        class Greeting {
            @JvmField var greeting: String = ""
        }

        val transport = FakeTransport()
        transport.response = response(200, "{\"unexpected\":true}")
        var error: Throwable? = null

        CloudCodeService(transport)
            .callTyped("typed", HttpMethodEnum.GET, null, null, null, Greeting::class.java)
            .onError { error = it }

        assertTrue(error is CloudCodeError)
        assertEquals(CloudCodeError.Code.DECODING, (error as CloudCodeError).code)
        assertTrue(error!!.message!!.contains("No response fields matched"))
    }

    @Test
    fun `typed successful empty body preserves nullable data and headers`() {
        val transport = FakeTransport()
        transport.response = response(200, null, mapOf("X-Request-Id" to "empty-body-id"))
        val service = CloudCodeService(transport)

        var received: CloudCodeResult<String>? = null
        var error: Throwable? = null
        service.callTyped("empty", HttpMethodEnum.GET, null, null, null, String::class.java)
            .then { received = it }
            .onError { error = it }

        assertNull(error)
        assertNotNull(received)
        assertNull(received!!.data)
        assertEquals(200, received!!.statusCode)
        assertEquals("empty-body-id", received!!.requestId)
        assertEquals("empty-body-id", received!!.headers["X-Request-Id"])
    }

    @Test
    fun `HTTP errors preserve status body and request id`() {
        val transport = FakeTransport()
        transport.response = response(429, "{\"error\":\"quota_exceeded\",\"request_id\":\"error-id\"}")
        val service = CloudCodeService(transport)

        var error: Throwable? = null
        service.call("limited", HttpMethodEnum.POST, null, null, null).onError { error = it }

        assertTrue(error is CloudCodeError)
        val cloudError = error as CloudCodeError
        assertEquals(CloudCodeError.Code.HTTP, cloudError.code)
        assertEquals(429, cloudError.statusCode)
        assertEquals("error-id", cloudError.requestId)
        assertEquals("quota_exceeded", (cloudError.body as Map<*, *>) ["error"])
        assertNull(cloudError.rawBody)
    }

    @Test
    fun `HTTP error scalars and HTML remain raw while objects and arrays are structured`() {
        val transport = FakeTransport()
        val service = CloudCodeService(transport)

        fun errorFor(body: String): CloudCodeError {
            transport.response = response(502, body)
            var received: Throwable? = null
            service.call("failure", HttpMethodEnum.GET, null, null, null)
                .onError { received = it }
            return received as CloudCodeError
        }

        val html = errorFor("<html>gateway failure</html>")
        assertNull(html.body)
        assertEquals("<html>gateway failure</html>", html.rawBody)

        val string = errorFor("\"gateway failure\"")
        assertNull(string.body)
        assertEquals("\"gateway failure\"", string.rawBody)

        val number = errorFor("42")
        assertNull(number.body)
        assertEquals("42", number.rawBody)

        val jsonNull = errorFor("null")
        assertNull(jsonNull.body)
        assertEquals("null", jsonNull.rawBody)

        val array = errorFor("[{\"code\":\"one\"}]")
        assertTrue(array.body is List<*>)
    }

    @Test
    fun `204 typed response completes with null data`() {
        val transport = FakeTransport()
        transport.response = response(204, null, mapOf("X-Request-Id" to "empty-id"))
        val service = CloudCodeService(transport)

        var received: CloudCodeResult<String>? = null
        var error: Throwable? = null
        service.callTyped("empty", HttpMethodEnum.DELETE, null, null, null, String::class.java)
            .then { received = it }
            .onError { error = it }

        assertNull(error)
        assertNotNull(received)
        assertNull(received!!.data)
        assertEquals("empty-id", received!!.requestId)
        assertEquals("empty-id", received!!.headers["X-Request-Id"])
    }

    @Test
    fun `cancel suppresses callback`() {
        val transport = FakeTransport()
        transport.response = null
        val service = CloudCodeService(transport)
        var callbackCalled = false
        val request = service.call("slow", HttpMethodEnum.POST, null, null, null)
            .then { callbackCalled = true }

        request.cancel()
        transport.deliver(response(200, "{\"ok\":true}"))

        assertTrue(request.isCancelled)
        assertFalse(callbackCalled)
    }

    @Test
    fun `invalid method fails through the request error channel`() {
        val transport = FakeTransport()
        val service = CloudCodeService(transport)
        var error: Throwable? = null

        val request = service.call("hello", null, null, null, null)
            .onError { error = it }

        assertTrue(request.isDone)
        assertTrue(error is CloudCodeError)
        assertEquals(CloudCodeError.Code.INVALID_METHOD, (error as CloudCodeError).code)
        assertEquals(0, transport.requestCount)
    }

    @Test
    fun `transport exception fails instead of escaping or leaving request pending`() {
        val transport = FakeTransport()
        transport.throwOnExecute = IllegalStateException("transport failed")
        val service = CloudCodeService(transport)
        var error: Throwable? = null

        val request = service.call("hello", HttpMethodEnum.POST, null, null, null)
            .onError { error = it }

        assertTrue(request.isDone)
        assertTrue(error is CloudCodeError)
        assertEquals(CloudCodeError.Code.TRANSPORT, (error as CloudCodeError).code)
    }

    @Test
    fun `transport exceptions retain their specific Cloud Code categories`() {
        fun errorFor(transportError: Throwable): CloudCodeError {
            val transport = FakeTransport()
            transport.throwOnExecute = transportError
            var error: Throwable? = null
            CloudCodeService(transport).call("hello", HttpMethodEnum.GET, null, null, null)
                .onError { error = it }
            return error as CloudCodeError
        }

        assertEquals(
            CloudCodeError.Code.INVALID_URL,
            errorFor(MalformedURLException("bad URL")).code
        )
        assertEquals(
            CloudCodeError.Code.NETWORK_UNAVAILABLE,
            errorFor(UnknownHostException("host")).code
        )
        assertEquals(
            CloudCodeError.Code.NETWORK_UNAVAILABLE,
            errorFor(ConnectException("connect")).code
        )
        assertEquals(
            CloudCodeError.Code.TIMED_OUT,
            errorFor(SocketTimeoutException("timeout")).code
        )
        assertEquals(
            CloudCodeError.Code.TRANSPORT,
            errorFor(SSLHandshakeException("handshake")).code
        )
        assertEquals(
            CloudCodeError.Code.TRANSPORT,
            errorFor(IOException("reset")).code
        )
    }

    @Test
    fun `invalid query and header values fail before transport`() {
        val transport = FakeTransport()
        val service = CloudCodeService(transport)
        var queryError: Throwable? = null
        var headerError: Throwable? = null

        service.call("hello", HttpMethodEnum.GET, mapOf("bad" to null), null, null)
            .onError { queryError = it }
        service.call("hello", HttpMethodEnum.GET, null, null, mapOf("X-Test" to "bad\nvalue"))
            .onError { headerError = it }

        assertEquals(CloudCodeError.Code.INVALID_QUERY, (queryError as CloudCodeError).code)
        assertEquals(CloudCodeError.Code.INVALID_HEADER, (headerError as CloudCodeError).code)
        assertEquals(0, transport.requestCount)
    }

    @Test
    fun `body is snapshotted before transport execution`() {
        val transport = FakeTransport()
        transport.response = response(200, "{}")
        val service = CloudCodeService(transport)
        val body = linkedMapOf<String, Any>("value" to "before")

        service.call("hello", HttpMethodEnum.POST, null, body, null)
        body["value"] = "after"
        assertEquals("before", (transport.lastEndpoint!!.payload as Map<*, *>) ["value"])
    }

    @Test
    fun `second 401 is returned after one retry`() {
        val server = HttpProbeServer(
            mapOf(
                "/fn/retry" to mutableListOf(
                    HttpProbeServer.Response(401, "{\"error\":\"expired\"}"),
                    HttpProbeServer.Response(401, "{\"error\":\"still_expired\"}")
                ),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"new-token\"}")
                )
            )
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val endpoint = CloudCodeEndpoint("retry", HttpMethodEnum.GET, null, null, null)
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("old-token")
            val completed = CountDownLatch(1)
            var received: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                received = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(401, received!!.statusCode)
            assertEquals(2, server.requestCount("/fn/retry"))
            assertEquals("requests=${server.requests}", 1, server.requestCount("/consumer/token"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `cancel makes the request terminal`() {
        val transport = FakeTransport()
        transport.response = null
        val service = CloudCodeService(transport)
        var callbackCalled = false
        var errorCallbackCalled = false
        val request = service.call("slow", HttpMethodEnum.POST, null, null, null)
            .then { callbackCalled = true }
            .onError { errorCallbackCalled = true }

        request.cancel()

        assertTrue(request.isCancelled)
        assertTrue(request.isDone)
        assertFalse(callbackCalled)
        assertFalse(errorCallbackCalled)
    }

    @Test
    fun `getBlocking rejects the Android main thread`() {
        val request = CloudCodeService(FakeTransport()).call(
            "pending", HttpMethodEnum.GET, null, null, null
        )
        every { Looper.myLooper() } returns Looper.getMainLooper()

        assertThrows(IllegalStateException::class.java) {
            request.getBlocking(1, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun `getBlocking rejects SDK-owned executor threads`() {
        val request = CloudCodeService(FakeTransport()).call(
            "pending", HttpMethodEnum.GET, null, null, null
        )
        val executor = Executors.newSingleThreadExecutor(SdkThreadFactory("test-sdk"))
        try {
            val completed = executor.submit<Boolean> {
                assertThrows(IllegalStateException::class.java) {
                    request.getBlocking(1, TimeUnit.MILLISECONDS)
                }
                true
            }
            assertTrue(completed.get(5, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `getBlocking remains available to an external worker`() {
        val future = AppAmbitTaskFuture<Int>()
        future.complete(7)

        assertEquals(7, future.getBlocking(1, TimeUnit.SECONDS))
    }

    @Test
    fun `typed response decodes nested model and model list`() {
        val transport = FakeTransport()
        transport.response = response(
            200,
            """{"base_name":"inherited","child":{"name":"one"},"children":[{"name":"two"}]}"""
        )
        val service = CloudCodeService(transport)
        var received: CloudCodeResult<NestedParent>? = null

        service.callTyped("nested", HttpMethodEnum.GET, null, null, null, NestedParent::class.java)
            .then { received = it }

        assertNotNull(received)
        assertEquals("inherited", received!!.data!!.baseName)
        assertEquals("one", received!!.data!!.child!!.name)
        assertEquals("two", received!!.data!!.children[0].name)
    }

    @Test
    fun `http transport does not send a body for GET`() {
        val server = HttpProbeServer(
            mapOf("/fn/read" to mutableListOf(HttpProbeServer.Response(200, "{}")))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = CloudCodeEndpoint(
                "read", HttpMethodEnum.GET, null, mapOf("ignored" to true), null
            )
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("transport-token")
            val completed = CountDownLatch(1)
            var received: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                received = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(null, received!!.error)
            assertTrue("requests=${server.requests}", server.requests.first().method == "GET")
            assertEquals("", server.requests.first().body)
            assertNull(server.requests.first().headers["content-type"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `http transport sends PUT body and standard headers`() {
        val server = HttpProbeServer(
            mapOf("/fn/update" to mutableListOf(HttpProbeServer.Response(200, "{}")))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = CloudCodeEndpoint(
                "update",
                HttpMethodEnum.PUT,
                null,
                mapOf<String, Any>("id" to 7, "name" to "Ada"),
                mapOf("X-Probe" to "put")
            )
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("transport-token")
            val completed = CountDownLatch(1)
            var received: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                received = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertNull(received!!.error)
            assertEquals("PUT", server.requests.first().method)
            val requestBody = JSONObject(server.requests.first().body)
            assertEquals(7, requestBody.getInt("id"))
            assertEquals("Ada", requestBody.getString("name"))
            assertEquals("application/json", server.requests.first().headers["content-type"])
            assertEquals("application/json", server.requests.first().headers["accept"])
            assertEquals("Bearer transport-token", server.requests.first().headers["authorization"])
            assertEquals("put", server.requests.first().headers["x-probe"])
            assertNull(server.requests.first().headers["x-http-method-override"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `http transport sends PATCH body without method override`() {
        val server = HttpProbeServer(
            mapOf("/fn/patch" to mutableListOf(HttpProbeServer.Response(200, "{}")))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = CloudCodeEndpoint(
                "patch",
                HttpMethodEnum.PATCH,
                null,
                mapOf<String, Any>("id" to 7, "name" to "Ada"),
                mapOf("X-Probe" to "patch")
            )
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("transport-token")
            val completed = CountDownLatch(1)
            var received: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                received = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertNull(received!!.error)
            assertEquals("PATCH", server.requests.first().method)
            val requestBody = JSONObject(server.requests.first().body)
            assertEquals(7, requestBody.getInt("id"))
            assertEquals("Ada", requestBody.getString("name"))
            assertEquals("application/json", server.requests.first().headers["content-type"])
            assertEquals("application/json", server.requests.first().headers["accept"])
            assertEquals("Bearer transport-token", server.requests.first().headers["authorization"])
            assertEquals("patch", server.requests.first().headers["x-probe"])
            assertNull(server.requests.first().headers["x-http-method-override"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `CMS request uses the shared transport and preserves app key`() {
        val server = HttpProbeServer(
            mapOf("/posts" to mutableListOf(HttpProbeServer.Response(200, "{\"ok\":true}")))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(AppAmbit::class)
        every { AppAmbit.getAppKey() } returns "cms-app-key"

        try {
            val endpoint = object : CmsEndpoint("posts", mapOf("locale" to "en")) {
                override fun getBaseUrl(): String = server.baseUrl
            }
            val result = HttpApiService(context, executor)
                .executeRequest(endpoint, String::class.java)

            assertEquals(ApiErrorType.None, result.errorType)
            assertEquals("{\"ok\":true}", result.data)
            assertEquals(1, server.requests.size)
            assertEquals("GET", server.requests.first().method)
            assertEquals("application/json", server.requests.first().headers["accept"])
            assertEquals("cms-app-key", server.requests.first().headers["x-app-key"])
            assertNull(server.requests.first().headers["authorization"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `http transport preserves error body and response headers`() {
        val server = HttpProbeServer(
            mapOf(
                "/fn/failure" to mutableListOf(
                    HttpProbeServer.Response(
                        503,
                        "{\"error\":\"unavailable\"}",
                        headers = mapOf("X-Request-Id" to "probe-id")
                    )
                )
            )
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = CloudCodeEndpoint("failure", HttpMethodEnum.GET, null, null, null)
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("transport-token")
            val completed = CountDownLatch(1)
            var received: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                received = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertNull(received!!.error)
            assertEquals(503, received!!.statusCode)
            assertEquals(
                "{\"error\":\"unavailable\"}",
                String(received!!.body!!, StandardCharsets.UTF_8)
            )
            assertEquals("probe-id", received!!.headers["X-Request-Id"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `blocking transport adapter returns a timeout without invoking a callback`() {
        val transport = FakeTransport()
        val endpoint = CloudCodeEndpoint("pending", HttpMethodEnum.GET, null, null, null)

        val response = transport.executeBlocking(endpoint, 20)

        assertTrue(response.error is SocketTimeoutException)
        assertNull(response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `http transport sends DELETE body and custom headers`() {
        val server = HttpProbeServer(
            mapOf("/fn/delete" to mutableListOf(HttpProbeServer.Response(200, "{}")))
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true

        try {
            val endpoint = CloudCodeEndpoint(
                "delete", HttpMethodEnum.DELETE, null, mapOf("id" to 7), mapOf("X-Probe" to "delete")
            )
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("transport-token")
            val completed = CountDownLatch(1)

            service.executeRaw(endpoint, 2_000) { completed.countDown() }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals("DELETE", server.requests.first().method)
            assertEquals("{\"id\":7}", server.requests.first().body)
            assertEquals("delete", server.requests.first().headers["x-probe"])
            assertEquals("application/json", server.requests.first().headers["content-type"])
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `http transport renews once and retries one 401`() {
        val server = HttpProbeServer(
            mapOf(
                "/fn/retry" to mutableListOf(
                    HttpProbeServer.Response(401, "{\"error\":\"expired\"}"),
                    HttpProbeServer.Response(200, "{\"ok\":true}")
                ),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"new-token\"}")
                )
            )
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val endpoint = CloudCodeEndpoint("retry", HttpMethodEnum.GET, null, null, null)
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("old-token")
            val completed = CountDownLatch(1)
            var received: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                received = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(null, received!!.error)
            assertEquals(200, received!!.statusCode)
            assertEquals(2, server.requestCount("/fn/retry"))
            assertTrue("requests=${server.requests}", server.requestCount("/consumer/token") == 1)
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `http transport retries mutating Cloud Code requests after 401`() {
        val server = HttpProbeServer(
            mapOf(
                "/fn/mutate" to mutableListOf(
                    HttpProbeServer.Response(401, "{\"error\":\"expired\"}"),
                    HttpProbeServer.Response(200, "{\"ok\":true}")
                ),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"new-token\"}")
                )
            )
        )
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val endpoint = CloudCodeEndpoint("mutate", HttpMethodEnum.POST, null, mapOf("id" to 1), null)
            endpoint.setBaseUrl(server.baseUrl)
            val service = HttpApiService(context, executor)
            service.setToken("old-token")
            val completed = CountDownLatch(1)
            var received: HttpTransportResponse? = null

            service.executeRaw(endpoint, 2_000) {
                received = it
                completed.countDown()
            }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(200, received!!.statusCode)
            assertEquals(2, server.requestCount("/fn/mutate"))
            assertEquals(1, server.requestCount("/consumer/token"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    @Test
    fun `concurrent 401 requests share token renewal`() {
        val server = HttpProbeServer(
            mapOf(
                "/fn/one" to mutableListOf(
                    HttpProbeServer.Response(401, "{}"),
                    HttpProbeServer.Response(200, "{\"ok\":1}")
                ),
                "/fn/two" to mutableListOf(
                    HttpProbeServer.Response(401, "{}"),
                    HttpProbeServer.Response(200, "{\"ok\":2}")
                ),
                "/consumer/token" to mutableListOf(
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"new-token\"}"),
                    HttpProbeServer.Response(200, "{\"id\":\"consumer\",\"token\":\"newer-token\"}")
                )
            )
        )
        val executor = Executors.newFixedThreadPool(2)
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        mockkStatic(InternetConnection::class)
        every { InternetConnection.hasInternetConnection(any()) } returns true
        mockkStatic(TokenService::class)
        val tokenEndpoint = TokenEndpoint(mockk(relaxed = true))
        tokenEndpoint.setBaseUrl(server.baseUrl)
        every { TokenService.createTokenendpoint() } returns tokenEndpoint

        try {
            val service = HttpApiService(context, executor)
            service.setToken("old-token")
            val completed = CountDownLatch(2)
            val endpointOne = CloudCodeEndpoint("one", HttpMethodEnum.GET, null, null, null)
            val endpointTwo = CloudCodeEndpoint("two", HttpMethodEnum.GET, null, null, null)
            endpointOne.setBaseUrl(server.baseUrl)
            endpointTwo.setBaseUrl(server.baseUrl)

            service.executeRaw(endpointOne, 2_000) { completed.countDown() }
            service.executeRaw(endpointTwo, 2_000) { completed.countDown() }

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(1, server.requestCount("/consumer/token"))
        } finally {
            executor.shutdownNow()
            server.close()
        }
    }

    private fun response(status: Int, body: String?, headers: Map<String, String> = emptyMap()) =
        HttpTransportResponse(
            status,
            body?.toByteArray(StandardCharsets.UTF_8),
            headers,
            null
        )

    private class FakeTransport : HttpTransport {
        var response: HttpTransportResponse? = null
        var requestCount: Int = 0
        var lastTimeoutMillis: Int? = null
        var throwOnExecute: Throwable? = null
        var lastEndpoint: com.appambit.sdk.services.interfaces.IEndpoint? = null
        private var pending: HttpTransport.Callback? = null

        override fun executeRaw(
            endpoint: com.appambit.sdk.services.interfaces.IEndpoint,
            timeoutMillis: Int,
            callback: HttpTransport.Callback
        ) {
            requestCount++
            lastTimeoutMillis = timeoutMillis
            lastEndpoint = endpoint
            throwOnExecute?.let { throw it }
            if (response == null) pending = callback else callback.onComplete(response!!)
        }

        fun deliver(response: HttpTransportResponse) {
            val callback = pending
            pending = null
            callback?.onComplete(response)
        }

    }

    internal class HttpProbeServer(
        private val responses: Map<String, MutableList<Response>>
    ) : AutoCloseable {
        data class Response(
            val status: Int,
            val body: String,
            val beforeResponse: (() -> Unit)? = null,
            val headers: Map<String, String> = emptyMap()
        )
        data class Request(
            val method: String,
            val path: String,
            val body: String,
            val headers: Map<String, String>,
            val target: String
        )

        private val socket = ServerSocket(0)
        private val requestsInternal = mutableListOf<Request>()
        private val thread = Thread {
            while (!socket.isClosed) {
                try {
                    handle(socket.accept())
                } catch (_: Exception) {
                    if (!socket.isClosed) throw RuntimeException("HTTP probe server failed")
                }
            }
        }.apply { start() }

        val baseUrl: String get() = "http://127.0.0.1:${socket.localPort}"
        val requests: List<Request> get() = synchronized(requestsInternal) { requestsInternal.toList() }

        fun requestCount(path: String): Int = requests.count { it.path == path }

        private fun handle(client: Socket) {
            client.use { connection ->
                val input = connection.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                val requestLine = input.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                    val separator = line.indexOf(':')
                    if (separator > 0) headers[line.substring(0, separator).lowercase()] = line.substring(separator + 1).trim()
                }
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                val body = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val count = input.read(body, read, contentLength - read)
                    if (count <= 0) break
                    read += count
                }
                val request = Request(
                    parts[0],
                    parts[1].substringBefore('?'),
                    String(body, 0, read),
                    headers,
                    parts[1]
                )
                synchronized(requestsInternal) { requestsInternal.add(request) }
                val response = synchronized(responses) {
                    responses[request.path]?.removeFirstOrNull() ?: Response(404, "{}")
                }
                response.beforeResponse?.invoke()
                val responseBytes = response.body.toByteArray(StandardCharsets.UTF_8)
                val output = connection.getOutputStream()
                output.write("HTTP/1.1 ${response.status} Test\r\nContent-Type: application/json\r\nContent-Length: ${responseBytes.size}\r\nConnection: close\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                response.headers.forEach { (name, value) ->
                    output.write("$name: $value\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                }
                output.write("\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                output.write(responseBytes)
                output.flush()
            }
        }

        override fun close() {
            socket.close()
            thread.join(1_000)
        }
    }
}
