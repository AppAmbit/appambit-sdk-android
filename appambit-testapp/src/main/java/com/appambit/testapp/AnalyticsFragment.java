package com.appambit.testapp;

import androidx.fragment.app.Fragment;

import com.appambit.sdk.analytics.Analytics;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.content.Context;
import android.os.Bundle;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


public class AnalyticsFragment extends Fragment {
    private static final String TAG = AnalyticsFragment.class.getSimpleName();
    Button btnEventWProperty;
    Button btnDefaultClickedEventWProperty;
    Button btnMax300LengthEvent;
    Button btnMax20PropertiesEvent;
    Button btn3DailyEvents;
    Button btn220BatchEvents;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

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



        return view;
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
        for (int index = 0; index < 30; index++) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.add(Calendar.DAY_OF_MONTH, -index);
            Date date = cal.getTime();

            Map<String, String> properties = new HashMap<>();
            properties.put("30 Daily events", "Event");

            Analytics.trackEvent("30 Daily events", properties, date);
        }

        Toast.makeText(context, "30 events generated", Toast.LENGTH_SHORT).show();
    }

    private static void onGenerateBatchEvents(Context context) {

        Map<String, String> properties = new HashMap<>();
        for (int index = 1; index <= 220; index++) {
            properties.put("property1", "value1" );
            Analytics.trackEvent("Events 220", properties, null);
        }

        Toast.makeText(context, "220 events generated", Toast.LENGTH_SHORT).show();

    }
}