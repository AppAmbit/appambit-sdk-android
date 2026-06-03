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
  - [Foreground Notifications](#foreground-notifications)
  - [Background Notifications](#background-notifications)
- [Advanced: Service Extension](#advanced-service-extension)
- [Payload Priority — Critical for Background Delivery](#payload-priority--critical-for-background-delivery)
- [Customization](#customization)
  - [Automatic Customization](#automatic-customization)
  - [Advanced Customization with NotificationCustomizer](#advanced-customization-with-notificationcustomizer)
- [API Reference](#api-reference)
  - [PushNotifications](#pushnotifications)
  - [AppAmbitNotification](#appambitnotification)
  - [IAppAmbitNotificationServiceExtension](#iappambitnotificationserviceextension)
  - [Manifest Components](#manifest-components-provided-by-the-sdk)

---

## Features

- **Simple Setup**: Integrates in minutes.
- **Full Lifecycle Coverage**: Callbacks for foreground, background, and tapped (opened) notifications — including when the app was completely closed.
- **Enable/Disable Notifications**: Easily manage user preferences at both the business and FCM level.
- **Automatic Field Handling**: Automatically handles standard FCM payload fields like `color`, `icon`, `channel_id`, `image`, `sound`, `ticker`, `visibility`, and more.
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
    implementation("com.appambit:appambit:1.0.1")
    implementation("com.appambit:appambit-push-notifications:1.0.1")

    // The Firebase BOM is required to align Firebase library versions.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
}
```

**Groovy**

```gradle
dependencies {
    implementation 'com.appambit:appambit:1.0.1'
    implementation 'com.appambit:appambit-push-notifications:1.0.1'

    // The Firebase BOM is required to align Firebase library versions.
    implementation platform('com.google.firebase:firebase-bom:33.1.2')
}
```

Ensure you have the Google Services plugin configured in your project-level `build.gradle`.

---

## Quickstart

**Step 1 — Initialize both SDKs** in your `Application` class or `MainActivity`:

**Kotlin**
```kotlin
AppAmbit.start(applicationContext, "<YOUR-APPKEY>")
PushNotifications.start(applicationContext)
```

**Java**
```java
AppAmbit.start(getApplicationContext(), "<YOUR-APPKEY>");
PushNotifications.start(getApplicationContext());
```

> `PushNotifications.start()` must be called **after** `AppAmbit.start()`. Calling it before will log an error and exit early.

**Step 2 — Register your listeners** (see [Usage](#usage) below for details):

**Kotlin**
```kotlin
// Called when the user taps a notification to open the app.
PushNotifications.setOpenedListener { notification ->
    Log.d(TAG, "Opened: ${notification.title}")
}

// Called when a notification arrives while the app is in the foreground.
PushNotifications.setForegroundListener { notification ->
    Log.d(TAG, "Foreground: ${notification.title}")
}

// Called when a notification arrives while the app is in the background.
PushNotifications.setBackgroundListener { notification ->
    Log.d(TAG, "Background: ${notification.title}")
}
```

**Java**
```java
// Called when the user taps a notification to open the app.
PushNotifications.setOpenedListener(notification -> {
    Log.d(TAG, "Opened: " + notification.getTitle());
});

// Called when a notification arrives while the app is in the foreground.
PushNotifications.setForegroundListener(notification -> {
    Log.d(TAG, "Foreground: " + notification.getTitle());
});

// Called when a notification arrives while the app is in the background.
PushNotifications.setBackgroundListener(notification -> {
    Log.d(TAG, "Background: " + notification.getTitle());
});
```

**Step 3 — Request the notification permission** (required on Android 13+):

**Kotlin**
```kotlin
PushNotifications.requestNotificationPermission(this)
```

**Java**
```java
PushNotifications.requestNotificationPermission(getApplicationContext());
```

**That's it!** Your app is now ready to receive and display push notifications. See the [Usage](#usage) section for handling taps, foreground/background delivery, and advanced customization.

---

## Usage

### Enabling and Disabling Notifications

By default, notifications are enabled when you first call `start()`. To manage user preferences afterward:

**Kotlin**
```kotlin
// Disable all future notifications for this device.
PushNotifications.setNotificationsEnabled(context, false)

// Re-enable them.
PushNotifications.setNotificationsEnabled(context, true)
```

**Java**
```java
// Disable all future notifications for this device.
PushNotifications.setNotificationsEnabled(context, false);

// Re-enable them.
PushNotifications.setNotificationsEnabled(context, true);
```

Disabling notifications deletes the FCM token from the device and updates the opt-in status on the AppAmbit backend. Re-enabling fetches a fresh token and re-syncs.

Check the current state at any time:

**Kotlin**
```kotlin
val isEnabled = PushNotifications.isNotificationsEnabled(this)
```

**Java**
```java
boolean isEnabled = PushNotifications.isNotificationsEnabled(getApplicationContext());
```

---

### Permission Request

On Android 13 (API 33) and above, the `POST_NOTIFICATIONS` permission must be requested at runtime. The SDK includes a transparent helper Activity to handle this for you.

> The `POST_NOTIFICATIONS` permission is automatically declared in the SDK's merged manifest. You do **not** need to add it to your own `AndroidManifest.xml`.

**Without a callback:**

**Kotlin**
```kotlin
PushNotifications.requestNotificationPermission(this)
```

**Java**
```java
PushNotifications.requestNotificationPermission(getApplicationContext());
```

**With a callback:**

**Kotlin**
```kotlin
PushNotifications.requestNotificationPermission(this) { isGranted ->
    if (isGranted) {
        Log.d(TAG, "Permission granted — notifications will be shown.")
    } else {
        Log.w(TAG, "Permission denied — notifications will not be shown.")
    }
}
```

**Java**
```java
PushNotifications.requestNotificationPermission(getApplicationContext(), isGranted -> {
    if (isGranted) {
        Log.d(TAG, "Permission granted — notifications will be shown.");
    } else {
        Log.w(TAG, "Permission denied — notifications will not be shown.");
    }
});
```

> `requestNotificationPermission` requires a `ComponentActivity`. Pass your current Activity instance.

On Android 12 (API 32) and below, the system permission dialog is not required. The callback will be invoked immediately with `isGranted = true`.

---

### Handling Notification Taps (Opened)

This is the most common use case: knowing when a user taps a notification to open the app. It works correctly regardless of whether the app was in the **foreground**, **background**, or **completely closed (killed state)**.

#### Step 1 — Register the listener and call `handleNotificationOpened` in `onCreate`

The listener **must** be registered before calling `handleNotificationOpened`. Both calls belong in `onCreate`.

**Kotlin**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    AppAmbit.start(applicationContext, "<YOUR-APPKEY>")
    PushNotifications.start(applicationContext)

    PushNotifications.setOpenedListener { notification ->
        Log.d(TAG, "[OPENED] Title: ${notification.title}")
        Log.d(TAG, "[OPENED] Body:  ${notification.body}")
        Log.d(TAG, "[OPENED] Data:  ${notification.data}")
    }

    PushNotifications.handleNotificationOpened(this, intent)
}
```

**Java**
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    AppAmbit.start(getApplicationContext(), "<YOUR-APPKEY>");
    PushNotifications.start(getApplicationContext());

    PushNotifications.setOpenedListener(notification -> {
        Log.d(TAG, "[OPENED] Title: " + notification.getTitle());
        Log.d(TAG, "[OPENED] Body:  " + notification.getBody());
        Log.d(TAG, "[OPENED] Data:  " + notification.getData());
    });

    PushNotifications.handleNotificationOpened(this, getIntent());
}
```

#### Step 2 — Override `onNewIntent` to handle the background-to-foreground case

When the app is already running in the background and the user taps a notification, Android calls `onNewIntent` instead of recreating the Activity. You must forward the new intent to the SDK.

**Kotlin**
```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    // Keep getIntent() consistent with the latest intent.
    setIntent(intent)
    // Dispatch the opened callback for the background → foreground case.
    PushNotifications.handleNotificationOpened(this, intent)
}
```

**Java**
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

`handleNotificationOpened` inspects the provided `Intent` for the SDK's internal action (`com.appambit.sdk.NOTIFICATION_OPENED`). If it matches, it deserializes the notification data and dispatches the `OpenedNotificationListener`. If the intent is a regular app launch intent, the call is a no-op.

**Pending notification (killed state):** If the app was killed and the user taps a notification, the SDK caches the notification internally. As soon as you register the listener via `setOpenedListener`, the pending notification is dispatched immediately — even if `handleNotificationOpened` has not been called yet.

---

### Foreground Notifications

Register a listener to be notified when an FCM message arrives while the app is **actively in the foreground**.

**Kotlin**
```kotlin
PushNotifications.setForegroundListener { notification ->
    Log.d(TAG, "[FOREGROUND] ${notification.title}")
    // You may choose to display a custom in-app banner here.
}
```

**Java**
```java
PushNotifications.setForegroundListener(notification -> {
    Log.d(TAG, "[FOREGROUND] " + notification.getTitle());
    // You may choose to display a custom in-app banner here.
});
```

> The SDK still builds and posts the system notification even in the foreground. Use this callback if you want to additionally show a custom in-app UI.

---

### Background Notifications

Register a listener to be notified when an FCM message arrives while the app is in the **background or closed**.

**Kotlin**
```kotlin
PushNotifications.setBackgroundListener { notification ->
    Log.d(TAG, "[BACKGROUND] ${notification.title}")
}
```

**Java**
```java
PushNotifications.setBackgroundListener(notification -> {
    Log.d(TAG, "[BACKGROUND] " + notification.getTitle());
});
```

> **Important:** This listener is called synchronously during `FirebaseMessagingService.handleIntent()`. Do not perform long-running work here. For heavy processing, use a `WorkManager` task initiated from this callback.

> **Reliable background delivery requires `PRIORITY_HIGH` in the FCM payload.** See [Payload Priority](#payload-priority--critical-for-background-delivery).

---

## Advanced: Service Extension

`IAppAmbitNotificationServiceExtension` is a **manifest-registered** interface that provides state-aware callbacks without any changes to your Activity. It is particularly useful for library or framework integrations where you cannot modify Activity code.

### 1. Implement the interface

**Kotlin**
```kotlin
class MyNotificationExtension : IAppAmbitNotificationServiceExtension {

    override fun onNotificationForeground(notification: AppAmbitNotification) {
        Log.d("Extension", "[FG] ${notification.title}")
    }

    override fun onNotificationBackground(notification: AppAmbitNotification) {
        Log.d("Extension", "[BG] ${notification.title}")
    }
}
```

**Java**
```java
public class MyNotificationExtension implements IAppAmbitNotificationServiceExtension {

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

The SDK instantiates your class automatically via reflection using its no-arg constructor. No Activity code changes are required.

**Invocation order:** The extension is called **before** any static listeners registered via `PushNotifications.setForegroundListener()` / `setBackgroundListener()`.

---

## Payload Priority — Critical for Background Delivery

> **This is required for correct push delivery in background and killed states on Android.**

Android aggressively throttles or drops low-priority FCM messages when the app is not in the foreground. To guarantee delivery to devices in background or killed state, you **must** send all push notifications with **high priority** at the FCM transport level.

---

## Customization

### Automatic Customization

The SDK reads standard FCM payload fields automatically. For most use cases, no custom code is needed.

**`notification` object fields:**

| Field | Description |
|---|---|
| `title` | The notification title. |
| `body` | The notification body text. |
| `icon` | Name of a drawable resource for the small icon (e.g., `ic_notification`). |
| `color` | Accent color in hex format (e.g., `#FF5722`). |
| `channel_id` | The ID of the notification channel to use. Falls back to `default_channel_id`. |
| `android_channel_id` | Alternative key for the channel ID (also accepted). |
| `image` | A URL for a large image shown in the expanded notification. |
| `image_url` | Alternative key for the image URL (also accepted). |
| `notification_priority` | Display priority: integer (`2` = HIGH, `1` = DEFAULT, `0` = LOW, `-1` = MIN, `2` = MAX) **or** string (`"high"`, `"max"`, `"low"`, `"min"`, `"default"`). |
| `sound` | Name of a raw resource for custom notification sound (e.g., `alert`), or `"default"`. |
| `ticker` | Accessibility text shown in the status bar when the notification arrives. |
| `visibility` | Lock-screen visibility: integer or string (`"public"`, `"private"`, `"secret"`). |
| `tag` | Notification tag. Notifications with the same tag replace each other. |
| `sticky` | Boolean (`"true"` / `"false"`). If `true`, the notification is not dismissed when tapped. |
| `click_action` | An explicit Intent action to launch when the notification is tapped. |

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

---

### Advanced Customization with `NotificationCustomizer`

For full control over the notification appearance, register a `NotificationCustomizer`. You receive the `NotificationCompat.Builder` and the `AppAmbitNotification` object, and can modify anything before the notification is posted.

**Kotlin**
```kotlin
PushNotifications.setNotificationCustomizer { context, builder, notification ->
    val data = notification.data

    val actionLabel  = data["action_label"]
    val actionFilter = data["action_filter"]

    if (actionLabel != null && actionFilter != null) {
        val actionIntent = Intent(actionFilter)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, actionLabel, pendingIntent)
    }
}
```

**Java**
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

The customizer is called **after** all built-in fields have been applied, so you can safely override or extend any property of the builder.

---

## API Reference

### `PushNotifications`

| Method | Description |
|---|---|
| `start(context)` | Initializes the Push SDK and binds it to the AppAmbit Core. Must be called after `AppAmbit.start()`. |
| `setNotificationsEnabled(context, enabled)` | Enables or disables push notifications. When disabled, deletes the FCM token. When re-enabled, fetches a new token and syncs with the AppAmbit backend. |
| `isNotificationsEnabled(context)` | Returns the current notification opt-in state (`true` by default). |
| `requestNotificationPermission(activity)` | Requests the `POST_NOTIFICATIONS` runtime permission (Android 13+). |
| `requestNotificationPermission(activity, listener)` | Same as above, with a callback for the permission result. |
| `setOpenedListener(listener)` | Registers a listener for when the user **taps** a notification to open the app. Pending notifications are delivered immediately upon registration. |
| `setForegroundListener(listener)` | Registers a listener for notifications received while the app is in the **foreground**. |
| `setBackgroundListener(listener)` | Registers a listener for notifications received while the app is in the **background or closed**. |
| `handleNotificationOpened(context, intent)` | Dispatches the opened callback from an `Intent`. Call in `onCreate` and `onNewIntent`. |
| `setNotificationCustomizer(customizer)` | Registers a hook to modify the `NotificationCompat.Builder` before the notification is posted. |

---

### `AppAmbitNotification`

| Method | Returns | Description |
|---|---|---|
| `getTitle()` | `String?` | The notification title. |
| `getBody()` | `String?` | The notification body text. |
| `getColor()` | `String?` | The accent color string (e.g., `#FF5722`). |
| `getSmallIconName()` | `String?` | The drawable resource name for the small icon. |
| `getImageUrl()` | `String?` | The URL of the large image for the expanded notification. |
| `getData()` | `Map<String, String>` | The full custom `data` payload from the FCM message. |
| `getChannelId()` | `String?` | The notification channel ID. |
| `getPriority()` | `String?` | The raw display priority string from the payload. |
| `getSound()` | `String?` | The custom sound resource name or `"default"`. |
| `getTicker()` | `String?` | The accessibility ticker text. |
| `getVisibility()` | `String?` | The lock-screen visibility value. |
| `getTag()` | `String?` | The notification tag for grouping/replacing notifications. |
| `getSticky()` | `Boolean?` | Whether the notification persists after being tapped. |
| `getClickAction()` | `String?` | The explicit Intent action used when the notification is tapped. |

---

### `IAppAmbitNotificationServiceExtension`

| Method | When called |
|---|---|
| `onNotificationForeground(notification)` | App is **in the foreground** when the FCM message arrives. |
| `onNotificationBackground(notification)` | App is **in the background or closed** when the FCM message arrives. |

> Both callbacks also have overloads that provide a `Context` parameter (`onNotificationForeground(context, notification)` and `onNotificationBackground(context, notification)`). Implementing the `Context`-free versions is sufficient; the `Context`-overloads delegate to them by default.

---

### Manifest Components (Provided by the SDK)

The following components are automatically merged into your app's manifest by the SDK. You do **not** need to declare them manually.

| Component | Class | Purpose |
|---|---|---|
| `<service>` | `MessagingService` | FCM message receiver. Handles all incoming push events. |
| `<activity>` | `PermissionRequestActivity` | Transparent helper for requesting `POST_NOTIFICATIONS` at runtime. |
| `<receiver>` | `NotificationOpenedReceiver` | Handles the notification tap intent and launches the app. |
| `<uses-permission>` | `POST_NOTIFICATIONS` | Declared automatically (API 33+, `tools:targetApi="33"`). |
