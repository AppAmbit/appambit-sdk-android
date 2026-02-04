package com.appambit.javaapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.PushNotifications;
import com.appambit.sdk.RemoteConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Comment the line for automatic session management
        //Analytics.enableManualSession();
        AppAmbit.start(getApplicationContext(), "<YOUR-APPKEY>");

        // Initialize Push SDK on app start
        PushNotifications.start(getApplicationContext());

        RemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        RemoteConfig.fetch().then(success -> {
            if (success) {
                Log.d(TAG, "Fetch remotely");
            } else {
                Log.d(TAG, "Failed to fetch Remote Config");
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_crashes) {
                selectedFragment = new CrashesFragment();
            } else if (itemId == R.id.nav_analytics) {
                selectedFragment = new AnalyticsFragment();
            }else if (itemId == R.id.nav_load) {
                selectedFragment = new LoadFragment();
            }else if (itemId == R.id.nav_remote_config) {
                selectedFragment = new RemoteConfigFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_crashes);
        }
    }
}
