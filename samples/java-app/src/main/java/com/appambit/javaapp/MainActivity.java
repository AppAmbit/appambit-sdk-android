package com.appambit.javaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.PushNotifications;
import com.appambit.sdk.RemoteConfig;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AppAmbitSample";

    private static final String[] TAB_LABELS = {
            "Crashes", "Analytics", "Load", "RemoteConfig", "CMS", "Database", "Cloud Code"
    };

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
        PushNotifications.setOpenedListener(notification -> {
            Log.d(TAG, "[OPENED] User tapped the notification");
            Log.d(TAG, "  Title : " + notification.getTitle());
            Log.d(TAG, "  Body  : " + notification.getBody());
            Log.d(TAG, "  Data  : " + notification.getData());
        });

        // Required to dispatch the opened callback when the app was completely closed.
        PushNotifications.handleNotificationOpened(this, getIntent());

        TabLayout tabLayout = findViewById(R.id.bottom_navigation);
        for (String label : TAB_LABELS) {
            tabLayout.addTab(tabLayout.newTab().setText(label));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabLayout.post(() -> tabLayout.selectTab(tab, true));
                Fragment fragment = fragmentForIndex(tab.getPosition());
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CrashesFragment())
                    .commit();
        }
    }

    private Fragment fragmentForIndex(int index) {
        switch (index) {
            case 1: return new AnalyticsFragment();
            case 2: return new LoadFragment();
            case 3: return new RemoteConfigFragment();
            case 4: return new CmsFragment();
            case 5: return new DatabaseFragment();
            case 6: return new CloudCodeFragment();
            default: return new CrashesFragment();
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
