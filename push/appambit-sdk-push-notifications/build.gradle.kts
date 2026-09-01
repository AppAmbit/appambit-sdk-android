plugins {
    alias(libs.plugins.android.library)
}

apply ("../../gradle/publish-notifications-package.gradle")

android {
    namespace = "com.appambit.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = (project.property("MIN_SDK_VERSION") as String).toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // compileOnly on purpose: the core SDK is a REQUIREMENT of this module, not a
    // transitive gift. It stays off the published POM / module metadata so consumers
    // must declare `com.appambit:appambit` themselves and therefore control its
    // version. PushNotifications.java is the only class here that touches core types.
    compileOnly(project(":appambit-sdk"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(platform(libs.firebaseBom))
    api(libs.firebaseMessaging)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
