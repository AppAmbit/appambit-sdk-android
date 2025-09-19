# AppAmbit Android SDK

**Track. Debug. Distribute.**
**AppAmbit: track, debug, and distribute—one SDK, one dashboard.**

The AppAmbit Android SDK adds lightweight analytics, event tracking, logs, crash reporting, and release distribution hooks to your Android apps. It is designed for simple setup, low overhead, and production-ready defaults.

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
* Event tracking with custom properties
* Structured logs with levels and tags
* Crash capture with stack traces
* Network-safe batching, retry, and offline queue
* Configurable endpoints for staging and production
* Small footprint, Kotlin-first API (Java supported)

---

## Requirements

* Android API level 21 (Lollipop) or newer
* Android Studio Giraffe or newer
* Gradle 8+
* Kotlin 1.8+ or Java 8+

---

## Install

### Gradle (Maven Central)

```gradle
// Project-level build.gradle
allprojects {
    repositories {
        mavenCentral()
    }
}

// App-level build.gradle
dependencies {
    implementation 'com.appambit:sdk:0.0.1'
}
```

---

## Quickstart

Initialize the SDK in your `Application` class with your **API key** and **base URL**.

### Kotlin

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppAmbit.init(
            context = this,
            apiKey = "YOUR_API_KEY"
        )
    }
}
```

### Java

```java
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppAmbit.init(
            this,
            "YOUR_API_KEY"
        );
    }
}
```

---

## Usage

### Track an Event

**Kotlin:**

```kotlin
AppAmbit.logEvent("AppStarted")
```

**Java:**

```java
AppAmbit.logEvent("AppStarted");
```

---

### Log an Error

**Kotlin:**

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    AppAmbit.logError(e)
}
```

**Java:**

```java
try {
    riskyOperation();
} catch (Exception e) {
    AppAmbit.logError(e);
}
```

---

### Crash Reporting

Uncaught crashes are automatically captured and sent on next launch.

---

## Release Distribution

* Optionally enable build update checks for tester workflows
* Safe to omit for production apps that only use telemetry

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
* **Dashboard**: AppAmbit workspace link
* **Examples**: Sample Android test app `AppAmbitTestApp` included in repo

