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
    static final String EXTRA_NOTIFICATION_IMAGE_URL  = "appambit_image_url";
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

    @Override
    public void handleIntent(Intent intent) {
        if (!PushKernel.isNotificationsEnabled(this)) {
            Log.d(TAG, "Notification received but push is disabled locally. Skipping.");
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            String title       = firstNonNull(extras.getString("gcm.notification.title"),    extras.getString("title"));
            String body        = firstNonNull(extras.getString("gcm.notification.body"),     extras.getString("body"));
            String color       = firstNonNull(extras.getString("gcm.notification.color"),    extras.getString("color"));
            String icon        = firstNonNull(extras.getString("gcm.notification.icon"),     extras.getString("icon"));
            String imageUrl    = firstNonNull(extras.getString("gcm.notification.image"),    extras.getString("image"), extras.getString("image_url"));
            String clickAction = firstNonNull(extras.getString("gcm.notification.click_action"), extras.getString("click_action"));
            String ticker      = firstNonNull(extras.getString("gcm.notification.ticker"),   extras.getString("ticker"));
            String visibility  = firstNonNull(extras.getString("gcm.notification.visibility"), extras.getString("visibility"));
            String channelId   = firstNonNull(extras.getString("gcm.notification.android_channel_id"), extras.getString("channel_id"), extras.getString("android_channel_id"));
            String priority    = firstNonNull(extras.getString("gcm.notification.notification_priority"), extras.getString("notification_priority"));
            String tag         = firstNonNull(extras.getString("gcm.notification.tag"),      extras.getString("tag"));
            String sound       = firstNonNull(extras.getString("gcm.notification.sound"),    extras.getString("sound"));
            String stickyRaw   = firstNonNull(extras.getString("gcm.notification.sticky"),   extras.getString("sticky"));
            Boolean sticky     = stickyRaw != null ? Boolean.parseBoolean(stickyRaw) : null;

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

            if (!isAppInForeground()) {
                AppAmbitNotification notification = new AppAmbitNotification(
                        title, body, color, icon, imageUrl, data,
                        ticker, sticky, visibility, channelId, priority, tag, sound, clickAction);
                IAppAmbitNotificationServiceExtension ext = getExtension(this);
                if (ext != null) {
                    ext.onNotificationBackground(this, notification);
                }
                PushKernel.BackgroundNotificationListener bgListener = PushKernel.getBackgroundNotificationListener();
                if (bgListener != null) {
                    bgListener.onBackgroundNotificationReceived(notification);
                }

                if (title != null || body != null) {
                    buildAndPostNotification(notification, imageUrl != null ? Uri.parse(imageUrl) : null);

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

        RemoteMessage.Notification fcm = message.getNotification();
        Map<String, String> data = message.getData();

        String title, body, color, icon, imageUrl, ticker, channelId, tag, sound, clickAction, visibility, priority;
        Boolean sticky;
        Uri fcmImage = null;

        if (fcm != null) {
            fcmImage    = fcm.getImageUrl();
            title       = firstNonNull(fcm.getTitle(),    data.get("title"));
            body        = firstNonNull(fcm.getBody(),     data.get("body"));
            color       = firstNonNull(fcm.getColor(),    data.get("color"));
            icon        = firstNonNull(fcm.getIcon(),     data.get("icon"));
            imageUrl    = fcmImage != null ? fcmImage.toString() : firstNonNull(data.get("image"), data.get("image_url"));
            ticker      = firstNonNull(fcm.getTicker(),   data.get("ticker"));
            sticky      = fcm.getSticky() ? Boolean.TRUE
                    : (data.containsKey("sticky") ? Boolean.parseBoolean(data.get("sticky")) : null);
            visibility  = fcm.getVisibility() != null ? String.valueOf(fcm.getVisibility()) : data.get("visibility");
            channelId   = firstNonNull(fcm.getChannelId(), data.get("channel_id"), data.get("android_channel_id"));
            priority    = fcm.getNotificationPriority() != null ? String.valueOf(fcm.getNotificationPriority())
                    : data.get("notification_priority");
            tag         = firstNonNull(fcm.getTag(),         data.get("tag"));
            sound       = firstNonNull(fcm.getSound(),       data.get("sound"));
            clickAction = firstNonNull(fcm.getClickAction(), data.get("click_action"));
        } else {
            title       = data.get("title");
            body        = data.get("body");
            color       = data.get("color");
            icon        = data.get("icon");
            imageUrl    = firstNonNull(data.get("image"), data.get("image_url"));
            ticker      = data.get("ticker");
            sticky      = data.containsKey("sticky") ? Boolean.parseBoolean(data.get("sticky")) : null;
            visibility  = data.get("visibility");
            channelId   = firstNonNull(data.get("channel_id"), data.get("android_channel_id"));
            priority    = data.get("notification_priority");
            tag         = data.get("tag");
            sound       = data.get("sound");
            clickAction = data.get("click_action");
        }

        AppAmbitNotification notification = new AppAmbitNotification(
                title, body, color, icon, imageUrl, data,
                ticker, sticky, visibility, channelId, priority, tag, sound, clickAction);

        Uri imageUri = fcmImage != null ? fcmImage : (imageUrl != null ? Uri.parse(imageUrl) : null);
        buildAndPostNotification(notification, imageUri);

        IAppAmbitNotificationServiceExtension ext = getExtension(this);
        if (ext != null) {
            ext.onNotificationForeground(this, notification);
        }

        PushKernel.ForegroundNotificationListener fgListener = PushKernel.getForegroundNotificationListener();
        if (fgListener != null) {
            fgListener.onForegroundNotificationReceived(notification);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PushKernel.handleNewToken(token);
    }

    private void buildAndPostNotification(
            @NonNull AppAmbitNotification notification,
            @Nullable Uri imageUrl) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
            return;
        }

        String channelId = !TextUtils.isEmpty(notification.getChannelId())
                ? notification.getChannelId()
                : DEFAULT_CHANNEL_ID;
        int priority = parsePriority(notification.getPriority());
        Uri soundUri = getSoundUri(notification.getSound());

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = getImportanceFromPriority(priority);
            NotificationChannel channel = new NotificationChannel(channelId, DEFAULT_CHANNEL_NAME, importance);
            channel.setSound(soundUri, null);
            notificationManager.createNotificationChannel(channel);
        }

        boolean sticky = Boolean.TRUE.equals(notification.getSticky());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(getSmallIcon(notification))
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setContentIntent(buildOpenedPendingIntent(notification))
                .setAutoCancel(!sticky)
                .setOngoing(sticky)
                .setPriority(priority)
                .setSound(soundUri);

        if (notification.getTicker() != null) {
            builder.setTicker(notification.getTicker());
        }

        Integer visibility = parseVisibility(notification.getVisibility());
        if (visibility != null) {
            builder.setVisibility(visibility);
        }

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

        notificationManager.notify(notification.getTag(), (int) System.currentTimeMillis(), builder.build());
    }

    private static int parsePriority(@Nullable String value) {
        if (value == null) return NotificationCompat.PRIORITY_DEFAULT;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) { }
        switch (value.toLowerCase()) {
            case "max":     return NotificationCompat.PRIORITY_MAX;
            case "high":    return NotificationCompat.PRIORITY_HIGH;
            case "low":     return NotificationCompat.PRIORITY_LOW;
            case "min":     return NotificationCompat.PRIORITY_MIN;
            default:        return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    @Nullable
    private static Integer parseVisibility(@Nullable String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) { }
        switch (value.toLowerCase()) {
            case "public":  return NotificationCompat.VISIBILITY_PUBLIC;
            case "private": return NotificationCompat.VISIBILITY_PRIVATE;
            case "secret":  return NotificationCompat.VISIBILITY_SECRET;
            default:        return null;
        }
    }

    private PendingIntent buildOpenedPendingIntent(@NonNull AppAmbitNotification notification) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent == null) {
            intent = new Intent();
            intent.setPackage(getPackageName());
        }

        intent.setAction(ACTION_NOTIFICATION_OPENED);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra(EXTRA_NOTIFICATION_TITLE, notification.getTitle());
        intent.putExtra(EXTRA_NOTIFICATION_BODY,  notification.getBody());
        intent.putExtra(EXTRA_NOTIFICATION_COLOR, notification.getColor());
        intent.putExtra(EXTRA_NOTIFICATION_ICON,  notification.getSmallIconName());
        intent.putExtra(EXTRA_NOTIFICATION_IMAGE_URL, notification.getImageUrl());
        if (notification.getClickAction() != null) {
            intent.putExtra("appambit_click_action", notification.getClickAction());
        }

        if (!notification.getData().isEmpty()) {
            String[] keys   = notification.getData().keySet().toArray(new String[0]);
            String[] values = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                values[i] = notification.getData().get(keys[i]);
            }
            intent.putExtra(EXTRA_NOTIFICATION_DATA, keys);
            intent.putExtra(EXTRA_NOTIFICATION_DATA + "_values", values);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, flags);
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