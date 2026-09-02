<picture>
  <source media="(prefers-color-scheme: light)" srcset="https://assets.appambit.com/logo-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://assets.appambit.com/logo-dark.svg">
  <img alt="AppAmbit logo" src="https://assets.appambit.com/logo-dark.svg" width="280">
</picture>

# AppAmbit Android SDK

**The App Command Center.**
Everything your app needs after you build it, in one connected platform instead of stitching together separate tools.

[![Discord](https://img.shields.io/discord/1418426396836888617?label=Discord&logo=discord&color=5865F2)](https://discord.gg/nJyetYue2s)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.appambit/appambit.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.appambit/appambit)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://developer.android.com/tools/releases/platforms)

---

## Quick start

1. Sign up free at [appambit.com](https://appambit.com), no credit card required
2. Create an app in the dashboard and grab your app key
3. Install the SDK ([see below](#install))
4. Initialize it at app launch:

**Kotlin**

```kotlin
import com.appambit.sdk.AppAmbit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppAmbit.start(this, "<YOUR-APPKEY>")
    }
}
```

**Java**

```java
import com.appambit.sdk.AppAmbit;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppAmbit.start(getApplicationContext(), "<YOUR-APPKEY>");
    }
}
```

That's it. Crashes, sessions, and analytics start flowing immediately. Full setup guides live in the [docs](https://docs.appambit.com).

---

## What's inside

### 🚀 Ship
- **Build delivery**: push a build from GitHub, Bitbucket, Azure DevOps, or manually, then send it to team, testers, or clients by email or direct install, and track who installed it
- **Live updates**: ship changes without waiting on an app store review

### 📊 Monitor
- **Crash & error monitoring**: uncaught crashes are captured with full stack traces and threads, then uploaded on the next launch, grouped with who's affected and email alerts on new issues
- **Error logging**: structured log messages with custom properties for quick diagnostics, sent even when the app does not crash
- **Session timeline & breadcrumbs**: automatic activity navigation trail so you see exactly what led to a crash
- **Analytics & event tracking**: automatic session starts, stops, and durations plus structured events with custom properties, live and compared across versions

### 📈 Grow
- **Push notifications**: FCM through the optional `appambit.push.notifications` artifact, targeted by segment and scheduled from the dashboard
- **Remote config & feature flags**: typed keys (`getString`, `getBoolean`, `getInt`, `getDouble`) with version targeting, so you can flip features, run gradual rollouts, or hit the kill switch without a release
- **CMS**: define content types and entries in the dashboard, then read articles, FAQs, and promos with a fluent query builder that supports filters, full-text search, sorting, and pagination, decoded straight into your own model classes

### 🗄️ Backend
- **App database**: a managed SQL database with a fluent query builder, batches, and transactions, straight from the SDK or the dashboard
- **Cloud code**: deploy JavaScript functions triggered by HTTP, data events, or manually, then invoke them from the app with typed results, cancellation, and request correlation. Every deploy is a version, so rollback is one click
- **AI agent (MCP)**: build your backend from a conversation with Claude or Cursor ([more below](#built-for-agentic-coding))

### 👥 Teams
- Workspaces, squads, roles and access, per-app reporting

---

## Built for agentic coding

Point Claude or Cursor at the AppAmbit MCP server and it can provision your entire backend from a conversation (content types, database schema, and cloud code functions) while writing the app code that calls them. Paired with a [sample app](#sample-apps) or a [starter app](#starter-apps), that means going from a prompt to a working app with a live backend in a single sitting.

Set it up from the AppAmbit dashboard under **Settings → AI Assistant**, where you create the personal access token and get the connection details for your assistant.

---

## Requirements

* Android API level 21 (Lollipop) or newer
* Android Studio Giraffe or newer
* Gradle 8 or newer (the SDK builds against AGP 8.9 and Gradle 8.11)
* **Java 11** source and target compatibility, since both published artifacts are compiled to Java 11 bytecode
* Kotlin 1.8 or newer, if you use Kotlin

---

## Getting started

- [Install](#install)
  - [Gradle](#gradle)
  - [Manifest permissions](#manifest-permissions)
  - [Push setup](#choose-a-push-setup)
- [Track events](#track-events)
- [Logs](#logs)
- [Breadcrumbs](#breadcrumbs)
- [Remote config](#remote-config)
- [Release distribution](#release-distribution)
- [CMS](#cms)
- [Database](#database)
- [Cloud code](#cloud-code)

### Install

#### Gradle

> Requires **v1.2.0 or newer**. Earlier versions do not include Cloud Code support.

The SDK is published to Maven Central, so make sure `mavenCentral()` is in your repositories.

**Kotlin DSL**

```kotlin
dependencies {
    implementation("com.appambit:appambit:1.2.1")
}
```

**Groovy**

```gradle
dependencies {
    implementation 'com.appambit:appambit:1.2.1'
}
```

| Artifact | Add when | Import |
|---|---|---|
| `com.appambit:appambit` | Always | `com.appambit.sdk.*` |
| `com.appambit:appambit.push.notifications` | *(optional, only if you use push)* | `com.appambit.sdk.PushNotifications` |

#### Manifest permissions

Add these to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

#### Choose a push setup

Push notifications are delivered over Firebase Cloud Messaging and ship as a separate artifact:

```kotlin
dependencies {
    implementation("com.appambit:appambit:1.2.1")
    implementation("com.appambit:appambit.push.notifications:1.2.1")
}
```

The push module ships its own manifest entries, so a host app needs no manifest edits: the FCM messaging service, the Android 13 `POST_NOTIFICATIONS` permission prompt, and the notification-opened receiver are all registered for you. Start it after the core SDK:

```kotlin
AppAmbit.start(this, "<YOUR-APPKEY>")
PushNotifications.start(this)
```

You still need a `google-services.json` from your Firebase project. To customize behavior, set a listener before or after `start`: `setForegroundListener`, `setBackgroundListener`, `setOpenedListener`, or `setNotificationCustomizer`. To take over message handling entirely, implement `IAppAmbitNotificationServiceExtension` or declare a `MessagingService` subclass with `tools:node="replace"`.

See the [Push Notifications guide](push/appambit-sdk-push-notifications/README.md) for the complete setup, including Firebase configuration, permissions, and troubleshooting.

---

### Usage

Everything below works once `AppAmbit.start(context, appKey)` has run. Session activity (starts, stops, and durations) is tracked automatically, and uncaught crashes are captured and uploaded on the next launch with no extra code.

Note that `Analytics.trackEvent` and error logging are session-gated: they no-op when no session is active. `AppAmbitDb` and `CloudCode` are not.

### Track events

Send structured events with custom properties.

**Kotlin**

```kotlin
val properties = mapOf("Count" to "41")
Analytics.trackEvent("ButtonClicked", properties)
```

**Java**

```java
Map<String, String> properties = new HashMap<>();
properties.put("Count", "41");
Analytics.trackEvent("ButtonClicked", properties);
```

---

### Logs

Add structured log messages for debugging, sent even when the app does not crash.

**Kotlin**

```kotlin
try {
    // ...
} catch (exception: Exception) {
    val properties = mapOf("user_id" to "1")
    Crashes.logError(exception, properties)
}
```

**Java**

```java
try {
    // ...
} catch (Exception exception) {
    Map<String, String> properties = new HashMap<>();
    properties.put("user_id", "1");
    Crashes.logError(exception, properties);
}
```

Also available: `Crashes.logError(message)` and `Crashes.logError(message, properties)`.

---

### Breadcrumbs

Screen-change breadcrumbs are recorded automatically as you move between activities, using the activity's simple class name (`onAppear:` / `onDisappear:`). Dialog-like activities (translucent or floating) are skipped, so a dialog does not pollute the trail.

To make the dashboard timeline readable, give your activities meaningful class names. There is no per-screen title to set: the class name is what you see.

---

### Remote config

Fetch and apply remote configuration values asynchronously using type-safe methods.

**Kotlin**

```kotlin
// Enable remote config
RemoteConfig.enable()

// Get remote config values with type-safe methods
val message = RemoteConfig.getString("data")
val isFeatureEnabled = RemoteConfig.getBoolean("banner")
val discount = RemoteConfig.getInt("discount")
val maxUpload = RemoteConfig.getDouble("max_upload")
```

**Java**

```java
// Enable remote config
RemoteConfig.enable();

// Get remote config values with type-safe methods
String message = RemoteConfig.getString("data");
boolean isFeatureEnabled = RemoteConfig.getBoolean("banner");
int discount = RemoteConfig.getInt("discount");
double maxUpload = RemoteConfig.getDouble("max_upload");
```

---

### Release distribution

Ship a build to your team, testers, or clients without waiting on a store review. Connect GitHub, Bitbucket, or Azure DevOps so every pipeline run uploads its artifact. Send it out by email or a direct install link, and see who actually installed it.

This repo ships a pipeline for each one that assembles and signs the APK, ready to copy into your own app:

| CI | Pipeline |
| --- | --- |
| GitHub Actions | [.github/workflows/build-apk.yml](.github/workflows/build-apk.yml) |
| Bitbucket Pipelines | [bitbucket-pipelines.yml](bitbucket-pipelines.yml) |
| Azure DevOps | [azure-devops-pipelines-testapp.yml](azure-devops-pipelines-testapp.yml) |

---

### CMS

Read content you publish from the dashboard (articles, FAQs, promos) without shipping a new build. `Cms.content(type, modelClass)` decodes entries into your own model, and `Cms.content(type)` returns them as `JSONObject`.

**Kotlin**

```kotlin
val result = Cms.content("blog_extended", BlogPost::class.java)
    .equals("is_published", "true")
    .orderByDescending("views_count")
    .getPerPage(20)
    .getList()

result.then { posts -> println(posts) }
    .onError { error -> println(error.message) }
```

Also available: `search`, `notEquals`, `contains`, `startsWith`, `greaterThan(OrEqual)`, `lessThan(OrEqual)`, `inList`, `notInList`, `orderByAscending`, `getPage`.

**Java**

```java
Cms.content("blog_extended")
    .equals("is_published", "true")
    .getList()
    .then(items -> Log.d("Cms", String.valueOf(items)));
```

Typed models map JSON field names directly; use `@JsonKey` to map a different wire name. See the R8 note under [Cloud code](#cloud-code).

---

### Database

Query, insert, update, and delete rows in your AppAmbit database with a fluent builder.

**Kotlin**

```kotlin
// Query rows
AppAmbitDb.from("users")
    .where("status", "active")
    .orderByDesc("created_at")
    .limit(10)
    .get()
    .then { rows -> println(rows) }
    .onError { error -> println(error.message) }

// Insert a row
AppAmbitDb.from("users")
    .insert(mapOf("name" to "Jane", "status" to "active"))
    .then { result -> println(result) }

// Update requires at least one where()
AppAmbitDb.from("users")
    .where("id", 1)
    .update(mapOf("status" to "inactive"))
    .then { result -> println(result) }
```

**Java**

```java
AppAmbitDb.from("users", User.class)
    .where("age", ">", 18)
    .get()
    .then(users -> { /* List<User> */ });
```

Also available: `select`, `whereIn`, `orderBy`, `offset`, `first`, `count`, `delete`, plus raw SQL through `AppAmbitDb.execute`, `batch`, and `batchInTransaction`. Typed rows map columns with `@DbColumn`.

`AppAmbitDb` throws `IllegalStateException` if used before `AppAmbit.start()`.

---

### Cloud code

Invoke authenticated HTTP functions hosted by AppAmbit. Cloud Code uses the same consumer and Bearer token as the rest of the SDK, so no extra setup is needed beyond `AppAmbit.start()`. Configure an active Cloud Function with an enabled HTTP trigger and slug in the dashboard, then call it. Cloud Code calls are request/response operations and are not queued for offline upload.

**Kotlin**

```kotlin
import com.appambit.sdk.CloudCode
import com.appambit.sdk.enums.CloudCodeHttpMethod

CloudCode.call(
    "hello",
    CloudCodeHttpMethod.POST,
    mapOf("source" to "android"),
    mapOf("name" to "Ada"),
    null
).then { response ->
    println("HTTP ${response.statusCode}: ${response.data}")
}.onError { error ->
    println(error.message)
}
```

**Java**

```java
import com.appambit.sdk.CloudCode;
import com.appambit.sdk.enums.CloudCodeHttpMethod;

CloudCode.call(
        "hello",
        CloudCodeHttpMethod.POST,
        null,
        Collections.singletonMap("name", "Ada"),
        null)
    .then(response -> Log.d("CloudCode", String.valueOf(response.getData())))
    .onError(error -> Log.e("CloudCode", "Request failed", error));
```

With the dynamic response API, a successful empty body, a `204 No Content` response, and an explicit JSON `null` are all represented as `null` in `CloudCodeResponse.data`. iOS exposes the equivalent value as `NSNull()`. Typed responses preserve their status and request metadata, and an empty successful body produces `null` typed data.

The typed overload accepts a model class with a public no-argument constructor:

```java
public class Greeting {
    @JsonKey("greeting")
    public String greeting = "";
}

CloudCode.call("hello", CloudCodeHttpMethod.GET, null, null, null, Greeting.class)
    .then(result -> Log.d("CloudCode", result.getData().greeting));
```

**R8 and minification.** Typed models are mapped by reflection, so when minification is enabled you must annotate every model field with `@JsonKey` (Cloud Code and CMS) or `@DbColumn` (Database), or keep the model in your rules:

```proguard
-keep class com.example.app.cloudcode.** {
    <fields>;
    public <init>();
}
```

Without one of these, R8 may rename or remove fields read through reflection, and a typed response can silently decode with no matching fields. Dynamic responses returned as maps and request bodies passed as maps are not affected.

`getBlocking()` is intended for tests or externally controlled worker threads. It throws on the Android main thread and on SDK-owned executors; use `then()` and `onError()` in application code.

See the [Cloud Code mobile guide](https://docs.appambit.com/sdk-guides/cloud-code/) for function setup, HTTP triggers, typed and dynamic responses, errors, request IDs, cancellation, timeouts, and backend examples.

---

## Sample apps

This repo ships two manual-test apps that exercise every public feature, one screen per capability:

| App | Language | Path |
| --- | --- | --- |
| `kotlin-app` | Kotlin / Compose | [samples/kotlin-app](samples/kotlin-app) |
| `java-app` | Java / Views | [samples/java-app](samples/java-app) |

Replace `<YOUR-APPKEY>` with a real app key before running them, and drop in your own `google-services.json` (gitignored) if you want push. The Cloud Code screens are backed by the deployable handlers in [samples/CloudCodeExamples.js](samples/CloudCodeExamples.js).

---

## Starter apps

Skip the blank-project setup. Clone a starter with AppAmbit already wired in: auth, push notifications, analytics, and a CMS-driven feed that needs no rebuild to change content. Each one ships with ready-made content sets you can import directly into your AppAmbit dashboard, then customize to make the app your own.

| Starter | Repo |
| --- | --- |
| .NET MAUI | [organization-app-starter-maui](https://github.com/AppAmbit/organization-app-starter-maui) |
| Flutter | [organization-app-starter-flutter](https://github.com/AppAmbit/organization-app-starter-flutter) |
| React Native | [organization-app-starter-react-native](https://github.com/AppAmbit/organization-app-starter-react-native) |

---

## Other SDKs

Open-source, one per platform. Analytics, crashes, session timeline, CMS, database, and remote config all in the same package.

| Platform | Repo | Package |
| --- | --- | --- |
| **Android** | [appambit-sdk-android](https://github.com/AppAmbit/appambit-sdk-android) | [Maven Central](https://central.sonatype.com/artifact/com.appambit/appambit) |
| iOS | [appambit-sdk-ios](https://github.com/AppAmbit/appambit-sdk-ios) | [CocoaPods](https://cocoapods.org/pods/appambitsdk) · [Swift Package Manager](https://github.com/AppAmbit/appambit-sdk-ios) |
| .NET MAUI | [appambit-sdk-dotnet](https://github.com/AppAmbit/appambit-sdk-dotnet) | [NuGet](https://www.nuget.org/packages/com.AppAmbit.Maui) |
| Flutter | [appambit-sdk-flutter](https://github.com/AppAmbit/appambit-sdk-flutter) | [pub.dev](https://pub.dev/packages/appambit_sdk_flutter) |
| React Native | [appambit-sdk-react-native](https://github.com/AppAmbit/appambit-sdk-react-native) | [npm](https://www.npmjs.com/package/appambit) |
| .NET (WPF/WinUI) | [appambit-sdk-dotnet](https://github.com/AppAmbit/appambit-sdk-dotnet) | [NuGet](https://www.nuget.org/packages/com.AppAmbit.Sdk) |
| Avalonia | [appambit-sdk-dotnet](https://github.com/AppAmbit/appambit-sdk-dotnet) | [NuGet](https://www.nuget.org/packages/com.AppAmbit.Avalonia) |

---

## REST API

No SDK? No problem. Every capability (sessions, events, logs, breadcrumbs, consumers, CMS, and the database) is also reachable directly over HTTP, for web apps, backend services, or anything without a native SDK.

📖 [Getting started guide](https://docs.appambit.com/Rest/getting-started/)

---

## Troubleshooting

* **No data in dashboard** → check app key, endpoint, and network access
* **Gradle dependency not resolving** → confirm `mavenCentral()` is in your repositories, then run `./gradlew clean build`
* **Crash not appearing** → crashes are sent on next launch
* **Typed response decodes empty** → add `@JsonKey` / `@DbColumn` or a keep rule, see [Cloud code](#cloud-code)
* **Push not arriving** → confirm `google-services.json` and that `PushNotifications.start(context)` runs after `AppAmbit.start`

---

## Documentation

📚 [docs.appambit.com](https://docs.appambit.com)

---

## Community

- 💬 [Discord](https://discord.gg/nJyetYue2s)
- ✉️ [hello@appambit.com](mailto:hello@appambit.com)

---

## Pricing

Free plan with all core features, no credit card required. Paid plans start at $5.99/mo with hard spend caps, so there are no overage surprises.

🔗 [appambit.com](https://appambit.com) · [See pricing](https://appambit.com/pricing)

---

## License

Open source under the MIT License. See the [LICENSE](./LICENSE) file for the full terms.
