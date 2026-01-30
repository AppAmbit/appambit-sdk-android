package com.appambit.javaapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appambit.sdk.RemoteConfig;

public class RemoteConfigFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_config, container, false);

        RemoteConfig.fetch();

        String data = RemoteConfig.getString("data");

        System.out.println("Info consumer remote config: " + data);

        return view;
    }
}