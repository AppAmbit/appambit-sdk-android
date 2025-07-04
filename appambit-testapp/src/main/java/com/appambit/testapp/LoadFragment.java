package com.appambit.testapp;

import static com.appambit.sdk.core.utils.InternetConnection.hasInternetConnection;
import static java.sql.DriverManager.println;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.crashes.Crashes;
import com.appambit.testapp.utils.AlertsUtils;
import java.util.HashMap;
import java.util.Map;

public class LoadFragment extends Fragment {

    Handler handler = new Handler();

    Button btnSend500Events;
    Button btnSend500Logs;
    Button btnSend500Sessions;

    EditText etLoadCustomMessage;
    TextView tvEventsLabel;
    TextView tvLogsLabel;
    TextView tvSendSessionsLabel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_load, container, false);

        etLoadCustomMessage = view.findViewById(R.id.etLoadCustomMessage);
        etLoadCustomMessage.setText("Test Message");

        tvEventsLabel = view.findViewById(R.id.tvEventsLabel);
        tvLogsLabel = view.findViewById(R.id.tvLogsLabel);
        tvSendSessionsLabel = view.findViewById(R.id.tvSendSessionsLabel);

        btnSend500Events = view.findViewById(R.id.btnSend500Events);
        btnSend500Events.setOnClickListener(v -> {
            tvEventsLabel.setVisibility(View.VISIBLE);
            Map<String, String> eventProperties = new HashMap<>();
            eventProperties.put("Test 500", "Events");
            final int[] index = {0};

            Runnable sendEventRunnable = new Runnable() {
                @Override
                public void run() {
                    if (index[0] < 500) {
                        Analytics.trackEvent(etLoadCustomMessage.getText().toString(), eventProperties, null);
                        tvEventsLabel.setText("Sending event: " + (index[0] + 1) + " of 500");
                        index[0]++;
                        if(hasInternetConnection(requireContext())) {
                            handler.postDelayed(this, 500);
                        }else {
                            handler.postDelayed(this, 5);
                        }
                    } else {
                        tvEventsLabel.setVisibility(View.INVISIBLE);
                        AlertsUtils.showAlert(requireContext(), "Info", "500 Events generated");
                    }
                }
            };
            handler.post(sendEventRunnable);
        });

        btnSend500Logs = view.findViewById(R.id.btnSend500Logs);
        btnSend500Logs.setOnClickListener(v -> {
            tvLogsLabel.setVisibility(View.VISIBLE);
            final int[] index = {0};

            Runnable sendLogRunnable = new Runnable() {
                @Override
                public void run() {
                    if (index[0] < 500) {
                        Crashes.LogError(
                                requireContext(),
                                etLoadCustomMessage.getText().toString(),
                                null, null, null, null, 0, null);
                        tvLogsLabel.setText("Sending log: " + (index[0] + 1) + " of 500");
                        index[0]++;
                        if(hasInternetConnection(requireContext())) {
                            handler.postDelayed(this, 500);
                        }else {
                            handler.postDelayed(this, 5);
                        }
                    } else {
                        tvLogsLabel.setVisibility(View.INVISIBLE);
                        AlertsUtils.showAlert(requireContext(), "Info", "500 Logs generated");
                    }
                }
            };
            handler.post(sendLogRunnable);
        });

        btnSend500Sessions = view.findViewById(R.id.btnSend500Sessions);
        btnSend500Sessions.setOnClickListener(v -> {
            if (!hasInternetConnection(requireContext())) {
                AlertsUtils.showAlert(requireContext(), "Info", "Turn on internet and try again");
                return;
            }
            tvSendSessionsLabel.setVisibility(View.VISIBLE);
            final int[] index = {0};

            Runnable sendSessionRunnable = new Runnable() {
                @Override
                public void run() {
                    Analytics.startSession();
                    tvSendSessionsLabel.setText("Sending session: " + (index[0] + 1) + " of 500");

                    handler.postDelayed(() -> {
                        Analytics.endSession();
                        index[0]++;
                        println("Session " + index[0] + " sent");
                        if (index[0] < 5) {
                            handler.post(this);
                        } else {
                            tvSendSessionsLabel.setVisibility(View.INVISIBLE);
                            AlertsUtils.showAlert(requireContext(), "Info", "5 Sessions generated");
                        }
                    }, 5000);
                }
            };

            handler.post(sendSessionRunnable);
        });

        return view;
    }
}