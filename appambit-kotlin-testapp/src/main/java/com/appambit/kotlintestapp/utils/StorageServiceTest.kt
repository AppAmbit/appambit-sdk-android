package com.appambit.kotlintestapp.utils

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.appambit.kotlintestapp.utils.storage.DataStoreTest
import com.appambit.kotlintestapp.utils.storage.StorableTest
import com.appambit.sdk.AppAmbit
import com.appambit.sdk.enums.SessionType
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.services.storage.contract.EventEntityContract
import com.appambit.sdk.services.storage.contract.LogEntityContract
import com.appambit.sdk.services.storage.contract.SessionContract
import com.appambit.sdk.utils.DateUtils
import java.io.Closeable
import java.io.IOException

class StorageServiceTest(context: Context) : StorableTest, Closeable {
    private val dataStoreTest = DataStoreTest(context)

    override fun putSessionData(sessionData: SessionData?) {
        var cursor: android.database.Cursor? = null
        try {
            when (sessionData?.sessionType) {
                SessionType.START -> {
                    val db = dataStoreTest.readableDatabase
                    val sqlCheckSession = """
                        SELECT ${SessionContract.Columns.ID}
                        FROM ${SessionContract.TABLE_NAME}
                        WHERE ${SessionContract.Columns.END_SESSION_DATE} IS NULL
                        ORDER BY ${SessionContract.Columns.START_SESSION_DATE} DESC
                        LIMIT 1
                    """.trimIndent()

                    cursor = db.rawQuery(sqlCheckSession, null)

                    var openId: String? = null
                    if (cursor.moveToNext()) {
                        openId = cursor.getString(cursor.getColumnIndexOrThrow(SessionContract.Columns.ID))
                    }

                    cursor.close()
                    cursor = null

                    if (openId != null) {
                        val updateSql = """
                            UPDATE ${SessionContract.TABLE_NAME}
                            SET ${SessionContract.Columns.END_SESSION_DATE} = ?
                            WHERE ${SessionContract.Columns.ID} = ?
                        """.trimIndent()

                        db.execSQL(updateSql, arrayOf(DateUtils.toIsoUtcWithMillis(sessionData.timestamp), openId))
                        Log.d(AppAmbit::class.java.simpleName, "SESSION START UPDATE - ${sessionData.id}")
                    }

                    val cv = ContentValues().apply {
                        put(SessionContract.Columns.ID, sessionData.id.toString())
                        put(SessionContract.Columns.SESSION_ID, sessionData.sessionId)
                        put(SessionContract.Columns.START_SESSION_DATE, DateUtils.toIsoUtcWithMillis(sessionData.timestamp))
                        putNull(SessionContract.Columns.END_SESSION_DATE)
                    }

                    db.insert(SessionContract.TABLE_NAME, null, cv)
                    Log.d(AppAmbit::class.java.simpleName, "SESSION START CREATE - ${sessionData.id}")
                }

                SessionType.END -> {
                    val db2 = dataStoreTest.readableDatabase
                    val selectSql = """
                        SELECT ${SessionContract.Columns.ID}
                        FROM ${SessionContract.TABLE_NAME}
                        WHERE ${SessionContract.Columns.END_SESSION_DATE} IS NULL
                        ORDER BY ${SessionContract.Columns.START_SESSION_DATE} DESC
                        LIMIT 1
                    """.trimIndent()

                    cursor = db2.rawQuery(selectSql, null)

                    var querySessionId: String? = null
                    if (cursor.moveToFirst()) {
                        querySessionId = cursor.getString(cursor.getColumnIndexOrThrow(SessionContract.Columns.ID))
                    }

                    cursor.close()
                    cursor = null

                    if (!querySessionId.isNullOrEmpty()) {
                        val updateSql = """
                            UPDATE ${SessionContract.TABLE_NAME}
                            SET ${SessionContract.Columns.END_SESSION_DATE} = ?
                            WHERE ${SessionContract.Columns.ID} = ?
                        """.trimIndent()

                        db2.execSQL(updateSql, arrayOf(DateUtils.toIsoUtcWithMillis(sessionData.timestamp), querySessionId))
                        Log.d(AppAmbit::class.java.simpleName, "SESSION END UPDATE - ${sessionData.sessionId}")
                    } else {
                        val cv2 = ContentValues().apply {
                            put(SessionContract.Columns.ID, sessionData.id.toString())
                            put(SessionContract.Columns.SESSION_ID, sessionData.sessionId)
                            putNull(SessionContract.Columns.START_SESSION_DATE)
                            put(SessionContract.Columns.END_SESSION_DATE, DateUtils.toIsoUtcWithMillis(sessionData.timestamp))
                        }
                        try {
                            db2.insert(SessionContract.TABLE_NAME, null, cv2)
                            Log.d(AppAmbit::class.java.simpleName, "SESSION END CREATE - ${sessionData.sessionId}")
                        } catch (_: SQLiteConstraintException) {
                            Log.d(AppAmbit::class.java.simpleName, "Session duplicate avoided - ${sessionData.sessionId}")
                        } catch (e: Exception) {
                            Log.e(AppAmbit::class.java.simpleName, "Error inserting end session", e)
                        }
                    }
                }

                null -> {}
            }
        } catch (e: Exception) {
            Log.e(AppAmbit::class.java.simpleName, "Error inserting log session", e)
        } finally {
            cursor?.close()
        }
    }

