package com.appambit.javaapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.appambit.sdk.RemoteConfig;

public class RemoteConfigFragment extends Fragment {
    private static final String TAG = "RemoteConfigFragment";
    private TextView txtRemoteGetString;
    private CardView cardBanner;
    private CardView cardDiscount;
    private TextView txtDiscount;
    private TextView txtMaxUpload;
    private Button btnFetch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_config, container, false);

        initializeViews(view);
        fetchRemoteConfig();

        btnFetch.setOnClickListener(v ->
            RemoteConfig.fetch().then(success -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String message = success ? "Fetch Succeeded" : "Fetch Throttled";
                        Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
        }));

        return view;
    }

    private void initializeViews(View view) {
        txtRemoteGetString = view.findViewById(R.id.txtRemoteGetString);
        cardBanner = view.findViewById(R.id.cardBanner);
        cardDiscount = view.findViewById(R.id.cardDiscount);
        txtDiscount = view.findViewById(R.id.txtDiscount);
        txtMaxUpload = view.findViewById(R.id.txtMaxUpload);
        btnFetch = view.findViewById(R.id.btnFetch);
    }

    private void fetchRemoteConfig() {
        String data = RemoteConfig.getString("data");
        boolean showBanner = RemoteConfig.getBoolean("banner");
        int discountValue = RemoteConfig.getInt("discount");
        double maxUpload = RemoteConfig.getDouble("max_upload");

        txtRemoteGetString.setText(data);

        cardBanner.setVisibility(showBanner ? View.VISIBLE : View.GONE);

        if (discountValue > 0) {
            cardDiscount.setVisibility(View.VISIBLE);
            txtDiscount.setText(discountValue + "% OFF");
        } else {
            cardDiscount.setVisibility(View.GONE);
        }

        txtMaxUpload.setText(String.format("%.1f MB", maxUpload));
    }
}