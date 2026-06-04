package com.appambitsdk.test.unit

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.appambit.sdk.AppAmbitDb
import com.appambit.sdk.annotations.DbColumn
import com.appambit.sdk.models.db.DbResponse
import com.appambit.sdk.models.db.DbResult
import com.appambit.sdk.models.db.DbStatement
import com.appambit.sdk.services.DbService
import com.appambit.sdk.utils.AppAmbitTaskFuture
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppAmbitDbTest {

    @MockK
    private lateinit var mockDbService: DbService

    @Before
    fun setup() {
        MockKAnnotations.init(this)

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
    }

    @After
    fun tearDown() {
        unmockkAll()
        injectDbService(null)
    }

    // region helpers

    private fun injectDbService(dbService: DbService?) {
        val field = AppAmbitDb::class.java.getDeclaredField("mDbService")
        field.isAccessible = true
        field.set(null, dbService)
    }

    private fun successFuture(json: String): AppAmbitTaskFuture<DbResponse> {
        val future = AppAmbitTaskFuture<DbResponse>()
        future.complete(DbResponse.fromJson(json))
        return future
    }

    private fun errorFuture(error: Throwable): AppAmbitTaskFuture<DbResponse> {
        val future = AppAmbitTaskFuture<DbResponse>()
        future.fail(error)
        return future
    }

    // endregion

    // region not-initialized guard

    @Test(expected = IllegalStateException::class)
    fun `execute without init throws`() {
        injectDbService(null)
        AppAmbitDb.execute("SELECT 1")
    }

    @Test(expected = IllegalStateException::class)
    fun `batch without init throws`() {
        injectDbService(null)
        AppAmbitDb.batch(DbStatement.of("SELECT 1"))
    }

    @Test(expected = IllegalStateException::class)
    fun `from without init throws`() {
        injectDbService(null)
        AppAmbitDb.from("users", User::class.java)
    }

    // endregion

    // region execute()

    @Test
    fun `execute success completes with DbResult`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":["id"],"rows":[[1]],"rows_read":1,"rows_written":0}]}"""
        )

        var result: DbResult? = null
        AppAmbitDb.execute("SELECT 1").then { result = it }

        assertNotNull(result)
        assertFalse(result!!.hasError())
        assertEquals(1, result!!.rowsRead)
    }

    @Test
    fun `execute with params passes params to service`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        val paramsSlot = slot<List<*>>()
        every { mockDbService.query(capture(sqlSlot), capture(paramsSlot)) } returns successFuture(
            """{"results":[{"columns":["id"],"rows":[[42]],"rows_read":1,"rows_written":0}]}"""
        )

        AppAmbitDb.execute("SELECT * FROM users WHERE id = ?", 42)

        assertEquals("SELECT * FROM users WHERE id = ?", sqlSlot.captured)
        assertEquals(listOf(42), paramsSlot.captured)
    }

    @Test
    fun `execute propagates service network error`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns errorFuture(RuntimeException("network error"))

        var error: Throwable? = null
        AppAmbitDb.execute("SELECT 1").onError { error = it }

        assertNotNull(error)
        assertEquals("network error", error!!.message)
    }

    @Test
    fun `execute fails when response contains no result`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture("""{"results":[]}""")

        var error: Throwable? = null
        AppAmbitDb.execute("SELECT 1").onError { error = it }

        assertNotNull(error)
    }

    // endregion

    // region batch()

    @Test
    fun `batch success returns all results`() {
        injectDbService(mockDbService)
        every { mockDbService.batch(any(), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":1},{"columns":[],"rows":[],"rows_read":0,"rows_written":1}]}"""
        )

        var results: List<DbResult>? = null
        AppAmbitDb.batch(
            DbStatement.of("INSERT INTO t (a) VALUES (?)", 1),
            DbStatement.of("INSERT INTO t (a) VALUES (?)", 2)
        ).then { results = it }

        assertNotNull(results)
        assertEquals(2, results!!.size)
    }

    @Test
    fun `batch completes even when individual result has error (known issue)`() {
        injectDbService(mockDbService)
        every { mockDbService.batch(any(), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":0,"error":"constraint violation"}]}"""
        )

        var completed = false
        AppAmbitDb.batch(DbStatement.of("INSERT INTO t (a) VALUES (1)")).then { completed = true }

        assertTrue("batch() should complete even when results contain errors", completed)
    }

    // endregion

    // region batchInTransaction()

    @Test
    fun `batchInTransaction fails when any result has error`() {
        injectDbService(mockDbService)
        every { mockDbService.batch(any(), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":0,"error":"constraint violation"}]}"""
        )

        var error: Throwable? = null
        AppAmbitDb.batchInTransaction(DbStatement.of("INSERT INTO t (a) VALUES (1)"))
            .onError { error = it }

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("constraint violation"))
    }

    @Test
    fun `batchInTransaction passes transaction=true to service`() {
        injectDbService(mockDbService)
        val transactionSlot = slot<Boolean>()
        every { mockDbService.batch(any(), capture(transactionSlot)) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":1}]}"""
        )

        AppAmbitDb.batchInTransaction(DbStatement.of("UPDATE t SET a=1 WHERE id=1"))

        assertTrue(transactionSlot.captured)
    }

    // endregion

    // region DbQueryBuilder SQL generation

    @Test
    fun `from get builds SELECT star SQL`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).get()

        assertEquals("""SELECT * FROM "users"""", sqlSlot.captured)
    }

    @Test
    fun `select builds projected column list`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).select("id", "name").get()

        assertEquals("""SELECT "id", "name" FROM "users"""", sqlSlot.captured)
    }

    @Test
    fun `where with value builds equality condition`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        val paramsSlot = slot<List<*>>()
        every { mockDbService.query(capture(sqlSlot), capture(paramsSlot)) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).where("id", 5).get()

        assertTrue(sqlSlot.captured.contains(""""id" = ?"""))
        assertEquals(listOf(5), paramsSlot.captured)
    }

    @Test
    fun `where with operator builds comparison condition`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).where("age", ">", 18).get()

        assertTrue(sqlSlot.captured.contains(""""age" > ?"""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `where with disallowed operator throws`() {
        injectDbService(mockDbService)
        AppAmbitDb.from("users", User::class.java).where("id", "DROP TABLE", 1)
    }

    @Test
    fun `whereIn builds IN condition with correct placeholders`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        val paramsSlot = slot<List<*>>()
        every { mockDbService.query(capture(sqlSlot), capture(paramsSlot)) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).whereIn("id", listOf(1, 2, 3)).get()

        assertTrue(sqlSlot.captured.contains(""""id" IN (?, ?, ?)"""))
        assertEquals(listOf(1, 2, 3), paramsSlot.captured)
    }

    @Test
    fun `multiple where conditions combine with AND`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("orders", User::class.java)
            .where("status", "active")
            .where("amount", ">", 100)
            .get()

        val sql = sqlSlot.captured
        assertTrue(sql.contains(""""status" = ?"""))
        assertTrue(sql.contains(""""amount" > ?"""))
        assertTrue(sql.contains("AND"))
    }

    @Test
    fun `orderBy builds ORDER BY clause without DESC`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).orderBy("name").get()

        assertTrue(sqlSlot.captured.contains("""ORDER BY "name""""))
        assertFalse(sqlSlot.captured.contains("DESC"))
    }

    @Test
    fun `orderByDesc builds ORDER BY DESC clause`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).orderByDesc("created_at").get()

        assertTrue(sqlSlot.captured.contains("""ORDER BY "created_at" DESC"""))
    }

    @Test
    fun `limit and offset build correct SQL`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).limit(10).offset(20).get()

        assertTrue(sqlSlot.captured.contains("LIMIT 10"))
        assertTrue(sqlSlot.captured.contains("OFFSET 20"))
    }

    // endregion

    // region first()

    @Test
    fun `first adds LIMIT 1 to SQL`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(emptyResult())

        AppAmbitDb.from("users", User::class.java).first()

        assertTrue(sqlSlot.captured.contains("LIMIT 1"))
    }

    @Test
    fun `first returns null when result set is empty`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":["id","name"],"rows":[],"rows_read":0,"rows_written":0}]}"""
        )

        var result: User? = User()
        AppAmbitDb.from("users", User::class.java).first().then { result = it }

        assertNull(result)
    }

    @Test
    fun `first propagates server error`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":0,"error":"table not found"}]}"""
        )

        var error: Throwable? = null
        AppAmbitDb.from("users", User::class.java).first().onError { error = it }

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("table not found"))
    }

    // endregion

    // region count()

    @Test
    fun `count builds SELECT COUNT(*) SQL`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(
            """{"results":[{"columns":["COUNT(*)"],"rows":[[42]],"rows_read":1,"rows_written":0}]}"""
        )

        var count = -1
        AppAmbitDb.from("users", User::class.java).count().then { count = it }

        assertTrue(sqlSlot.captured.startsWith("SELECT COUNT(*) FROM"))
        assertEquals(42, count)
    }

    @Test
    fun `count with where includes WHERE clause`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(
            """{"results":[{"columns":["COUNT(*)"],"rows":[[5]],"rows_read":1,"rows_written":0}]}"""
        )

        AppAmbitDb.from("users", User::class.java).where("active", true).count()

        assertTrue(sqlSlot.captured.contains("WHERE"))
    }

    @Test
    fun `count propagates server error`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":0,"error":"syntax error"}]}"""
        )

        var error: Throwable? = null
        AppAmbitDb.from("users", User::class.java).count().onError { error = it }

        assertNotNull(error)
    }

    // endregion

    // region update() / delete() guards

    @Test
    fun `update without WHERE fails immediately`() {
        injectDbService(mockDbService)

        var error: Throwable? = null
        AppAmbitDb.from("users", User::class.java)
            .update(mapOf("name" to "Bob"))
            .onError { error = it }

        assertNotNull(error)
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `delete without WHERE fails immediately`() {
        injectDbService(mockDbService)

        var error: Throwable? = null
        AppAmbitDb.from("users", User::class.java)
            .delete()
            .onError { error = it }

        assertNotNull(error)
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `update with WHERE calls service`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":1}]}"""
        )

        var result: DbResult? = null
        AppAmbitDb.from("users", User::class.java)
            .where("id", 1)
            .update(mapOf("name" to "Bob"))
            .then { result = it }

        assertNotNull(result)
        assertTrue(sqlSlot.captured.startsWith("UPDATE"))
        assertTrue(sqlSlot.captured.contains("WHERE"))
    }

    @Test
    fun `delete with WHERE calls service`() {
        injectDbService(mockDbService)
        val sqlSlot = slot<String>()
        every { mockDbService.query(capture(sqlSlot), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":1}]}"""
        )

        var result: DbResult? = null
        AppAmbitDb.from("users", User::class.java)
            .where("id", 1)
            .delete()
            .then { result = it }

        assertNotNull(result)
        assertTrue(sqlSlot.captured.startsWith("DELETE FROM"))
        assertTrue(sqlSlot.captured.contains("WHERE"))
    }

    // endregion

    // region get() error propagation

    @Test
    fun `get propagates server error field`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":0,"error":"table not found"}]}"""
        )

        var error: Throwable? = null
        AppAmbitDb.from("nonexistent", User::class.java).get().onError { error = it }

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("table not found"))
    }

    @Test
    fun `get returns empty list when result has no rows`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":["id","name"],"rows":[],"rows_read":0,"rows_written":0}]}"""
        )

        var users: List<User>? = null
        AppAmbitDb.from("users", User::class.java).get().then { users = it }

        assertNotNull(users)
        assertTrue(users!!.isEmpty())
    }

    // endregion

    // region model mapping

    @Test
    fun `get maps rows to model class by field name`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":["id","name"],"rows":[[1,"Alice"],[2,"Bob"]],"rows_read":2,"rows_written":0}]}"""
        )

        var users: List<User>? = null
        AppAmbitDb.from("users", User::class.java).get().then { users = it }

        assertNotNull(users)
        assertEquals(2, users!!.size)
        assertEquals(1, users!![0].id)
        assertEquals("Alice", users!![0].name)
        assertEquals(2, users!![1].id)
        assertEquals("Bob", users!![1].name)
    }

    @Test
    fun `get maps column to field using DbColumn annotation`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":["user_name"],"rows":[["Charlie"]],"rows_read":1,"rows_written":0}]}"""
        )

        var items: List<AnnotatedUser>? = null
        AppAmbitDb.from("users", AnnotatedUser::class.java).get().then { items = it }

        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals("Charlie", items!![0].displayName)
    }

    @Test
    fun `get maps results as maps when no model class provided`() {
        injectDbService(mockDbService)
        every { mockDbService.query(any(), any()) } returns successFuture(
            """{"results":[{"columns":["id","name"],"rows":[[1,"Alice"]],"rows_read":1,"rows_written":0}]}"""
        )

        var maps: List<Map<String, Any?>>? = null
        AppAmbitDb.from("users").get().then { maps = it }

        assertNotNull(maps)
        assertEquals(1, maps!!.size)
        assertEquals(1, maps!![0]["id"])
        assertEquals("Alice", maps!![0]["name"])
    }

    // endregion

    // region helpers

    private fun emptyResult() =
        """{"results":[{"columns":[],"rows":[],"rows_read":0,"rows_written":0}]}"""

    class User {
        var id: Int = 0
        var name: String? = null
    }

    class AnnotatedUser {
        @DbColumn("user_name")
        var displayName: String? = null
    }

    // endregion
}
