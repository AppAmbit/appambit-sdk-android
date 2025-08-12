package com.appambit.sdk.services;

import static com.appambit.sdk.services.storage.contract.AppSecretContract.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.enums.LogType;
import com.appambit.sdk.models.analytics.EventEntity;
import com.appambit.sdk.models.logs.LogEntity;
import com.appambit.sdk.services.storage.DataStore;
import com.appambit.sdk.services.storage.contract.AppSecretContract;
import com.appambit.sdk.services.storage.contract.EventEntityContract;
import com.appambit.sdk.services.storage.contract.LogEntityContract;
import com.appambit.sdk.services.interfaces.Storable;

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
                    new String[]{ AppSecretContract.Columns.DEVICE_ID},
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
                    new String[]{ AppSecretContract.Columns.APP_ID},
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
                    new String[]{ AppSecretContract.Columns.USER_ID},
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
                    new String[]{ AppSecretContract.Columns.USER_EMAIL},
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
        }finally {
            // Ensure the cursor is closed to prevent memory leaks
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
                    new String[]{ AppSecretContract.Columns.CONSUMER_ID},
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
        }finally {
            // Ensure the cursor is closed to prevent memory leaks
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
                    new String[]{ AppSecretContract.Columns.SESSION_ID},
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
        }catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error inserting log event", e);
        }
    }

    @Override
    public void putLogAnalyticsEvent(EventEntity eventEntity) {
        try {
            SQLiteDatabase db = dataStore.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put(EventEntityContract.Columns.ID, eventEntity.getId().toString());
            cv.put(EventEntityContract.Columns.NAME, eventEntity.getName());
            cv.put(EventEntityContract.Columns.DATA_JSON, eventEntity.getDataJson());
            cv.put(EventEntityContract.Columns.CREATED_AT, eventEntity.getCreatedAt().getTime());

            db.insert(
                    EventEntityContract.TABLE_NAME,
                    null,
                    cv
            );
            Log.d(AppAmbit.class.getSimpleName(), "EVENT CREATE - " + eventEntity.getId());
        }catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error inserting event entity", e);
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
        }catch (Exception e) {
            Log.e(AppAmbit.class.getSimpleName(), "Error deleting log list", e);
        }
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
                    logs.add(log);
                } while (c.moveToNext());
            }
        } finally {
            if(c != null) {
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
                    event.setCreatedAt(new Date(c.getLong(c.getColumnIndexOrThrow(LogEntityContract.Columns.CREATED_AT))));
                    event.setDataJson(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.DATA_JSON)));
                    event.setName(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.NAME)));
                    events.add(event);
                } while (c.moveToNext());
            }
        }finally {
            // Ensure the cursor is closed to prevent memory leaks
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

    @Override
    public void close() throws IOException {

    }
}
