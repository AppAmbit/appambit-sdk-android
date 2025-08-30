package com.appambit.testapp;

import static com.appambit.sdk.utils.InternetConnection.hasInternetConnection;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.appambit.sdk.Analytics;
import com.appambit.sdk.ServiceLocator;
import com.appambit.sdk.SessionManager;
import com.appambit.sdk.enums.SessionType;
import com.appambit.sdk.models.analytics.SessionData;
import com.appambit.sdk.models.logs.ExceptionInfo;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.JsonConvertUtils;
import com.appambit.sdk.Crashes;
import com.appambit.testapp.utils.AlertsUtils;
import org.json.JSONException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class CrashesFragment extends Fragment {

    private final ExecutorService mExecutor = ServiceLocator.getExecutorService();

    Button btnDidCrash;
    Button btnSendCustomLogError;
    Button btnSendDefaultLogError;
    Button btnSendExceptionLogError;
    Button btnClassInfoLogError;
    Button btnGenerateLast30DailyErrors;
    Button btnSetUserId;
    Button btnSetUserEmail;
    Button btnGenerateLast30DailyCrashes;
    Button btnThrowNewCrash;
    Button btnGenerateTestCrash;

    EditText etUserId;
    EditText etUserEmail;
    EditText etCustomLogErrorText;

    Storable StorableApp = ServiceLocator.getStorageService();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_crashes, container, false);
        final String TAG = CrashesFragment.class.getSimpleName();
        Context context = requireContext();

        btnDidCrash = view.findViewById(R.id.btnDidCrash);
        btnDidCrash.setOnClickListener(v -> mExecutor.execute(() -> {
           if(Crashes.didCrashInLastSession(context)) {
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
            Crashes.LogError(context, customLogMessage, null, null, null, this.getClass().getName(), 0, new Date());
            AlertsUtils.showAlert(context, "Info", "LogError sent");
        }));

        btnSendDefaultLogError = view.findViewById(R.id.btnSendDefaultLogError);
        btnSendDefaultLogError.setOnClickListener(v -> mExecutor.execute(() -> {
            Map<String, String> properties = new HashMap<>();
            properties.put("user_id", "1");
            Crashes.LogError(context, "Test Log Error", properties, null, null, this.getClass().getName(), 0, new Date());
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
                Crashes.LogError(context, exception, properties, null, null, 0, DateUtils.getUtcNow());
                AlertsUtils.showAlert(context, "Info", "Test Exception LogError sent");
            }
        });

        btnClassInfoLogError = view.findViewById(R.id.btnClassInfoLogError);
        btnClassInfoLogError.setOnClickListener(v -> {
            Map<String, String> properties = new HashMap<>();
            properties.put("user_id", "1");
            Crashes.LogError(context, "Test Log Error", properties, this.getClass().getName(), null, null, 0, DateUtils.getUtcNow());
            AlertsUtils.showAlert(context, "Info", "LogError sent");
        });

        btnGenerateLast30DailyErrors = view.findViewById(R.id.btnGenerateLast30DailyErrors);
        btnGenerateLast30DailyErrors.setOnClickListener(v -> {
            if (hasInternetConnection(context)) {
                AlertsUtils.showAlert(context, "Info", "Turn off internet and try again");
                return;
            }

            Exception exception = new NullPointerException();
            for (int index = 1; index <= 30; index++) {

                SessionData sessionData = new SessionData();
                UUID sessionId = UUID.randomUUID();
                SessionManager.setCurrentSessionId(sessionId.toString());
                sessionData.setId(sessionId);
                sessionData.setSessionType(SessionType.START);
                Date errorDate = DateUtils.getDateDaysAgo(30 - index);
                sessionData.setTimestamp(errorDate);

                try {
                    StorableApp.putSessionData(sessionData);
                } catch (Exception e) {
                    Log.e(TAG, "Error inserting start session", e);
                    continue;
                }

                ExceptionInfo info = ExceptionInfo.fromException(context, exception);
                info.setCreatedAt(errorDate);

                Crashes.LogError(context, "Test 30 Last Days Errors", null, null, null, null, 0, errorDate);

                try {
                    int finalIndex = index;
                    Random random = new Random();
                    long randomOffset = random.nextInt(60 * 60 * 1000);
                    StorableApp.putSessionData(new SessionData() {
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
            AlertsUtils.showAlert(context, "Info", "Logs generated, turn on internet");
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
            if (hasInternetConnection(context)) {
                AlertsUtils.showAlert(context, "Info", "Turn off internet and try again");
                return;
            }

            Exception exception = new NullPointerException();
            for (int index = 1; index <= 30; index++) {

                SessionData sessionData = new SessionData();
                UUID sessionId = UUID.randomUUID();
                SessionManager.setCurrentSessionId(sessionId.toString());
                sessionData.setId(sessionId);
                sessionData.setSessionType(SessionType.START);
                Date crashDate = DateUtils.getDateDaysAgo(30 - index);
                sessionData.setTimestamp(crashDate);

                try {
                    StorableApp.putSessionData(sessionData);
                } catch (Exception e) {
                    Log.e(TAG, "Error inserting start session", e);
                    continue;
                }

                ExceptionInfo info = ExceptionInfo.fromException(context, exception);
                info.setCreatedAt(crashDate);

                try {
                    String crashJson = JsonConvertUtils.toJson(info);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedDate = sdf.format(crashDate);

                    String fileName = "crash_" + formattedDate + "_" + index + ".json";

                    File crashFile = new File(context.getFilesDir(), fileName);
                    Log.d(TAG, "Crash file saved to: " + crashFile.getAbsolutePath());

                    try (FileWriter writer = new FileWriter(crashFile)) {
                        writer.write(crashJson);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error saving crash file", e);
                }

                try {
                    int finalIndex = index;
                    Random random = new Random();
                    long randomOffset = random.nextInt(60 * 60 * 1000);
                    StorableApp.putSessionData(new SessionData() {
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
            AlertsUtils.showAlert(context, "Info", "Crashes generated, turn on internet");
        });

        return view;
    }

}