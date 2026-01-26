package com.appambit.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
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
    public void handleIntent(Intent intent) {
        if (!PushKernel.isNotificationsEnabled(this)) {
            Log.d(TAG, "Notification received but push is disabled locally. Skipping.");
            return;
        }
        super.handleIntent(intent);
    }

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

        AppAmbitNotification appAmbitNotification = new AppAmbitNotification(
                notification.getTitle(),
                notification.getBody(),
                notification.getColor(),
                notification.getIcon(),
                data
        );

        String channelId = notification.getChannelId();
        if (TextUtils.isEmpty(channelId)) {
            channelId = "default_channel_id";
        }
        String channelName = "Default Channel";

        Intent intent;
        String clickAction = notification.getClickAction();
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
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            if (notification.getNotificationPriority() != null) {
                importance = getImportanceFromPriority(notification.getNotificationPriority());
            }
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            Uri soundUri = getSoundUri(notification.getSound());
            channel.setSound(soundUri, null);
            notificationManager.createNotificationChannel(channel);
        }

        int appIcon = getSmallIcon(appAmbitNotification);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(appIcon)
                .setContentTitle(appAmbitNotification.getTitle())
                .setContentText(appAmbitNotification.getBody())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notification.getNotificationPriority() != null) {
            builder.setPriority(notification.getNotificationPriority());
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        if (appAmbitNotification.getColor() != null) {
            try {
                builder.setColor(Color.parseColor(appAmbitNotification.getColor()));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid color format: " + appAmbitNotification.getColor() + ". Using default.");
            }
        }

        builder.setSound(getSoundUri(notification.getSound()));

        if (notification.getTicker() != null) {
            builder.setTicker(notification.getTicker());
        }

        builder.setOngoing(notification.getSticky());

        if (notification.getVisibility() != null) {
            builder.setVisibility(notification.getVisibility());
        }

        Uri imageUrl = remoteMessage.getNotification().getImageUrl();
        if (imageUrl != null) {
            Bitmap bitmap = getBitmapFromUrl(imageUrl.toString());
            if (bitmap != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon((Bitmap) null));
            }
        }

        PushKernel.NotificationCustomizer customizer = PushKernel.getNotificationCustomizer();
        if (customizer != null) {
            customizer.customize(this, builder, appAmbitNotification);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notification.getTag(), (int) System.currentTimeMillis(), builder.build());
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
        }
    }

    private Uri getSoundUri(String sound) {
        if (sound == null || sound.isEmpty() || "default".equalsIgnoreCase(sound)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        int soundResourceId = getResources().getIdentifier(sound, "raw", getPackageName());
        if (soundResourceId != 0) {
            return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + soundResourceId);
        }

        Log.w(TAG, "Sound resource '" + sound + "' not found. Using default sound.");
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    private int getImportanceFromPriority(int priority) {
        switch (priority) {
            case NotificationCompat.PRIORITY_MAX:
                return NotificationManager.IMPORTANCE_MAX;
            case NotificationCompat.PRIORITY_HIGH:
                return NotificationManager.IMPORTANCE_HIGH;
            case NotificationCompat.PRIORITY_LOW:
                return NotificationManager.IMPORTANCE_LOW;
            case NotificationCompat.PRIORITY_MIN:
                return NotificationManager.IMPORTANCE_MIN;
            case NotificationCompat.PRIORITY_DEFAULT:
            default:
                return NotificationManager.IMPORTANCE_DEFAULT;
        }
    }

    private int getSmallIcon(AppAmbitNotification notification) {
        String customIconName = notification.getSmallIconName();
        if (customIconName != null && !customIconName.isEmpty()) {
            try {
                String iconNameWithoutExtension = customIconName.split("\\.")[0];
                int iconId = getResources().getIdentifier(iconNameWithoutExtension, "drawable", getPackageName());
                if (iconId != 0) return iconId;
                Log.w(TAG, "Custom icon '" + customIconName + "' not found in drawables.");
            } catch (Exception e) {
                Log.e(TAG, "Error getting custom icon", e);
            }
        }
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            if (appInfo.icon != 0) return appInfo.icon;
            Log.w(TAG, "App icon not found, using default.");
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
        PushKernel.handleNewToken(token);
    }
}
