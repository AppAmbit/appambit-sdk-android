package com.appambit.testapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.appambit.sdk.analytics.Analytics;
import com.appambit.sdk.core.AppAmbit;
import com.appambit.testapp.databinding.ActivityMainBinding;
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Comment the line for automatic session management
        //Analytics.enableManualSession();
        AppAmbit.init(getApplicationContext(), "80add826-80ab-4273-8327-14f403e4d2c1");

        if (savedInstanceState == null) {
            replaceFragment(new CrashesFragment(), "CrashesFragment");
            binding.bottomNavigation.setSelectedItemId(R.id.nav_crashes);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_crashes) {
                replaceFragment(new CrashesFragment(), "CrashesFragment");
                return true;
            } else if (itemId == R.id.nav_analytics) {
                replaceFragment(new AnalyticsFragment(), "AnalyticsFragment");
                return true;
            } else if (itemId == R.id.nav_load) {
                replaceFragment(new LoadFragment(), "LoadFragment");
                return true;
            }
            return false;
        });
    }

    private void replaceFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomNavSelection();
    }

    private void updateBottomNavSelection() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof CrashesFragment) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_crashes);
        } else if (currentFragment instanceof AnalyticsFragment) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_analytics);
        } else if (currentFragment instanceof LoadFragment) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_load);
        }
    }
}