___

## Version 1.2.1

### AppAmbit

* **[Fix]** Fixed the published Maven metadata: the POM and Gradle Module Metadata now declare the SDK's real dependency graph, so OkHttp, AppCompat and Material resolve transitively instead of having to be declared by hand in the consuming app.
* **[Docs]** Rewrote the README around a quick start, a per-feature guide, and the push setup options.

### AppAmbit Push Notifications

* **[Fix]** Fixed the same metadata gap in `com.appambit:appambit.push.notifications`: Firebase Messaging now resolves transitively. `com.appambit:appambit` stays a dependency the app declares and versions itself.

___

## Version 1.2.0

### AppAmbit

- **[Feature]** Added Cloud Code HTTP invocation for Kotlin and Java.
- **[Feature]** Added typed and untyped JSON responses, request IDs, cancellation, reserved-header validation, and a 60-second Cloud Code timeout.
- **[Breaking]** Changed the public Cloud Code HTTP method API from `HttpMethodEnum` to `CloudCodeHttpMethod`; update Cloud Code imports before upgrading.
- **[Docs]** Added Cloud Code sample tabs and backend demonstration functions for Database, CMS, Push, event triggers, manual triggers, errors, and timeout behavior.

___

## Version 1.1.0

### AppAmbit

* **[Feature]** Added database client for executing SQL, running batched/transactional operations, and querying data with a fluent query builder.

___

## Version 1.0.2

### AppAmbit

* **[Refactor]** Removed local CMS cache: `Cms` now fetches content directly from the API on every call, eliminating the SQLite cache table, related storage methods, and migration logic.

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

### AppAmbit

* **[Feature]** Integrated push notification support.

___

## Version 0.1.0

### AppAmbit

* **[Feature]** Add Ambit Trail – records user navigation, app lifecycle events, network activity, and errors to provide deeper context for debugging and issue analysis

___


## Version 0.0.9

### AppAmbit

* **[Fix]** Fixed bug in detecting user app version
* **[Fix]** didCrashInLastSession Context
* **[Fix]** didCrashInLastSession Function

___


## Version 0.0.8

### AppAmbit

* **[Chore]** AppAmbit release description updated.

___

## Version 0.0.7

### AppAmbit Distribute

* AppAmbit release description updated.

___

## Version 0.0.6

### AppAmbit Distribute

* AppAmbit release to new production url

___

## Version 0.0.5

First publish fully automatized with CICD.

___

## Version 0.0.4

First publish automatized with CICD.

___

## Version 0.0.3

Changing to plugin nexus to deploy.

___

## Version 0.0.2

Fixing CICD pipeline.

___

## Version 0.0.1

First publish.