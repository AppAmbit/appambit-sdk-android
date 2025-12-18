plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

}

android {
    namespace = "com.appambitsdk.test"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(project(":appambit-sdk"))

    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("org.json:json:20250517")
}