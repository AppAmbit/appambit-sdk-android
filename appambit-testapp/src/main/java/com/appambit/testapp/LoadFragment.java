package com.appambit.testapp;

import static com.appambit.sdk.core.utils.InternetConnection.hasInternetConnection;
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
    Button btnSend500StartSessions;
    Button btnSend500EndSessions;
    Button btnSend500Tokens;

    EditText etLoadCustomMessage;
    TextView tvEventsLabel;
    TextView tvLogsLabel;
    TextView tvStartSessionsLabel;
    TextView tvEndSessionsLabel;
    TextView tvTokensLabel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_load, container, false);

        etLoadCustomMessage = view.findViewById(R.id.etLoadCustomMessage);
        etLoadCustomMessage.setText("Test Message");

        tvEventsLabel = view.findViewById(R.id.tvEventsLabel);
        tvLogsLabel = view.findViewById(R.id.tvLogsLabel);
        tvStartSessionsLabel = view.findViewById(R.id.tvStartSessionsLabel);
        tvEndSessionsLabel = view.findViewById(R.id.tvEndSessionsLabel);
        tvTokensLabel = view.findViewById(R.id.tvTokensLabel);

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

        btnSend500StartSessions = view.findViewById(R.id.btnSend500StartSessions);
        btnSend500StartSessions.setOnClickListener(v -> {
            tvStartSessionsLabel.setVisibility(View.VISIBLE);
            final int[] index = {0};

            Runnable startSessionRunnable = new Runnable() {
                @Override
                public void run() {
                    if (index[0] < 500) {
                        Analytics.validateOrInvalidateSession(false);
                        Analytics.startSession();
                        tvStartSessionsLabel.setText("Starting session: " + (index[0] + 1) + " of 500");
                        index[0]++;
                        if(hasInternetConnection(requireContext())) {
                            handler.postDelayed(this, 500);
                        }else {
                            handler.postDelayed(this, 5);
                        }
                    } else {
                        tvStartSessionsLabel.setVisibility(View.INVISIBLE);
                        AlertsUtils.showAlert(requireContext(), "Info", "500 StartSessions generated");
                    }
                }
            };
            handler.post(startSessionRunnable);
        });

        btnSend500EndSessions = view.findViewById(R.id.btnSend500EndSessions);
        btnSend500EndSessions.setOnClickListener(v -> {
            tvEndSessionsLabel.setVisibility(View.VISIBLE);
            final int[] index = {0};

            Runnable endSessionRunnable = new Runnable() {
                @Override
                public void run() {
                    if(index[0] < 500) {
                        Analytics.validateOrInvalidateSession(true);
                        Analytics.endSession();
                        tvEndSessionsLabel.setText("Ending session: " + (index[0] + 1) + " of 500");
                        index[0]++;
                        if(hasInternetConnection(requireContext())) {
                            handler.postDelayed(this, 500);
                        }else {
                            handler.postDelayed(this, 5);
                        }
                    } else {
                        tvEndSessionsLabel.setVisibility(View.INVISIBLE);
                        AlertsUtils.showAlert(requireContext(), "Info", "500 EndSessions generated");
                    }
                }
            };
            handler.post(endSessionRunnable);
        });

        btnSend500Tokens = view.findViewById(R.id.btnSend500Tokens);
        btnSend500Tokens.setOnClickListener(v -> {
            tvTokensLabel.setVisibility(View.VISIBLE);
            final int[] index = {0};

            Runnable sendTokenRunnable = new Runnable() {
                @Override
                public void run() {
                    if(index[0] < 500) {
                        Analytics.requestToken();
                        tvTokensLabel.setText("Sending token: " + (index[0] + 1) + " of 500");
                        index[0]++;
                        if(hasInternetConnection(requireContext())) {
                            handler.postDelayed(this, 500);
                        }else {
                            handler.postDelayed(this, 5);
                        }
                    } else {
                        tvTokensLabel.setVisibility(View.INVISIBLE);
                        AlertsUtils.showAlert(requireContext(), "Info", "500 Tokens requested");
                    }
                }
            };
            handler.post(sendTokenRunnable);
        });

        return view;
    }
}