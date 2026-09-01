plugins {
    alias(libs.plugins.android.library)
}

apply ("../gradle/publish-package.gradle")

android {
    namespace = "com.appambit.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = (project.property("MIN_SDK_VERSION") as String).toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Exposes the `release` software component so maven-publish can derive the POM
    // (and Gradle Module Metadata) from the real dependency graph. Without this the
    // publication has to attach the AAR as a raw file and ships no dependencies.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