    override fun updateLogSessionId(sessionId: String?) {
        val db = dataStoreTest.writableDatabase
        val updateSql = """
            UPDATE ${LogEntityContract.TABLE_NAME}
            SET ${LogEntityContract.Columns.SESSION_ID} = ?
            WHERE ${LogEntityContract.Columns.ID} = (
                SELECT ${LogEntityContract.Columns.ID}
                FROM ${LogEntityContract.TABLE_NAME}
                ORDER BY ${LogEntityContract.Columns.CREATED_AT} DESC
                LIMIT 1
            )
        """.trimIndent()

        try {
            db.execSQL(updateSql, arrayOf(sessionId))
            val affected = DatabaseUtils.longForQuery(db, "SELECT changes()", null)
            if (affected > 0) {
                Log.d(AppAmbit::class.java.simpleName, "LOG UPDATE OK → last log linked to session $sessionId")
            } else {
                Log.d(AppAmbit::class.java.simpleName, "No log updated (table empty?)")
            }
        } catch (e: Exception) {
            Log.e(AppAmbit::class.java.simpleName, "Error updating session ID", e)
        }
    }

    override fun updateEventSessionId(sessionId: String?) {
        val db = dataStoreTest.readableDatabase
        val updateSql = """
            UPDATE ${EventEntityContract.TABLE_NAME}
            SET ${EventEntityContract.Columns.SESSION_ID} = ?
            WHERE ${EventEntityContract.Columns.ID} = (
                SELECT ${EventEntityContract.Columns.ID}
                FROM ${EventEntityContract.TABLE_NAME}
                ORDER BY ${EventEntityContract.Columns.CREATED_AT} DESC
                LIMIT 1
            )
        """.trimIndent()

        try {
            db.execSQL(updateSql, arrayOf(sessionId))
            val affected = DatabaseUtils.longForQuery(db, "SELECT changes()", null)
            if (affected > 0) {
                Log.d(AppAmbit::class.java.simpleName, "LOG UPDATE OK → last event linked to session $sessionId")
            } else {
                Log.d(AppAmbit::class.java.simpleName, "No event updated (table empty?)")
            }
        } catch (e: Exception) {
            Log.e(AppAmbit::class.java.simpleName, "Error updating session ID", e)
        }
    }

    @Throws(IOException::class)
    override fun close() {}
}