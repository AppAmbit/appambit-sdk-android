package com.appambit.testapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.core.AppAmbit;

public class CrashesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_crashes, container, false);

        Button btnGenerateLogs = view.findViewById(R.id.btnGenerateLogs);
        btnGenerateLogs.setOnClickListener(v -> {
            Analytics.generateSampleLogsEvents();
            Log.d(AppAmbit.class.getSimpleName(), "LOG CREADO UI");
            Toast.makeText(requireContext(), "Generate logs...", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}