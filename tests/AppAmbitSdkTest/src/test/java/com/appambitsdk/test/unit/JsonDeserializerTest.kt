package com.appambitsdk.test.unit

import android.util.Log
import com.appambit.sdk.models.responses.BatchResponse
import com.appambit.sdk.utils.JsonDeserializer
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * A successful HTTP response must never be reported as a failure just because the caller
 * did not ask for a typed payload. BreadcrumbManager used to request Object.class, which
 * declares no fields, so every 201 came back as ApiErrorType.Unknown and the breadcrumb was
 * re-queued in SQLite and uploaded a second time by the batch.
 */
class JsonDeserializerTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() = unmockkAll()

    private val breadcrumbBody =
        """{"id":2579,"name":"On Resume","app_id":392,"occurrences_count":22}"""

    @Test
    fun `Object class returns null instead of throwing`() {
        assertNull(JsonDeserializer.deserializeFromJSONResponse(breadcrumbBody, Object::class.java))
    }

    @Test
    fun `Void class returns null instead of throwing`() {
        assertNull(JsonDeserializer.deserializeFromJSONResponse(breadcrumbBody, Void::class.java))
    }

    @Test
    fun `an empty body is not a parse error`() {
        assertNull(JsonDeserializer.deserializeFromJSONResponse("", BatchResponse::class.java))
        assertNull(JsonDeserializer.deserializeFromJSONResponse("   ", BatchResponse::class.java))
    }

    @Test
    fun `String class still returns the body verbatim`() {
        assertEquals(
            breadcrumbBody,
            JsonDeserializer.deserializeFromJSONResponse(breadcrumbBody, String::class.java)
        )
    }

    @Test
    fun `a real model still deserializes`() {
        val result = JsonDeserializer.deserializeFromJSONResponse(
            """{"message":"ok"}""", BatchResponse::class.java
        )
        assertEquals("ok", result.message)
    }

    /**
     * The R8 diagnostic must survive: a model whose fields were stripped (or renamed without
     * @JsonKey) still has to fail loudly rather than silently decode to an empty object.
     */
    @Test
    fun `a model whose fields match nothing still throws`() {
        assertThrows(RuntimeException::class.java) {
            JsonDeserializer.deserializeFromJSONResponse(breadcrumbBody, BatchResponse::class.java)
        }
    }
}
