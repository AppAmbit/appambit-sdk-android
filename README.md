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
    implementation("com.appambit:appambit:0.1.0")
}
```

**Groovy**

```gradle
dependencies {
    implementation 'com.appambit:appambit:0.1.0'
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

