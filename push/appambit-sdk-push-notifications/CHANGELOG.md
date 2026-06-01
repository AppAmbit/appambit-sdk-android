___

## Version 1.0.1

### AppAmbit

* **[Fix]** Fixed consumer update issue where `getPushEnabled()` returning `null` caused incorrect push state to be sent on consumer sync.

___

## Version 1.0.0

### AppAmbit Push Notifications

* **[Breaking Change]** - The `AppAmbitPushNotifications` class has been renamed to `PushNotifications`. All related SDK methods must now be called on this new class, and the older method names have been deprecated.

* **[Feature]** Added specific lifecycle listeners (`setOpenedListener` replacing `setOpenedNotificationListener`, `setForegroundListener` replacing `setForegroundNotificationListener`, `setBackgroundListener` replacing `setBackgroundNotificationListener`) and `IAppAmbitNotificationServiceExtension` for improved and decoupled background notification handling.

* **[Feature]** Added support for push notifications in AppAmbit, allowing developers send and receive data payloads through push notifications, foreground and background handling of push notifications, ensuring that users receive timely and relevant information even when the app is not actively in use.

___

## Version 0.5.0

### AppAmbit

* **[Feature]** Added support for CMS (Content Management System) integration, allowing dynamic content updates and management within the app without requiring app updates. Using fluent API design for easy integration and configuration of CMS features.

___

## Version 0.4.1

### AppAmbit

* **[Fix]** Fixed issue with race conditions with breadcrumbs and remote config to ensure that breadcrumbs are sent only on crashes when the option `live_session_streaming` is false.

___

## Version 0.4.0

### AppAmbit

* **[Feature]** Added option to send breadcrumbs only on crashes to improve performance and resource efficiency.

___

## Version 0.3.1

### AppAmbit

* **[Refactor]** Updated RemoteConfig method `getInt` to `getLong` to be more precise with the size of values that the method can handle

___

## Version 0.3.0

### AppAmbit Push Notifications

* **[Refactor]** - Removed deprecated function `getNotificationCustomizer` from the AppAmbit Push Notifications SDK.

### AppAmbit

* **[Feature]** - Added Remote Config support to AppAmbit, allowing dynamic configuration of app behavior without requiring app updates.

___

## Version 0.2.2

### AppAmbit

* **[Fix]** - Fixed breadcrumbs behavior for network status detection when the app starts

___

## Version 0.2.1

### AppAmbit

* **[Internal]** Ambit Trail integration for hybrid platforms (no user-facing changes)

___

## Version 0.2.0

First publish.