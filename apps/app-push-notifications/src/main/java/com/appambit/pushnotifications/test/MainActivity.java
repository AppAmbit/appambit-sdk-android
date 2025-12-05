package com.appambit.pushnotifications.test;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.appambit.pushnotifications.AppAmbitPushNotifications;
import com.appambit.pushnotifications.NotificationPermissionHelper;

public class MainActivity extends AppCompatActivity {

    private NotificationPermissionHelper notificationPermissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tokenValue = findViewById(R.id.tokenValue);

        notificationPermissionHelper = new NotificationPermissionHelper(this);

        notificationPermissionHelper.requestPermission();

        AppAmbitPushNotifications.setTokenListener(token ->
                runOnUiThread(() -> tokenValue.setText(token)));
        AppAmbitPushNotifications.initialize(getApplicationContext());
    }
}
