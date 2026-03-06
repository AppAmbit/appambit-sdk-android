package com.appambit.javaapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appambit.sdk.RemoteConfig;

public class RemoteConfigFragment extends Fragment {
    private TextView txtRemoteGetString;
    private CardView cardBanner;
    private CardView cardDiscount;
    private TextView txtDiscount;
    private TextView txtMaxUpload;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_config, container, false);

        initializeViews(view);
        loadSection();

        return view;
    }

    private void initializeViews(View view) {
        txtRemoteGetString = view.findViewById(R.id.txtRemoteGetString);
        cardBanner = view.findViewById(R.id.cardBanner);
        cardDiscount = view.findViewById(R.id.cardDiscount);
        txtDiscount = view.findViewById(R.id.txtDiscount);
        txtMaxUpload = view.findViewById(R.id.txtMaxUpload);
    }

    private void loadSection() {
        String data = RemoteConfig.getString("data");
        boolean showBanner = RemoteConfig.getBoolean("banner");
        long discountValue = RemoteConfig.getLong("discount");
        double maxUpload = RemoteConfig.getDouble("max_upload");

        if (data != null && !data.isEmpty()) {
            txtRemoteGetString.setText(data);
            txtRemoteGetString.setVisibility(View.VISIBLE);
        } else {
            txtRemoteGetString.setText("You're without Remote values");
        }

        cardBanner.setVisibility(showBanner ? View.VISIBLE : View.GONE);

        if (discountValue > 0) {
            cardDiscount.setVisibility(View.VISIBLE);
            txtDiscount.setText(discountValue + "% OFF");
        } else {
            cardDiscount.setVisibility(View.GONE);
        }
        if(maxUpload > 0) {
            txtMaxUpload.setText(String.format("%.1f MB", maxUpload));
        }else {
            txtMaxUpload.setText("100 MB");
        }
    }
}