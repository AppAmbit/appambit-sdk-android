plugins {
    alias(libs.plugins.android.library)
}

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
}

dependencies {
    implementation(project(":appambit-sdk"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(platform(libs.firebaseBom))
    api(libs.firebaseMessaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}