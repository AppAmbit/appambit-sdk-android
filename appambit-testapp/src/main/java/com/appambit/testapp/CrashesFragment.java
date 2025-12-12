package com.appambit.testapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.appambit.sdk.Analytics;
import com.appambit.sdk.Crashes;
import com.appambit.sdk.PushNotifications;
import com.appambit.sdk.ServiceLocator;
import com.appambit.testapp.utils.AlertsUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class CrashesFragment extends Fragment {

    private final ExecutorService mExecutor = ServiceLocator.getExecutorService();

    private Button notificationButton;

    // Other buttons
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crashes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationButton = view.findViewById(R.id.btn_notifications);
        setupNotificationButton();

        // Setup for other buttons
        setupOtherButtons(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update button state in case permissions changed while the app was in the background
        updateNotificationButtonState();
    }

    private void setupNotificationButton() {
        notificationButton.setOnClickListener(v -> {
            if (hasNotificationPermission()) {
                // If we have permission, the button toggles the enabled state.
                boolean newState = !PushNotifications.isNotificationsEnabled(requireContext());
                PushNotifications.setNotificationsEnabled(requireContext(), newState);
                String message = "Notifications have been " + (newState ? "enabled" : "disabled") + ".";
                showAlert("Notification Status", message);
                updateNotificationButtonState();
            } else {
                // If we don't have permission, the button requests it.
                PushNotifications.requestNotificationPermission(requireActivity(), granted -> {
                    if (granted) {
                        // Once permission is granted, we enable notifications.
                        PushNotifications.setNotificationsEnabled(requireContext(), true);
                        requireActivity().runOnUiThread(() -> {
                            showAlert("Notification Status", "Notifications have been enabled.");
                            updateNotificationButtonState();
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> showAlert("Permission Denied", "Notifications cannot be enabled without permission."));
                    }
                });
            }
        });
    }

    private void updateNotificationButtonState() {
        if (hasNotificationPermission()) {
            boolean isEnabled = PushNotifications.isNotificationsEnabled(requireContext());
            notificationButton.setText(isEnabled ? "Disable Notifications" : "Enable Notifications");
        } else {
            notificationButton.setText("Allow Notifications");
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permissions are implicitly granted on older versions.
    }

    private void showAlert(String title, String message) {
        if (getContext() != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void setupOtherButtons(View view) {
        Context context = requireContext();

        btnDidCrash = view.findViewById(R.id.btnDidCrash);
        btnDidCrash.setOnClickListener(v -> mExecutor.execute(() -> {
            if (Crashes.didCrashInLastSession()) {
                AlertsUtils.showAlert(context, "Crash", "Application crashed in the last session");
            } else {
                AlertsUtils.showAlert(context, "Crash", "Application did not crash in the last session");
            }
        }));

        etCustomLogErrorText = view.findViewById(R.id.etCustomLogErrorText);
        etCustomLogErrorText.setText("Test Log Message");
        btnSendCustomLogError = view.findViewById(R.id.btnSendCustomLogError);
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
            } catch (Exception exception) {
                Map<String, String> properties = new HashMap<>();
                properties.put("user_id", "1");
                Crashes.logError(exception, properties);
                AlertsUtils.showAlert(context, "Info", "Test Exception LogError sent");
            }
        });

        etUserId = view.findViewById(R.id.etUserId);
        etUserId.setText(UUID.randomUUID().toString());
        btnSetUserId = view.findViewById(R.id.btnSetUserId);
        btnSetUserId.setOnClickListener(v -> {
            Analytics.setUserId(etUserId.getText().toString());
            AlertsUtils.showAlert(context, "Info", "User ID changed");
        });

        etUserEmail = view.findViewById(R.id.etUserEmail);
        etUserEmail.setText("test@gmail.com");
        btnSetUserEmail = view.findViewById(R.id.btnSetUserEmail);
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

        // Buttons without implementation
        btnGenerateLast30DailyErrors = view.findViewById(R.id.btnGenerateLast30DailyErrors);
        btnGenerateLast30DailyCrashes = view.findViewById(R.id.btnGenerateLast30DailyCrashes);
    }
}
