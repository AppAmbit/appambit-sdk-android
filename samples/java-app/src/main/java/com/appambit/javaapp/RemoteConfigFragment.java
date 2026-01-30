package com.appambit.javaapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appambit.sdk.RemoteConfig;

public class RemoteConfigFragment extends Fragment {

    TextView txtRemoteGetString;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_config, container, false);

        txtRemoteGetString = view.findViewById(R.id.txtRemoteGetString);

        RemoteConfig.fetch().then(success -> {
            if (success) {
                String data = RemoteConfig.getString("data");
                boolean banner = RemoteConfig.getBoolean("banner");
                int discount = RemoteConfig.getNumber("discount");

                System.out.println("Info consumer remote config 1: " + data);
                System.out.println("Info consumer remote config 2: " + banner);
                System.out.println("Info consumer remote config 3: " + discount);
                fetchData(data);
            } else {
                System.out.println("Remote Config fetch failed");
            }
        });

        return view;
    }

    private void fetchData(String data) {
        txtRemoteGetString.setText(data);
    }
}