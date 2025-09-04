package com.appambit.testapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.services.storage.contract.EventEntityContract;
import com.appambit.sdk.services.storage.contract.LogEntityContract;
import com.appambit.sdk.services.storage.contract.SessionContract;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.testapp.utils.storage.DataStoreTest;
import com.appambit.testapp.utils.storage.StorableTest;

import java.io.IOException;

public class StorageServiceTest implements StorableTest {
    private final DataStoreTest dataStoreTest;

    public StorageServiceTest(Context context) {
        dataStoreTest = new DataStoreTest(context);
    }

    @Override
    public void putSessionData(SessionData sessionData) {
        Cursor c = null;
        try {
            switch (sessionData.getSessionType()) {
                case START:
                    SQLiteDatabase db = dataStoreTest.getReadableDatabase();

                    String sqlCheckSession =
                            "SELECT " + SessionContract.Columns.ID +
                                    " FROM " + SessionContract.TABLE_NAME +
                                    " WHERE " + SessionContract.Columns.END_SESSION_DATE + " IS NULL" +
                                    " ORDER BY " + SessionContract.Columns.START_SESSION_DATE + " DESC" +
                                    " LIMIT 1";

                    c = db.rawQuery(sqlCheckSession, null);

                    String openId = "";
                    if(c.moveToNext()) {
                        openId = c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.ID));
                    }

                    c.close();
                    c = null;

                    if(openId != null) {
                        String updateSql = "UPDATE " + SessionContract.TABLE_NAME +
                                " SET " + SessionContract.Columns.END_SESSION_DATE + " = ? " +
                                " WHERE " + SessionContract.Columns.ID + " = ?";
                        c = db.rawQuery(updateSql, new String[]{DateUtils.toIsoUtcWithMillis(sessionData.getTimestamp()), openId});

                        if(c.getCount() > 0) {
                            Log.d(AppAmbit.class.getSimpleName(), "SESSION START UPDATE - " + sessionData.getId());
                        }
                    }

                    ContentValues cv = new ContentValues();

                    cv.put(SessionContract.Columns.ID, sessionData.getId().toString());
                    cv.put(SessionContract.Columns.SESSION_ID, sessionData.getSessionId());
                    cv.put(SessionContract.Columns.START_SESSION_DATE, DateUtils.toIsoUtcWithMillis(sessionData.getTimestamp()));
                    cv.putNull(SessionContract.Columns.END_SESSION_DATE);

                    db.insert(
                            SessionContract.TABLE_NAME,
                            null,
                            cv
                    );
                    Log.d(AppAmbit.class.getSimpleName(), "SESSION START CREATE - " + sessionData.getId());
                    break;
                case END:
                    String selectSql = "SELECT " + SessionContract.Columns.ID +
                            " FROM " + SessionContract.TABLE_NAME +
                            " WHERE " + SessionContract.Columns.END_SESSION_DATE + " IS NULL " +
                            " ORDER BY " + SessionContract.Columns.START_SESSION_DATE + " DESC " +
                            " LIMIT 1";
                    SQLiteDatabase db2 = dataStoreTest.getReadableDatabase();
                    c = db2.rawQuery(selectSql, null);

                    String querySessionId = "";
                    if (c.moveToFirst()) {
                        querySessionId = c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.ID));
                    }

                    c.close();
                    c = null;

                    if (!querySessionId.isEmpty()) {
                        String updateSql = "UPDATE " + SessionContract.TABLE_NAME +
                                " SET " + SessionContract.Columns.END_SESSION_DATE + " = ? " +
                                " WHERE " + SessionContract.Columns.ID + " = ?";
                        c = db2.rawQuery(updateSql, new String[]{DateUtils.toIsoUtcWithMillis(sessionData.getTimestamp()), querySessionId});

                        if (c.getCount() > 0) {
                            Log.d(AppAmbit.class.getSimpleName(), "SESSION END UPDATE - " + sessionData.getSessionId());
                        } else {
                            Log.d(AppAmbit.class.getSimpleName(), "No session found to update for session ID");
                        }

                    } else {
                        ContentValues cv2 = new ContentValues();
                        cv2.put(SessionContract.Columns.ID, sessionData.getId().toString());
                        cv2.put(SessionContract.Columns.SESSION_ID, sessionData.getSessionId());
                        cv2.putNull(SessionContract.Columns.START_SESSION_DATE);
                        cv2.put(SessionContract.Columns.END_SESSION_DATE, DateUtils.toIsoUtcWithMillis(sessionData.getTimestamp()));

                        try {
                            db2.insert(
                                    SessionContract.TABLE_NAME,
                                    null,
                                    cv2
                            );
                        }catch (SQLiteConstraintException e) {
                            Log.d(AppAmbit.class.getSimpleName(), "Session duplicate avoided - " + sessionData.getSessionId());
                        }catch (Exception e) {
                            Log.d(AppAmbit.class.getSimpleName(), "Error inserting end session", e);
                        }

                        Log.d(AppAmbit.class.getSimpleName(), "SESSION END CREATE - " + sessionData.getSessionId());
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error inserting log session", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void updateLogSessionId(String sessionId) {
        SQLiteDatabase db = dataStoreTest.getWritableDatabase();
        Cursor c = null;
//        String openSessionId;
//
//        String sqlOpenSession = "SELECT " + SessionContract.Columns.ID +
//                " FROM " + SessionContract.TABLE_NAME +
//                " WHERE " + SessionContract.Columns.END_SESSION_DATE + " IS NULL" +
//                " ORDER BY " + SessionContract.Columns.START_SESSION_DATE + " DESC" +
//                " LIMIT 1";
//
//        try {
//            c = db.rawQuery(sqlOpenSession, null);
//            if (c.moveToFirst()) {
//                openSessionId = c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.ID));
//            } else {
//                Log.d(AppAmbit.class.getSimpleName(), "No open session found to update log session ID");
//                return;
//            }
//        } catch (Exception e) {
//            Log.e(AppAmbit.class.getSimpleName(), "Error querying open session", e);
//            return;
//        } finally {
//            if (c != null) c.close();
//        }
//
//        if (openSessionId == null) {
//            Log.d(AppAmbit.class.getSimpleName(), "No sessionId found");
//            return;
//        }

        String updateSql =
            "UPDATE " + LogEntityContract.TABLE_NAME +
            " SET " + LogEntityContract.Columns.SESSION_ID + " = ?" +
            " WHERE " + LogEntityContract.Columns.ID + " = (" +
                "SELECT " + LogEntityContract.Columns.ID +
                " FROM " + LogEntityContract.TABLE_NAME +
                " ORDER BY " + LogEntityContract.Columns.CREATED_AT + " DESC" +
                " LIMIT 1" +
            ")";

        try {
            db.execSQL(updateSql, new Object[]{sessionId});

            long affected = DatabaseUtils.longForQuery(db, "SELECT changes()", null);
            if (affected > 0) {
                Log.d(AppAmbit.class.getSimpleName(), "LOG UPDATE OK → last log linked to session " + sessionId);
            } else {
                Log.d(AppAmbit.class.getSimpleName(), "No log updated (table empty?)");
            }
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error updating session ID", e);
        }
    }

    public void updateEventSessionId(String sessionId) {
        SQLiteDatabase db = dataStoreTest.getReadableDatabase();

        String updateSql =
            "UPDATE " + EventEntityContract.TABLE_NAME +
            " SET " + EventEntityContract.Columns.SESSION_ID + " = ?" +
            " WHERE " + EventEntityContract.Columns.ID + " = (" +
                "SELECT " + EventEntityContract.Columns.ID +
                " FROM " + EventEntityContract.TABLE_NAME +
                " ORDER BY " + EventEntityContract.Columns.CREATED_AT + " DESC" +
                " LIMIT 1" +
            ")";

        try {
            db.execSQL(updateSql, new Object[]{sessionId});

            long affected = DatabaseUtils.longForQuery(db, "SELECT changes()", null);
            if (affected > 0) {
                Log.d(AppAmbit.class.getSimpleName(), "LOG UPDATE OK → last event linked to session " + sessionId);
            } else {
                Log.d(AppAmbit.class.getSimpleName(), "No event updated (table empty?)");
            }
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error updating session ID", e);
        }
    }

    public void updateAllEventsWithSessionId(String sessionId) {
        SQLiteDatabase db = dataStoreTest.getWritableDatabase();

        String updateSql =
            "UPDATE " + EventEntityContract.TABLE_NAME +
            " SET " + EventEntityContract.Columns.SESSION_ID + " = ? ";

        try {
            db.execSQL(updateSql, new Object[]{sessionId});

            long affected = DatabaseUtils.longForQuery(db, "SELECT changes()", null);
            if (affected > 0) {
                Log.d(AppAmbit.class.getSimpleName(), "LOG UPDATE OK → " + affected + " logs linked to session " + sessionId);
            } else {
                Log.d(AppAmbit.class.getSimpleName(), "No log updated (table empty?)");
            }
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error updating session ID", e);
        }
    }

    @Override
    public void close() throws IOException {}
}