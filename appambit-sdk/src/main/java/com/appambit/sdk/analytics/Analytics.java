package com.appambit.sdk.analytics;

import android.util.Log;

import com.appambit.sdk.core.AppAmbit;
import com.appambit.sdk.core.enums.LogType;
import com.appambit.sdk.core.models.analytics.EventEntity;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.storage.Storable;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public final class Analytics {

    private static Storable mStorable;
    private static ExecutorService mExecutorService;
    private static boolean isManualSessionEnabled = false;

    public static void Initialize(Storable storable, ExecutorService executorService) {
        mStorable = storable;
        mExecutorService = executorService;
    }


    public static void generateSampleLogsEvents() {
        mExecutorService.execute(() -> {
            loadLogs();
            loadEvents();
            loadAppSecrets();
        });

    }

    private static void loadAppSecrets() {

        mStorable.putAppId("API_KEY-" + UUID.randomUUID().toString());
        Log.d(Analytics.class.getSimpleName(), "APPSECRETS --> " + mStorable.getAppId());

        mStorable.putDeviceId("DEVICE_ID-" + UUID.randomUUID().toString());
        Log.d(Analytics.class.getSimpleName(),  "APPSECRETS --> " + mStorable.getDeviceId());

        mStorable.putUserId("USER_ID-" + UUID.randomUUID().toString());
        Log.d(Analytics.class.getSimpleName(),  "APPSECRETS --> " + mStorable.getUserId());

        mStorable.putUserEmail("example@mail.com");
        Log.d(Analytics.class.getSimpleName(),  "APPSECRETS --> " + mStorable.getUserEmail());

        mStorable.putSessionId("1234");
        Log.d(Analytics.class.getSimpleName(),  "APPSECRETS --> " + mStorable.getSessionId());
    }

    private static void loadEvents() {
        Random random = new Random();

        Calendar calendar = Calendar.getInstance();
        Date todayBase = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date yesterdayBase = calendar.getTime();
        Map<String, String> dataEvent = new HashMap<>();
        dataEvent.put("eventProperty1", "Value Event 1");
        dataEvent.put("eventProperty2", "Value Event 2");

        for (int x = 0; x < 200; x++) {

            EventEntity eventEntity = new EventEntity();

            Calendar date = Calendar.getInstance();
            date.setTime(x < 100 ? yesterdayBase : todayBase);

            date.set(Calendar.HOUR_OF_DAY, random.nextInt(24)); // 0–23
            date.set(Calendar.MINUTE, random.nextInt(60));      // 0–59
            date.set(Calendar.SECOND, random.nextInt(60));      // 0–59
            date.set(Calendar.MILLISECOND, 0);


            eventEntity.setId( UUID.randomUUID());
            eventEntity.setData(dataEvent);
            eventEntity.setName("NAME FOR EVENT: " + x);
            eventEntity.setCreatedAt(date.getTime());

            mStorable.putLogAnalyticsEvent(eventEntity);
        }
    }

    private static void loadLogs() {
        Random random = new Random();

        Calendar calendar = Calendar.getInstance();
        Date todayBase = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date yesterdayBase = calendar.getTime();
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("property1", "Value 1");
        contextMap.put("property2", "Value 2");

        for (int x = 0; x < 200; x++) {
            LogEntity logEntity = new LogEntity();
            logEntity.setId(UUID.randomUUID());
            logEntity.setAppVersion("1.2.1");
            logEntity.setClassFQN(AppAmbit.class.getSimpleName());
            logEntity.setFileName("PATH FILE NAME " + x);
            logEntity.setLineNumber(200);
            logEntity.setMessage("MESSAGE DATA " + x);
            logEntity.setStackTrace("STACK TRACE DATA " + x);
            logEntity.setContext(contextMap);
            logEntity.setType(LogType.CRASH);
            logEntity.setFile("CONTENT FILE " + x);

            Calendar date = Calendar.getInstance();
            date.setTime(x < 100 ? yesterdayBase : todayBase);

            date.set(Calendar.HOUR_OF_DAY, random.nextInt(24)); // 0–23
            date.set(Calendar.MINUTE, random.nextInt(60));      // 0–59
            date.set(Calendar.SECOND, random.nextInt(60));      // 0–59
            date.set(Calendar.MILLISECOND, 0);

            logEntity.setCreatedAt(date.getTime());

            mStorable.putLogEvent(logEntity);
        }
    }



    public static void sendBatchesEvents() {
        mExecutorService.execute(() -> {
            try {
                List<EventEntity> events = mStorable.getOldest100Events();
                if (!events.isEmpty()) {
                    mStorable.deleteEventList(events);
                }
            } catch (Exception ex) {
                Log.e(Analytics.class.getSimpleName(), "Error to process Events", ex);
            }
        });
    }

    public static void sendBatchesLogs() {
        mExecutorService.execute(() -> {
            try {

                List<LogEntity> logs = mStorable.getOldest100Logs();
                if (!logs.isEmpty()) {
                    mStorable.deleteLogList(logs);
                }
            } catch (Exception ex) {
                Log.e(Analytics.class.getSimpleName(), "Error to process Logs", ex);
            }
        });
    }

    public static  void startSession() {
        SessionManager.startSession();
    }

    public static void endSession() {
        SessionManager.endSession();
    }


    public static void enableManualSession() {
        isManualSessionEnabled = true;
    }

    public static boolean isManualSessionEnabled() {
        return isManualSessionEnabled;
    }
}
