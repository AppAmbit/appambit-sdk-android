package com.appambit.testapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.appambit.sdk.core.AppAmbit;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.services.ApiService;
import com.appambit.testapp.databinding.ActivityMainBinding;
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppAmbit.init(getApplicationContext(), "b9d1c6e1-ef63-46cb-88e5-65d9c7d8bd8c");

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