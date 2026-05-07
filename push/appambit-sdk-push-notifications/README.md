# AppAmbit Push Notifications SDK

**Seamlessly integrate push notifications with your AppAmbit analytics.**

This SDK is an extension of the core AppAmbit Android SDK, providing a simple and powerful way to handle Firebase Cloud Messaging (FCM) notifications across all application lifecycle states.

---

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Install](#install)
- [Quickstart](#quickstart)
- [Usage](#usage)
  - [Enabling and Disabling Notifications](#enabling-and-disabling-notifications)
  - [Permission Request](#permission-request)
  - [Handling Notification Taps (Opened)](#handling-notification-taps-opened)
  - [Receiving Notifications in Background / App closed](#receiving-notifications-in-background--app-closed)
- [Advanced: Service Extension](#advanced-service-extension)
- [Customization](#customization)
- [API Reference](#api-reference)

---

## Features

- **Simple Setup**: Integrates in minutes.
- **Full Lifecycle Coverage**: Callbacks for foreground, background, and tapped (opened) notifications — including when the app was completely closed.
- **Enable/Disable Notifications**: Easily manage user preferences at both the business and FCM level.
- **Automatic Field Handling**: Automatically uses standard FCM payload fields like `color`, `icon`, `channel_id`, and `image`.
- **Smart Icon Selection**: Automatically uses your app's icon with a safe fallback.
- **Service Extension**: A manifest-registered interface for decoupled, state-aware notification handling without modifying your Activity.
- **Advanced Customization**: A powerful hook to modify the `NotificationCompat.Builder` before a notification is displayed.
- **Permission Helper**: Includes a simple utility to request the `POST_NOTIFICATIONS` permission.

---

## Requirements

- **AppAmbit Core SDK**: Requires the core `appambit-sdk` to be installed and initialized first.
- **Firebase Project**: A configured Firebase project and a `google-services.json` file in your application module.
- Android API level 21 (Lollipop) or newer.

---

## Install

Add the following dependencies to your app's `build.gradle` file.

**Kotlin DSL**

```kotlin
dependencies {
    implementation("com.appambit:appambit:0.5.0")
    implementation("com.appambit:appambit-push-notifications:0.5.0")

    // The Firebase BOM is required to align Firebase library versions.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
}
```

**Groovy**

```gradle
dependencies {
    implementation 'com.appambit:appambit:0.5.0'
    implementation 'com.appambit:appambit-push-notifications:0.5.0'

    // The Firebase BOM is required to align Firebase library versions.
    implementation platform('com.google.firebase:firebase-bom:33.1.2')
}
```

Ensure you have the Google Services plugin configured in your project-level `build.gradle`.

---

## Quickstart

**1. Initialize both SDKs** in your `Application` class or `MainActivity`:

```java
AppAmbit.start(getApplicationContext(), "<YOUR-APPKEY>");
PushNotifications.start(getApplicationContext());
```

**3. Request the notification permission** (required on Android 13+):

```java
PushNotifications.requestNotificationPermission(this);
```

**That's it!** Your app is now ready to receive and display push notifications. See the [Usage](#usage) section for handling taps, background delivery, and advanced customization.

---

## Usage

### Enabling and Disabling Notifications

By default, notifications are enabled when you first call `start()`. To manage user preferences afterward:

```java
// Disable all future notifications for this device.
PushNotifications.setNotificationsEnabled(context, false);

// Re-enable them.
PushNotifications.setNotificationsEnabled(context, true);
```

This updates the opt-in status on the AppAmbit dashboard and manages the FCM token lifecycle. Check the current state at any time:

```java
boolean isEnabled = PushNotifications.isNotificationsEnabled(context);
```

---

### Permission Request

On Android 13 (API 33) and above, the `POST_NOTIFICATIONS` permission must be requested at runtime.

**Without a callback:**

```java
PushNotifications.requestNotificationPermission(this);
```

**With a callback:**

```java
PushNotifications.requestNotificationPermission(this, isGranted -> {
    if (isGranted) {
        Log.d(TAG, "Permission granted — notifications will be shown.");
    } else {
        Log.w(TAG, "Permission denied — notifications will not be shown.");
    }
});
```

---

### Handling Notification Taps (Opened)

This is the most common use case: knowing when a user taps a notification to open the app. It works correctly regardless of whether the app was in the **foreground**, **background**, or **completely closed**.

#### Step 1 — Register the listener and call `handleNotificationOpened` in `onCreate`

The listener **must** be registered before calling `handleNotificationOpened`. Both calls belong in `onCreate`.

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    AppAmbit.start(getApplicationContext(), "<YOUR-APPKEY>");
    PushNotifications.start(getApplicationContext());

    PushNotifications.setOpenedNotificationListener(notification -> {
        Log.d(TAG, "[OPENED] Title: " + notification.getTitle());
        Log.d(TAG, "[OPENED] Body:  " + notification.getBody());
        Log.d(TAG, "[OPENED] Data:  " + notification.getData());
    });

    PushNotifications.handleNotificationOpened(this, getIntent());
}
```

#### Step 2 — Override `onNewIntent` to handle the background-to-foreground case

When the app is already running in the background and the user taps a notification, Android calls `onNewIntent` instead of recreating the Activity.

```java
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    // Keep getIntent() consistent with the latest intent.
    setIntent(intent);
    // Dispatch the opened callback for the background → foreground case.
    PushNotifications.handleNotificationOpened(this, intent);
}
```

#### How `handleNotificationOpened` works

`handleNotificationOpened` checks if the provided `Intent` was created by the SDK (it carries a special action). If it was, the `OpenedNotificationListener` is called with the notification data. If the intent is a regular app launch intent, the method is a no-op.

---


## Advanced: Service Extension

`INotificationServiceExtension` is a **manifest-registered** interface that provides state-aware callbacks without any changes to your Activity.

### 1. Implement the interface

```java
public class MyNotificationExtension implements INotificationServiceExtension {

    @Override
    public void onNotificationForeground(@NonNull AppAmbitNotification notification) {
        Log.d("Extension", "[FG] " + notification.getTitle());
    }

    @Override
    public void onNotificationBackground(@NonNull AppAmbitNotification notification) {
        Log.d("Extension", "[BG] " + notification.getTitle());
    }
}
```

### 2. Register it in `AndroidManifest.xml`

```xml
<application ...>
    <meta-data
        android:name="com.appambit.sdk.NotificationServiceExtension"
        android:value="com.yourapp.MyNotificationExtension" />
</application>
```

The SDK will instantiate your class automatically via reflection. No Activity code changes are required. The extension is called **before** any static listeners registered via `PushNotifications`.

---

## Customization

### Automatic Customization

The SDK reads standard FCM payload fields automatically. For most use cases, no custom code is needed.

**`notification` object fields:**

| Field | Description |
|---|---|
| `title` | The notification title. |
| `body` | The notification body text. |
| `icon` | Name of a drawable resource for the small icon. |
| `color` | Accent color in hex format (e.g., `#FF5722`). |
| `channel_id` | The ID of the notification channel to use. |
| `image` | A URL for a large image shown in the expanded notification. |
| `notification_priority` | Integer priority (e.g., `1` for `PRIORITY_HIGH`). |

**Example FCM payload:**

```json
{
    "notification": {
        "title": "New Message",
        "body": "You have a new message from a friend."
    },
    "data": {
        "key1": "Value1",
        "key2": "Value2",
        "any_other_key": "any_value"
    }
}
```

**`data` object:**

Send any custom key-value pairs alongside the notification. They are available in every callback and the `NotificationCustomizer` via `notification.getData()`.

```json
"data": {
  "user_id":  "123",
  "screen":   "chat",
  "order_id": "ORD-456"
}
```

You can also use `title`, `body`, `color`, and `icon` inside the `data` object for **data-only messages** (messages without a `notification` block). The SDK will read them and build the notification automatically.

```json
{
  "message": {
    "token": "<DEVICE_TOKEN>",
    "data": {
      "title": "Silent update",
      "body":  "Your order has shipped.",
      "color": "#4CAF50",
      "icon":  "ic_shipping",
      "order_id": "ORD-456"
    }
  }
}
```

---


### Advanced Customization with `NotificationCustomizer`

For full control over the notification appearance, register a `NotificationCustomizer`. You receive the `NotificationCompat.Builder` and the `AppAmbitNotification` object, and can modify anything before the notification is shown.

```java
PushNotifications.setNotificationCustomizer((context, builder, notification) -> {
    Map<String, String> data = notification.getData();

    String actionLabel  = data.get("action_label");
    String actionFilter = data.get("action_filter");

    if (actionLabel != null && actionFilter != null) {
        Intent actionIntent = new Intent(actionFilter);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.addAction(0, actionLabel, pendingIntent);
    }
});
```

---

## API Reference

### `PushNotifications`

| Method | Description |
|---|---|
| `start(context)` | Initializes the Push SDK and binds it to the AppAmbit Core. Must be called after `AppAmbit.start()`. |
| `setNotificationsEnabled(context, enabled)` | Enables or disables push notifications. Updates the FCM token and syncs with the AppAmbit backend. |
| `isNotificationsEnabled(context)` | Returns the current notification opt-in state. |
| `requestNotificationPermission(activity)` | Requests the `POST_NOTIFICATIONS` runtime permission (Android 13+). |
| `requestNotificationPermission(activity, listener)` | Same as above, with a callback for the permission result. |
| `setOpenedNotificationListener(listener)` | Registers a listener for when the user **taps** a notification to open the app. |
| `handleNotificationOpened(context, intent)` | Dispatches the opened callback from an `Intent`. Call in `onCreate` and `onNewIntent`. |
| `setNotificationCustomizer(customizer)` | Registers a hook to modify the `NotificationCompat.Builder` before display. |

### `AppAmbitNotification`

| Method | Returns | Description |
|---|---|---|
| `getTitle()` | `String?` | The notification title. |
| `getBody()` | `String?` | The notification body text. |
| `getColor()` | `String?` | The accent color string (e.g., `#FF5722`). |
| `getSmallIconName()` | `String?` | The drawable resource name for the small icon. |
| `getData()` | `Map<String, String>` | The full custom `data` payload from the FCM message. |

### `INotificationServiceExtension`

| Method | When called |
|---|---|
| `onNotificationForeground(notification)` | App is **in the foreground** when the FCM message arrives. |
| `onNotificationBackground(notification)` | App is **in the background or closed** when the FCM message arrives. |
