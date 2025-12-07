# AppAmbit Push Notifications SDK

**Seamlessly integrate push notifications with your AppAmbit analytics.**

This SDK is an extension of the core AppAmbit Android SDK, providing a simple and powerful way to handle Firebase Cloud Messaging (FCM) notifications.

---

## Contents

* [Features](#features)
* [Requirements](#requirements)
* [Install](#install)
* [Quickstart](#quickstart)
* [Usage](#usage)
* [Customization](#customization)

---

## Features

* **Simple Setup**: Integrates in minutes.
* **Automatic Click Handling**: Notifications open your app by default.
* **Smart Icon Selection**: Automatically uses your app's icon, with a safe fallback.
* **Rich Customization**: Provides hooks to easily modify notifications based on push data.
* **Permission Helper**: Includes a simple utility to request the `POST_NOTIFICATIONS` permission.

---

## Requirements

* **AppAmbit Core SDK**: This SDK is an extension and requires the core `appambit-sdk` to be installed and configured.
* **Firebase Project**: A configured Firebase project and a `google-services.json` file in your application module.
* Android API level 21 (Lollipop) or newer.

---

## Install

Add the following dependencies to your app's `build.gradle` file. Your app is still responsible for providing the Firebase Bill of Materials (BOM) to ensure version compatibility.

**Kotlin DSL**

```kotlin
dependencies {
    implementation("com.appambit:appambit:0.1.0")
    implementation("com.appambit:appambit-push-notifications:0.1.0")

    // The Firebase BOM is required to align Firebase library versions.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
}
```

**Groovy**

```gradle
dependencies {
    implementation 'com.appambit:appambit:0.1.0'
    implementation 'com.appambit:appambit-push-notifications:0.1.0'

    // The Firebase BOM is required to align Firebase library versions.
    implementation platform('com.google.firebase:firebase-bom:33.1.2')
}
```

Also, ensure you have the Google Services plugin configured in your project.

---

## Quickstart

1.  **Initialize the Core SDK**: In your `Application` class or `MainActivity`, initialize the core AppAmbit SDK with your App Key.

    ```kotlin
    AppAmbit.start(this, "<YOUR-APPKEY>")
    ```

2.  **Initialize the Push SDK**: Immediately after, start the Push Notifications SDK.

    ```kotlin
    AppAmbitPushNotifications.start(applicationContext)
    ```

3.  **Request Permissions**: In your main activity, request the required notification permission.

    ```kotlin
    AppAmbitPushNotifications.requestNotificationPermission(this)
    ```

**That's it!** Your app is now ready to receive basic push notifications.

---

## Usage

### Default Behavior

By default, the SDK handles notifications automatically:

*   **Click Action**: Tapping a notification will open your app's main launcher activity.
*   **Small Icon**: The SDK will use your app's launcher icon. If not found, it uses a safe system default.
*   **Image**: If the push payload contains an `imageUrl`, the SDK will download and display it as a `BigPictureStyle` notification.

### Permission Listener (Optional)

To know if the user granted or denied the notification permission, you can provide an optional listener.

```kotlin
AppAmbitPushNotifications.requestNotificationPermission(this) { isGranted ->
    if (isGranted) {
        Log.d(TAG, "Permission granted!")
    } else {
        Log.w(TAG, "Permission denied. We can't show notifications.")
    }
}
```

---

## Customization

The true power of the SDK lies in its simple customization. The SDK looks for specific keys in the `data` payload of your FCM message. You can override almost any aspect of the notification by sending the right data from your backend.

### Example Data Payload

Here is an example `JSON` data payload you can send from your server:

```json
{
  "data": {
    "priority": "high",
    "deep_link": "https://appambit.com/special-offer",
    "color": "#FF5722",
    "large_icon_url": "https://appambit.com/assets/icon.png",
    "small_icon_name": "ic_notification_custom"
  }
}
```

### Using the `NotificationCustomizer`

To use this data, register a `NotificationCustomizer`. The SDK provides the default `Notification.Builder` and a clean `AppAmbitNotification` object containing the parsed data.

**Example Implementation:**

```kotlin
AppAmbitPushNotifications.setNotificationCustomizer { context, builder, notification ->
    // Change priority based on data
    notification.priority?.let {
        if (it == "high") builder.priority = NotificationCompat.PRIORITY_HIGH
    }

    // Set a custom deep link action
    notification.deepLink?.let {
        if (it.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            builder.setContentIntent(pendingIntent)
        }
    }

    // Change notification color
    notification.color?.let {
        try {
            builder.color = Color.parseColor(it)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid color format in push data: $it")
        }
    }
}
```

**Note on Icons:** The SDK automatically handles the `small_icon_name` and downloads the image for `large_icon_url` for you. You do not need to write code for this in the customizer.
