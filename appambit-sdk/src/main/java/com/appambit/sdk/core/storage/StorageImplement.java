package com.appambit.sdk.core.storage;

import static com.appambit.sdk.core.storage.contract.AppSecretContract.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.appambit.sdk.core.AppAmbit;
import com.appambit.sdk.core.enums.LogType;
import com.appambit.sdk.core.models.analytics.EventEntity;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.storage.contract.AppSecretContract;
import com.appambit.sdk.core.storage.contract.EventEntityContract;
import com.appambit.sdk.core.storage.contract.LogEntityContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class StorageImplement implements StorageService {
    private final StorageProvider storageProvider;

    public StorageImplement(Context context) {
        storageProvider = new StorageProvider(context);
    }


    @Override
    public void putDeviceId(String deviceId) {
        try (SQLiteDatabase db = storageProvider.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.DEVICE_ID, deviceId);

            try (Cursor cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                    db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
                } else {
                    cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                    db.insert(AppSecretContract.TABLE_NAME, null, cv);
                }
            }
        }
    }

    @Override
    public String getDeviceId() {
        String deviceId = "";
        try (
                SQLiteDatabase db = storageProvider.getReadableDatabase();
                Cursor c = db.query(
                        AppSecretContract.TABLE_NAME,
                        new String[]{ AppSecretContract.Columns.DEVICE_ID},
                        null, null,
                        null, null,
                        null,
                        "1"
                )
        ) {
            if (c.moveToFirst()) {
                deviceId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.DEVICE_ID)
                );
            }
        }
        return deviceId;
    }

    @Override
    public void putAppId(String appId) {
        try (SQLiteDatabase db = storageProvider.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.APP_ID, appId);

            try (Cursor cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                    db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
                } else {
                    cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                    db.insert(AppSecretContract.TABLE_NAME, null, cv);
                }
            }
        }
    }

    @Override
    public String getAppId() {
        String appId = "";
        try (
                SQLiteDatabase db = storageProvider.getReadableDatabase();
                Cursor c = db.query(
                        AppSecretContract.TABLE_NAME,
                        new String[]{ AppSecretContract.Columns.APP_ID},
                        null, null,
                        null, null,
                        null,
                        "1"
                )
        ) {
            if (c.moveToFirst()) {
                appId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.APP_ID)
                );
            }
        }
        return appId;
    }

    @Override
    public void putUserId(String userId) {
        try (SQLiteDatabase db = storageProvider.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(Columns.USER_ID, userId);

            try (Cursor cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                    db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
                } else {
                    cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                    db.insert(AppSecretContract.TABLE_NAME, null, cv);
                }
            }
        }
    }

    @Override
    public String getUserId() {
        String userId = "";
        try (
                SQLiteDatabase db = storageProvider.getReadableDatabase();
                Cursor c = db.query(
                        AppSecretContract.TABLE_NAME,
                        new String[]{ AppSecretContract.Columns.USER_ID},
                        null, null,
                        null, null,
                        null,
                        "1"
                )
        ) {
            if (c.moveToFirst()) {
                userId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.USER_ID)
                );
            }
        }
        return userId;
    }

    @Override
    public void putUserEmail(String email) {
        try (SQLiteDatabase db = storageProvider.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.USER_EMAIL, email);

            try (Cursor cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                    db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
                } else {
                    cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                    db.insert(AppSecretContract.TABLE_NAME, null, cv);
                }
            }
        }
    }

    @Override
    public String getUserEmail() {
        String email = "";
        try (
                SQLiteDatabase db = storageProvider.getReadableDatabase();
                Cursor c = db.query(
                        AppSecretContract.TABLE_NAME,
                        new String[]{ AppSecretContract.Columns.USER_EMAIL},
                        null, null,
                        null, null,
                        null,
                        "1"
                )
        ) {
            if (c.moveToFirst()) {
                email = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.USER_EMAIL)
                );
            }
        }
        return email;
    }

    @Override
    public void putSessionId(String sessionId) {
        try (SQLiteDatabase db = storageProvider.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(AppSecretContract.Columns.SESSION_ID, sessionId);

            try (Cursor cursor = db.query(AppSecretContract.TABLE_NAME, new String[]{AppSecretContract.Columns.ID}, null, null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    String existingId = cursor.getString(cursor.getColumnIndexOrThrow(AppSecretContract.Columns.ID));
                    db.update(AppSecretContract.TABLE_NAME, cv, AppSecretContract.Columns.ID + " = ?", new String[]{existingId});
                } else {
                    cv.put(AppSecretContract.Columns.ID, UUID.randomUUID().toString());
                    db.insert(AppSecretContract.TABLE_NAME, null, cv);
                }
            }
        }
    }

    @Override
    public String getSessionId() {
        String sessionId = "";
        try (
                SQLiteDatabase db = storageProvider.getReadableDatabase();
                Cursor c = db.query(
                        AppSecretContract.TABLE_NAME,
                        new String[]{ AppSecretContract.Columns.SESSION_ID},
                        null, null,
                        null, null,
                        null,
                        "1"
                )
        ) {
            if (c.moveToFirst()) {
                sessionId = c.getString(
                        c.getColumnIndexOrThrow(AppSecretContract.Columns.SESSION_ID)
                );
            }
        }
        return sessionId;
    }

    @Override
    public void putLogEvent(LogEntity logEntity) {
        try (SQLiteDatabase db = storageProvider.getWritableDatabase()) {
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
        }
    }

    @Override
    public void putLogAnalyticsEvent(EventEntity eventEntity) {
        try (SQLiteDatabase db = storageProvider.getWritableDatabase()) {
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

        try (SQLiteDatabase db = storageProvider.getReadableDatabase()) {
            db.delete(
                    LogEntityContract.TABLE_NAME,
                    where.toString(),
                    args
            );
        }
    }

    @Override
    public List<LogEntity> getOldest100Logs() {
        List<LogEntity> logs = new ArrayList<>();
        SQLiteDatabase db = storageProvider.getReadableDatabase();

        String sql = "SELECT * FROM " + LogEntityContract.TABLE_NAME + " " +
                "ORDER BY " + LogEntityContract.Columns.CREATED_AT + " ASC " +
                "LIMIT 100";
        Cursor c = db.rawQuery(sql, null);

        try {
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
                    log.setCreatedAt(new Date(c.getInt(c.getColumnIndexOrThrow(LogEntityContract.Columns.CREATED_AT))));
                    logs.add(log);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return logs;
    }

    @Override
    public List<EventEntity> getOldest100Events() {
        List<EventEntity> events = new ArrayList<>();
        SQLiteDatabase db = storageProvider.getReadableDatabase();

        String sql = "SELECT * FROM " + EventEntityContract.TABLE_NAME + " " +
                "ORDER BY " + EventEntityContract.Columns.CREATED_AT + " ASC " +
                "LIMIT 100";
        Cursor c = db.rawQuery(sql, null);

        try {
            if (c.moveToFirst()) {
                do {
                    EventEntity event = new EventEntity();
                    event.setId(UUID.fromString(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.ID))));
                    event.setCreatedAt(new Date(c.getInt(c.getColumnIndexOrThrow(LogEntityContract.Columns.CREATED_AT))));
                    event.setDataJson(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.DATA_JSON)));
                    event.setName(c.getString(c.getColumnIndexOrThrow(EventEntityContract.Columns.NAME)));
                    events.add(event);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
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

        try (SQLiteDatabase db = storageProvider.getReadableDatabase()) {
            db.delete(
                    EventEntityContract.TABLE_NAME,
                    where.toString(),
                    args
            );
        }
    }

    @Override
    public void close() throws IOException {

    }
}
