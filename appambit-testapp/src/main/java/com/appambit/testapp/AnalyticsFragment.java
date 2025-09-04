package com.appambit.testapp;

import static com.appambit.sdk.utils.InternetConnection.hasInternetConnection;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.appambit.sdk.Analytics;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.enums.SessionType;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.Crashes;
import com.appambit.testapp.utils.AlertsUtils;
import com.appambit.testapp.utils.StorageServiceTest;
import com.appambit.testapp.utils.storage.StorableTest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class AnalyticsFragment extends Fragment {
    private static final String TAG = AnalyticsFragment.class.getSimpleName();
    Button btnStartSession, btnEndSession, btnGenerate30DaysTestSessions;
    Button btnClearToken, btnTokenRenew;
    Button btnEventWProperty, btnDefaultClickedEventWProperty, btnMax300LengthEvent, btnMax20PropertiesEvent, btn3DailyEvents, btn220BatchEvents, btnSecondActivity;
    static StorableTest storableApp;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        try {
            storableApp = new StorageServiceTest(requireContext());
        }catch (Exception e) {
            Log.e(TAG, "Error initializing storage", e);
        }

        btnStartSession = view.findViewById(R.id.btnStartSession);
        btnStartSession.setOnClickListener(v ->  {
            try {
                Analytics.startSession();
            }catch (Exception e) {
                Log.e(TAG, "Error during log creation: " + e.getMessage());
            }
        });

        btnEndSession = view.findViewById(R.id.btnEndSession);
        btnEndSession.setOnClickListener(v ->  {
            try {
                Analytics.endSession();
            }catch (Exception e) {
                Log.e(TAG, "Error during log creation: " + e.getMessage());
            }
        });

        btnGenerate30DaysTestSessions = view.findViewById(R.id.btnGenerate30DaysTestSessions);
        btnGenerate30DaysTestSessions.setOnClickListener(v -> onGenerate30DaysTestSessions(requireContext()));

        btnEventWProperty = view.findViewById(R.id.btnEventWProperty);
        btnEventWProperty.setOnClickListener(v ->  buttonOnClicked(requireContext()));
        btnDefaultClickedEventWProperty = view.findViewById(R.id.btnDefaultClickedEventWProperty);
        btnDefaultClickedEventWProperty.setOnClickListener(v ->  buttonOnClickedTestEvent(requireContext()));
        btnMax300LengthEvent = view.findViewById(R.id.btnMax300LengthEvent);
        btnMax300LengthEvent.setOnClickListener(v ->  buttonOnClickedTestLimitsEvent(requireContext()));
        btnMax20PropertiesEvent = view.findViewById(R.id.btnMax20PropertiesEvent);
        btnMax20PropertiesEvent.setOnClickListener(v ->  buttonOnClickedTestMaxPropertiesEvent(requireContext()));
        btn3DailyEvents = view.findViewById(R.id.btn3DailyEvents);
        btn3DailyEvents.setOnClickListener(v ->  onSend30DailyEvents(requireContext()));
        btn220BatchEvents = view.findViewById(R.id.btn220BatchEvents);
        btn220BatchEvents.setOnClickListener(v ->  onGenerateBatchEvents(requireContext()));
        btnClearToken = view.findViewById(R.id.btnClearToken);
        btnClearToken.setOnClickListener(v-> onClearToken());
        btnTokenRenew = view.findViewById(R.id.btnTokenRenew);
        btnTokenRenew.setOnClickListener(v -> onTokenRefreshTest(requireContext()));
        btnSecondActivity = view.findViewById(R.id.btnSecondActivity);
        btnSecondActivity.setOnClickListener(v -> changeToSecondActivity());
       return view;
    }

    public static void onGenerate30DaysTestSessions(Context context) {
        if (hasInternetConnection(context)) {
            AlertsUtils.showAlert(context, "Info", "Turn off internet and try again");
            return;
        }

        for (int index = 1; index <= 30; index++) {

            SessionData sessionData = new SessionData();
            UUID sessionId = UUID.randomUUID();

            sessionData.setId(sessionId);
            sessionData.setSessionType(SessionType.START);
            Date sessionDate = DateUtils.getDateDaysAgo(30 - index);
            sessionData.setTimestamp(sessionDate);

            try {
                storableApp.putSessionData(sessionData);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting start session", e);
                continue;
            }

            try {
                int finalIndex = index;
                Random random = new Random();
                long randomOffset = random.nextInt(60 * 60 * 1000);
                storableApp.putSessionData(new SessionData() {
                    {
                        setId(UUID.randomUUID());
                        setSessionType(SessionType.END);
                        setTimestamp(new Date(DateUtils.getDateDaysAgo(30 - finalIndex).getTime() + randomOffset));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error inserting end session", e);
            }

        }
        AlertsUtils.showAlert(context, "Info", "Turn off and Turn on internet to send the sessions.");
    }

    private static void buttonOnClicked(Context context) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("Count", "41");
            Analytics.trackEvent("ButtonClicked", map, null);
            Toast.makeText(context, "OnClick event generated", Toast.LENGTH_SHORT).show();
        }catch (Exception e) {
            Log.e(TAG, "Error during log creation: " + e.getMessage());
        }
    }

    private static void buttonOnClickedTestEvent(Context context) {
        Analytics.generateTestEvent();
        Toast.makeText(context, "Event generated", Toast.LENGTH_SHORT).show();
    }

    private static void buttonOnClickedTestLimitsEvent(Context context) {
        Map<String, String> properties = new HashMap<>();
        String characters300 = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        String characters302 = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

        properties.put(characters300, characters300);
        properties.put(characters302, characters302);

        Analytics.trackEvent(characters300, properties, null);
        Toast.makeText(context, "1 event generated", Toast.LENGTH_SHORT).show();

    }

    private static void buttonOnClickedTestMaxPropertiesEvent(Context context) {
        Map<String, String> properties = new HashMap<>();
        properties.put( "01", "01");
        properties.put( "02", "02");
        properties.put( "03", "03");
        properties.put( "04", "04");
        properties.put( "05", "05");
        properties.put( "06", "06");
        properties.put( "07", "07");
        properties.put( "08", "08");
        properties.put( "09", "09");
        properties.put( "10", "10");
        properties.put( "11", "11");
        properties.put( "12", "12");
        properties.put( "13", "13");
        properties.put( "14", "14");
        properties.put( "15", "15");
        properties.put( "16", "16");
        properties.put( "17", "17");
        properties.put( "18", "18");
        properties.put( "19", "19");
        properties.put( "20", "20");
        properties.put( "21", "21");
        properties.put( "22", "22");
        properties.put( "23", "23");
        properties.put( "24", "24");
        properties.put( "25", "25");//25

        Analytics.trackEvent("TestMaxProperties", properties, null);
        Toast.makeText(context, "1 event generated", Toast.LENGTH_SHORT).show();

    }

    private static void onSend30DailyEvents(Context context) {
        if (hasInternetConnection(context)) {
            AlertsUtils.showAlert(context, "Info", "Turn off internet and try again");
            return;
        }

        for (int index = 0; index < 30; index++) {

            SessionData sessionData = new SessionData();
            UUID sessionId = UUID.randomUUID();

            sessionData.setId(sessionId);
            sessionData.setSessionType(SessionType.START);
            Date eventDate = DateUtils.getDateDaysAgo(30 - index);
            sessionData.setTimestamp(eventDate);

            try {
                storableApp.putSessionData(sessionData);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting start session", e);
                continue;
            }

            Map<String, String> properties = new HashMap<>();
            properties.put("30 Daily events", "Event");

            Analytics.trackEvent("30 Daily events", properties, eventDate);

            try {
                Thread.sleep(100);
            }catch (Exception e) {
                Log.e(TAG, "Error during log creation: " + e.getMessage());
            }

            storableApp.updateEventSessionId(sessionId.toString());

            try {
                int finalIndex = index;
                Random random = new Random();
                long randomOffset = random.nextInt(60 * 60 * 1000);
                storableApp.putSessionData(new SessionData() {
                    {
                        setId(UUID.randomUUID());
                        setSessionType(SessionType.END);
                        setTimestamp(new Date(DateUtils.getDateDaysAgo(30 - finalIndex).getTime() + randomOffset));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error inserting end session", e);
            }

        }
        AlertsUtils.showAlert(context, "Info", "30 events generated, turn on internet to send them");
    }

    private static void onGenerateBatchEvents(Context context) {
        if (hasInternetConnection(context)) {
            AlertsUtils.showAlert(context, "Info", "Turn off internet and try again");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();

        SessionData sessionData = new SessionData();
        UUID sessionId = UUID.randomUUID();

        sessionData.setId(sessionId);
        sessionData.setSessionType(SessionType.START);
        sessionData.setTimestamp(DateUtils.getUtcNow());

        try {
            storableApp.putSessionData(sessionData);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting start session", e);
        }

        Map<String, String> properties = new HashMap<>();
        for (int index = 1; index <= 220; index++) {
            properties.put("property1", "value1" );
            Analytics.trackEvent("Events 220", properties, null);
        }

        try {
            Thread.sleep(1000);
        }catch (Exception e) {
            Log.e(TAG, "Error during log creation: " + e.getMessage());
        }

        executor.execute(() -> storableApp.updateAllEventsWithSessionId(sessionId.toString()));

        try {
            Random random = new Random();
            long randomOffset = random.nextInt(60 * 60 * 1000);
            storableApp.putSessionData(new SessionData() {
                {
                    setId(sessionId);
                    setSessionType(SessionType.END);
                    setTimestamp(new Date(DateUtils.getUtcNow().getTime() + randomOffset));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error inserting end session", e);
        }
        AlertsUtils.showAlert(context, "Info", "220 events generated, turn on internet to send them");
    }

    private  static void onClearToken() {
        Analytics.clearToken();
    }


    public void onTokenRefreshTest(Context context) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Analytics.clearToken();

        Map<String, String> properties = new HashMap<>();
        properties.put("user_id", "1");
        List<Future<?>> logTasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            logTasks.add(executor.submit(() -> {
                Crashes.LogError(context, "Sending 5 errors after an invalid token", properties, this.getClass().getName(), null, null, 0, DateUtils.getUtcNow());
            }));
        }

        waitAll(logTasks);

        Analytics.clearToken();

        List<Future<?>> eventTasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            eventTasks.add(executor.submit(() -> {
                Map<String, String> eventData = new HashMap<>();
                eventData.put("Test Token", "5 events sent");
                Analytics.trackEvent("Sending 5 events after an invalid token", eventData, null);
            }));
        }

        waitAll(eventTasks);

        executor.shutdown();

        AlertsUtils.showAlert(context, "Info", "5 events and errors sent");
    }

    public void changeToSecondActivity() {
        requireActivity().finish();
        startActivity(new Intent(requireContext(), SecondActivity.class));
    }

    private void waitAll(List<Future<?>> tasks) {
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}