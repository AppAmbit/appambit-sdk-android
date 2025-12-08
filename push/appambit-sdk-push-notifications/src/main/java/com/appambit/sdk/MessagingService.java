package com.appambit.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.appambit.sdk.models.AppAmbitNotification;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "AppAmbitPushSDK";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        if (message.getNotification() != null) {
            sendNotification(message);
        }
    }

    @SuppressLint("MissingPermission")
    private void sendNotification(@NonNull RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification == null) return;

        Map<String, String> data = remoteMessage.getData();

        String title = notification.getTitle();
        String body = notification.getBody();
        String deepLink = data.get("deep_link");
        String largeIconUrl = data.get("large_icon_url");
        String smallIconName = notification.getIcon();
        String color = notification.getColor();
        String channelId = notification.getChannelId();
        String clickAction = notification.getClickAction();
        String notificationPriorityString = data.get("notification_priority");

        AppAmbitNotification appAmbitNotification = new AppAmbitNotification(
                title,
                body,
                notificationPriorityString,
                deepLink,
                color,
                largeIconUrl,
                smallIconName,
                data
        );

        if (TextUtils.isEmpty(channelId)) {
            channelId = "default_channel_id";
        }
        String channelName = "Default Channel";

        Intent intent;
        if (!TextUtils.isEmpty(clickAction)) {
            intent = new Intent(clickAction);
        } else {
            intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        }

        PendingIntent pendingIntent = null;
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        int priority = NotificationCompat.PRIORITY_DEFAULT;
        Integer notificationObjectPriority = notification.getNotificationPriority();
        if (notificationObjectPriority != null) {
            priority = notificationObjectPriority;
        } else if (notificationPriorityString != null) {
            priority = getPriorityFromString(notificationPriorityString);
        }

        int appIcon = getSmallIcon(appAmbitNotification);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(appIcon)
                .setContentTitle(appAmbitNotification.getTitle())
                .setContentText(appAmbitNotification.getBody())
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (appAmbitNotification.getColor() != null) {
            try {
                builder.setColor(Color.parseColor(appAmbitNotification.getColor()));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid color format: " + appAmbitNotification.getColor());
            }
        }

        Bitmap largeIcon = getBitmapFromUrl(appAmbitNotification.getLargeIconUrl());
        if(largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        Uri imageUrl = remoteMessage.getNotification().getImageUrl();
        if (imageUrl != null) {
            Bitmap bitmap = getBitmapFromUrl(imageUrl.toString());
            if (bitmap != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon((Bitmap) null));
            }
        }

        PushNotifications.NotificationCustomizer customizer = PushNotifications.getNotificationCustomizer();
        if (customizer != null) {
            customizer.customize(this, builder, appAmbitNotification);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
        }
    }

    private int getPriorityFromString(String priority) {
        if (priority == null) {
            return NotificationCompat.PRIORITY_DEFAULT;
        }
        switch (priority) {
            case "PRIORITY_MIN":
                return NotificationCompat.PRIORITY_MIN;
            case "PRIORITY_LOW":
                return NotificationCompat.PRIORITY_LOW;
            case "PRIORITY_HIGH":
                return NotificationCompat.PRIORITY_HIGH;
            case "PRIORITY_MAX":
                return NotificationCompat.PRIORITY_MAX;
            default:
                return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    private int getSmallIcon(AppAmbitNotification notification) {
        String customIconName = notification.getSmallIconName();
        if (customIconName != null && !customIconName.isEmpty()) {
            try {
                int iconId = getResources().getIdentifier(customIconName, "drawable", getPackageName());
                if (iconId != 0) return iconId;
                Log.w(TAG, "Custom icon '" + customIconName + "' not found.");
            } catch (Exception e) {
                Log.e(TAG, "Error getting custom icon", e);
            }
        }
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            if (appInfo.icon != 0) return appInfo.icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find application package icon", e);
        }
        return android.R.drawable.sym_def_app_icon;
    }

    private Bitmap getBitmapFromUrl(String src) {
        if (src == null || src.isEmpty()) return null;
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e(TAG, "Failed to download image from: " + src, e);
            return null;
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PushNotifications.handleNewToken(token);
    }
}
