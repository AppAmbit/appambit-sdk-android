package com.appambit.testapp;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.core.AppAmbit;
import com.appambit.sdk.core.enums.LogType;
import com.appambit.sdk.core.models.analytics.SessionData;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.services.endpoints.LogEndpoint;
import com.appambit.sdk.core.enums.SessionType;
import com.appambit.sdk.core.utils.JsonConvertUtils;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;



public class AnalyticsFragment extends Fragment {
    private static final String TAG = AnalyticsFragment.class.getSimpleName();
    Button btnStartSession;
    Button btnEndSession;
    Button btnGenerate30DaysTestSessions;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.fragment_analytics, container, false);

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
        btnGenerate30DaysTestSessions.setOnClickListener(v -> {
            onGenerate30DaysTestSessions(requireContext());
            Snackbar.make(view, "generated sessions, close and reopen the application", Snackbar.LENGTH_LONG).show();

        });

       return view;
    }

    public static void onGenerate30DaysTestSessions(Context context) {
        File outputDirectory = context.getFilesDir();
        String offlineSessionsFile = "OfflineSessions.json";

        Random random = new Random();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date startDate = calendar.getTime();

        List<SessionData> offlineSessions = new ArrayList<>();

        for (int index = 1; index <= 30; index++) {
            Calendar sessionStartCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            sessionStartCal.setTime(startDate);
            sessionStartCal.add(Calendar.DAY_OF_YEAR, index);
            sessionStartCal.set(Calendar.HOUR_OF_DAY, random.nextInt(23));
            sessionStartCal.set(Calendar.MINUTE, random.nextInt(59));
            sessionStartCal.set(Calendar.SECOND, 0);
            sessionStartCal.set(Calendar.MILLISECOND, 0);

            Date dateStartSession = sessionStartCal.getTime();

            SessionData sessionData = new SessionData();
            sessionData.setId(UUID.randomUUID());
            sessionData.setSessionId(null);
            sessionData.setTimestamp(dateStartSession);
            sessionData.setSessionType(SessionType.START);

            offlineSessions.add(sessionData);

            int durationMinutes = random.nextInt(24 * 60) + 1;
            Calendar sessionEndCal = (Calendar) sessionStartCal.clone();
            sessionEndCal.add(Calendar.MINUTE, durationMinutes);
            Date dateEndSession = sessionEndCal.getTime();

            sessionData = new SessionData();
            sessionData.setId(UUID.randomUUID());
            sessionData.setSessionId(null);
            sessionData.setTimestamp(dateEndSession);
            sessionData.setSessionType(SessionType.END);


            offlineSessions.add(sessionData);
        }

        try {
            var result = JsonConvertUtils.toJson(offlineSessions);
            File file = new File(outputDirectory, offlineSessionsFile);
            FileWriter writer = new FileWriter(file);
            writer.write(result);
            writer.close();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }catch (Exception e) {
            Log.e(TAG, e.toString());
        }


    }
}