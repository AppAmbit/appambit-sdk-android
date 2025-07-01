package com.appambit.testapp;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.core.AppAmbit;
import com.appambit.sdk.core.ServiceLocator;
import com.appambit.sdk.core.enums.LogType;
import com.appambit.sdk.core.models.logs.LogBatch;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.services.ApiService;
import com.appambit.sdk.core.services.HttpApiService;
import com.appambit.sdk.core.services.endpoints.LogBatchEndpoint;
import com.appambit.sdk.core.services.endpoints.LogEndpoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrashesFragment extends Fragment {

    Button btnSendDefaultLogError;
    Button btnSendDefaultLogCrash;
    Button btnSendLogBatches;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_crashes, container, false);

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Context context = requireContext();
        ApiService apiService = ServiceLocator.getApiService();

        btnSendDefaultLogError = view.findViewById(R.id.btnSendDefaultLogError);
        btnSendDefaultLogError.setOnClickListener(v -> executor.execute(() -> {
            try {
                Map<String, String> contextMap = new HashMap<>();
                LogEntity logEntity = new LogEntity();
                logEntity.setId(UUID.randomUUID());
                logEntity.setAppVersion("1.0 (1)");
                logEntity.setClassFQN(AppAmbit.class.getSimpleName());
                logEntity.setFileName("PATH FILE NAME ");
                logEntity.setLineNumber(200);
                logEntity.setMessage("Test Log Error");
                logEntity.setStackTrace("NoStackTraceAvailable");
                logEntity.setContext(contextMap);
                logEntity.setType(LogType.ERROR);
                logEntity.setCreatedAt(new Date());

                LogEndpoint logEndpoint = new LogEndpoint(logEntity);
                apiService.executeRequest(logEndpoint, LogEntity.class);
                Log.d("AppAmbit", "Crash log sent successfully");
            }catch (Exception e) {
                Log.e("AppAmbit", "Error during log creation: " + e.getMessage());
            }
        }));

        btnSendDefaultLogCrash = view.findViewById(R.id.btnSendDefaultLogCrash);
        btnSendDefaultLogCrash.setOnClickListener(v -> executor.execute(() -> {
            try {
                Map<String, String> contextMap = new HashMap<>();
                LogEntity logEntity = new LogEntity();
                logEntity.setId(UUID.randomUUID());
                logEntity.setAppVersion("1.0 (1)");
                logEntity.setClassFQN(AppAmbit.class.getSimpleName());
                logEntity.setFileName("PATH FILE NAME ");
                logEntity.setLineNumber(200);
                logEntity.setMessage("Test Log Error");
                logEntity.setStackTrace("NoStackTraceAvailable");
                logEntity.setContext(contextMap);
                logEntity.setType(LogType.CRASH);
                logEntity.setFile("LOG FILE CONTENT");
                logEntity.setCreatedAt(new Date());
                LogEndpoint logEndpoint = new LogEndpoint(logEntity);

                apiService.executeRequest(logEndpoint, LogEntity.class);
                Log.d("AppAmbit", "Crash log sent successfully");
            }catch (Exception e) {
                Log.e("AppAmbit", "Error during log creation: " + e.getMessage());
            }
        }));

        btnSendLogBatches = view.findViewById(R.id.btnSendLogBatches);
        btnSendLogBatches.setOnClickListener(v -> executor.execute(() -> {

            LogEntity log1 = new LogEntity();
            log1.setId(UUID.randomUUID());
            log1.setAppVersion("1.0 (1)");
            log1.setClassFQN("Test LogBatches");
            log1.setFileName("TestLogBatches FILE");
            log1.setLineNumber(124);
            log1.setMessage("Test Log Batch");
            log1.setStackTrace("NoStackTraceAvailable");
            log1.setContext(new HashMap<>());
            log1.setType(LogType.CRASH);
            log1.setFile("File content for log batch");
            log1.setCreatedAt(new Date());

            List<LogEntity> logs = new ArrayList<>();

            for(int i = 0; i < 10; i++) {
                logs.add(log1);
            }

            LogBatch logBatch = new LogBatch();
            logBatch.setLogs(logs);

            LogBatchEndpoint logBatchEndpoint = new LogBatchEndpoint(logBatch);

            apiService.executeRequest(logBatchEndpoint, LogBatch.class);
            Log.d("AppAmbit", "Log batch sent successfully with " + logBatch.Logs.size() + " logs.");
        }));

        Button btnGenerateLogs = view.findViewById(R.id.btnGenerateLogs);
        btnGenerateLogs.setOnClickListener(v -> {
            Log.d(AppAmbit.class.getSimpleName(), "LOG CREADO UI");
            Toast.makeText(requireContext(), "Generate logs...", Toast.LENGTH_SHORT).show();
        });

        return view;
    }



}