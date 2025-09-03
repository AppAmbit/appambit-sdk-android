package com.appambit.sdk.services;

import static com.appambit.sdk.services.storage.contract.AppSecretContract.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.enums.LogType;
import com.appambit.sdk.enums.SessionType;
import com.appambit.sdk.models.analytics.EventEntity;
import com.appambit.sdk.models.analytics.SessionBatch;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.models.logs.LogEntity;
import com.appambit.sdk.services.storage.DataStore;
import com.appambit.sdk.services.storage.contract.AppSecretContract;
import com.appambit.sdk.services.storage.contract.EventEntityContract;
import com.appambit.sdk.services.storage.contract.LogEntityContract;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.services.storage.contract.SessionContract;
import com.appambit.sdk.utils.DateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class StorageService implements Storable {
    private final DataStore dataStore;

    public StorageService(Context context) {
        dataStore = new DataStore(context);
    }

    @Override
    public void putDeviceId(String deviceId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.DEVICE_ID, deviceId);

            cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
            } else {
                cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                db.insert(AppSecretContract.TABLE_NAME, null, cv);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String getDeviceId() {
        String deviceId = "";
        Cursor c = null;
        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            c = db.query(
                    AppSecretContract.TABLE_NAME,
                    new String[]{AppSecretContract.Columns.DEVICE_ID},
                    null, null,
                    null, null,
                    null,
                    "1"
            );
            if (c.moveToFirst()) {
                deviceId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.DEVICE_ID)
                );
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return deviceId;
    }

    @Override
    public void putAppId(String appId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.APP_ID, appId);

            cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
            } else {
                cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                db.insert(AppSecretContract.TABLE_NAME, null, cv);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String getAppId() {
        String appId = "";
        Cursor c = null;
        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            c = db.query(
                    AppSecretContract.TABLE_NAME,
                    new String[]{AppSecretContract.Columns.APP_ID},
                    null, null,
                    null, null,
                    null,
                    "1"
            );
            if (c.moveToFirst()) {
                appId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.APP_ID)
                );
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return appId;
    }

    @Override
    public void putUserId(String userId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(Columns.USER_ID, userId);

            cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
            } else {
                cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                db.insert(AppSecretContract.TABLE_NAME, null, cv);
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String getUserId() {
        String userId = "";
        Cursor c = null;
        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            c = db.query(
                    AppSecretContract.TABLE_NAME,
                    new String[]{AppSecretContract.Columns.USER_ID},
                    null, null,
                    null, null,
                    null,
                    "1"
            );
            if (c.moveToFirst()) {
                userId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.USER_ID)
                );
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return userId;
    }

    @Override
    public void putUserEmail(String email) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.USER_EMAIL, email);

            cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
            } else {
                cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                db.insert(AppSecretContract.TABLE_NAME, null, cv);
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String getUserEmail() {
        String email = "";
        Cursor c = null;
        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            c = db.query(
                    AppSecretContract.TABLE_NAME,
                    new String[]{AppSecretContract.Columns.USER_EMAIL},
                    null, null,
                    null, null,
                    null,
                    "1"
            );
            if (c.moveToFirst()) {
                email = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.USER_EMAIL)
                );
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return email;
    }

    @Override
    public void putConsumerId(String consumerId) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.CONSUMER_ID, consumerId);

            cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
            } else {
                cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                db.insert(AppSecretContract.TABLE_NAME, null, cv);
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String getConsumerId() {
        String consumerId = "";
        Cursor c = null;
        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            c = db.query(
                    AppSecretContract.TABLE_NAME,
                    new String[]{AppSecretContract.Columns.CONSUMER_ID},
                    null, null,
                    null, null,
                    null,
                    "1"
            );
            if (c.moveToFirst()) {
                consumerId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.CONSUMER_ID)
                );
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return consumerId;
    }

    @Override
    public void putSessionId(String sessionId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.SESSION_ID, sessionId);

            cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
            } else {
                cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                db.insert(AppSecretContract.TABLE_NAME, null, cv);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String getSessionId() {
        String sessionId = "";
        Cursor c = null;
        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            c = db.query(
                    AppSecretContract.TABLE_NAME,
                    new String[]{AppSecretContract.Columns.SESSION_ID},
                    null, null,
                    null, null,
                    null,
                    "1"
            );
            if (c.moveToFirst()) {
                sessionId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.SESSION_ID)
                );
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return sessionId;
    }

    @Override
    public void putLogEvent(LogEntity logEntity) {
        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put(LogEntityContract.Columns.ID, logEntity.getId().toString());
            cv.put(LogEntityContract.Columns.SESSION_ID, logEntity.getSessionId());
            cv.put(LogEntityContract.Columns.CREATED_AT, logEntity.getCreatedAt().getTime());
            cv.put(LogEntityContract.Columns.APP_VERSION, logEntity.getAppVersion());
            cv.put(LogEntityContract.Columns.CLASS_FQN, logEntity.getClassFQN());
            cv.put(LogEntityContract.Columns.FILE_NAME, logEntity.getFileName());
            cv.put(LogEntityContract.Columns.LINE_NUMBER, logEntity.getLineNumber());
            cv.put(LogEntityContract.Columns.MESSAGE, logEntity.getMessage());
            cv.put(LogEntityContract.Columns.STACK_TRACE, logEntity.getStackTrace());
            cv.put(LogEntityContract.Columns.CONTEXT_JSON, logEntity.getContextJson());
            cv.put(LogEntityContract.Columns.TYPE, logEntity.getType().getValue());
            cv.put(LogEntityContract.Columns.FILE, logEntity.getFile());

            db.insert(
                    LogEntityContract.TABLE_NAME,
                    null,
                    cv
            );
            Log.d(AppAmbit.class.getSimpleName(), "LOG CREATE - " + logEntity.getId());
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error inserting log event", e);
        }
    }

    @Override
    public void putLogAnalyticsEvent(EventEntity eventEntity) {
        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put(EventEntityContract.Columns.ID, eventEntity.getId().toString());
            cv.put(EventEntityContract.Columns.SESSION_ID, eventEntity.getSessionId());
            cv.put(EventEntityContract.Columns.NAME, eventEntity.getName());
            cv.put(EventEntityContract.Columns.DATA_JSON, eventEntity.getDataJson());
            cv.put(EventEntityContract.Columns.CREATED_AT, eventEntity.getCreatedAt().getTime());

            db.insert(
                    EventEntityContract.TABLE_NAME,
                    null,
                    cv
            );
            Log.d(AppAmbit.class.getSimpleName(), "EVENT CREATE - " + eventEntity.getId());
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error inserting event entity", e);
        }
    }

    @Override
    public void putSessionData(SessionData sessionData) {
        Cursor c = null;
        try {
            switch (sessionData.getSessionType()) {
                case START:
                    SQLiteDatabase db = dataStore.getReadableDatabase();

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
                    SQLiteDatabase db2 = dataStore.getReadableDatabase();
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

    @Override
    public void deleteLogList(List<LogEntity> logs) {
        if (logs == null || logs.isEmpty()) return;

        StringBuilder where = new StringBuilder();
        where.append(LogEntityContract.Columns.ID)
                .append(" IN (");
        String[] args = new String[logs.size()];
        for (int i = 0; i < logs.size(); i++) {
            args[i] = logs.get(i).getId().toString();
            where.append("?");
            if (i < logs.size() - 1) {
                where.append(",");
            }
        }
        where.append(")");

        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            db.delete(
                    LogEntityContract.TABLE_NAME,
                    where.toString(),
                    args
            );
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error deleting log list", e);
        }
    }

    public List<SessionBatch> getOldest100Session() {
        List<SessionBatch> sessionDataList = new ArrayList<>();
        SQLiteDatabase db = dataStore.getReadableDatabase();

        String selectSessionsSql =
            "SELECT " +
                SessionContract.Columns.ID                 + ", " +
                SessionContract.Columns.SESSION_ID         + ", " +
                SessionContract.Columns.START_SESSION_DATE + ", " +
                SessionContract.Columns.END_SESSION_DATE   + " "  +
            "FROM " + SessionContract.TABLE_NAME + " " +
            "WHERE " + SessionContract.Columns.START_SESSION_DATE + " IS NOT NULL " +
            "AND " + SessionContract.Columns.END_SESSION_DATE   + " IS NOT NULL " +
            "ORDER BY " + SessionContract.Columns.START_SESSION_DATE + " DESC " +
            "LIMIT 100";
        Cursor c = null;

        try {
            c = db.rawQuery(selectSessionsSql, null);
            if (c.moveToFirst()) {
                do {
                    SessionBatch sessionBatch = new SessionBatch();
                    sessionBatch.setId(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.ID)));
                    sessionBatch.setSessionId(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.SESSION_ID)));
                    sessionBatch.setStartedAt(DateUtils.fromIsoUtc(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.START_SESSION_DATE))));
                    sessionBatch.setEndedAt(DateUtils.fromIsoUtc(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.END_SESSION_DATE))));
                    sessionDataList.add(sessionBatch);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error fetching oldest 100 sessions", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return sessionDataList;
    }

    public List<SessionData> getSessionsEnd() {
        String sqlUnpairedSessions =
            "SELECT " +
                SessionContract.Columns.ID                 + ", " +
                SessionContract.Columns.SESSION_ID         + ", " +
                SessionContract.Columns.START_SESSION_DATE + ", " +
                SessionContract.Columns.END_SESSION_DATE   + " "  +
            "FROM " + SessionContract.TABLE_NAME + " " +
            "WHERE " + SessionContract.Columns.START_SESSION_DATE + " IS NULL " +
            "AND " + SessionContract.Columns.END_SESSION_DATE   + " IS NOT NULL " +
            "ORDER BY " + SessionContract.Columns.END_SESSION_DATE + " ASC";

        List<SessionData> results = new ArrayList<>();
        Cursor c = null;
        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            c = db.rawQuery(sqlUnpairedSessions, null);
            if (c.moveToFirst()) {
                do {
                    SessionData sessionData = new SessionData();
                    String startedAt = c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.START_SESSION_DATE));
                    String endedAt = c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.END_SESSION_DATE));

                    sessionData.setId(UUID.fromString(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.ID))));
                    sessionData.setSessionId(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.SESSION_ID)));

                    if(startedAt != null) {
                        sessionData.setSessionType(SessionType.START);
                        sessionData.setTimestamp(DateUtils.fromIsoUtc(startedAt));
                        results.add(sessionData);
                    }else if(endedAt != null) {
                        sessionData.setSessionType(SessionType.END);
                        sessionData.setTimestamp(DateUtils.fromIsoUtc(endedAt));
                        results.add(sessionData);
                    }

                }while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error fetching unpaired sessions", e);
        }finally {
            if(c != null) {
                c.close();
            }
        }
        return results;
    }

    public boolean isSessionOpen() {
        SQLiteDatabase db = dataStore.getReadableDatabase();
        Cursor c = null;
        try {
            String sql = "SELECT id FROM " + SessionContract.TABLE_NAME +
                    " WHERE " + SessionContract.Columns.END_SESSION_DATE + " IS NULL" +
                    " AND " + SessionContract.Columns.START_SESSION_DATE + " IS NOT NULL" +
                    " ORDER BY " + SessionContract.Columns.START_SESSION_DATE + " DESC" +
                    " LIMIT 1";

            c = db.rawQuery(sql, null);

            return c.moveToFirst();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public SessionData getLastStartSession() {
        SQLiteDatabase db = dataStore.getReadableDatabase();
        Cursor c = null;
        try {
            String sql = "SELECT " + SessionContract.Columns.ID + ", " +
                    SessionContract.Columns.START_SESSION_DATE +
                    " FROM " + SessionContract.TABLE_NAME +
                    " WHERE " + SessionContract.Columns.END_SESSION_DATE + " IS NULL" +
                    " AND " + SessionContract.Columns.START_SESSION_DATE + " IS NOT NULL" +
                    " ORDER BY " + SessionContract.Columns.START_SESSION_DATE + " DESC" +
                    " LIMIT 1";

            c = db.rawQuery(sql, null);

            if (c.moveToFirst()) {
                SessionData sessionData = new SessionData();
                sessionData.setId(UUID.fromString(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.ID))));
                sessionData.setTimestamp(DateUtils.fromIsoUtc(c.getString(c.getColumnIndexOrThrow(SessionContract.Columns.START_SESSION_DATE))));
                sessionData.setSessionType(SessionType.START);
                return sessionData;
            }
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error fetching last start session ID", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    public void updateLogsAndEventsId(String localId, String remoteId) {
        SQLiteDatabase db = dataStore.getReadableDatabase();

        String updateLogsSql = "UPDATE " + LogEntityContract.TABLE_NAME + " SET " +
                LogEntityContract.Columns.SESSION_ID + " = ? WHERE " +
                LogEntityContract.Columns.SESSION_ID + " = ?";

        String updateEventsSql = "UPDATE " + EventEntityContract.TABLE_NAME + " SET " +
                EventEntityContract.Columns.SESSION_ID + " = ? WHERE " +
                EventEntityContract.Columns.SESSION_ID + " = ?";

        try {
            db.execSQL(updateLogsSql, new String[]{remoteId, localId});
            db.execSQL(updateEventsSql, new String[]{remoteId, localId});
        }catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error updating logs and events with new session ID", e);
            return;
        }

        Log.d(AppAmbit.class.getSimpleName(), "Updated logs and events with new session ID: " + remoteId);
    }

    @Override
    public List<LogEntity> getOldest100Logs() {
        List<LogEntity> logs = new ArrayList<>();
        SQLiteDatabase db = dataStore.getReadableDatabase();

        String sql = "SELECT * FROM " + LogEntityContract.TABLE_NAME + " " +
                "ORDER BY " + LogEntityContract.Columns.CREATED_AT + " ASC " +
                "LIMIT 100";
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                do {
                    LogEntity log = new LogEntity();
                    log.setId(UUID.fromString(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.ID))));
                    log.setSessionId(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.SESSION_ID)));
                    log.setAppVersion(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.APP_VERSION)));
                    log.setClassFQN(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.CLASS_FQN)));
                    log.setFileName(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.FILE_NAME)));
                    log.setLineNumber(c.getInt(c.getColumnIndexOrThrow(LogEntityContract.Columns.LINE_NUMBER)));
                    log.setMessage(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.MESSAGE)));
                    log.setStackTrace(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.STACK_TRACE)));
                    log.setContextJson(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.CONTEXT_JSON)));
                    log.setType(LogType.fromValue(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.TYPE))));
                    log.setFile(c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.FILE)));
                    log.setCreatedAt(new Date(c.getLong(c.getColumnIndexOrThrow(LogEntityContract.Columns.CREATED_AT))));

                    String sessionId = c.getString(c.getColumnIndexOrThrow(LogEntityContract.Columns.SESSION_ID));
                    if (sessionId == null || TextUtils.isDigitsOnly(sessionId)) {
                        logs.add(log);
                    }

                } while (c.moveToNext());
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return logs;
    }

    @Override
    public List<EventEntity> getOldest100Events() {
        List<EventEntity> events = new ArrayList<>();
        SQLiteDatabase db = dataStore.getReadableDatabase();

        String sql = "SELECT * FROM " + EventEntityContract.TABLE_NAME + " " +
                "ORDER BY " + EventEntityContract.Columns.CREATED_AT + " ASC " +
                "LIMIT 100";
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                do {
                    EventEntity event = new EventEntity();
                    event.setId(UUID.fromString(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.ID))));
                    event.setSessionId(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.SESSION_ID)));
                    event.setCreatedAt(new Date(c.getLong(c.getColumnIndexOrThrow(LogEntityContract.Columns.CREATED_AT))));
                    event.setDataJson(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.DATA_JSON)));
                    event.setName(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.NAME)));
                    if(TextUtils.isDigitsOnly(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.SESSION_ID)))) {
                        events.add(event);
                    }
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return events;
    }

    @Override
    public void deleteEventList(List<EventEntity> events) {
        if (events == null || events.isEmpty()) return;

        StringBuilder where = new StringBuilder();
        where.append(EventEntityContract.Columns.ID)
                .append(" IN (");
        String[] args = new String[events.size()];
        for (int i = 0; i < events.size(); i++) {
            args[i] = events.get(i).getId().toString();
            where.append("?");
            if (i < events.size() - 1) {
                where.append(",");
            }
        }
        where.append(")");

        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            db.delete(
                    EventEntityContract.TABLE_NAME,
                    where.toString(),
                    args
            );
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error deleting event list", e);
        }
    }

    public void deleteSessionList(List<SessionBatch> sessions) {
        if (sessions == null || sessions.isEmpty()) return;

        StringBuilder where = new StringBuilder();
        where.append(SessionContract.Columns.ID)
                .append(" IN (");
        String[] args = new String[sessions.size()];
        for (int i = 0; i < sessions.size(); i++) {
            args[i] = sessions.get(i).getId();
            where.append("?");
            if (i < sessions.size() - 1) {
                where.append(",");
            }
        }
        where.append(")");

        try {
            SQLiteDatabase db = dataStore.getReadableDatabase();
            db.delete(
                    SessionContract.TABLE_NAME,
                    where.toString(),
                    args
            );
            Log.d(AppAmbit.class.getSimpleName(), "Deleted sessions size: " + sessions.size());
        } catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error deleting session list", e);
        }
    }

    public void deleteSessionById(UUID sessionId) {
        SQLiteDatabase db = dataStore.getReadableDatabase();

        String sqlDeleteSession = "DELETE FROM " + SessionContract.TABLE_NAME +
                " WHERE " + SessionContract.Columns.ID + " = ?";

        try {
            db.execSQL(sqlDeleteSession, new String[]{sessionId.toString()});
            Log.d(AppAmbit.class.getSimpleName(), "Deleted session by id: " + sessionId);
        }catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error deleting session by id", e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
