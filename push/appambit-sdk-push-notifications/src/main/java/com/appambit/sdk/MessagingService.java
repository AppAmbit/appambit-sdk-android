package com.appambit.sdk;

import android.app.ActivityManager;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.appambit.sdk.models.AppAmbitNotification;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "AppAmbitPushSDK";
    public static final String META_DATA_EXTENSION_KEY = "com.appambit.sdk.NotificationServiceExtension";

    static final String ACTION_NOTIFICATION_OPENED    = "com.appambit.sdk.NOTIFICATION_OPENED";
    static final String EXTRA_NOTIFICATION_TITLE      = "appambit_title";
    static final String EXTRA_NOTIFICATION_BODY       = "appambit_body";
    static final String EXTRA_NOTIFICATION_COLOR      = "appambit_color";
    static final String EXTRA_NOTIFICATION_ICON       = "appambit_icon";
    static final String EXTRA_NOTIFICATION_DATA       = "appambit_data_keys";

    private static final String DEFAULT_CHANNEL_ID    = "default_channel_id";
    private static final String DEFAULT_CHANNEL_NAME  = "Default Channel";

    @Nullable
    private static volatile IAppAmbitNotificationServiceExtension extensionInstance;

    @Nullable
    private static IAppAmbitNotificationServiceExtension getExtension(@NonNull Context context) {
        if (extensionInstance != null) return extensionInstance;
        synchronized (MessagingService.class) {
            if (extensionInstance != null) return extensionInstance;
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        context.getPackageName(), PackageManager.GET_META_DATA);
                if (appInfo.metaData == null) return null;
                String className = appInfo.metaData.getString(META_DATA_EXTENSION_KEY);
                if (className == null || className.isEmpty()) return null;
                Class<?> cls = Class.forName(className);
                extensionInstance = (IAppAmbitNotificationServiceExtension) cls.getDeclaredConstructor().newInstance();
                Log.d(TAG, "NotificationServiceExtension loaded: " + className);
            } catch (Exception e) {
                Log.e(TAG, "Failed to instantiate NotificationServiceExtension from meta-data.", e);
            }
        }
        return extensionInstance;
    }

    private static boolean isAppInForeground() {
        ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(info);
        return info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

    static void dispatchOpened(@NonNull AppAmbitNotification notification) {
        PushKernel.OpenedNotificationListener staticListener = PushKernel.getOpenedNotificationListener();
        if (staticListener != null) {
            staticListener.onOpenedNotification(notification);
        }
    }

    @Override
    public void handleIntent(Intent intent) {
        if (!PushKernel.isNotificationsEnabled(this)) {
            Log.d(TAG, "Notification received but push is disabled locally. Skipping.");
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            String title = firstNonNull(
                    extras.getString("gcm.notification.title"),
                    extras.getString("title"));
            String body = firstNonNull(
                    extras.getString("gcm.notification.body"),
                    extras.getString("body"));
            String color = extras.getString("gcm.notification.color");
            String icon  = extras.getString("gcm.notification.icon");
            String clickAction = firstNonNull(
                    extras.getString("gcm.notification.click_action"),
                    extras.getString("click_action"));

            Map<String, String> data = new HashMap<>();
            boolean isNotificationMessage = false;

            for (String key : extras.keySet()) {
                if (key.startsWith("gcm.n.") || key.startsWith("gcm.notification.")) {
                    isNotificationMessage = true;
                }
                if (!key.startsWith("google.")
                        && !key.startsWith("gcm.")
                        && !key.startsWith("android.")
                        && !key.equals("from")
                        && !key.equals("collapse_key")
                        && !key.equals("notification_foreground")) {
                    Object val = extras.get(key);
                    if (val != null) data.put(key, val.toString());
                }
            }

            if (!isAppInForeground(this)) {
                AppAmbitNotification notification = new AppAmbitNotification(title, body, color, icon, data);
                IAppAmbitNotificationServiceExtension ext = getExtension(this);
                if (ext != null) {
                    ext.onNotificationBackground(notification);
                }
                PushKernel.BackgroundNotificationListener bgListener = PushKernel.getBackgroundNotificationListener();
                if (bgListener != null) {
                    bgListener.onBackgroundNotificationReceived(notification);
                }

                if (title != null || body != null) {
                    buildAndPostNotification(notification, DEFAULT_CHANNEL_ID,
                            NotificationCompat.PRIORITY_DEFAULT,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            null, null, clickAction);

                    if (isNotificationMessage) {
                        return;
                    }
                }
            }
        }

        super.handleIntent(intent);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        RemoteMessage.Notification fcmNotification = message.getNotification();
        Map<String, String> data = message.getData();

        AppAmbitNotification notification;
        if (fcmNotification != null) {
            String clickAction = fcmNotification.getClickAction();

            notification = new AppAmbitNotification(
                    fcmNotification.getTitle(),
                    fcmNotification.getBody(),
                    fcmNotification.getColor(),
                    fcmNotification.getIcon(),
                    data);

            String channelId = TextUtils.isEmpty(fcmNotification.getChannelId())
                    ? DEFAULT_CHANNEL_ID
                    : fcmNotification.getChannelId();

            int priority = fcmNotification.getNotificationPriority() != null
                    ? fcmNotification.getNotificationPriority()
                    : NotificationCompat.PRIORITY_DEFAULT;

            buildAndPostNotification(notification, channelId, priority,
                    getSoundUri(fcmNotification.getSound()),
                    fcmNotification.getTag(),
                    fcmNotification.getImageUrl(),
                    clickAction);
        } else {
            String clickAction = data.get("click_action");
            notification = new AppAmbitNotification(
                    data.get("title"),
                    data.get("body"),
                    data.get("color"),
                    data.get("icon"),
                    data);

            buildAndPostNotification(notification, DEFAULT_CHANNEL_ID,
                    NotificationCompat.PRIORITY_DEFAULT,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null, null, clickAction);
        }

        IAppAmbitNotificationServiceExtension ext = getExtension(this);
        if (ext != null) {
            ext.onNotificationForeground(notification);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PushKernel.handleNewToken(token);
    }

    private void buildAndPostNotification(
            @NonNull AppAmbitNotification notification,
            @NonNull String channelId,
            int priority,
            @NonNull Uri soundUri,
            @Nullable String tag,
            @Nullable Uri imageUrl,
            @Nullable String clickAction) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = getImportanceFromPriority(priority);
            NotificationChannel channel = new NotificationChannel(channelId, DEFAULT_CHANNEL_NAME, importance);
            channel.setSound(soundUri, null);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(getSmallIcon(notification))
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setContentIntent(buildOpenedPendingIntent(notification, clickAction))
                .setAutoCancel(true)
                .setPriority(priority)
                .setSound(soundUri);

        if (notification.getColor() != null) {
            try {
                builder.setColor(Color.parseColor(notification.getColor()));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid color value: " + notification.getColor());
            }
        }

        if (imageUrl != null) {
            Bitmap bitmap = getBitmapFromUrl(imageUrl.toString());
            if (bitmap != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon((Bitmap) null));
            }
        }

        PushKernel.NotificationCustomizer customizer = PushKernel.getNotificationCustomizer();
        if (customizer != null) {
            customizer.customize(this, builder, notification);
        }

        notificationManager.notify(tag, (int) System.currentTimeMillis(), builder.build());
    }

    private PendingIntent buildOpenedPendingIntent(@NonNull AppAmbitNotification notification, @Nullable String clickAction) {
        Intent launchIntent;
        if (!TextUtils.isEmpty(clickAction)) {
            launchIntent = new Intent(clickAction);
        } else {
            launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent == null) launchIntent = new Intent();
            launchIntent.setAction(ACTION_NOTIFICATION_OPENED);
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        launchIntent.putExtra(EXTRA_NOTIFICATION_TITLE, notification.getTitle());
        launchIntent.putExtra(EXTRA_NOTIFICATION_BODY,  notification.getBody());
        launchIntent.putExtra(EXTRA_NOTIFICATION_COLOR, notification.getColor());
        launchIntent.putExtra(EXTRA_NOTIFICATION_ICON,  notification.getSmallIconName());

        if (!notification.getData().isEmpty()) {
            String[] keys   = notification.getData().keySet().toArray(new String[0]);
            String[] values = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                values[i] = notification.getData().get(keys[i]);
            }
            launchIntent.putExtra(EXTRA_NOTIFICATION_DATA, keys);
            launchIntent.putExtra(EXTRA_NOTIFICATION_DATA + "_values", values);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(launchIntent)
                .getPendingIntent(0, flags);
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private Uri getSoundUri(@Nullable String sound) {
        if (sound == null || sound.isEmpty() || "default".equalsIgnoreCase(sound)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        int resourceId = getResources().getIdentifier(sound, "raw", getPackageName());
        if (resourceId != 0) {
            return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + resourceId);
        }
        Log.w(TAG, "Sound resource '" + sound + "' not found. Using default.");
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
                String name = customIconName.split("\\.")[0];
                int iconId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (iconId != 0) return iconId;
                Log.w(TAG, "Custom icon '" + customIconName + "' not found in drawables.");
            } catch (Exception e) {
                Log.e(TAG, "Error resolving custom icon", e);
            }
        }
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            if (appInfo.icon != 0) return appInfo.icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not resolve application icon", e);
        }
        return android.R.drawable.sym_def_app_icon;
    }

    private Bitmap getBitmapFromUrl(@NonNull String src) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(src).openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e(TAG, "Failed to download image from: " + src, e);
            return null;
        }
    }


}
