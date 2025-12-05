package com.appambit.pushnotifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AppAmbitMessaingService extends FirebaseMessagingService {

    private static final String TAG = "AppAmbitPushSDK";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.i(TAG, "Push received from: " + message.getFrom());
        if (!message.getData().isEmpty()) {
            Log.i(TAG, "Message data payload: " + message.getData());
        }

        RemoteMessage.Notification notification = message.getNotification();
        if (notification != null) {
            Log.i(TAG, "Notification payload - title: " + notification.getTitle() + ", body: " + notification.getBody());
            sendNotification(notification);
        }
    }

    @SuppressLint("MissingPermission")
    private void sendNotification(@NonNull RemoteMessage.Notification notification) {
        String channelId = "default_channel_id";
        String channelName = "Default Channel";

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        int appIcon = android.R.drawable.ic_dialog_info;
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            if (appInfo.icon != 0) {
                appIcon = appInfo.icon;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find application package to get icon", e);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(appIcon)
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (notification.getImageUrl() != null) {
            try {
                URL url = new URL(notification.getImageUrl().toString());
                InputStream inputStream = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    builder.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon((Bitmap) null));
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to download notification image", e);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
                return;
            }
        }

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        AppAmbitPushNotifications.handleNewToken(token);
    }
}
