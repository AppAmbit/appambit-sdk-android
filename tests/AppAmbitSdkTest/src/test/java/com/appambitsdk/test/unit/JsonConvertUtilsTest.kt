package com.appambitsdk.test.unit

import android.util.Log
import com.appambit.sdk.models.db.DbBatchRequest
import com.appambit.sdk.models.db.DbQueryRequest
import com.appambit.sdk.models.db.DbStatement
import com.appambit.sdk.utils.JsonConvertUtils
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SQL parameters may be null. JsonConvertUtils was written for analytics, which never puts
 * null inside a list, so a null parameter threw a NullPointerException in isSimpleType and
 * the request failed on the device as ApiErrorType.Unknown. A null must be sent as a JSON
 * null in the same position so the backend binds NULL to the right placeholder.
 */
class JsonConvertUtilsTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `DbQueryRequest keeps a null parameter in its position`() {
        val request = DbQueryRequest("INSERT INTO t (a, b, c) VALUES (?, ?, ?)", listOf("x", null, 3))

        val params = JSONObject(JsonConvertUtils.toJson(request)).getJSONArray("params")

        assertEquals(3, params.length())
        assertEquals("x", params.get(0))
        assertTrue(params.isNull(1))
        assertEquals(3, params.getInt(2))
    }

    @Test
    fun `DbBatchRequest keeps null parameters in every statement`() {
        val request = DbBatchRequest(
            listOf(
                DbStatement.of("INSERT INTO t (a, b) VALUES (?, ?)", null, "y"),
                DbStatement.of("UPDATE t SET a = ? WHERE b = ?", "z", null)
            ),
            true
        )

        val statements = JSONObject(JsonConvertUtils.toJson(request)).getJSONArray("statements")

        val first = statements.getJSONObject(0).getJSONArray("params")
        assertTrue(first.isNull(0))
        assertEquals("y", first.get(1))

        val second = statements.getJSONObject(1).getJSONArray("params")
        assertEquals("z", second.get(0))
        assertTrue(second.isNull(1))
    }

    @Test
    fun `arrayToJson keeps a null item in its position`() {
        val json = JsonConvertUtils.arrayToJson(arrayOf<Any?>("x", null, true))

        assertEquals(3, json.length())
        assertEquals("x", json.get(0))
        assertTrue(json.isNull(1))
        assertEquals(true, json.get(2))
    }

    @Test
    fun `map keeps a null value under its key`() {
        val json = JSONObject(JsonConvertUtils.toJson(mapOf("a" to "x", "b" to null)))

        assertEquals("x", json.get("a"))
        assertTrue(json.has("b"))
        assertTrue(json.isNull("b"))
    }

    @Test
    fun `nested collections are serialized as arrays`() {
        val json = JsonConvertUtils.arrayToJson(arrayOf<Any?>(listOf(1, null), mapOf("k" to null)))

        val inner = json.getJSONArray(0)
        assertEquals(1, inner.getInt(0))
        assertTrue(inner.isNull(1))
        assertTrue(json.getJSONObject(1).isNull("k"))
    }
}
