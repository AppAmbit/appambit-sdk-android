package com.appambit.testapp;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.appambit.sdk.Analytics;
import com.appambit.sdk.ServiceLocator;
import com.appambit.sdk.Crashes;
import com.appambit.testapp.utils.AlertsUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class CrashesFragment extends Fragment {

    private final ExecutorService mExecutor = ServiceLocator.getExecutorService();

    Button btnDidCrash;
    Button btnSendCustomLogError;
    Button btnSendDefaultLogError;
    Button btnSendExceptionLogError;
    Button btnGenerateLast30DailyErrors;
    Button btnSetUserId;
    Button btnSetUserEmail;
    Button btnGenerateLast30DailyCrashes;
    Button btnThrowNewCrash;
    Button btnGenerateTestCrash;

    EditText etUserId;
    EditText etUserEmail;
    EditText etCustomLogErrorText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_crashes, container, false);
        Context context = requireContext();

        btnDidCrash = view.findViewById(R.id.btnDidCrash);
        btnDidCrash.setOnClickListener(v -> mExecutor.execute(() -> {
           if(Crashes.didCrashInLastSession()) {
               AlertsUtils.showAlert(context, "Crash", "Application crashed in the last session");
           }else {
                AlertsUtils.showAlert(context, "Crash", "Application did not crash in the last session");
           }
        }));

        btnSendCustomLogError = view.findViewById(R.id.btnSendCustomLogError);
        etCustomLogErrorText = view.findViewById(R.id.etCustomLogErrorText);
        etCustomLogErrorText.setText("Test Log Message");
        btnSendCustomLogError.setOnClickListener(v -> mExecutor.execute(() -> {
            String customLogMessage = etCustomLogErrorText.getText().toString();
            Crashes.logError(customLogMessage);
            AlertsUtils.showAlert(context, "Info", "LogError sent");
        }));

        btnSendDefaultLogError = view.findViewById(R.id.btnSendDefaultLogError);
        btnSendDefaultLogError.setOnClickListener(v -> mExecutor.execute(() -> {
            Map<String, String> properties = new HashMap<>();
            properties.put("user_id", "1");
            Crashes.logError("Test Log Error", properties);
            AlertsUtils.showAlert(context, "Info", "Test Default LogError sent");
        }));

        btnSendExceptionLogError = view.findViewById(R.id.btnSendExceptionLogError);
        btnSendExceptionLogError.setOnClickListener(v -> {
            try {
                throw new NullPointerException();
            }
            catch (Exception exception)
            {
                Map<String, String> properties = new HashMap<>();
                properties.put("user_id", "1");
                Crashes.logError(exception, properties);
                AlertsUtils.showAlert(context, "Info", "Test Exception LogError sent");
            }
        });

        btnGenerateLast30DailyErrors = view.findViewById(R.id.btnGenerateLast30DailyErrors);
        btnGenerateLast30DailyErrors.setOnClickListener(v -> {

        });

        btnSetUserId = view.findViewById(R.id.btnSetUserId);
        etUserId = view.findViewById(R.id.etUserId);
        etUserId.setText(UUID.randomUUID().toString());
        btnSetUserId.setOnClickListener(v -> {
                Analytics.setUserId(etUserId.getText().toString());
                AlertsUtils.showAlert(context, "Info", "User ID changed");
        });

        btnSetUserEmail = view.findViewById(R.id.btnSetUserEmail);
        etUserEmail = view.findViewById(R.id.etUserEmail);
        etUserEmail.setText("test@gmail.com");
        btnSetUserEmail.setOnClickListener(v -> {
            Analytics.setUserEmail(etUserEmail.getText().toString());
            AlertsUtils.showAlert(context, "Info", "User email changed");
        });

        btnThrowNewCrash = view.findViewById(R.id.btnThrowNewCrash);
        btnThrowNewCrash.setOnClickListener(v -> {
            throw new NullPointerException();
        });

        btnGenerateTestCrash = view.findViewById(R.id.btnGenerateTestCrash);
        btnGenerateTestCrash.setOnClickListener(v -> Crashes.generateTestCrash());

        btnGenerateLast30DailyCrashes = view.findViewById(R.id.btnGenerateLast30DailyCrashes);
        btnGenerateLast30DailyCrashes.setOnClickListener(v -> {

        });

        return view;
    }

}