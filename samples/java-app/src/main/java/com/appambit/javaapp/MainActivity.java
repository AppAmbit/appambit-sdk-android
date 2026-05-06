package com.appambit.javaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.PushNotifications;
import com.appambit.sdk.RemoteConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AppAmbitSample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Comment the line for automatic session management
        // Analytics.enableManualSession();
        RemoteConfig.enable();
        AppAmbit.start(getApplicationContext(), "<YOUR-APPKEY>");

        // Initialize Push SDK on app start
        PushNotifications.start(getApplicationContext());

        // Handle notification taps (user pressed the notification to open the app).
        // This is a simple listener, separate from the service extension.
        PushNotifications.setOpenedNotificationListener(notification -> {
            Log.d(TAG, "[OPENED] User tapped the notification");
            Log.d(TAG, "  Title : " + notification.getTitle());
            Log.d(TAG, "  Body  : " + notification.getBody());
            Log.d(TAG, "  Data  : " + notification.getData());
        });

        // Required to dispatch the opened callback when the app was completely closed.
        PushNotifications.handleNotificationOpened(this, getIntent());

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
            }else if (itemId == R.id.nav_cms) {
                selectedFragment = new CmsFragment();
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Update the activity's intent so getIntent() always returns the latest one.
        // This follows the Firebase recommended pattern for handling notification taps.
        setIntent(intent);
        // Dispatch the callback when the app was already running in the background
        // and the user tapped a notification to bring it to the foreground.
        PushNotifications.handleNotificationOpened(this, intent);
    }
}
