# AppAmbit Android SDK

**Track. Debug. Distribute.**
**AppAmbit: track, debug, and distribute your apps from one dashboard.**

Lightweight SDK for analytics, events, logging, crashes, and offline support. Simple setup, minimal overhead.

> Full product docs live here: **[docs.appambit.com](https://docs.appambit.com)**

---

## Contents

* [Features](#features)
* [Requirements](#requirements)
* [Install](#install)
* [Quickstart](#quickstart)
* [Usage](#usage)
* [Release Distribution](#release-distribution)
* [Privacy and Data](#privacy-and-data)
* [Troubleshooting](#troubleshooting)
* [Contributing](#contributing)
* [Versioning](#versioning)
* [Security](#security)
* [License](#license)

---

## Features

* Session analytics with automatic lifecycle tracking
* Ambit Trail records detailed navigation for debugging
* Event tracking with custom properties
* Error logging for quick diagnostics 
* Crash capture with stack traces and threads
* Offline support with batching, retry, and queue
* Cloud SQLite database access with raw SQL, batch/transaction support, and a fluent query builder
* Create mutliple app profiles for staging and production
* Small footprint, Kotlin-first API (Java supported)

---

## Requirements

* Android API level 21 (Lollipop) or newer
* Android Studio Giraffe or newer
* Gradle 8+
* Kotlin 1.8+ or Java 8+

---


## Install

Add the AppAmbit Android SDK to your app’s `build.gradle`.

**Kotlin DSL**

```kotlin
dependencies {
    implementation("com.appambit:appambit:1.2.0")
}
```

**Groovy**

```gradle
dependencies {
    implementation 'com.appambit:appambit:1.2.0'
}
```

---

## Quickstart

Initialize the SDK with your **API key**.

### Kotlin

```kotlin
import com.appambit.sdk.AppAmbit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppAmbit.start(this, "<YOUR-APIKEY>")
    }
}
```

### Java

```java
import com.appambit.sdk.AppAmbit;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppAmbit.start(getApplicationContext(), "<YOUR-APIKEY>");
    }
}
```

---

## Android App Requirements

Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Usage

* **Session activity** – automatically tracks user session starts, stops, and durations
* **Ambit Trail** – records detailed navigation of user and system actions leading up to an issue for easier debugging
* **Track events** – send structured events with custom properties
* **Remote Config** – dynamic configuration values fetched and applied at runtime

### Kotlin

```kotlin
val properties: Map<String, String> = mapOf(
    "Count" to "41",
)
Analytics.trackEvent("ButtonClicked", properties)
```
### Java

```java
Map<String, String> properties = new HashMap<>();
properties.put("Count", "41");
Analytics.trackEvent("ButtonClicked", properties);
```
* **Logs**: add structured log messages for debugging
### Kotlin

```kotlin
try {
  ...
} catch (exception : Exception) {
    val properties: Map<String, String> = mapOf(
        "user_id" to "1"
    )
    Crashes.logError(exception, properties);
}
```
### Java

```java
try {
    ...
} catch (Exception exception) {
    Map<String, String> properties = new HashMap<>();
    properties.put("user_id", "1");
    Crashes.logError(exception, properties);
}
```
* **Crash Reporting**: uncaught crashes are automatically captured and uploaded on next launch

### Kotlin

```kotlin
// Enable remote config
RemoteConfig.enable()
```

```kotlin
// Get remote config values with type-safe methods
val message = RemoteConfig.getString("data")
val isFeatureEnabled = RemoteConfig.getBoolean("banner")
val discount = RemoteConfig.getInt("discount")
val maxUpload = RemoteConfig.getDouble("max_upload")
```

### Java

```java
// Enable remote config
RemoteConfig.enable();
```

```java
// Get remote config values with type-safe methods
String message = RemoteConfig.getString("data");
boolean isFeatureEnabled = RemoteConfig.getBoolean("banner");
int discount = RemoteConfig.getInt("discount");
double maxUpload = RemoteConfig.getDouble("max_upload");
```

* **Remote Config**: fetch and apply remote configuration values asynchronously using type-safe methods (`getString`, `getBoolean`, `getInt`, `getDouble`).

```java
AppAmbitDb.from("users", User.class)
    .where("age", ">", 18)
    .get()
    .then(users -> { /* List<User> */ });
```

* **Database**: query and update your AppAmbit cloud database.

* **Cloud Code**: call an HTTP-triggered AppAmbit function. Cloud Code calls are request/response operations and are not queued for offline upload.

### Kotlin

```kotlin
import com.appambit.sdk.enums.CloudCodeHttpMethod

val request = CloudCode.call(
    "hello",
    CloudCodeHttpMethod.POST,
    mapOf("source" to "android"),
    mapOf("name" to "Ada"),
    null
)
request.then { response ->
    println("HTTP ${response.statusCode}: ${response.data}")
}.onError { error ->
    println(error.message)
}
```

### Java

```java
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

For the dynamic response API, a successful empty body, a `204 No Content` response, and an explicit JSON `null` are represented as `null` in `CloudCodeResponse.data`. iOS exposes the equivalent value as `NSNull()`. Typed responses preserve their status and request metadata; an empty successful body produces `null` typed data.

The typed overload accepts a model class with a public no-argument constructor.

```java
import com.appambit.sdk.enums.CloudCodeHttpMethod;
import com.appambit.sdk.utils.JsonKey;

public class Greeting {
    @JsonKey("greeting")
    public String greeting = "";
}

CloudCode.call("hello", CloudCodeHttpMethod.GET, null, null, null, Greeting.class)
    .then(result -> Log.d("CloudCode", result.getData().greeting));
```

Kotlin models can map a different JSON field name with `@JsonKey`:

```kotlin
import com.appambit.sdk.enums.CloudCodeHttpMethod
import com.appambit.sdk.utils.JsonKey

class Greeting {
    @field:JsonKey("display_name")
    var displayName: String = ""
}

CloudCode.call("profile", CloudCodeHttpMethod.GET, null, null, null, Greeting::class.java)
    .then { result -> println(result.data?.displayName) }
```

Typed models use the SDK's reflection mapper. Field names map directly to JSON names, and `@JsonKey` makes a different mapping explicit. When R8/minification is enabled, annotate every consumer model field with `@JsonKey` or keep the model fields and public no-argument constructor in the app's rules:

```proguard
-keep class com.example.app.cloudcode.** {
    <fields>;
    public <init>();
}
```

Without one of these options, R8 may rename or remove fields that are read through reflection, so a typed response can silently decode without matching fields. The SDK forwards the consumer token automatically, rejects reserved headers, and preserves HTTP status and `X-Request-Id`. Configure Database, CMS, secrets, and Push inside the backend function, not in the mobile app.

This R8 requirement applies to every typed model mapped by reflection:

- Cloud Code and CMS JSON models use `@JsonKey`.
- Database models use `@DbColumn`.
- Dynamic Cloud Code responses returned as maps and request bodies passed as maps are not affected.

`getBlocking()` is intended for tests or externally controlled worker threads. It throws on the Android main thread and on SDK-owned executors; use `then()` and `onError()` in application code.

---

## Release Distribution

* Push the artifact to your AppAmbit dashboard for distribution via email and direct installation.

---

## Privacy and Data

* The SDK batches and transmits data efficiently
* You control what is sent — avoid secrets or sensitive PII
* Supports compliance with Google Play policies

For details, see the docs: **[docs.appambit.com](https://docs.appambit.com)**

---

## Troubleshooting

* **No data in dashboard** → check API key, endpoint, and network access
* **Gradle dependency not resolving** → run `./gradlew clean build` and verify Maven Central availability
* **Crash not appearing** → crashes are sent on next launch

---

## Contributing

We welcome issues and pull requests.

* Fork the repo
* Create a feature branch
* Add tests where applicable
* Open a PR with a clear summary

Please follow Kotlin and Java API design guidelines and document public APIs.

---

## Versioning

Semantic Versioning (`MAJOR.MINOR.PATCH`) is used.

* Breaking changes → **major**
* New features → **minor**
* Fixes → **patch**

---

## Security

If you find a security issue, please contact us at **[hello@appambit.com](mailto:hello@appambit.com)** rather than opening a public issue.

---

## License

Open source under the terms described in the [LICENSE](./LICENSE) file.

---

## Links

* **Docs**: [docs.appambit.com](https://docs.appambit.com)
* **Dashboard**: [appambit.com](https://appambit.com)
* **Discord**: [discord.gg](https://discord.gg/nJyetYue2s)
* **Examples**: Sample Android test app included in repo.
