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
* **Enable/Disable Notifications**: Easily manage user preferences at both the business and FCM level.
* **Automatic Field Handling**: Automatically uses standard fields from the FCM payload like `color`, `icon`, `channel_id`, and `click_action`.
* **Smart Icon Selection**: Automatically uses your app's icon, with a safe fallback.
* **Advanced Customization**: Provides a powerful hook to modify notifications for advanced use cases.
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
    implementation("com.appambit:appambit:0.2.2")
    implementation("com.appambit:appambit-push-notifications:0.2.2")

    // The Firebase BOM is required to align Firebase library versions.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
}
```

**Groovy**

```gradle
dependencies {
    implementation 'com.appambit:appambit:0.2.2'
    implementation 'com.appambit:appambit-push-notifications:0.2.2'

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

**That's it!** Your app is now ready to receive and display push notifications.

---

## Usage

### Enabling and Disabling Notifications

By default, notifications are enabled when you first call `start()`. To manage user preferences afterward, use `setNotificationsEnabled`.

```kotlin
// To disable all future notifications
AppAmbitPushNotifications.setNotificationsEnabled(context, false)

// To re-enable them
AppAmbitPushNotifications.setNotificationsEnabled(context, true)
```

This method updates the opt-out status on the AppAmbit dashboard and stops the device from receiving FCM messages. You can check the current setting at any time:

```kotlin
val isEnabled = AppAmbitPushNotifications.isNotificationsEnabled(context)
```

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

The SDK is designed to be highly customizable, automatically adapting to the data you send in your FCM payload, while also offering a powerful hook for advanced modifications.

### Automatic Customization

The SDK automatically configures the notification by reading standard fields from your FCM message. **For most use cases, you won't need to write any custom code.**

**`notification` object:**

The SDK uses the standard keys from the FCM `notification` object.

- **`title`**: The notification's title.
- **`body`**: The notification's main text.
- **`icon`**: The name of a drawable resource for the small icon.
- **`color`**: The notification's accent color (e.g., `#FF5722`).
- **`click_action`**: An intent filter name to be triggered when the notification is tapped.
- **`channel_id`**: The ID of the notification channel to use.
- **`image`**: A URL to an image to be displayed in the notification.
- **`notification_priority`**: The integer priority of the notification (e.g., `1` for `PRIORITY_HIGH`).

**`data` object:**

The `data` object is a free-form container for any custom key-value pairs you wish to send (e.g., `{"your_key": "your_value", "another_key": 123}`). Its sole purpose is to pass custom data to your application, which you can then access using the `NotificationCustomizer` to implement any advanced logic you require.

### Advanced Customization with `NotificationCustomizer`

For scenarios that require custom logic or advanced UI modifications, you can register a `NotificationCustomizer`. This is a powerful hook that gives you **complete freedom** to modify the notification before it's displayed. You receive the `NotificationCompat.Builder` and an `AppAmbitNotification` object, which contains the entire `data` payload from your FCM message.


The `data` payload is a **free-form key-value map**. You are not limited to any specific keys; you can send any data you need and use it to build your custom notification.

**Example: Building a Custom Notification**

The following example shows how to read custom fields from the `data` payload to add a custom action button. This is just one of many possibilities.

1.  **Send any custom data** you need. The keys and values are completely up to you. For example:

    ```json
    {
      "notification": {
        "title": "New Message",
        "body": "You have a new message from a friend."
      },
      "data": {
        "key1": "Mark as Read",
        "key2": "MARK_AS_READ_ACTION",
        "any_other_key": "any_value"
      }
    }
    ```

2.  **Register the `NotificationCustomizer`** and use your custom keys:

    ```kotlin
    AppAmbitPushNotifications.setNotificationCustomizer { context, builder, notification ->
        val data = notification.data

        // You have full access to the builder and the data map.
        // You can add action buttons, apply a custom style, or change any aspect of the notification.

        // Let's use the custom keys from our example payload to add an action button.
        // Remember to replace these with your actual keys.
        val actionTitle = data["key1"]
        val actionIntentFilter = data["key2"]

        if (!actionTitle.isNullOrEmpty() && !actionIntentFilter.isNullOrEmpty()) {
            val intent = Intent(actionIntentFilter).apply {
                putExtra("EXTRA_DATA", data["any_other_key"])
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Add the action to the notification
            builder.addAction(0, actionTitle, pendingIntent)
        }
    }
    ```
